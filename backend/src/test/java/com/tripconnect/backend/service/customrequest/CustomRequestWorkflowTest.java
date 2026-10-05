package com.tripconnect.backend.service.customrequest;

import com.tripconnect.backend.dto.customrequest.CustomRequestResponses;
import com.tripconnect.backend.entity.CustomRequest;
import com.tripconnect.backend.entity.CustomRequestAssignment;
import com.tripconnect.backend.entity.User;
import com.tripconnect.backend.enums.AssignmentStatus;
import com.tripconnect.backend.enums.CustomRequestStatus;
import com.tripconnect.backend.repository.CustomRequestAssignmentRepository;
import com.tripconnect.backend.repository.CustomRequestRepository;
import com.tripconnect.backend.repository.UserRepository;
import com.tripconnect.backend.service.NotificationEvents;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Giao việc (Admin) và nhận / từ chối (Agent). */
@ExtendWith(MockitoExtension.class)
class CustomRequestWorkflowTest {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 4, 9, 0);
    private static final long ADMIN_ID = 1L;
    private static final long AGENT_ID = 9L;

    @Mock private CustomRequestRepository requestRepository;
    @Mock private CustomRequestAssignmentRepository assignmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private CandidateFinder candidateFinder;
    @Mock private CustomRequestAssembler assembler;
    @Mock private CustomRequestLifecycle lifecycle;
    @Mock private ApplicationEventPublisher eventPublisher;

    private CustomRequestAdminService adminService;
    private CustomRequestAgentService agentService;
    private CustomRequest request;
    private User agent;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(VN).toInstant(), VN);
        adminService = new CustomRequestAdminService(requestRepository, assignmentRepository, userRepository, candidateFinder,
                assembler, lifecycle, eventPublisher, clock);
        agentService = new CustomRequestAgentService(requestRepository, assignmentRepository, assembler, lifecycle, eventPublisher, clock);

        User customer = new User();
        customer.setId(5L);
        customer.setEmail("khach@example.com");
        agent = new User();
        agent.setId(AGENT_ID);
        agent.setEmail("agent@example.com");
        request = new CustomRequest();
        request.setId(30L);
        request.setCode("YC261004000001");
        request.setCustomer(customer);
        request.setStatus(CustomRequestStatus.NEW);
        lenient().when(requestRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(request));
        lenient().when(userRepository.getReferenceById(AGENT_ID)).thenReturn(agent);
        lenient().when(userRepository.getReferenceById(ADMIN_ID)).thenReturn(new User());
        lenient().when(userRepository.findById(AGENT_ID)).thenReturn(Optional.of(agent));
        lenient().when(lifecycle.url(any())).thenAnswer(inv -> "http://localhost:3000" + inv.getArgument(0));
        lenient().when(assembler.companyName(any())).thenReturn("Công ty A");
    }

    private static CustomRequestResponses.Candidate candidate(long agentId) {
        return new CustomRequestResponses.Candidate(agentId, 2L, "Công ty A", null, 0, BigDecimal.TEN, BigDecimal.TEN,
                new BigDecimal("72.5"), 1, 5, List.of(), null, true);
    }

    @Test
    void assign_createsPendingAssignmentWith48hDeadline_andNotifiesAgentByWebAndEmail() {
        when(candidateFinder.find(request)).thenReturn(List.of(candidate(AGENT_ID)));

        adminService.assign(ADMIN_ID, 30L, AGENT_ID);

        ArgumentCaptor<CustomRequestAssignment> saved = ArgumentCaptor.forClass(CustomRequestAssignment.class);
        verify(assignmentRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(AssignmentStatus.PENDING);
        assertThat(saved.getValue().getDeadline()).isEqualTo(NOW.plusHours(48));
        assertThat(saved.getValue().getMatchScore()).isEqualByComparingTo("72.5");
        assertThat(request.getStatus()).isEqualTo(CustomRequestStatus.WAITING_AGENT);
        assertThat(request.getAgent()).isSameAs(agent);
        verify(eventPublisher).publishEvent(any(NotificationEvents.UserWebEvent.class));
        verify(eventPublisher).publishEvent(any(NotificationEvents.UserEmailEvent.class));
    }

    @Test
    void assign_rejectsIneligibleAgentOrNonNewRequest() {
        when(candidateFinder.find(request)).thenReturn(List.of(candidate(77L)));
        assertThatThrownBy(() -> adminService.assign(ADMIN_ID, 30L, AGENT_ID)).hasMessageContaining("không đủ điều kiện");

        request.setStatus(CustomRequestStatus.IN_PROGRESS);
        assertThatThrownBy(() -> adminService.assign(ADMIN_ID, 30L, AGENT_ID)).hasMessageContaining("đang chờ giao");
        verify(assignmentRepository, never()).save(any());
    }

    private CustomRequestAssignment pendingAssignment(LocalDateTime deadline) {
        request.setStatus(CustomRequestStatus.WAITING_AGENT);
        request.setAgent(agent);
        CustomRequestAssignment a = new CustomRequestAssignment();
        a.setRequest(request);
        a.setAgent(agent);
        a.setStatus(AssignmentStatus.PENDING);
        a.setDeadline(deadline);
        when(assignmentRepository.findFirstByRequestIdAndAgentIdOrderByIdDesc(30L, AGENT_ID)).thenReturn(Optional.of(a));
        return a;
    }

    @Test
    void accept_startsWork_with72hProposalDeadline_andTellsCustomer() {
        CustomRequestAssignment a = pendingAssignment(NOW.plusHours(10));

        agentService.accept(AGENT_ID, 30L);

        assertThat(a.getStatus()).isEqualTo(AssignmentStatus.ACCEPTED);
        assertThat(request.getStatus()).isEqualTo(CustomRequestStatus.IN_PROGRESS);
        assertThat(request.getProposalDeadline()).isEqualTo(NOW.plusHours(72));
        verify(eventPublisher).publishEvent(any(NotificationEvents.UserWebEvent.class));
        verify(eventPublisher).publishEvent(any(NotificationEvents.UserEmailEvent.class));
    }

    @Test
    void decline_needsReason_returnsRequestToQueue_andTellsAdmins() {
        CustomRequestAssignment a = pendingAssignment(NOW.plusHours(10));
        assertThatThrownBy(() -> agentService.decline(AGENT_ID, 30L, " ")).hasMessageContaining("lý do");

        agentService.decline(AGENT_ID, 30L, "Không phục vụ khu vực này");

        assertThat(a.getStatus()).isEqualTo(AssignmentStatus.DECLINED);
        assertThat(a.getDeclineReason()).isEqualTo("Không phục vụ khu vực này");
        verify(lifecycle).backToQueue(request);
        verify(eventPublisher).publishEvent(any(NotificationEvents.AdminWebEvent.class));
    }

    @Test
    void cannotRespondAfter48h_orToSomeoneElsesRequest() {
        pendingAssignment(NOW.minusMinutes(1));
        assertThatThrownBy(() -> agentService.accept(AGENT_ID, 30L)).hasMessageContaining("48 giờ");

        assertThatThrownBy(() -> agentService.accept(77L, 30L)).hasMessageContaining("Không tìm thấy");
    }
}
