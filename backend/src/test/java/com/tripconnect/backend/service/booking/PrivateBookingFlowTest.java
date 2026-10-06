package com.tripconnect.backend.service.booking;

import com.tripconnect.backend.entity.*;
import com.tripconnect.backend.enums.*;
import com.tripconnect.backend.payment.VnPayClient;
import com.tripconnect.backend.repository.*;
import com.tripconnect.backend.service.NotificationEvents;
import com.tripconnect.backend.service.tour.TourBookingStats;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Đơn tour riêng: đặt cọc -> trả phần còn lại, gia hạn, hủy (không hoàn cọc), hoàn tiền chia theo giao dịch. */
@ExtendWith(MockitoExtension.class)
class PrivateBookingFlowTest {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 6, 9, 0);
    private static final LocalDate START = LocalDate.of(2026, 11, 5);
    private static final String TXN = "TC26100600000101";

    @Mock private PaymentRepository paymentRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private TourDepartureRepository departureRepository;
    @Mock private TourBookingStats bookingStats;
    @Mock private BookingCancellation cancellationMock;
    @Mock private RefundService refundServiceMock;
    @Mock private VnPayClient vnPayClient;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private UserRepository userRepository;
    @Mock private AgentProfileRepository agentProfileRepository;
    @Mock private BookingAssembler assembler;
    @Mock private RefundRepository refundRepository;

    private final Clock clock = Clock.fixed(NOW.atZone(VN).toInstant(), VN);
    private PaymentService paymentService;
    private BookingService bookingService;
    private Booking booking;

    @BeforeEach
    void setUp() {
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        paymentService = new PaymentService(paymentRepository, bookingRepository, departureRepository, bookingStats,
                cancellationMock, refundServiceMock, vnPayClient, eventPublisher, new TransactionTemplate(transactionManager), clock);
        bookingService = new BookingService(bookingRepository, departureRepository, userRepository, agentProfileRepository,
                bookingStats, paymentService, cancellationMock, assembler,
                new BookingSettings(new BigDecimal("0.10"), 30, 15, 7, 3, 50), eventPublisher, clock);

        User customer = new User();
        customer.setId(5L);
        User agent = new User();
        agent.setId(9L);
        Tour tour = new Tour();
        tour.setTitle("Hà Giang mùa tam giác mạch");
        tour.setStatus(TourStatus.PRIVATE);
        tour.setCustomRequestId(30L);
        tour.setMeetingPoint("Nhà hát Lớn");
        tour.setMeetingTime(LocalTime.of(6, 30));
        TourDeparture departure = new TourDeparture();
        departure.setId(10L);
        departure.setTour(tour);
        departure.setStartDate(START);
        booking = new Booking();
        booking.setId(1L);
        booking.setCode("TC261006000001");
        booking.setCustomer(customer);
        booking.setAgent(agent);
        booking.setTour(tour);
        booking.setDeparture(departure);
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setAdults((short) 2);
        booking.setChildren((short) 1);
        booking.setTotalAmount(7_200_000);
        booking.setDepositAmount(2_160_000);
        booking.setHoldExpiresAt(NOW.plusHours(48));
        booking.setBalanceDueDate(LocalDate.of(2026, 10, 29));
        booking.setContactEmail("lan@example.com");

        lenient().when(paymentRepository.findBookingIdByTxnRef(TXN)).thenReturn(Optional.of(1L));
        lenient().when(bookingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));
        lenient().when(paymentRepository.findByBookingIdAndStatus(any(), any())).thenReturn(List.of());
    }

    private Payment payment(PaymentPurpose purpose, long amount) {
        Payment p = new Payment();
        p.setTxnRef(TXN);
        p.setBooking(booking);
        p.setAmount(amount);
        p.setPurpose(purpose);
        p.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findByTxnRefForUpdate(TXN)).thenReturn(Optional.of(p));
        return p;
    }

    private void passengers(int count) {
        booking.getPassengers().clear();
        for (int i = 0; i < count; i++) booking.getPassengers().add(new BookingPassenger());
    }

    @Test
    void depositThenBalance_movesPendingToDepositPaidToPaid() {
        payment(PaymentPurpose.DEPOSIT, 2_160_000);
        PaymentService.Outcome deposit = paymentService.applyResult(TXN, true, 2_160_000, "00", "1", "NCB", null);

        assertThat(deposit.paid()).isTrue();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.DEPOSIT_PAID);
        assertThat(booking.getDepositPaidAt()).isEqualTo(NOW);
        assertThat(booking.paidAmount()).isEqualTo(2_160_000);
        assertThat(booking.nextPaymentAmount()).isEqualTo(5_040_000);
        // Khách: web + email; Agent: web
        verify(eventPublisher, times(2)).publishEvent(any(NotificationEvents.UserWebEvent.class));

        payment(PaymentPurpose.BALANCE, 5_040_000);
        paymentService.applyResult(TXN, true, 5_040_000, "00", "2", "NCB", null);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PAID);
        assertThat(booking.paidAmount()).isEqualTo(7_200_000);
    }

    @Test
    void depositPaidTwice_secondIsRefunded() {
        booking.setStatus(BookingStatus.DEPOSIT_PAID);
        Payment extra = payment(PaymentPurpose.DEPOSIT, 2_160_000);

        paymentService.applyResult(TXN, true, 2_160_000, "00", "3", "NCB", null);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.DEPOSIT_PAID);
        verify(refundServiceMock).refundExtraPayment(eq(booking), eq(extra), any());
    }

    @Test
    void payBalance_needsFullPassengerListAndOpenDeadline() {
        booking.setStatus(BookingStatus.DEPOSIT_PAID);
        passengers(2);
        assertThatThrownBy(() -> bookingService.pay(5L, 1L, "ip")).hasMessageContaining("nhập đủ thông tin 3 người đi");

        passengers(3);
        when(vnPayClient.createPaymentUrl(any(), eq(5_040_000L), any(), any(), any(), any())).thenReturn("https://vnpay/balance");
        assertThat(bookingService.pay(5L, 1L, "ip")).isEqualTo("https://vnpay/balance");
        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(saved.capture());
        assertThat(saved.getValue().getPurpose()).isEqualTo(PaymentPurpose.BALANCE);

        booking.setBalanceDueDate(NOW.toLocalDate().minusDays(1));
        assertThatThrownBy(() -> bookingService.pay(5L, 1L, "ip")).hasMessageContaining("quá hạn");
    }

    @Test
    void extendBalance_onceByThreeDays_andTellsAgent() {
        booking.setStatus(BookingStatus.DEPOSIT_PAID);
        booking.setInfants((short) 1);

        bookingService.extendBalance(5L, 1L);

        assertThat(booking.getBalanceDueDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(booking.isBalanceExtended()).isTrue();
        verify(eventPublisher).publishEvent(any(NotificationEvents.UserWebEvent.class));
        assertThatThrownBy(() -> bookingService.extendBalance(5L, 1L)).hasMessageContaining("đã dùng lượt gia hạn");
    }

    @Test
    void cancellingAfterDeposit_keepsDeposit() {
        booking.setStatus(BookingStatus.DEPOSIT_PAID);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        var quote = bookingService.quote(5L, 1L);

        assertThat(quote.cancellable()).isTrue();
        assertThat(quote.refundAmount()).isZero();
        assertThat(quote.explanation()).contains("Tiền cọc không được hoàn");
    }

    @Test
    void cancellation_refundIsCappedAtWhatWasPaid_andClosesTheRequest() {
        BookingCancellation cancellation = new BookingCancellation(bookingRepository, paymentRepository, refundServiceMock,
                eventPublisher, clock);
        booking.setStatus(BookingStatus.DEPOSIT_PAID);

        // Agent hủy chuyến: hoàn 100% số đã trả = tiền cọc
        cancellation.cancel(booking, CancelledBy.AGENT, "Thời tiết xấu", booking.getTotalAmount(), true);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(refundServiceMock).refundBooking(booking, 2_160_000, "Thời tiết xấu");
        ArgumentCaptor<Object> events = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeastOnce()).publishEvent(events.capture());
        assertThat(events.getAllValues()).anySatisfy(e -> assertThat(e)
                .isEqualTo(new BookingCancellation.PrivateBookingCancelled(30L, CancelledBy.AGENT, "Thời tiết xấu")));
    }

    @Test
    void refund_isSplitAcrossDepositAndBalanceTransactions_newestFirst() {
        RefundService refundService = new RefundService(refundRepository, paymentRepository, userRepository, vnPayClient,
                eventPublisher, new TransactionTemplate(transactionManager), clock);
        Payment deposit = new Payment();
        deposit.setId(1L);
        deposit.setAmount(2_160_000);
        Payment balance = new Payment();
        balance.setId(2L);
        balance.setAmount(5_040_000);
        when(paymentRepository.findByBookingIdAndStatus(1L, PaymentStatus.SUCCESS)).thenReturn(List.of(deposit, balance));
        when(refundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        refundService.refundBooking(booking, 6_000_000, "Agent hủy chuyến");

        ArgumentCaptor<Refund> refunds = ArgumentCaptor.forClass(Refund.class);
        verify(refundRepository, times(2)).save(refunds.capture());
        assertThat(refunds.getAllValues()).extracting(Refund::getAmount).containsExactly(5_040_000L, 960_000L);
        assertThat(refunds.getAllValues()).extracting(Refund::getPayment).containsExactly(balance, deposit);
        assertThat(booking.getRefundStatus()).isEqualTo(RefundStatus.PENDING);
    }
}
