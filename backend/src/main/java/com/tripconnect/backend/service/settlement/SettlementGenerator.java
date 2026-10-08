package com.tripconnect.backend.service.settlement;

import com.tripconnect.backend.entity.Booking;
import com.tripconnect.backend.entity.Settlement;
import com.tripconnect.backend.entity.SettlementItem;
import com.tripconnect.backend.enums.BookingStatus;
import com.tripconnect.backend.enums.SettlementItemKind;
import com.tripconnect.backend.enums.SettlementStatus;
import com.tripconnect.backend.repository.BookingRepository;
import com.tripconnect.backend.repository.PaymentRepository;
import com.tripconnect.backend.repository.RefundRepository;
import com.tripconnect.backend.repository.SettlementRepository;
import com.tripconnect.backend.service.EmailTemplates;
import com.tripconnect.backend.service.NotificationEvents;
import com.tripconnect.backend.service.WebNotifications;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Lập bảng đối soát: mỗi Agent một bảng gồm các đơn chưa đối soát phát sinh trước thời điểm chốt.
 * Mỗi Agent một transaction (lỗi của một Agent không chặn Agent khác); ràng buộc UNIQUE booking_id chặn đối soát trùng.
 */
@Slf4j
@Component
public class SettlementGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter CODE_MONTH = DateTimeFormatter.ofPattern("yyMM");

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final SettlementRepository settlementRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final String frontendUrl;

    public SettlementGenerator(BookingRepository bookingRepository, PaymentRepository paymentRepository,
                               RefundRepository refundRepository, SettlementRepository settlementRepository,
                               ApplicationEventPublisher eventPublisher, TransactionTemplate transactionTemplate, Clock clock,
                               @Value("${app.frontend-url}") String frontendUrl) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.settlementRepository = settlementRepository;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    /** @return số bảng đối soát đã lập */
    public int generate(LocalDateTime cutoff) {
        List<Long> agentIds = transactionTemplate.execute(status -> bookingRepository.findUnsettled(cutoff).stream()
                .map(b -> b.getAgent().getId()).distinct().toList());
        int created = 0;
        for (Long agentId : agentIds == null ? List.<Long>of() : agentIds) {
            try {
                Boolean done = transactionTemplate.execute(status -> generateFor(agentId, cutoff));
                if (Boolean.TRUE.equals(done)) created++;
            } catch (RuntimeException e) {
                log.warn("Lập đối soát cho Agent {} lỗi: {}", agentId, e.getMessage());
            }
        }
        return created;
    }

    private boolean generateFor(Long agentId, LocalDateTime cutoff) {
        List<Booking> bookings = bookingRepository.findUnsettled(cutoff).stream()
                .filter(b -> b.getAgent().getId().equals(agentId)).toList();
        if (bookings.isEmpty()) return false;
        List<Long> ids = bookings.stream().map(Booking::getId).toList();
        Map<Long, Long> paid = toMap(paymentRepository.sumSuccessByBookingIds(ids));
        Map<Long, Long> refunded = toMap(refundRepository.sumByBookingIds(ids));

        LocalDateTime now = LocalDateTime.now(clock);
        Settlement settlement = new Settlement();
        settlement.setCode(newCode(cutoff.minusSeconds(1)));
        settlement.setAgent(bookings.get(0).getAgent());
        settlement.setCutoffAt(cutoff);
        settlement.setStatus(SettlementStatus.PENDING_CONFIRM);
        settlement.setConfirmDeadline(now.plusDays(SettlementRules.CONFIRM_DAYS));
        settlement.setCreatedAt(now);
        for (Booking b : bookings) {
            long paidAmount = paid.getOrDefault(b.getId(), 0L);
            long refundedAmount = refunded.getOrDefault(b.getId(), 0L);
            SettlementRules.Amounts amounts = SettlementRules.split(paidAmount, refundedAmount, b.getCommissionRate());
            if (amounts.retained() <= 0) continue;
            SettlementItem item = new SettlementItem();
            item.setSettlement(settlement);
            item.setBooking(b);
            boolean completed = b.getStatus() == BookingStatus.COMPLETED;
            item.setKind(completed ? SettlementItemKind.COMPLETED : SettlementItemKind.CANCELLED);
            item.setEventAt(completed ? b.getCompletedAt() : b.getCancelledAt());
            item.setPaidAmount(paidAmount);
            item.setRefundedAmount(refundedAmount);
            item.setRetainedAmount(amounts.retained());
            item.setCommissionRate(b.getCommissionRate());
            item.setCommissionAmount(amounts.commission());
            item.setPayoutAmount(amounts.payout());
            settlement.getItems().add(item);
        }
        if (settlement.getItems().isEmpty()) return false;
        settlement.recalculate();
        settlementRepository.save(settlement);

        String period = SettlementRules.periodLabel(cutoff);
        Long agentUserId = settlement.getAgent().getId();
        eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(agentUserId, WebNotifications.settlementCreated(
                settlement.getId(), settlement.getCode(), period, settlement.getPayoutAmount(),
                settlement.getConfirmDeadline().toLocalDate())));
        eventPublisher.publishEvent(new NotificationEvents.UserEmailEvent(settlement.getAgent().getEmail(),
                EmailTemplates.settlementCreated(settlement.getCode(), period, settlement.getItemCount(),
                        settlement.getRetainedAmount(), settlement.getCommissionAmount(), settlement.getPayoutAmount(),
                        settlement.getConfirmDeadline().toLocalDate(), frontendUrl + "/agent/settlements/" + settlement.getId())));
        return true;
    }

    private static Map<Long, Long> toMap(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(r -> ((Number) r[0]).longValue(), r -> ((Number) r[1]).longValue()));
    }

    /** "DS" + năm tháng của kỳ + 6 số ngẫu nhiên. */
    private String newCode(LocalDateTime periodTime) {
        for (int i = 0; i < 10; i++) {
            String code = "DS" + periodTime.format(CODE_MONTH) + String.format("%06d", RANDOM.nextInt(1_000_000));
            if (!settlementRepository.existsByCode(code)) return code;
        }
        throw new IllegalStateException("Không tạo được mã đối soát, vui lòng thử lại");
    }
}
