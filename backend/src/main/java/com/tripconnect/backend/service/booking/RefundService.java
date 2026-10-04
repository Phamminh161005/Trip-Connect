package com.tripconnect.backend.service.booking;

import com.tripconnect.backend.entity.Booking;
import com.tripconnect.backend.entity.Payment;
import com.tripconnect.backend.entity.Refund;
import com.tripconnect.backend.enums.PaymentStatus;
import com.tripconnect.backend.enums.RefundRecordStatus;
import com.tripconnect.backend.enums.RefundStatus;
import com.tripconnect.backend.exception.ResourceNotFoundException;
import com.tripconnect.backend.payment.VnPayClient;
import com.tripconnect.backend.repository.PaymentRepository;
import com.tripconnect.backend.repository.RefundRepository;
import com.tripconnect.backend.repository.UserRepository;
import com.tripconnect.backend.service.EmailTemplates;
import com.tripconnect.backend.service.NotificationEvents;
import com.tripconnect.backend.service.WebNotifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Hoàn tiền. Tạo yêu cầu hoàn trong transaction của nghiệp vụ (hủy đơn...), còn việc GỌI VNPAY chạy SAU KHI
 * transaction đó commit, ở luồng riêng: mạng chậm / VNPay lỗi không làm hỏng hay treo thao tác hủy đơn.
 * VNPay báo lỗi -> chuyển "Cần hoàn thủ công" và báo Admin.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    /** Phát sau khi tạo yêu cầu hoàn — bộ xử lý gọi VNPay sau commit. */
    public record RefundRequested(Long refundId) {
    }

    private static final String SERVER_IP = "127.0.0.1";

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final VnPayClient vnPayClient;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    /**
     * Hoàn tiền khi hủy đơn đã thanh toán. amount = 0 -> không hoàn (hủy sát ngày theo chính sách).
     * Phải gọi trong transaction đang mở (đơn đã được khóa).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void refundBooking(Booking booking, long amount, String reason) {
        booking.setRefundAmount(Math.max(amount, 0));
        if (amount <= 0) {
            booking.setRefundStatus(RefundStatus.NONE);
            return;
        }
        Payment payment = paymentRepository.findFirstByBookingIdAndStatus(booking.getId(), PaymentStatus.SUCCESS)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy giao dịch thanh toán của đơn " + booking.getCode()));
        booking.setRefundStatus(RefundStatus.PENDING);
        createRefund(booking, payment, amount, reason);
    }

    /** Hoàn một giao dịch thừa (vd khách thanh toán 2 lần) — không đổi trạng thái hoàn tiền của đơn. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void refundExtraPayment(Booking booking, Payment payment, String reason) {
        createRefund(booking, payment, payment.getAmount(), reason);
    }

    private void createRefund(Booking booking, Payment payment, long amount, String reason) {
        Refund refund = new Refund();
        refund.setBooking(booking);
        refund.setPayment(payment);
        refund.setAmount(amount);
        refund.setStatus(RefundRecordStatus.PENDING);
        refund.setReason(reason);
        refundRepository.save(refund);
        eventPublisher.publishEvent(new RefundRequested(refund.getId()));
    }

    /** Admin thử hoàn tự động lại sau khi VNPay lỗi. */
    @Transactional
    public void retry(Long refundId) {
        Refund refund = requireManual(refundId);
        refund.setStatus(RefundRecordStatus.PENDING);
        refund.setResponseCode(null);
        refund.setMessage(null);
        if (refund.getBooking().getRefundStatus() == RefundStatus.MANUAL_REQUIRED) {
            refund.getBooking().setRefundStatus(RefundStatus.PENDING);
        }
        eventPublisher.publishEvent(new RefundRequested(refund.getId()));
    }

    /** Admin đã tự chuyển khoản trả khách. */
    @Transactional
    public void markManualDone(Long refundId, Long adminId, String note) {
        Refund refund = requireManual(refundId);
        refund.setStatus(RefundRecordStatus.MANUAL_DONE);
        refund.setMessage(note == null || note.isBlank() ? "Đã hoàn thủ công" : note.trim());
        refund.setProcessedBy(userRepository.getReferenceById(adminId));
        refund.setProcessedAt(LocalDateTime.now(clock));
        Booking booking = refund.getBooking();
        if (booking.getRefundStatus() == RefundStatus.MANUAL_REQUIRED) {
            booking.setRefundStatus(RefundStatus.REFUNDED);
        }
        notifyRefunded(booking, refund.getAmount());
    }

    private void notifyRefunded(Booking booking, long amount) {
        eventPublisher.publishEvent(new NotificationEvents.UserEmailEvent(booking.getContactEmail(),
                EmailTemplates.refundCompleted(booking.getCode(), amount)));
        eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(booking.getCustomer().getId(),
                WebNotifications.refundCompleted(booking.getId(), booking.getCode(), amount)));
    }

    // ===================== Gọi VNPay (sau commit, luồng riêng) =====================

    private record RefundCall(String txnRef, long amount, boolean full, String transactionNo, String transactionDate) {
    }

    @Async
    @TransactionalEventListener
    public void onRefundRequested(RefundRequested event) {
        try {
            process(event.refundId());
        } catch (RuntimeException e) {
            log.error("Xử lý hoàn tiền id={} lỗi", event.refundId(), e);
        }
    }

    private void process(Long refundId) {
        // B1: đọc dữ liệu cần gửi VNPay (transaction ngắn, không giữ kết nối DB trong lúc gọi mạng)
        RefundCall call = transactionTemplate.execute(status -> refundRepository.findById(refundId)
                .filter(r -> r.getStatus() == RefundRecordStatus.PENDING)
                .map(r -> new RefundCall(r.getPayment().getTxnRef(), r.getAmount(), r.getAmount() == r.getPayment().getAmount(),
                        r.getPayment().getVnpTransactionNo(), r.getPayment().getVnpCreateDate()))
                .orElse(null));
        if (call == null) return;

        // B2: gọi VNPay
        VnPayClient.RefundResult result = vnPayClient.isConfigured()
                ? vnPayClient.refund(call.txnRef(), call.amount(), call.transactionNo(), call.transactionDate(), call.full(),
                "tripconnect", SERVER_IP, LocalDateTime.now(clock))
                : new VnPayClient.RefundResult(false, null, "Chưa cấu hình VNPay");

        // B3: ghi kết quả
        transactionTemplate.executeWithoutResult(status -> {
            Refund refund = refundRepository.findByIdForUpdate(refundId).orElseThrow();
            if (refund.getStatus() != RefundRecordStatus.PENDING) return;
            Booking booking = refund.getBooking();
            refund.setResponseCode(result.responseCode());
            refund.setMessage(result.message());
            refund.setProcessedAt(LocalDateTime.now(clock));
            if (result.success()) {
                refund.setStatus(RefundRecordStatus.SUCCESS);
                if (booking.getRefundStatus() == RefundStatus.PENDING) booking.setRefundStatus(RefundStatus.REFUNDED);
                notifyRefunded(booking, refund.getAmount());
            } else {
                refund.setStatus(RefundRecordStatus.MANUAL_REQUIRED);
                if (booking.getRefundStatus() == RefundStatus.PENDING) booking.setRefundStatus(RefundStatus.MANUAL_REQUIRED);
                log.warn("Hoàn tiền VNPay đơn {} thất bại: {} {}", booking.getCode(), result.responseCode(), result.message());
                eventPublisher.publishEvent(new NotificationEvents.AdminEmailEvent(
                        EmailTemplates.manualRefundNeeded(booking.getCode(), refund.getAmount(),
                                result.responseCode() == null ? result.message() : result.responseCode() + " - " + result.message())));
                eventPublisher.publishEvent(new NotificationEvents.AdminWebEvent(
                        WebNotifications.manualRefundNeeded(booking.getId(), booking.getCode(), refund.getAmount())));
            }
        });
    }

    private Refund requireManual(Long refundId) {
        Refund refund = refundRepository.findByIdForUpdate(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu hoàn tiền"));
        if (refund.getStatus() != RefundRecordStatus.MANUAL_REQUIRED) {
            throw new IllegalStateException("Chỉ xử lý được khoản hoàn đang chờ hoàn thủ công");
        }
        return refund;
    }
}
