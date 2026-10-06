package com.tripconnect.backend.service.booking;

import com.tripconnect.backend.entity.*;
import com.tripconnect.backend.enums.*;
import com.tripconnect.backend.repository.BookingRepository;
import com.tripconnect.backend.repository.TourDepartureRepository;
import com.tripconnect.backend.repository.TourRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Hạn thanh toán tour riêng và việc tạo tour + đơn khi khách đồng ý đề xuất. */
@ExtendWith(MockitoExtension.class)
class PrivateTourFactoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 6, 9, 0);

    @Mock private TourRepository tourRepository;
    @Mock private TourDepartureRepository departureRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private BookingService bookingService;

    private PrivateTourFactory factory;
    private CustomRequest request;
    private CustomProposal proposal;
    private Location hagiang;

    @BeforeEach
    void setUp() {
        factory = new PrivateTourFactory(tourRepository, departureRepository, bookingRepository, bookingService,
                new BookingSettings(new BigDecimal("0.10"), 30, 15, 7, 3, 50));
        lenient().when(bookingService.newCode(any())).thenReturn("TC261006000001");
        lenient().when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        hagiang = new Location();
        hagiang.setId(6L);
        hagiang.setCountry("Việt Nam");
        hagiang.setProvince("Tỉnh Hà Giang");
        User customer = new User();
        customer.setId(5L);
        customer.setEmail("Lan@Example.com");
        customer.setFullName("Nguyễn Thị Lan");
        User agent = new User();
        agent.setId(9L);
        request = new CustomRequest();
        request.setId(30L);
        request.setCustomer(customer);
        request.setDepartureLocation(hagiang);
        request.getDestinations().add(hagiang);
        request.setAdults((short) 2);
        request.setChildren((short) 1);
        request.setInfants((short) 1);

        proposal = new CustomProposal();
        proposal.setAgent(agent);
        proposal.setTitle("Hà Giang mùa tam giác mạch");
        proposal.setStartDate(LocalDate.of(2026, 11, 5));
        proposal.setDurationDays((short) 2);
        proposal.setDurationNights((short) 1);
        proposal.setAccommodationType(AccommodationType.HOMESTAY);
        proposal.setTransportModes(Set.of(TransportMode.BUS));
        proposal.setMeetingPoint("Nhà hát Lớn");
        proposal.setMeetingTime(LocalTime.of(6, 30));
        proposal.setIncludedServices(List.of("Xe 16 chỗ"));
        proposal.setAdultPrice(2_600_000);
        proposal.setChildPrice(2_000_000);
        proposal.setTotalPrice(7_200_000);
        proposal.setDepositAmount(2_160_000);
        for (int i = 1; i <= 2; i++) {
            CustomProposalDay day = new CustomProposalDay();
            day.setDayNumber((short) i);
            day.setTitle("Ngày " + i);
            day.setDescription("Nội dung");
            proposal.getDays().add(day);
        }
    }

    @Test
    void rules_balanceDueBeforeTrip_extensionCappedBeforeDeparture() {
        LocalDate start = LocalDate.of(2026, 11, 5);
        assertThat(BookingRules.balanceDueDate(start, false)).isEqualTo(LocalDate.of(2026, 10, 29));
        assertThat(BookingRules.balanceDueDate(start, true)).isEqualTo(LocalDate.of(2026, 10, 21));
        assertThat(BookingRules.extendedDueDate(LocalDate.of(2026, 10, 29), start)).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(BookingRules.extendedDueDate(LocalDate.of(2026, 11, 3), start)).isEqualTo(LocalDate.of(2026, 11, 4));
        // Hạn cọc 08/10: hạn trả nốt 29/10 đủ xa -> chia 2 lần; 10/10 thì quá sát -> trả 1 lần
        assertThat(BookingRules.splitPayment(LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 29))).isTrue();
        assertThat(BookingRules.splitPayment(LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 10))).isFalse();
    }

    @Test
    void rules_customerCancellingAfterFullPayment_neverGetsDepositBack() {
        Booking b = new Booking();
        b.setTotalAmount(7_200_000);
        b.setDepositAmount(2_160_000);
        b.setRefundFullDays((short) 7);
        b.setRefundPartialDays((short) 3);
        b.setRefundPartialPercent((short) 50);

        assertThat(BookingRules.customerRefund(b, LocalDate.of(2026, 11, 5), LocalDate.of(2026, 10, 20)).amount())
                .isEqualTo(5_040_000);
        assertThat(BookingRules.customerRefund(b, LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 1)).amount())
                .isEqualTo(2_520_000);
    }

    @Test
    void create_buildsPrivateTourAndClosedDeparture_andBookingAwaitingDeposit() {
        Booking booking = factory.create(request, proposal, NOW);

        ArgumentCaptor<Tour> tour = ArgumentCaptor.forClass(Tour.class);
        verify(tourRepository).save(tour.capture());
        assertThat(tour.getValue().getStatus()).isEqualTo(TourStatus.PRIVATE);
        assertThat(tour.getValue().getCustomRequestId()).isEqualTo(30L);
        assertThat(tour.getValue().getItineraryDays()).hasSize(2);
        assertThat(tour.getValue().isInternational()).isFalse();
        ArgumentCaptor<TourDeparture> departure = ArgumentCaptor.forClass(TourDeparture.class);
        verify(departureRepository).save(departure.capture());
        assertThat(departure.getValue().getStatus()).isEqualTo(DepartureStatus.CLOSED);
        assertThat(departure.getValue().getCapacity()).isEqualTo(3);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(booking.getTotalAmount()).isEqualTo(7_200_000);
        assertThat(booking.getDepositAmount()).isEqualTo(2_160_000);
        assertThat(booking.nextPaymentAmount()).isEqualTo(2_160_000);
        assertThat(booking.getHoldExpiresAt()).isEqualTo(NOW.plusHours(48));
        assertThat(booking.getBalanceDueDate()).isEqualTo(LocalDate.of(2026, 10, 29));
        assertThat(booking.getContactEmail()).isEqualTo("lan@example.com");
        assertThat(booking.getPassengers()).isEmpty();
    }

    @Test
    void create_tripTooSoonForTwoPayments_paysEverythingAtOnce() {
        proposal.setStartDate(LocalDate.of(2026, 10, 14));

        Booking booking = factory.create(request, proposal, NOW);

        assertThat(booking.getDepositAmount()).isZero();
        assertThat(booking.getBalanceDueDate()).isNull();
        assertThat(booking.nextPaymentAmount()).isEqualTo(7_200_000);
    }
}
