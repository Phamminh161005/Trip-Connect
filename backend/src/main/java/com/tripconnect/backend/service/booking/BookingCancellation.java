package com.tripconnect.backend.service.booking;

import com.tripconnect.backend.entity.Booking;
import com.tripconnect.backend.entity.Payment;
import com.tripconnect.backend.enums.BookingStatus;
import com.tripconnect.backend.enums.CancelledBy;
import com.tripconnect.backend.enums.PaymentStatus;
import com.tripconnect.backend.repository.BookingRepository;
import com.tripconnect.backend.repository.PaymentRepository;
import com.tripconnect.backend.service.EmailTemplates;
import com.tripconnect.backend.service.NotificationEvents;
import com.tripconnect.backend.service.WebNotifications;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Hủy đơn — dùng chung cho mọi trường hợp:
 *  - Khách tự hủy: hoàn theo chính sách (tính ở nơi gọi).
 *  - Agent hủy chuyến / Admin hủy vì bất khả kháng: hoàn 100%.
 *  - Hệ thống: quá hạn giữ chỗ (chưa trả tiền -> không hoàn).
 * Phải gọi trong transaction, đơn đã được khóa / nạp mới.
 */
@Component
@RequiredArgsConstructor
public class BookingCancellation {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RefundService refundService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /**
     * @param refundAmount số tiền hoàn (chỉ áp dụng khi đơn đã thanh toán)
     * @param notify       gửi email báo khách (tự hủy vì quá hạn thì không cần)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void cancel(Booking booking, CancelledBy by, String reason, long refundAmount, boolean notify) {
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT && booking.getStatus() != BookingStatus.PAID) {
            throw new IllegalStateException("Đơn " + booking.getCode() + " không ở trạng thái có thể hủy");
        }
        boolean paid = booking.getStatus() == BookingStatus.PAID;
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now(clock));
        booking.setCancelledBy(by);
        booking.setCancelReason(reason);

        // Link VNPay chưa dùng hết hạn luôn -> nếu khách vẫn trả, phần xử lý kết quả sẽ hoàn tiền
        for (Payment payment : paymentRepository.findByBookingIdAndStatus(booking.getId(), PaymentStatus.PENDING)) {
            payment.setStatus(PaymentStatus.EXPIRED);
        }
        if (paid) {
            refundService.refundBooking(booking, Math.min(refundAmount, booking.getTotalAmount()), reason);
        }
        if (notify) {
            long refund = paid ? booking.getRefundAmount() : 0;
            eventPublisher.publishEvent(new NotificationEvents.UserEmailEvent(booking.getContactEmail(),
                    EmailTemplates.bookingCancelled(booking.getCode(), booking.getTour().getTitle(), reason, refund)));
            eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(booking.getCustomer().getId(),
                    WebNotifications.bookingCancelled(booking.getId(), booking.getCode(), booking.getTour().getTitle(), refund)));
        }
    }

    /** Agent hủy chuyến: mọi đơn còn hiệu lực của lịch bị hủy, khách đã trả được hoàn 100%. */
    @Transactional(propagation = Propagation.MANDATORY)
    public int cancelAllForDeparture(Long departureId, CancelledBy by, String reason) {
        List<Booking> bookings = bookingRepository.findByDepartureIdAndStatusIn(departureId,
                List.of(BookingStatus.PENDING_PAYMENT, BookingStatus.PAID));
        for (Booking booking : bookings) {
            cancel(booking, by, "Chuyến đi bị hủy: " + reason, booking.getTotalAmount(), true);
        }
        return bookings.size();
    }
}
