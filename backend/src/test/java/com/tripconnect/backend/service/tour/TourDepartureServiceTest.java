package com.tripconnect.backend.service.tour;

import com.tripconnect.backend.dto.tour.DepartureRequest;
import com.tripconnect.backend.entity.Tour;
import com.tripconnect.backend.entity.TourDeparture;
import com.tripconnect.backend.enums.DepartureStatus;
import com.tripconnect.backend.enums.TourStatus;
import com.tripconnect.backend.repository.TourDepartureRepository;
import com.tripconnect.backend.service.booking.BookingCancellation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TourDepartureServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 29);
    private static final TourActor AGENT = new TourActor(7L, false);
    private static final long TOUR_ID = 1L;

    @Mock private TourDepartureRepository departureRepository;
    @Mock private TourAccess access;
    @Mock private TourAssembler assembler;
    @Mock private TourBookingStats bookingStats;
    @Mock private BookingCancellation bookingCancellation;

    private TourDepartureService service;
    private Tour tour;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(TODAY.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant(),
                ZoneId.of("Asia/Ho_Chi_Minh"));
        service = new TourDepartureService(departureRepository, access, assembler, bookingStats, bookingCancellation, clock);

        tour = new Tour();
        tour.setId(TOUR_ID);
        tour.setDurationDays((short) 2);
        tour.setStatus(TourStatus.PUBLISHED);
        lenient().when(access.requireManageable(TOUR_ID, AGENT)).thenReturn(tour);
    }

    @Test
    void add_startDateTooSoon_isRejected() {
        assertThatThrownBy(() -> service.add(AGENT, TOUR_ID, request(TODAY.plusDays(2), 3_000_000, 2_000_000)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ít nhất 3 ngày");
        verify(departureRepository, never()).save(any());
    }

    @Test
    void add_exactlyMinimumLeadDays_isAccepted() {
        when(departureRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.add(AGENT, TOUR_ID, request(TODAY.plusDays(3), 3_000_000, 2_000_000));

        verify(departureRepository).save(argThat(d -> d.getStatus() == DepartureStatus.OPEN));
    }

    @Test
    void add_childPriceHigherThanAdult_isRejected() {
        assertThatThrownBy(() -> service.add(AGENT, TOUR_ID, request(TODAY.plusDays(10), 1_000_000, 1_500_000)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Giá trẻ em");
    }

    @Test
    void add_sameDateAsExistingDeparture_isRejected() {
        LocalDate date = TODAY.plusDays(10);
        when(departureRepository.existsActiveOnDate(TOUR_ID, date, DepartureStatus.CANCELLED, null)).thenReturn(true);

        assertThatThrownBy(() -> service.add(AGENT, TOUR_ID, request(date, 3_000_000, 2_000_000)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("đã có lịch");
    }

    @Test
    void add_onSuspendedTour_isRejected() {
        tour.setStatus(TourStatus.SUSPENDED);

        assertThatThrownBy(() -> service.add(AGENT, TOUR_ID, request(TODAY.plusDays(10), 3_000_000, 2_000_000)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("đình chỉ");
    }

    @Test
    void update_departureWithBookings_cannotChangePrice() {
        TourDeparture departure = departure(TODAY.plusDays(10), 3_000_000);
        when(departureRepository.findByIdAndTourId(5L, TOUR_ID)).thenReturn(Optional.of(departure));
        when(bookingStats.seatsBooked(5L)).thenReturn(4);

        assertThatThrownBy(() -> service.update(AGENT, TOUR_ID, 5L, request(departure.getStartDate(), 2_500_000, 1_000_000)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("đã có khách");
        assertThat(departure.getAdultPrice()).isEqualTo(3_000_000);
    }

    @Test
    void update_departureWithBookings_canIncreaseCapacityButNotBelowBooked() {
        TourDeparture departure = departure(TODAY.plusDays(10), 3_000_000);
        when(departureRepository.findByIdAndTourId(5L, TOUR_ID)).thenReturn(Optional.of(departure));
        when(bookingStats.seatsBooked(5L)).thenReturn(4);

        DepartureRequest more = request(departure.getStartDate(), 3_000_000, 2_000_000);
        more.setCapacity(30);
        service.update(AGENT, TOUR_ID, 5L, more);
        assertThat(departure.getCapacity()).isEqualTo(30);

        DepartureRequest fewer = request(departure.getStartDate(), 3_000_000, 2_000_000);
        fewer.setCapacity(3);
        assertThatThrownBy(() -> service.update(AGENT, TOUR_ID, 5L, fewer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("(4)");
    }

    @Test
    void cancel_departureAlreadyStarted_isRejected() {
        TourDeparture departure = departure(TODAY, 3_000_000);
        when(departureRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(departure));

        assertThatThrownBy(() -> service.cancel(AGENT, TOUR_ID, 5L, "Bão"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("đã tới ngày đi");
        assertThat(departure.getStatus()).isEqualTo(DepartureStatus.OPEN);
    }

    @Test
    void cancel_futureDeparture_cancelsAllBookingsWithFullRefund() {
        TourDeparture departure = departure(TODAY.plusDays(10), 3_000_000);
        when(departureRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(departure));

        service.cancel(AGENT, TOUR_ID, 5L, " Không đủ khách ");

        assertThat(departure.getStatus()).isEqualTo(DepartureStatus.CANCELLED);
        assertThat(departure.getCancelReason()).isEqualTo("Không đủ khách");
        verify(bookingCancellation).cancelAllForDeparture(5L, com.tripconnect.backend.enums.CancelledBy.AGENT, "Không đủ khách");
    }

    @Test
    void delete_departureWithBookings_isRejected() {
        TourDeparture departure = departure(TODAY.plusDays(10), 3_000_000);
        when(departureRepository.findByIdAndTourId(5L, TOUR_ID)).thenReturn(Optional.of(departure));
        when(bookingStats.departureHasBookings(5L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(AGENT, TOUR_ID, 5L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Hủy chuyến");
        verify(departureRepository, never()).delete(any());
    }

    private static DepartureRequest request(LocalDate startDate, long adultPrice, long childPrice) {
        DepartureRequest request = new DepartureRequest();
        request.setStartDate(startDate);
        request.setCapacity(20);
        request.setAdultPrice(adultPrice);
        request.setChildPrice(childPrice);
        return request;
    }

    private TourDeparture departure(LocalDate startDate, long adultPrice) {
        TourDeparture departure = new TourDeparture();
        departure.setId(5L);
        departure.setTour(tour);
        departure.setStartDate(startDate);
        departure.setCapacity(20);
        departure.setAdultPrice(adultPrice);
        departure.setChildPrice(2_000_000);
        departure.setStatus(DepartureStatus.OPEN);
        return departure;
    }
}
