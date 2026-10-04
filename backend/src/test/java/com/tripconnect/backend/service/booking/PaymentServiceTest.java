package com.tripconnect.backend.service.booking;

import com.tripconnect.backend.entity.*;
import com.tripconnect.backend.enums.*;
import com.tripconnect.backend.payment.VnPayClient;
import com.tripconnect.backend.repository.BookingRepository;
import com.tripconnect.backend.repository.PaymentRepository;
import com.tripconnect.backend.repository.TourDepartureRepository;
import com.tripconnect.backend.service.NotificationEvents;
import com.tripconnect.backend.service.tour.TourBookingStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 3, 10, 0);
    private static final String TXN = "TC26100312345601";

    @Mock private PaymentRepository paymentRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private TourDepartureRepository departureRepository;
    @Mock private TourBookingStats bookingStats;
    @Mock private BookingCancellation cancellation;
    @Mock private RefundService refundService;
    @Mock private VnPayClient vnPayClient;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private PlatformTransactionManager transactionManager;

    private PaymentService service;
    private Booking booking;
    private Payment payment;
    private TourDeparture departure;

    @BeforeEach
    void setUp() {
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        Clock clock = Clock.fixed(NOW.atZone(VN).toInstant(), VN);
        service = new PaymentService(paymentRepository, bookingRepository, departureRepository, bookingStats, cancellation,
                refundService, vnPayClient, eventPublisher, new TransactionTemplate(transactionManager), clock);

        Tour tour = new Tour();
        tour.setTitle("Hạ Long");
        tour.setMeetingPoint("Nhà hát Lớn");
        tour.setMeetingTime(LocalTime.of(7, 30));
        tour.setDurationDays((short) 3);
        departure = new TourDeparture();
        departure.setId(10L);
        departure.setTour(tour);
        departure.setStartDate(LocalDate.of(2026, 11, 15));
        departure.setCapacity(20);
        departure.setStatus(DepartureStatus.OPEN);

        booking = new Booking();
        User customer = new User();
        customer.setId(5L);
        booking.setCustomer(customer);
        booking.setId(1L);
        booking.setCode("TC261003123456");
        booking.setTour(tour);
        booking.setDeparture(departure);
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setAdults((short) 2);
        booking.setTotalAmount(8_000_000);
        booking.setContactEmail("khach@example.com");

        payment = new Payment();
        payment.setTxnRef(TXN);
        payment.setBooking(booking);
        payment.setAmount(8_000_000);
        payment.setStatus(PaymentStatus.PENDING);

        lenient().when(paymentRepository.findBookingIdByTxnRef(TXN)).thenReturn(Optional.of(1L));
        lenient().when(bookingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));
        lenient().when(paymentRepository.findByTxnRefForUpdate(TXN)).thenReturn(Optional.of(payment));
        lenient().when(paymentRepository.findByBookingIdAndStatus(any(), any())).thenReturn(List.of());
    }

    @Test
    void success_marksBookingPaid_once_evenIfReturnAndIpnBothArrive() {
        PaymentService.Outcome first = service.applyResult(TXN, true, 8_000_000, "00", "14000001", "NCB", "20261003101500");
        PaymentService.Outcome second = service.applyResult(TXN, true, 8_000_000, "00", "14000001", "NCB", "20261003101500");

        assertThat(first.rspCode()).isEqualTo("00");
        assertThat(first.paid()).isTrue();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PAID);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        // Lần 2 (IPN đến sau Return URL): báo "đã xác nhận", không gửi email lần nữa
        assertThat(second.rspCode()).isEqualTo("02");
        verify(eventPublisher, times(1)).publishEvent(any(NotificationEvents.UserEmailEvent.class));
    }

    @Test
    void wrongAmount_isRejected_andBookingUnchanged() {
        PaymentService.Outcome outcome = service.applyResult(TXN, true, 1_000, "00", "1", "NCB", null);
        assertThat(outcome.rspCode()).isEqualTo("04");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }

    @Test
    void failedPayment_keepsBookingPendingForRetry() {
        service.applyResult(TXN, false, 8_000_000, "24", null, "NCB", null);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }

    @Test
    void latePayment_afterExpiry_isRecovered_whenSeatsStillAvailable() {
        expired();
        when(departureRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(departure));
        when(bookingStats.seatsBooked(10L)).thenReturn(5);

        service.applyResult(TXN, true, 8_000_000, "00", "1", "NCB", null);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PAID);
        assertThat(booking.getCancelledBy()).isNull();
        verify(refundService, never()).refundExtraPayment(any(), any(), any());
    }

    @Test
    void latePayment_afterExpiry_isRefunded_whenSoldOut() {
        expired();
        when(departureRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(departure));
        when(bookingStats.seatsBooked(10L)).thenReturn(19);

        service.applyResult(TXN, true, 8_000_000, "00", "1", "NCB", null);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(refundService).refundExtraPayment(any(), any(), any());
    }

    @Test
    void secondSuccessfulPayment_forPaidBooking_isRefunded() {
        booking.setStatus(BookingStatus.PAID);
        service.applyResult(TXN, true, 8_000_000, "00", "1", "NCB", null);
        verify(refundService).refundExtraPayment(any(), any(), any());
    }

    @Test
    void unknownTransaction_returnsOrderNotFound() {
        assertThat(service.applyResult("KHONGCO", true, 1, "00", null, null, null).rspCode()).isEqualTo("01");
    }

    private void expired() {
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledBy(CancelledBy.SYSTEM);
        payment.setStatus(PaymentStatus.EXPIRED);
    }
}
