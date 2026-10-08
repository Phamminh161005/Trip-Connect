package com.tripconnect.backend.service.settlement;

import com.tripconnect.backend.dto.settlement.SettlementRequests;
import com.tripconnect.backend.entity.*;
import com.tripconnect.backend.enums.*;
import com.tripconnect.backend.repository.*;
import com.tripconnect.backend.service.NotificationEvents;
import com.tripconnect.backend.storage.FileStorageService;
import com.tripconnect.backend.storage.FileValidator;
import com.tripconnect.backend.storage.TransactionalFileCleanup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Cách tính đối soát, lập bảng, xác nhận / khiếu nại / xử lý / thanh toán. */
@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 1, 3, 0);
    private static final long AGENT_ID = 9L;
    private static final long ADMIN_ID = 1L;

    @Mock private BookingRepository bookingRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private RefundRepository refundRepository;
    @Mock private SettlementRepository settlementRepository;
    @Mock private AgentProfileRepository agentProfileRepository;
    @Mock private UserRepository userRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private FileValidator fileValidator;
    @Mock private TransactionalFileCleanup fileCleanup;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private PlatformTransactionManager transactionManager;

    private SettlementGenerator generator;
    private SettlementService service;
    private User agent;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(VN).toInstant(), VN);
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        generator = new SettlementGenerator(bookingRepository, paymentRepository, refundRepository, settlementRepository,
                eventPublisher, tx, clock, "http://localhost:3000");
        service = new SettlementService(settlementRepository, agentProfileRepository, userRepository, generator, fileStorageService,
                fileValidator, fileCleanup, eventPublisher, tx, clock, "http://localhost:3000");
        agent = new User();
        agent.setId(AGENT_ID);
        agent.setEmail("agent@example.com");
        lenient().when(userRepository.getReferenceById(any())).thenReturn(new User());
        lenient().when(agentProfileRepository.findByUserIdIn(any())).thenReturn(List.of());
    }

    @Test
    void split_retainedMinusRefund_commissionRoundedToDong() {
        assertThat(SettlementRules.split(8_000_000, 0, new BigDecimal("0.10")))
                .isEqualTo(new SettlementRules.Amounts(8_000_000, 800_000, 7_200_000));
        // Khách hủy, được hoàn 50%: TripConnect giữ 4tr -> Agent nhận 90%
        assertThat(SettlementRules.split(8_000_000, 4_000_000, new BigDecimal("0.10")))
                .isEqualTo(new SettlementRules.Amounts(4_000_000, 400_000, 3_600_000));
        assertThat(SettlementRules.split(2_345_675, 0, new BigDecimal("0.10")).commission()).isEqualTo(234_568);
        assertThat(SettlementRules.split(100, 200, new BigDecimal("0.10")).retained()).isZero();
    }

    @Test
    void periodLabel_monthlyCutoffShowsPreviousMonth_manualShowsTime() {
        assertThat(SettlementRules.periodLabel(LocalDateTime.of(2026, 11, 1, 0, 0))).isEqualTo("tháng 10/2026");
        assertThat(SettlementRules.periodLabel(LocalDateTime.of(2026, 10, 7, 15, 30))).isEqualTo("đến 15:30 07/10/2026");
    }

    private Booking booking(long id, BookingStatus status) {
        Booking b = new Booking();
        b.setId(id);
        b.setAgent(agent);
        b.setStatus(status);
        b.setCommissionRate(new BigDecimal("0.10"));
        b.setCompletedAt(NOW.minusDays(5));
        b.setCancelledAt(NOW.minusDays(3));
        return b;
    }

    @Test
    void generate_onePerAgent_withCompletedAndRetainedCancellations_andNotifies() {
        Booking completed = booking(1, BookingStatus.COMPLETED);
        Booking cancelledKeptDeposit = booking(2, BookingStatus.CANCELLED);
        LocalDateTime cutoff = LocalDate.of(2026, 11, 1).atStartOfDay();
        when(bookingRepository.findUnsettled(cutoff)).thenReturn(List.of(completed, cancelledKeptDeposit));
        when(paymentRepository.sumSuccessByBookingIds(any())).thenReturn(List.of(
                new Object[]{1L, 8_000_000L}, new Object[]{2L, 2_400_000L}));
        when(refundRepository.sumByBookingIds(any())).thenReturn(List.<Object[]>of());

        int created = generator.generate(cutoff);

        assertThat(created).isEqualTo(1);
        ArgumentCaptor<Settlement> saved = ArgumentCaptor.forClass(Settlement.class);
        verify(settlementRepository).save(saved.capture());
        Settlement s = saved.getValue();
        assertThat(s.getStatus()).isEqualTo(SettlementStatus.PENDING_CONFIRM);
        assertThat(s.getCode()).startsWith("DS2610");
        assertThat(s.getItems()).extracting(SettlementItem::getKind)
                .containsExactly(SettlementItemKind.COMPLETED, SettlementItemKind.CANCELLED);
        assertThat(s.getRetainedAmount()).isEqualTo(10_400_000);
        assertThat(s.getCommissionAmount()).isEqualTo(1_040_000);
        assertThat(s.getPayoutAmount()).isEqualTo(9_360_000);
        assertThat(s.getConfirmDeadline()).isEqualTo(NOW.plusDays(5));
        verify(eventPublisher).publishEvent(any(NotificationEvents.UserWebEvent.class));
        verify(eventPublisher).publishEvent(any(NotificationEvents.UserEmailEvent.class));
    }

    private Settlement settlement(SettlementStatus status) {
        Settlement s = new Settlement();
        s.setId(50L);
        s.setCode("DS2610000001");
        s.setAgent(agent);
        s.setStatus(status);
        s.setCutoffAt(LocalDate.of(2026, 11, 1).atStartOfDay());
        s.setConfirmDeadline(NOW.plusDays(2));
        SettlementItem item = new SettlementItem();
        item.setSettlement(s);
        Booking b = booking(1, BookingStatus.COMPLETED);
        Tour tour = new Tour();
        tour.setTitle("Hà Giang");
        TourDeparture d = new TourDeparture();
        d.setStartDate(LocalDate.of(2026, 10, 20));
        b.setTour(tour);
        b.setDeparture(d);
        item.setBooking(b);
        item.setRetainedAmount(8_000_000);
        item.setCommissionAmount(800_000);
        item.setPayoutAmount(7_200_000);
        s.getItems().add(item);
        s.recalculate();
        lenient().when(settlementRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(s));
        return s;
    }

    @Test
    void agentConfirm_movesToAwaitingPayment_andTellsAdmins() {
        Settlement s = settlement(SettlementStatus.PENDING_CONFIRM);

        service.confirm(AGENT_ID, 50L);

        assertThat(s.getStatus()).isEqualTo(SettlementStatus.AWAITING_PAYMENT);
        assertThat(s.getConfirmedAt()).isEqualTo(NOW);
        verify(eventPublisher).publishEvent(any(NotificationEvents.AdminWebEvent.class));
        assertThatThrownBy(() -> service.confirm(77L, 50L)).hasMessageContaining("Không tìm thấy");
    }

    @Test
    void dispute_thenAdjust_recalculatesAndAsksAgentAgain() {
        Settlement s = settlement(SettlementStatus.PENDING_CONFIRM);
        service.dispute(AGENT_ID, 50L, "Đơn TC... khách hủy nhưng chưa được tính");
        assertThat(s.getStatus()).isEqualTo(SettlementStatus.DISPUTED);

        service.resolve(ADMIN_ID, 50L, new SettlementRequests.Resolve(SettlementRequests.Decision.ADJUST, "Đã cộng thêm",
                List.of(new SettlementRequests.AdjustmentInput(500_000L, "Bù đơn bị sót"))));

        assertThat(s.getStatus()).isEqualTo(SettlementStatus.PENDING_CONFIRM);
        assertThat(s.getAdjustmentAmount()).isEqualTo(500_000);
        assertThat(s.getPayoutAmount()).isEqualTo(7_700_000);
        assertThat(s.getDisputes().get(0).getStatus()).isEqualTo(SettlementDisputeStatus.ADJUSTED);
        assertThat(s.getConfirmDeadline()).isEqualTo(NOW.plusDays(5));
    }

    @Test
    void rejectDispute_goesStraightToAwaitingPayment_andPayoutCannotGoNegative() {
        Settlement s = settlement(SettlementStatus.PENDING_CONFIRM);
        service.dispute(AGENT_ID, 50L, "Sai");
        assertThatThrownBy(() -> service.resolve(ADMIN_ID, 50L, new SettlementRequests.Resolve(SettlementRequests.Decision.ADJUST,
                "Trừ", List.of(new SettlementRequests.AdjustmentInput(-9_000_000L, "Trừ quá tay")))))
                .hasMessageContaining("không được âm");

        Settlement fresh = settlement(SettlementStatus.PENDING_CONFIRM);
        service.dispute(AGENT_ID, 50L, "Sai");
        service.resolve(ADMIN_ID, 50L, new SettlementRequests.Resolve(SettlementRequests.Decision.REJECT, "Số liệu đúng", null));
        assertThat(fresh.getStatus()).isEqualTo(SettlementStatus.AWAITING_PAYMENT);
        assertThat(fresh.getDisputes().get(0).getStatus()).isEqualTo(SettlementDisputeStatus.REJECTED);
        assertThat(s).isNotSameAs(fresh);
    }

    @Test
    void markPaid_needsTransactionRefAndBankAccount_snapshotsTheAccount() {
        Settlement s = settlement(SettlementStatus.AWAITING_PAYMENT);
        assertThatThrownBy(() -> service.markPaid(ADMIN_ID, 50L, " ", null)).hasMessageContaining("mã giao dịch");

        AgentProfile profile = new AgentProfile();
        when(agentProfileRepository.findByUserId(AGENT_ID)).thenReturn(Optional.of(profile));
        assertThatThrownBy(() -> service.markPaid(ADMIN_ID, 50L, "FT26305123", null)).hasMessageContaining("tài khoản ngân hàng");

        Bank bank = new Bank();
        bank.setBin("970436");
        bank.setShortName("Vietcombank");
        profile.setBank(bank);
        profile.setBankAccountNumber("0123456789");
        profile.setBankAccountHolder("CONG TY TNHH DU LICH B");
        service.markPaid(ADMIN_ID, 50L, "FT26305123", null);

        assertThat(s.getStatus()).isEqualTo(SettlementStatus.PAID);
        assertThat(s.getBankName()).isEqualTo("Vietcombank");
        assertThat(s.getBankAccountNumber()).isEqualTo("0123456789");
        assertThat(s.getTransactionRef()).isEqualTo("FT26305123");
        verify(eventPublisher).publishEvent(any(NotificationEvents.UserEmailEvent.class));
    }

    @Test
    void overdue_isAutoConfirmed() {
        Settlement s = settlement(SettlementStatus.PENDING_CONFIRM);
        s.setConfirmDeadline(NOW.minusMinutes(1));
        when(settlementRepository.findPastConfirmDeadline(NOW)).thenReturn(List.of(50L));

        assertThat(service.autoConfirmOverdue()).isEqualTo(1);
        assertThat(s.getStatus()).isEqualTo(SettlementStatus.AWAITING_PAYMENT);
        assertThat(s.isAutoConfirmed()).isTrue();
    }
}
