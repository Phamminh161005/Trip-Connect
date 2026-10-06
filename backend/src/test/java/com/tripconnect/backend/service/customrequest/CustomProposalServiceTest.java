package com.tripconnect.backend.service.customrequest;

import com.tripconnect.backend.dto.customrequest.CustomRequestRequests;
import com.tripconnect.backend.dto.tour.TourContentRequest.ItineraryDayRequest;
import com.tripconnect.backend.entity.CustomProposal;
import com.tripconnect.backend.entity.CustomRequest;
import com.tripconnect.backend.entity.CustomRequestAssignment;
import com.tripconnect.backend.entity.User;
import com.tripconnect.backend.enums.AccommodationType;
import com.tripconnect.backend.enums.AssignmentStatus;
import com.tripconnect.backend.enums.CustomRequestStatus;
import com.tripconnect.backend.enums.ProposalStatus;
import com.tripconnect.backend.enums.TransportMode;
import com.tripconnect.backend.repository.CustomProposalRepository;
import com.tripconnect.backend.repository.CustomRequestAssignmentRepository;
import com.tripconnect.backend.repository.CustomRequestRepository;
import com.tripconnect.backend.service.NotificationEvents;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Agent gửi đề xuất; khách đồng ý / yêu cầu chỉnh sửa. */
@ExtendWith(MockitoExtension.class)
class CustomProposalServiceTest {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 4, 9, 0);
    private static final long CUSTOMER_ID = 5L;
    private static final long AGENT_ID = 9L;

    @Mock private CustomRequestRepository requestRepository;
    @Mock private CustomRequestAssignmentRepository assignmentRepository;
    @Mock private CustomProposalRepository proposalRepository;
    @Mock private CustomRequestAssembler assembler;
    @Mock private CustomRequestLifecycle lifecycle;
    @Mock private com.tripconnect.backend.service.booking.PrivateTourFactory privateTourFactory;
    @Mock private ApplicationEventPublisher eventPublisher;

    private CustomProposalService service;
    private CustomRequest request;
    private User agent;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(VN).toInstant(), VN);
        service = new CustomProposalService(requestRepository, assignmentRepository, proposalRepository, assembler, lifecycle,
                privateTourFactory, eventPublisher, clock);
        User customer = new User();
        customer.setId(CUSTOMER_ID);
        customer.setEmail("khach@example.com");
        customer.setFullName("Khách Test");
        agent = new User();
        agent.setId(AGENT_ID);
        agent.setEmail("agent@example.com");
        request = new CustomRequest();
        request.setId(30L);
        request.setCode("YC261004000001");
        request.setCustomer(customer);
        request.setAgent(agent);
        request.setStatus(CustomRequestStatus.IN_PROGRESS);
        request.setEarliestStart(LocalDate.of(2026, 10, 20));
        request.setLatestStart(LocalDate.of(2026, 11, 10));
        request.setDurationDays((short) 3);
        request.setAdults((short) 2);
        request.setChildren((short) 1);
        request.setInfants((short) 1);
        request.setProposalDeadline(NOW.plusHours(10));
        lenient().when(requestRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(request));
        lenient().when(lifecycle.url(any())).thenAnswer(inv -> "http://localhost:3000" + inv.getArgument(0));
        lenient().when(assembler.companyName(any())).thenReturn("Công ty A");
        CustomRequestAssignment mine = new CustomRequestAssignment();
        mine.setAgent(agent);
        mine.setStatus(AssignmentStatus.ACCEPTED);
        lenient().when(assignmentRepository.findFirstByRequestIdAndAgentIdOrderByIdDesc(30L, AGENT_ID)).thenReturn(Optional.of(mine));
    }

    private static CustomRequestRequests.Proposal body(LocalDate start, int days) {
        CustomRequestRequests.Proposal b = new CustomRequestRequests.Proposal();
        b.setTitle("Hà Giang 3 ngày 2 đêm cho gia đình");
        b.setStartDate(start);
        b.setDurationDays(days);
        b.setDurationNights(days - 1);
        b.setItinerary(IntStream.range(0, days).mapToObj(i -> {
            ItineraryDayRequest d = new ItineraryDayRequest();
            d.setTitle("Ngày " + (i + 1));
            d.setDescription("Nội dung");
            return d;
        }).toList());
        b.setTransportModes(Set.of(TransportMode.BUS));
        b.setAccommodationType(AccommodationType.HOMESTAY);
        b.setMeetingPoint("Nhà hát Lớn");
        b.setMeetingTime(LocalTime.of(7, 30));
        b.setIncludedServices(List.of("Xe đưa đón"));
        b.setAdultPrice(3_000_000L);
        b.setChildPrice(1_500_000L);
        return b;
    }

    @Test
    void submit_savesVersionWithTotalAndDeposit_handsTurnToCustomer_andNotifies() {
        when(proposalRepository.countByRequestIdAndAgentId(30L, AGENT_ID)).thenReturn(1L);

        service.submit(AGENT_ID, 30L, body(LocalDate.of(2026, 10, 25), 3));

        ArgumentCaptor<CustomProposal> saved = ArgumentCaptor.forClass(CustomProposal.class);
        verify(proposalRepository).save(saved.capture());
        CustomProposal p = saved.getValue();
        assertThat(p.getVersionNo()).isEqualTo((short) 2);
        assertThat(p.getStatus()).isEqualTo(ProposalStatus.SENT);
        assertThat(p.getEndDate()).isEqualTo(LocalDate.of(2026, 10, 27));
        // 2 người lớn x 3tr + 1 trẻ em x 1,5tr; trẻ sơ sinh miễn phí
        assertThat(p.getTotalPrice()).isEqualTo(7_500_000L);
        assertThat(p.getDepositAmount()).isEqualTo(2_250_000L);
        assertThat(p.getDays()).hasSize(3);
        assertThat(p.getExpiresAt()).isEqualTo(NOW.plusDays(3));
        assertThat(request.getProposalDeadline()).isNull();
        assertThat(request.getLastActivityAt()).isEqualTo(NOW);
        verify(eventPublisher).publishEvent(any(NotificationEvents.UserWebEvent.class));
        verify(eventPublisher).publishEvent(any(NotificationEvents.UserEmailEvent.class));
    }

    @Test
    void submit_rejectsWhenNotAgentsTurn_orPastDeadline() {
        request.setProposalDeadline(null);
        assertThatThrownBy(() -> service.submit(AGENT_ID, 30L, body(LocalDate.of(2026, 10, 25), 3)))
                .hasMessageContaining("không ở bước chờ bạn");

        request.setProposalDeadline(NOW.minusMinutes(1));
        assertThatThrownBy(() -> service.submit(AGENT_ID, 30L, body(LocalDate.of(2026, 10, 25), 3)))
                .hasMessageContaining("quá hạn");
        verify(proposalRepository, never()).save(any());
    }

    @Test
    void validate_startInsideCustomerWindowAndLeadTime_itineraryMatchesDays_childNotAboveAdult() {
        LocalDate today = NOW.toLocalDate();
        assertThatThrownBy(() -> CustomProposalService.validate(body(LocalDate.of(2026, 10, 19), 3), request, today))
                .hasMessageContaining("trong khoảng khách mong muốn");
        assertThatThrownBy(() -> CustomProposalService.validate(body(LocalDate.of(2026, 10, 25), 3), request,
                LocalDate.of(2026, 10, 20))).hasMessageContaining("cần ít nhất 7 ngày");

        CustomRequestRequests.Proposal wrongDays = body(LocalDate.of(2026, 10, 25), 3);
        wrongDays.setItinerary(wrongDays.getItinerary().subList(0, 2));
        assertThatThrownBy(() -> CustomProposalService.validate(wrongDays, request, today)).hasMessageContaining("lịch trình");

        CustomRequestRequests.Proposal pricey = body(LocalDate.of(2026, 10, 25), 3);
        pricey.setChildPrice(4_000_000L);
        assertThatThrownBy(() -> CustomProposalService.validate(pricey, request, today)).hasMessageContaining("Giá trẻ em");
    }

    @Test
    void deposit_is30PercentRoundedUpToThousand() {
        assertThat(CustomRequestRules.deposit(7_500_000)).isEqualTo(2_250_000);
        assertThat(CustomRequestRules.deposit(1_000_001)).isEqualTo(301_000);
        assertThat(CustomRequestRules.deposit(500)).isEqualTo(500);
    }

    private CustomProposal sent(long id) {
        CustomProposal p = new CustomProposal();
        p.setId(id);
        p.setRequest(request);
        p.setAgent(agent);
        p.setStatus(ProposalStatus.SENT);
        p.setTitle("Hà Giang");
        p.setStartDate(LocalDate.of(2026, 10, 25));
        p.setTotalPrice(7_500_000);
        p.setExpiresAt(NOW.plusDays(2));
        request.setProposalDeadline(null);
        when(proposalRepository.findFirstByRequestIdAndAgentIdOrderByIdDesc(30L, AGENT_ID)).thenReturn(Optional.of(p));
        return p;
    }

    @Test
    void accept_agreesRequest_createsPrivateBooking_andTellsBothSides() {
        CustomProposal p = sent(70L);
        com.tripconnect.backend.entity.Booking booking = new com.tripconnect.backend.entity.Booking();
        booking.setId(500L);
        booking.setCode("TC261004000001");
        booking.setStatus(com.tripconnect.backend.enums.BookingStatus.PENDING_PAYMENT);
        booking.setTotalAmount(7_500_000);
        booking.setDepositAmount(2_250_000);
        booking.setHoldExpiresAt(NOW.plusHours(48));
        when(privateTourFactory.create(request, p, NOW)).thenReturn(booking);

        service.accept(CUSTOMER_ID, 30L, 70L);

        assertThat(p.getStatus()).isEqualTo(ProposalStatus.ACCEPTED);
        assertThat(request.getStatus()).isEqualTo(CustomRequestStatus.AGREED);
        assertThat(request.getAgreedAt()).isEqualTo(NOW);
        verify(privateTourFactory).create(request, p, NOW);
        verify(eventPublisher).publishEvent(any(NotificationEvents.UserWebEvent.class));
        // Agent: đã đồng ý; khách: đơn đã tạo, cần đặt cọc
        verify(eventPublisher, times(2)).publishEvent(any(NotificationEvents.UserEmailEvent.class));
    }

    @Test
    void accept_rejectsOldVersion_expiredProposal_andOtherCustomers() {
        CustomProposal p = sent(70L);
        assertThatThrownBy(() -> service.accept(CUSTOMER_ID, 30L, 69L)).hasMessageContaining("phiên bản đề xuất mới hơn");
        assertThatThrownBy(() -> service.accept(99L, 30L, 70L)).hasMessageContaining("Không tìm thấy");

        p.setStatus(ProposalStatus.EXPIRED);
        assertThatThrownBy(() -> service.accept(CUSTOMER_ID, 30L, 70L)).hasMessageContaining("không còn chờ");
        assertThat(request.getStatus()).isEqualTo(CustomRequestStatus.IN_PROGRESS);
    }

    @Test
    void requestRevision_givesAgent48h_countsRounds_evenOnExpiredProposal() {
        CustomProposal p = sent(70L);
        p.setStatus(ProposalStatus.EXPIRED);

        service.requestRevision(CUSTOMER_ID, 30L, 70L, "Đổi sang khách sạn 4 sao");

        assertThat(p.getStatus()).isEqualTo(ProposalStatus.REVISION_REQUESTED);
        assertThat(p.getCustomerFeedback()).isEqualTo("Đổi sang khách sạn 4 sao");
        assertThat(request.getRevisionCount()).isEqualTo((short) 1);
        assertThat(request.getProposalDeadline()).isEqualTo(NOW.plusHours(48));
        verify(eventPublisher).publishEvent(any(NotificationEvents.UserWebEvent.class));
    }

    @Test
    void requestRevision_needsFeedback_andStopsAfterFiveRounds() {
        sent(70L);
        assertThatThrownBy(() -> service.requestRevision(CUSTOMER_ID, 30L, 70L, " ")).hasMessageContaining("muốn chỉnh sửa gì");

        request.setRevisionCount((short) 5);
        assertThatThrownBy(() -> service.requestRevision(CUSTOMER_ID, 30L, 70L, "Sửa giá")).hasMessageContaining("Đã hết 5 lượt");
    }
}
