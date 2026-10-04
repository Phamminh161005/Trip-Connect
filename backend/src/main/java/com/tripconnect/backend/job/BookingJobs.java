package com.tripconnect.backend.job;

import com.tripconnect.backend.enums.BookingStatus;
import com.tripconnect.backend.repository.BookingRepository;
import com.tripconnect.backend.service.booking.PaymentService;
import com.tripconnect.backend.service.booking.TripReminderService;
import com.tripconnect.backend.service.review.ReviewInviter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Các việc chạy định kỳ của đặt tour. */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingJobs {

    /** Đợi thêm sau hạn giữ chỗ để kết quả VNPay (Return URL / IPN) kịp về trước khi tự hủy. */
    static final int GRACE_MINUTES = 1;
    static final int BATCH = 50;
    /** Đơn tự chuyển "Hoàn thành" sau ngày về ngần này ngày (nghiệp vụ: 3 ngày). */
    static final int COMPLETE_AFTER_DAYS = 3;

    private final BookingRepository bookingRepository;
    private final PaymentService paymentService;
    private final TripReminderService tripReminderService;
    private final ReviewInviter reviewInviter;
    private final Clock clock;

    /** Mỗi phút: đơn chờ thanh toán quá hạn -> hỏi lại VNPay, chưa trả thì hủy và nhả chỗ. */
    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void settleExpiredBookings() {
        List<Long> ids = bookingRepository.findExpiredPendingIds(
                LocalDateTime.now(clock).minusMinutes(GRACE_MINUTES), PageRequest.of(0, BATCH));
        for (Long id : ids) {
            try {
                paymentService.settleExpired(id);
            } catch (RuntimeException e) {
                log.warn("Xử lý đơn quá hạn id={} lỗi: {}", id, e.getMessage());
            }
        }
    }

    /**
     * Mỗi giờ từ 8h đến 20h: email nhắc lịch khởi hành (trước 3 ngày và 1 ngày) và nhắc lịch ít khách (trước 7 ngày).
     * Chạy nhiều lượt trong ngày để gửi bù nếu backend tắt lúc sáng; đã gửi thì không gửi lại.
     */
    @Scheduled(cron = "0 0 8-20 * * *", zone = "Asia/Ho_Chi_Minh")
    public void sendTripReminders() {
        tripReminderService.sendDueReminders();
    }

    /** 01:00 mỗi ngày: đơn đã thanh toán của chuyến đã về quá 3 ngày -> Hoàn thành + mời khách đánh giá tour. */
    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void completeFinishedTrips() {
        LocalDate lastEndDate = LocalDate.now(clock).minusDays(COMPLETE_AFTER_DAYS);
        List<Long> ids = bookingRepository.findPaidIdsEndedOnOrBefore(lastEndDate);
        LocalDateTime now = LocalDateTime.now(clock);
        bookingRepository.findAllById(ids).forEach(b -> {
            if (b.getStatus() == BookingStatus.PAID) {
                b.setStatus(BookingStatus.COMPLETED);
                b.setCompletedAt(now);
                reviewInviter.invite(b);
            }
        });
        if (!ids.isEmpty()) log.info("Đã chuyển {} đơn sang Hoàn thành", ids.size());
    }
}
