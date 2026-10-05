package com.tripconnect.backend.service.customrequest;

import com.tripconnect.backend.entity.CustomProposal;
import com.tripconnect.backend.entity.CustomRequest;
import com.tripconnect.backend.entity.CustomRequestAssignment;
import com.tripconnect.backend.entity.User;
import com.tripconnect.backend.enums.AssignmentStatus;
import com.tripconnect.backend.enums.CustomRequestStatus;
import com.tripconnect.backend.enums.ProposalStatus;
import com.tripconnect.backend.repository.CustomProposalRepository;
import com.tripconnect.backend.repository.CustomRequestAssignmentRepository;
import com.tripconnect.backend.repository.CustomRequestRepository;
import com.tripconnect.backend.service.NotificationEvents;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Các hạn của phần đề xuất: Agent quá hạn gửi, đề xuất hết hạn, yêu cầu bỏ dở. */
@ExtendWith(MockitoExtension.class)
class CustomRequestLifecycleTest {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 4, 9, 0);

    @Mock private CustomRequestRepository requestRepository;
    @Mock private CustomRequestAssignmentRepository assignmentRepository;
    @Mock private CustomProposalRepository proposalRepository;
    @Mock private CustomRequestAssembler assembler;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private TransactionTemplate transactionTemplate;

    private CustomRequestLifecycle lifecycle;
    private CustomRequest request;
    private User agent;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(VN).toInstant(), VN);
        lifecycle = new CustomRequestLifecycle(requestRepository, assignmentRepository, proposalRepository, assembler, eventPublisher,
                transactionTemplate, clock, "http://localhost:3000/");
        lenient().when(transactionTemplate.execute(any()))
                .thenAnswer(inv -> inv.<TransactionCallback<?>>getArgument(0).doInTransaction(null));
        lenient().when(assembler.companyName(any())).thenReturn("Công ty A");

        User customer = new User();
        customer.setId(5L);
        customer.setEmail("khach@example.com");
        agent = new User();
        agent.setId(9L);
        request = new CustomRequest();
        request.setId(30L);
        request.setCode("YC261004000001");
        request.setCustomer(customer);
        request.setAgent(agent);
        request.setStatus(CustomRequestStatus.IN_PROGRESS);
        request.setAcceptedAt(NOW.minusDays(4));
        lenient().when(requestRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(request));
    }

    @Test
    void agentMissingProposalDeadline_isDropped_requestBackToQueue_andEveryoneNotified() {
        request.setProposalDeadline(NOW.minusMinutes(5));
        request.setRevisionCount((short) 2);
        CustomRequestAssignment accepted = new CustomRequestAssignment();
        accepted.setStatus(AssignmentStatus.ACCEPTED);
        when(requestRepository.findProposalDueBefore(NOW, false)).thenReturn(List.of(30L));
        when(assignmentRepository.findFirstByRequestIdAndStatus(30L, AssignmentStatus.ACCEPTED)).thenReturn(Optional.of(accepted));

        assertThat(lifecycle.dropOverdueAgents()).isEqualTo(1);

        assertThat(accepted.getStatus()).isEqualTo(AssignmentStatus.OVERDUE);
        assertThat(request.getStatus()).isEqualTo(CustomRequestStatus.NEW);
        assertThat(request.getAgent()).isNull();
        assertThat(request.getRevisionCount()).isZero();
        assertThat(request.getProposalDeadline()).isNull();
        verify(eventPublisher, times(2)).publishEvent(any(NotificationEvents.UserWebEvent.class));
        verify(eventPublisher).publishEvent(any(NotificationEvents.AdminWebEvent.class));
    }

    @Test
    void deadlineStillAhead_isLeftAlone() {
        request.setProposalDeadline(NOW.plusHours(1));
        when(requestRepository.findProposalDueBefore(NOW, false)).thenReturn(List.of(30L));

        assertThat(lifecycle.dropOverdueAgents()).isZero();
        assertThat(request.getStatus()).isEqualTo(CustomRequestStatus.IN_PROGRESS);
    }

    @Test
    void unansweredProposal_expires_andBothSidesNotified() {
        CustomProposal p = new CustomProposal();
        p.setAgent(agent);
        p.setStatus(ProposalStatus.SENT);
        p.setExpiresAt(NOW.minusMinutes(1));
        when(proposalRepository.findRequestIdsWithExpiredProposal(NOW)).thenReturn(List.of(30L));
        when(proposalRepository.findFirstByRequestIdAndStatus(30L, ProposalStatus.SENT)).thenReturn(Optional.of(p));

        assertThat(lifecycle.expireUnansweredProposals()).isEqualTo(1);

        assertThat(p.getStatus()).isEqualTo(ProposalStatus.EXPIRED);
        assertThat(request.getStatus()).isEqualTo(CustomRequestStatus.IN_PROGRESS);
        verify(eventPublisher, times(2)).publishEvent(any(NotificationEvents.UserWebEvent.class));
    }

    @Test
    void noActivityForFiveDays_whileWaitingCustomer_closesRequest() {
        request.setLastActivityAt(NOW.minusDays(5).minusMinutes(1));
        LocalDateTime cutoff = NOW.minusDays(5);
        when(requestRepository.findInactiveSince(cutoff)).thenReturn(List.of(30L));
        when(proposalRepository.findFirstByRequestIdAndStatus(eq(30L), any())).thenReturn(Optional.empty());

        assertThat(lifecycle.closeInactive()).isEqualTo(1);

        assertThat(request.getStatus()).isEqualTo(CustomRequestStatus.CLOSED);
        assertThat(request.getClosedReason()).contains("5 ngày");
        verify(eventPublisher).publishEvent(any(NotificationEvents.UserEmailEvent.class));
    }

    @Test
    void closingRequest_withdrawsProposalWaitingForCustomer_customerCancelMarksItDeclined() {
        CustomProposal p = new CustomProposal();
        p.setStatus(ProposalStatus.SENT);
        when(proposalRepository.findFirstByRequestIdAndStatus(30L, ProposalStatus.SENT)).thenReturn(Optional.of(p));

        lifecycle.finish(request, CustomRequestStatus.CANCELLED, "Khách hủy yêu cầu");

        assertThat(p.getStatus()).isEqualTo(ProposalStatus.DECLINED);
        assertThat(request.getStatus()).isEqualTo(CustomRequestStatus.CANCELLED);
    }
}
