package com.tripconnect.backend.service.booking;

import com.tripconnect.backend.dto.booking.CreateBookingRequest;
import com.tripconnect.backend.dto.booking.PassengerRequest;
import com.tripconnect.backend.entity.*;
import com.tripconnect.backend.enums.*;
import com.tripconnect.backend.exception.ForbiddenException;
import com.tripconnect.backend.repository.*;
import com.tripconnect.backend.service.tour.TourBookingStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 3, 10, 0);
    private static final LocalDate START = LocalDate.of(2026, 11, 15);
    private static final long CUSTOMER_ID = 5L;
    private static final long AGENT_ID = 99L;

    @Mock private BookingRepository bookingRepository;
    @Mock private TourDepartureRepository departureRepository;
    @Mock private UserRepository userRepository;
    @Mock private AgentProfileRepository agentProfileRepository;
    @Mock private TourBookingStats bookingStats;
    @Mock private PaymentService paymentService;
    @Mock private BookingCancellation cancellation;
    @Mock private BookingAssembler assembler;

    private BookingService service;
    private TourDeparture departure;
    private Tour tour;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(VN).toInstant(), VN);
        BookingSettings settings = new BookingSettings(new BigDecimal("0.10"), 30, 15, 7, 3, 50);
        service = new BookingService(bookingRepository, departureRepository, userRepository, agentProfileRepository,
                bookingStats, paymentService, cancellation, assembler, settings, clock);

        User agent = new User();
        agent.setId(AGENT_ID);
        agent.setActive(true);
        tour = new Tour();
        tour.setId(1L);
        tour.setAgent(agent);
        tour.setStatus(TourStatus.PUBLISHED);
        departure = new TourDeparture();
        departure.setId(10L);
        departure.setTour(tour);
        departure.setStartDate(START);
        departure.setCapacity(20);
        departure.setAdultPrice(4_000_000);
        departure.setChildPrice(3_000_000);
        departure.setStatus(DepartureStatus.OPEN);

        AgentProfile profile = new AgentProfile();
        profile.setStatus(AgentStatus.APPROVED);
        lenient().when(departureRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(departure));
        lenient().when(agentProfileRepository.findByUserId(AGENT_ID)).thenReturn(Optional.of(profile));
        lenient().when(bookingRepository.findFirstByCustomerIdAndDepartureIdAndStatusAndHoldExpiresAtAfter(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        lenient().when(paymentService.createPaymentUrl(any(), any())).thenReturn("https://vnpay/pay");
    }

    private Booking savedBooking() {
        ArgumentCaptor<Booking> saved = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(saved.capture());
        return saved.getValue();
    }

    @Test
    void create_familyBooking_pricesByTypeAndSavesEveryPassenger() {
        var result = service.create(CUSTOMER_ID, "CUSTOMER", request(2, 1, 1), "1.2.3.4");

        Booking booking = savedBooking();
        assertThat(booking.getAdults()).isEqualTo((short) 2);
        assertThat(booking.getChildren()).isEqualTo((short) 1);
        assertThat(booking.getInfants()).isEqualTo((short) 1);
        assertThat(booking.getPassengers()).extracting(BookingPassenger::getType)
                .containsExactly(PassengerType.ADULT, PassengerType.ADULT, PassengerType.CHILD, PassengerType.INFANT);
        assertThat(booking.getTotalAmount()).isEqualTo(2 * 4_000_000 + 3_000_000);
        assertThat(booking.getCommissionAmount()).isEqualTo(1_100_000);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(booking.getHoldExpiresAt()).isEqualTo(NOW.plusMinutes(15));
        assertThat(booking.getCode()).startsWith("TC261003");
        assertThat(result.paymentUrl()).isEqualTo("https://vnpay/pay");
    }

    @Test
    void create_incompleteList_isRejected() {
        assertThatThrownBy(() -> service.create(CUSTOMER_ID, "CUSTOMER", request(2, 1, 0, adult("Nguyễn Văn A")), "ip"))
                .hasMessageContaining("nhập đủ thông tin 3 hành khách");
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void create_listNotMatchingBookedTypes_isRejected() {
        // Đặt 1 người lớn + 1 trẻ em, nhưng ngày sinh "trẻ em" lại là người lớn -> giá sai, phải chặn
        assertThatThrownBy(() -> service.create(CUSTOMER_ID, "CUSTOMER", request(1, 1, 0, adult("A"), adult("B")), "ip"))
                .hasMessageContaining("Danh sách có 2 người lớn (từ 12 tuổi) nhưng đơn đặt 1");
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void create_moreSeatsThanAvailable_isRejected_butInfantsDoNotTakeSeats() {
        departure.setCapacity(3);
        when(bookingStats.seatsBooked(10L)).thenReturn(1);

        assertThatThrownBy(() -> service.create(CUSTOMER_ID, "CUSTOMER", request(3, 0, 0), "ip"))
                .hasMessageContaining("chỉ còn 2 chỗ");

        service.create(CUSTOMER_ID, "CUSTOMER", request(2, 0, 1), "ip");
        verify(bookingRepository).save(any());
    }

    @Test
    void create_invalidGroups_areRejected() {
        assertThatThrownBy(() -> service.create(CUSTOMER_ID, "CUSTOMER", request(0, 2, 0), "ip"))
                .hasMessageContaining("ít nhất 1 người lớn");
        assertThatThrownBy(() -> service.create(CUSTOMER_ID, "CUSTOMER", request(1, 0, 2), "ip"))
                .hasMessageContaining("trẻ sơ sinh");
        assertThatThrownBy(() -> service.create(CUSTOMER_ID, "CUSTOMER", request(20, 11, 0), "ip"))
                .hasMessageContaining("tối đa 30 khách");
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void create_internationalTourWithoutPassport_isRejected() {
        tour.setInternational(true);
        assertThatThrownBy(() -> service.create(CUSTOMER_ID, "CUSTOMER", request(1, 0, 0, adult("A")), "ip"))
                .hasMessageContaining("hộ chiếu");
    }

    @Test
    void agentCannotBookOwnTour_andAdminCannotBook() {
        assertThatThrownBy(() -> service.create(AGENT_ID, "AGENT", request(1, 0, 0), "ip"))
                .hasMessageContaining("chính mình");
        assertThatThrownBy(() -> service.create(1L, "ADMIN", request(1, 0, 0), "ip"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void create_closedOrHiddenTour_isRejected() {
        departure.setStatus(DepartureStatus.CLOSED);
        assertThatThrownBy(() -> service.create(CUSTOMER_ID, "CUSTOMER", request(1, 0, 0), "ip"))
                .hasMessageContaining("không còn nhận đặt chỗ");

        departure.setStatus(DepartureStatus.OPEN);
        tour.setStatus(TourStatus.HIDDEN);
        assertThatThrownBy(() -> service.create(CUSTOMER_ID, "CUSTOMER", request(1, 0, 0), "ip"))
                .hasMessageContaining("không nhận đặt chỗ");
    }

    // ===================== Sửa danh sách hành khách =====================

    private Booking paidBooking() {
        User customer = new User();
        customer.setId(CUSTOMER_ID);
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setCustomer(customer);
        booking.setTour(tour);
        booking.setDeparture(departure);
        booking.setStatus(BookingStatus.PAID);
        booking.setAdults((short) 2);
        booking.setChildren((short) 1);
        when(bookingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));
        return booking;
    }

    @Test
    void updatePassengers_beforeDeadline_replacesList_butMustStayComplete() {
        Booking booking = paidBooking();
        PassengerRequest child = passenger("Bé", LocalDate.of(2018, 1, 1));

        service.updatePassengers(CUSTOMER_ID, 1L, List.of(adult("A sửa tên"), adult("B"), child));
        assertThat(booking.getPassengers()).extracting(BookingPassenger::getFullName).containsExactly("A sửa tên", "B", "Bé");

        assertThatThrownBy(() -> service.updatePassengers(CUSTOMER_ID, 1L, List.of(adult("A"), child)))
                .hasMessageContaining("nhập đủ thông tin 3 hành khách");
    }

    @Test
    void updatePassengers_afterDeadline_isRejected() {
        paidBooking();
        departure.setStartDate(NOW.toLocalDate().plusDays(2));

        assertThatThrownBy(() -> service.updatePassengers(CUSTOMER_ID, 1L, List.of(adult("A"))))
                .hasMessageContaining("quá hạn");
    }

    @Test
    void updatePassengers_cancelledOrSomeoneElsesBooking_isRejected() {
        Booking booking = paidBooking();
        booking.setStatus(BookingStatus.CANCELLED);
        assertThatThrownBy(() -> service.updatePassengers(CUSTOMER_ID, 1L, List.of(adult("A"))))
                .hasMessageContaining("không ở trạng thái");

        assertThatThrownBy(() -> service.updatePassengers(77L, 1L, List.of(adult("A"))))
                .hasMessageContaining("Không tìm thấy");
    }

    /** Không truyền hành khách -> tự sinh danh sách đúng số khách từng loại. */
    private static CreateBookingRequest request(int adults, int children, int infants, PassengerRequest... passengers) {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setDepartureId(10L);
        request.setContactName("Nguyễn Văn A");
        request.setContactPhone("0912345678");
        request.setContactEmail("A@Example.com");
        request.setAdults(adults);
        request.setChildren(children);
        request.setInfants(infants);
        List<PassengerRequest> list = new ArrayList<>(List.of(passengers));
        if (list.isEmpty()) {
            for (int i = 0; i < adults; i++) list.add(adult("Người lớn " + i));
            for (int i = 0; i < children; i++) list.add(passenger("Trẻ em " + i, LocalDate.of(2018, 5, 1)));
            for (int i = 0; i < infants; i++) list.add(passenger("Trẻ sơ sinh " + i, LocalDate.of(2026, 1, 1)));
        }
        request.setPassengers(list);
        return request;
    }

    private static PassengerRequest adult(String name) {
        return passenger(name, LocalDate.of(1990, 1, 1));
    }

    private static PassengerRequest passenger(String name, LocalDate dob) {
        PassengerRequest p = new PassengerRequest();
        p.setFullName(name);
        p.setDateOfBirth(dob);
        return p;
    }
}
