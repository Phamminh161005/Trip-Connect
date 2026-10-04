package com.tripconnect.backend.service.booking;

import com.tripconnect.backend.dto.booking.BookingResponses;
import com.tripconnect.backend.entity.Booking;
import com.tripconnect.backend.entity.Payment;
import com.tripconnect.backend.entity.TourDeparture;
import com.tripconnect.backend.enums.*;
import com.tripconnect.backend.payment.VnPayClient;
import com.tripconnect.backend.repository.BookingRepository;
import com.tripconnect.backend.repository.PaymentRepository;
import com.tripconnect.backend.repository.TourDepartureRepository;
import com.tripconnect.backend.service.EmailTemplates;
import com.tripconnect.backend.service.NotificationEvents;
import com.tripconnect.backend.service.WebNotifications;
import com.tripconnect.backend.service.tour.TourBookingStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Thanh toán VNPay:
 *  - Tạo giao dịch + link chuyển khách sang VNPay.
 *  - Nhận kết quả từ 2 đường: Return URL (trình duyệt khách quay về) và IPN (máy chủ VNPay gọi thẳng).
 *    Cả hai cùng đi qua {@link #applyResult}: kiểm chữ ký, khóa giao dịch, chỉ ghi nhận MỘT lần.
 *  - Đối soát: đơn quá hạn mà chưa nhận kết quả -> hỏi VNPay (querydr) trước khi hủy.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String SERVER_IP = "127.0.0.1";
    static final int UNREACHABLE_GIVE_UP_MINUTES = 30;

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final TourDepartureRepository departureRepository;
    private final TourBookingStats bookingStats;
    private final BookingCancellation cancellation;
    private final RefundService refundService;
    private final VnPayClient vnPayClient;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    // ===================== Tạo link thanh toán =====================

    /** Tạo một giao dịch mới cho đơn đang chờ thanh toán. Phải gọi trong transaction (đơn đã khóa). */
    @Transactional(propagation = Propagation.MANDATORY)
    public String createPaymentUrl(Booking booking, String ipAddr) {
        vnPayClient.requireConfigured();
        LocalDateTime now = LocalDateTime.now(clock);
        long attempt = paymentRepository.countByBookingId(booking.getId()) + 1;

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setTxnRef(booking.getCode() + String.format("%02d", attempt));
        payment.setAmount(booking.getTotalAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setVnpCreateDate(now.format(VnPayClient.VNP_DATE));
        paymentRepository.save(payment);

        return vnPayClient.createPaymentUrl(payment.getTxnRef(), payment.getAmount(),
                "Thanh toan don dat tour " + booking.getCode(), ipAddr, now, booking.getHoldExpiresAt());
    }

    // ===================== Nhận kết quả =====================

    /** Kết quả xử lý, cũng là nội dung trả cho IPN của VNPay (RspCode theo tài liệu VNPay). */
    public record Outcome(String rspCode, String message, Long bookingId, String bookingCode, BookingStatus bookingStatus,
                          boolean paid) {
        static Outcome of(String code, String message) {
            return new Outcome(code, message, null, null, null, false);
        }
    }

    /** Trang quay về (Return URL): khách quay lại từ VNPay. Có chữ ký hợp lệ -> ghi nhận luôn (cần khi chạy localhost). */
    public BookingResponses.PaymentResult handleReturn(Map<String, String> params) {
        Outcome outcome = handle(params);
        boolean success = outcome.paid();
        String message = switch (outcome.rspCode()) {
            case "97" -> "Chữ ký không hợp lệ — kết quả thanh toán không được ghi nhận";
            case "01" -> "Không tìm thấy giao dịch";
            default -> success ? "Thanh toán thành công" : vnPayMessage(params.get("vnp_ResponseCode"));
        };
        return new BookingResponses.PaymentResult(success, outcome.bookingId(), outcome.bookingCode(),
                outcome.bookingStatus(), message);
    }

    /** IPN: máy chủ VNPay gọi thẳng vào (cần địa chỉ public, dùng ngrok khi chạy trên máy). */
    public Outcome handleIpn(Map<String, String> params) {
        return handle(params);
    }

    private Outcome handle(Map<String, String> params) {
        if (!vnPayClient.verify(params)) {
            log.warn("Nhận kết quả VNPay với chữ ký sai (txnRef={})", params.get("vnp_TxnRef"));
            return Outcome.of("97", "Invalid Checksum");
        }
        long amount;
        try {
            amount = Long.parseLong(params.getOrDefault("vnp_Amount", "0")) / 100;
        } catch (NumberFormatException e) {
            return Outcome.of("04", "Invalid amount");
        }
        boolean success = "00".equals(params.get("vnp_ResponseCode")) && "00".equals(params.get("vnp_TransactionStatus"));
        return applyResult(params.get("vnp_TxnRef"), success, amount, params.get("vnp_ResponseCode"),
                params.get("vnp_TransactionNo"), params.get("vnp_BankCode"), params.get("vnp_PayDate"));
    }

    /**
     * Ghi nhận kết quả một giao dịch — CHỈ MỘT LẦN dù Return URL, IPN, job đối soát cùng gọi (khóa dòng payment).
     * Thanh toán thành công:
     *  - Đơn đang chờ -> Đã thanh toán.
     *  - Đơn đã bị hủy vì quá hạn: còn chỗ thì khôi phục, hết chỗ thì hoàn 100%.
     *  - Đơn đã thanh toán bằng giao dịch khác (trả 2 lần) -> hoàn giao dịch thừa.
     */
    public Outcome applyResult(String txnRef, boolean success, long amount, String responseCode, String transactionNo,
                               String bankCode, String payDate) {
        return transactionTemplate.execute(status -> {
            Optional<Long> bookingId = txnRef == null ? Optional.empty() : paymentRepository.findBookingIdByTxnRef(txnRef);
            if (bookingId.isEmpty()) return Outcome.of("01", "Order not found");
            // Thứ tự khóa thống nhất mọi nơi: ĐƠN trước, GIAO DỊCH sau -> không có 2 luồng chờ nhau (deadlock)
            Booking booking = bookingRepository.findByIdForUpdate(bookingId.get()).orElseThrow();
            Payment payment = paymentRepository.findByTxnRefForUpdate(txnRef).orElseThrow();

            if (payment.getAmount() != amount) {
                log.warn("Số tiền VNPay ({}) khác số tiền giao dịch {} ({})", amount, txnRef, payment.getAmount());
                return Outcome.of("04", "Invalid amount");
            }
            boolean alreadyHandled = payment.getStatus() == PaymentStatus.SUCCESS || payment.getStatus() == PaymentStatus.FAILED;
            if (alreadyHandled) {
                return new Outcome("02", "Order already confirmed", booking.getId(), booking.getCode(), booking.getStatus(),
                        payment.getStatus() == PaymentStatus.SUCCESS);
            }

            payment.setVnpResponseCode(responseCode);
            payment.setVnpTransactionNo(transactionNo);
            payment.setBankCode(bankCode);
            payment.setPayDate(payDate);
            if (!success) {
                payment.setStatus(PaymentStatus.FAILED);
                return new Outcome("00", "Confirm Success", booking.getId(), booking.getCode(), booking.getStatus(), false);
            }

            payment.setStatus(PaymentStatus.SUCCESS);
            switch (booking.getStatus()) {
                case PENDING_PAYMENT -> markPaid(booking);
                case CANCELLED -> recoverLatePayment(booking, payment);
                default -> refundService.refundExtraPayment(booking, payment, "Thanh toán trùng cho đơn " + booking.getCode());
            }
            return new Outcome("00", "Confirm Success", booking.getId(), booking.getCode(), booking.getStatus(),
                    booking.getStatus() == BookingStatus.PAID);
        });
    }

    private void markPaid(Booking booking) {
        booking.setStatus(BookingStatus.PAID);
        booking.setPaidAt(LocalDateTime.now(clock));
        // Các link thanh toán khác của đơn không còn dùng
        paymentRepository.findByBookingIdAndStatus(booking.getId(), PaymentStatus.PENDING)
                .forEach(p -> p.setStatus(PaymentStatus.EXPIRED));

        var tour = booking.getTour();
        eventPublisher.publishEvent(new NotificationEvents.UserEmailEvent(booking.getContactEmail(),
                EmailTemplates.bookingPaid(booking.getCode(), tour.getTitle(), booking.getDeparture().getStartDate(),
                        tour.getMeetingPoint(), tour.getMeetingTime().toString(), booking.travellers(), booking.getTotalAmount())));
        eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(booking.getCustomer().getId(),
                WebNotifications.bookingPaid(booking.getId(), booking.getCode(), tour.getTitle(),
                        booking.getDeparture().getStartDate())));
        // Agent chỉ nhận thông báo trên web (đông khách thì email quá nhiều)
        if (booking.getAgent() != null) {
            eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(booking.getAgent().getId(),
                    WebNotifications.newBooking(booking.getId(), booking.getCode(), tour.getTitle(),
                            booking.getDeparture().getStartDate(), booking.travellers(), booking.getTotalAmount())));
        }
    }

    /** Khách trả tiền sau khi đơn đã bị hủy vì quá hạn giữ chỗ (VNPay báo trễ). */
    private void recoverLatePayment(Booking booking, Payment payment) {
        if (booking.getCancelledBy() == CancelledBy.SYSTEM) {
            TourDeparture departure = departureRepository.findByIdForUpdate(booking.getDeparture().getId()).orElseThrow();
            int free = departure.getCapacity() - bookingStats.seatsBooked(departure.getId());
            boolean stillBookable = departure.getStatus() == DepartureStatus.OPEN
                    && departure.getStartDate().isAfter(LocalDate.now(clock)) && free >= booking.seats();
            if (stillBookable) {
                booking.setCancelledAt(null);
                booking.setCancelledBy(null);
                booking.setCancelReason(null);
                markPaid(booking);
                return;
            }
        }
        // Hết chỗ / đơn bị hủy vì lý do khác -> hoàn lại toàn bộ khoản vừa trả
        refundService.refundExtraPayment(booking, payment,
                "Đơn " + booking.getCode() + " đã bị hủy trước khi nhận được thanh toán");
        eventPublisher.publishEvent(new NotificationEvents.UserEmailEvent(booking.getContactEmail(),
                EmailTemplates.bookingCancelled(booking.getCode(), booking.getTour().getTitle(),
                        "Thanh toán đến sau khi hết thời gian giữ chỗ và lịch khởi hành đã hết chỗ", payment.getAmount())));
        eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(booking.getCustomer().getId(),
                WebNotifications.bookingCancelled(booking.getId(), booking.getCode(), booking.getTour().getTitle(),
                        payment.getAmount())));
    }

    // ===================== Đối soát đơn quá hạn =====================

    /**
     * Đơn chờ thanh toán đã quá hạn: hỏi VNPay từng giao dịch còn treo (phòng trường hợp mất Return URL / IPN),
     * rồi nếu vẫn chưa thanh toán thì hủy đơn và nhả chỗ.
     */
    public void settleExpired(Long bookingId) {
        List<Payment> pending = transactionTemplate.execute(status ->
                paymentRepository.findByBookingIdAndStatus(bookingId, PaymentStatus.PENDING));
        LocalDateTime holdExpiresAt = transactionTemplate.execute(status ->
                bookingRepository.findById(bookingId).map(Booking::getHoldExpiresAt).orElse(null));
        if (vnPayClient.isConfigured() && pending != null && holdExpiresAt != null) {
            for (Payment payment : pending) {
                VnPayClient.QueryResult result = vnPayClient.query(payment.getTxnRef(), payment.getVnpCreateDate(),
                        SERVER_IP, LocalDateTime.now(clock));
                if (!result.reachable()) {
                    // VNPay chưa trả lời: chờ thêm vài lượt. Quá lâu thì vẫn hủy (chỗ đã nhả từ lúc hết hạn;
                    // nếu sau đó VNPay báo đã trả tiền, recoverLatePayment sẽ khôi phục hoặc hoàn tiền)
                    if (holdExpiresAt.isAfter(LocalDateTime.now(clock).minusMinutes(UNREACHABLE_GIVE_UP_MINUTES))) return;
                    break;
                }
                if (result.paid() && result.amount() != null) {
                    applyResult(payment.getTxnRef(), true, result.amount(), result.responseCode(), result.transactionNo(),
                            result.bankCode(), result.payDate());
                }
            }
        }
        transactionTemplate.executeWithoutResult(status -> {
            Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElseThrow();
            if (booking.getStatus() == BookingStatus.PENDING_PAYMENT && booking.getHoldExpiresAt().isBefore(LocalDateTime.now(clock))) {
                cancellation.cancel(booking, CancelledBy.SYSTEM, "Quá thời gian giữ chỗ mà chưa thanh toán", 0, false);
            }
        });
    }

    /** Ý nghĩa mã lỗi VNPay thường gặp (hiện cho khách). */
    static String vnPayMessage(String code) {
        if (code == null) return "Thanh toán không thành công";
        return switch (code) {
            case "00" -> "Thanh toán thành công";
            case "24" -> "Bạn đã hủy giao dịch";
            case "11" -> "Hết thời gian chờ thanh toán";
            case "12" -> "Thẻ / tài khoản bị khóa";
            case "13", "79" -> "Nhập sai mật khẩu / OTP quá số lần cho phép";
            case "51" -> "Tài khoản không đủ số dư";
            case "65" -> "Tài khoản vượt hạn mức giao dịch trong ngày";
            case "75" -> "Ngân hàng đang bảo trì";
            default -> "Thanh toán không thành công (mã " + code + ")";
        };
    }
}
