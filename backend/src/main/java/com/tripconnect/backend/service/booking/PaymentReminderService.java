package com.tripconnect.backend.service.booking;

import com.tripconnect.backend.entity.Booking;
import com.tripconnect.backend.enums.BookingStatus;
import com.tripconnect.backend.repository.BookingRepository;
import com.tripconnect.backend.service.EmailTemplates;
import com.tripconnect.backend.service.NotificationEvents;
import com.tripconnect.backend.service.WebNotifications;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Predicate;

/** Nhắc khách tour riêng: sắp hết hạn đặt cọc (còn 12 giờ), sắp tới hạn trả phần còn lại (trước 3 ngày và 1 ngày). */
@Slf4j
@Service
public class PaymentReminderService {

    static final int DEPOSIT_REMIND_HOURS = 12;
    static final int BALANCE_REMIND_DAYS = 3;

    private final BookingRepository bookingRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final String frontendUrl;

    public PaymentReminderService(BookingRepository bookingRepository, ApplicationEventPublisher eventPublisher,
                                  TransactionTemplate transactionTemplate, Clock clock,
                                  @Value("${app.frontend-url}") String frontendUrl) {
        this.bookingRepository = bookingRepository;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    public int remindDeposits() {
        LocalDateTime now = LocalDateTime.now(clock);
        return forEach(bookingRepository.findDepositDueIds(now, now.plusHours(DEPOSIT_REMIND_HOURS)), booking -> {
            if (booking.getStatus() != BookingStatus.PENDING_PAYMENT || booking.getPaymentReminderStage() >= 1
                    || !booking.getHoldExpiresAt().isAfter(LocalDateTime.now(clock))) return false;
            booking.setPaymentReminderStage((short) 1);
            eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(booking.getCustomer().getId(),
                    WebNotifications.depositDue(booking.getId(), booking.getCode(), booking.getHoldExpiresAt())));
            return true;
        });
    }

    public int remindBalances() {
        LocalDate today = LocalDate.now(clock);
        return forEach(bookingRepository.findBalanceDueIds(today.plusDays(BALANCE_REMIND_DAYS)), booking -> {
            if (booking.getStatus() != BookingStatus.DEPOSIT_PAID || booking.getBalanceDueDate().isBefore(today)) return false;
            long daysLeft = ChronoUnit.DAYS.between(today, booking.getBalanceDueDate());
            // 2 = nhắc trước 3 ngày, 3 = trước 1 ngày (vào sát hạn thì chỉ gửi lần sau)
            short stage = (short) (daysLeft <= 1 ? 3 : 2);
            if (booking.getPaymentReminderStage() >= stage) return false;
            booking.setPaymentReminderStage(stage);
            long balance = booking.getTotalAmount() - booking.getDepositAmount();
            boolean canExtend = !booking.isBalanceExtended();
            eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(booking.getCustomer().getId(),
                    WebNotifications.balanceDue(booking.getId(), booking.getCode(), balance, booking.getBalanceDueDate(), canExtend)));
            eventPublisher.publishEvent(new NotificationEvents.UserEmailEvent(booking.getContactEmail(),
                    EmailTemplates.balanceDue(booking.getCode(), booking.getTour().getTitle(), balance, booking.getBalanceDueDate(),
                            canExtend, frontendUrl + "/account/bookings/" + booking.getId())));
            return true;
        });
    }

    /** Mỗi đơn một transaction (khóa đơn), lỗi một đơn không chặn các đơn khác. */
    private int forEach(List<Long> ids, Predicate<Booking> action) {
        int done = 0;
        for (Long id : ids) {
            try {
                Boolean sent = transactionTemplate.execute(status ->
                        action.test(bookingRepository.findByIdForUpdate(id).orElseThrow()));
                if (Boolean.TRUE.equals(sent)) done++;
            } catch (RuntimeException e) {
                log.warn("Nhắc thanh toán đơn id={} lỗi: {}", id, e.getMessage());
            }
        }
        return done;
    }
}
