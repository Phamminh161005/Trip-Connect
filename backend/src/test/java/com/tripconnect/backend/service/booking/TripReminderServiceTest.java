package com.tripconnect.backend.service.booking;

import com.tripconnect.backend.entity.*;
import com.tripconnect.backend.enums.BookingStatus;
import com.tripconnect.backend.enums.DepartureStatus;
import com.tripconnect.backend.enums.PassengerType;
import com.tripconnect.backend.repository.AgentProfileRepository;
import com.tripconnect.backend.repository.BookingRepository;
import com.tripconnect.backend.repository.TourDepartureRepository;
import com.tripconnect.backend.service.NotificationEvents;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
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
class TripReminderServiceTest {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 10);

    @Mock private BookingRepository bookingRepository;
    @Mock private TourDepartureRepository departureRepository;
    @Mock private AgentProfileRepository agentProfileRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private PlatformTransactionManager transactionManager;

    private TripReminderService service;
    private Tour tour;
    private TourDeparture departure;
    private Booking booking;

    @BeforeEach
    void setUp() {
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        Clock clock = Clock.fixed(TODAY.atTime(8, 0).atZone(VN).toInstant(), VN);
        service = new TripReminderService(bookingRepository, departureRepository, agentProfileRepository, eventPublisher,
                new TransactionTemplate(transactionManager), clock, "http://localhost:3000/");

        User agent = new User();
        agent.setId(7L);
        agent.setFullName("Đối tác A");
        agent.setEmail("agent@example.com");
        tour = new Tour();
        tour.setId(5L);
        tour.setAgent(agent);
        tour.setTitle("Sa Pa 3N2Đ");
        tour.setMeetingPoint("Nhà hát Lớn");
        tour.setMeetingTime(LocalTime.of(6, 30));
        tour.setDurationDays((short) 3);
        departure = new TourDeparture();
        departure.setId(20L);
        departure.setTour(tour);
        departure.setStatus(DepartureStatus.OPEN);
        departure.setStartDate(TODAY.plusDays(3));

        booking = new Booking();
        User customer = new User();
        customer.setId(5L);
        booking.setCustomer(customer);
        booking.setId(1L);
        booking.setCode("TC261001123456");
        booking.setTour(tour);
        booking.setDeparture(departure);
        booking.setStatus(BookingStatus.PAID);
        booking.setPaidAt(TODAY.minusDays(10).atTime(9, 0));
        booking.setAdults((short) 1);
        booking.setChildren((short) 1);
        booking.setContactName("Khách A");
        booking.setContactPhone("0900000000");
        booking.setContactEmail("khach@example.com");
        booking.getPassengers().add(passenger("Khách A", PassengerType.ADULT));
        booking.getPassengers().add(passenger("Bé B", PassengerType.CHILD));
    }

    private static BookingPassenger passenger(String name, PassengerType type) {
        BookingPassenger p = new BookingPassenger();
        p.setFullName(name);
        p.setType(type);
        return p;
    }

    @ParameterizedTest(name = "còn {0} ngày, đã nhắc {1} -> {2}")
    @CsvSource({"4,0,0", "3,0,1", "2,0,1", "3,1,0", "2,1,0", "1,0,2", "1,1,2", "1,2,0", "0,0,0"})
    void dueStage(long daysLeft, short sent, short expected) {
        assertThat(TripReminderService.dueStage(daysLeft, sent)).isEqualTo(expected);
    }

    @Test
    void customerGetsFirstReminderThreeDaysBefore() {
        when(bookingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));
        when(agentProfileRepository.findByUserId(7L)).thenReturn(Optional.empty());

        service.remindCustomer(1L, TODAY);

        assertThat(booking.getReminderStage()).isEqualTo((short) 1);
        var event = ArgumentCaptor.forClass(NotificationEvents.UserEmailEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().toEmail()).isEqualTo("khach@example.com");
        assertThat(event.getValue().email().subject()).contains("còn 3 ngày nữa");
        assertThat(event.getValue().email().body())
                .contains("Bé B (Trẻ em)", "06:30", "13/10/2026 - 15/10/2026", "http://localhost:3000/account/bookings/1");
    }

    @Test
    void customerGetsLastReminderTheDayBefore() {
        departure.setStartDate(TODAY.plusDays(1));
        booking.setReminderStage((short) 1);
        when(bookingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));

        service.remindCustomer(1L, TODAY);

        assertThat(booking.getReminderStage()).isEqualTo((short) 2);
        verify(eventPublisher).publishEvent(any(NotificationEvents.UserEmailEvent.class));
    }

    @Test
    void bookingPaidInsideReminderWindowIsMarkedWithoutEmail() {
        departure.setStartDate(TODAY.plusDays(2));
        booking.setPaidAt(TODAY.minusDays(1).atTime(15, 0)); // trả tiền đúng ngày nhắc đầu (còn 3 ngày)
        when(bookingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));

        service.remindCustomer(1L, TODAY);

        assertThat(booking.getReminderStage()).isEqualTo((short) 1);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void cancelledBookingIsSkipped() {
        booking.setStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));

        service.remindCustomer(1L, TODAY);

        assertThat(booking.getReminderStage()).isZero();
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void agentGetsOneEmailPerDepartureWithPassengerSummary() {
        when(departureRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(departure));
        when(bookingRepository.findWithPassengersByDepartureId(20L, List.of(BookingStatus.PAID))).thenReturn(List.of(booking));

        service.remindOrganizer(20L, TODAY);

        assertThat(departure.getOrganizerReminderStage()).isEqualTo((short) 1);
        var event = ArgumentCaptor.forClass(NotificationEvents.UserEmailEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().toEmail()).isEqualTo("agent@example.com");
        assertThat(event.getValue().email().body())
                .contains("Tổng khách: 2 (1 người lớn, 1 trẻ em, 0 trẻ sơ sinh)", "TC261001123456 · Khách A · 0900000000",
                        "http://localhost:3000/agent/tours/5/departures/20");
    }

    @Test
    void platformTourReminderGoesToAdmins() {
        tour.setAgent(null);
        when(departureRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(departure));
        when(bookingRepository.findWithPassengersByDepartureId(20L, List.of(BookingStatus.PAID))).thenReturn(List.of(booking));

        service.remindOrganizer(20L, TODAY);

        verify(eventPublisher).publishEvent(any(NotificationEvents.AdminEmailEvent.class));
    }

    private void departureInSevenDays(LocalDateTime createdAt) {
        departure.setStartDate(TODAY.plusDays(7));
        departure.setCapacity(20);
        departure.setCreatedAt(createdAt);
        when(departureRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(departure));
    }

    @Test
    void lowBookingsRemindedOnceWhenUnderThirtyPercent() {
        departureInSevenDays(TODAY.minusDays(20).atTime(9, 0));
        when(bookingRepository.sumPaidSeats(20L)).thenReturn(5L);

        service.remindLowBookings(20L, TODAY);

        assertThat(departure.isLowBookingReminded()).isTrue();
        var event = ArgumentCaptor.forClass(NotificationEvents.UserEmailEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().toEmail()).isEqualTo("agent@example.com");
        assertThat(event.getValue().email().body()).contains("còn 7 ngày nữa là khởi hành", "5/20 chỗ", "dưới 30%");
    }

    @Test
    void enoughBookingsIsNotMarkedSoALaterDropCanStillBeReminded() {
        departureInSevenDays(TODAY.minusDays(20).atTime(9, 0));
        when(bookingRepository.sumPaidSeats(20L)).thenReturn(6L);

        service.remindLowBookings(20L, TODAY);

        assertThat(departure.isLowBookingReminded()).isFalse();
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void departureAddedInsideTheWindowIsNotReminded() {
        departureInSevenDays(TODAY.atTime(7, 0));

        service.remindLowBookings(20L, TODAY);

        assertThat(departure.isLowBookingReminded()).isTrue();
        verifyNoInteractions(eventPublisher);
        verify(bookingRepository, never()).sumPaidSeats(any());
    }

    @Test
    void lowBookingsOfPlatformTourGoToAdmins() {
        tour.setAgent(null);
        departureInSevenDays(TODAY.minusDays(20).atTime(9, 0));
        when(bookingRepository.sumPaidSeats(20L)).thenReturn(0L);

        service.remindLowBookings(20L, TODAY);

        verify(eventPublisher).publishEvent(any(NotificationEvents.AdminEmailEvent.class));
    }

    @Test
    void organizerLineShowsCountsAndMissingPassengerInfo() {
        booking.setAdults((short) 2);
        assertThat(TripReminderService.organizerLine(booking))
                .isEqualTo("TC261001123456 · Khách A · 0900000000 · 2 người lớn, 1 trẻ em: Khách A, Bé B (thiếu thông tin 1 khách)");

        booking.getPassengers().clear();
        assertThat(TripReminderService.organizerLine(booking)).endsWith("2 người lớn, 1 trẻ em (chưa có danh sách)");
    }

    @Test
    void organizerAlreadyRemindedTwiceGetsNothing() {
        departure.setStartDate(TODAY.plusDays(1));
        departure.setOrganizerReminderStage((short) 2);
        when(departureRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(departure));

        service.remindOrganizer(20L, TODAY);

        verifyNoInteractions(eventPublisher);
        verify(bookingRepository, never()).findWithPassengersByDepartureId(any(), any());
    }
}
