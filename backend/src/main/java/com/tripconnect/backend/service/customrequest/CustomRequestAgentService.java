package com.tripconnect.backend.service.customrequest;

import com.tripconnect.backend.dto.PageResponse;
import com.tripconnect.backend.dto.customrequest.CustomRequestResponses;
import com.tripconnect.backend.entity.CustomRequest;
import com.tripconnect.backend.entity.CustomRequestAssignment;
import com.tripconnect.backend.enums.AssignmentStatus;
import com.tripconnect.backend.enums.CustomRequestStatus;
import com.tripconnect.backend.exception.ResourceNotFoundException;
import com.tripconnect.backend.repository.CustomRequestAssignmentRepository;
import com.tripconnect.backend.repository.CustomRequestRepository;
import com.tripconnect.backend.repository.StablePaging;
import com.tripconnect.backend.service.EmailTemplates;
import com.tripconnect.backend.service.NotificationEvents;
import com.tripconnect.backend.service.WebNotifications;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/** Agent: yêu cầu được giao — nhận (trong 48 giờ) hoặc từ chối. */
@Service
@RequiredArgsConstructor
public class CustomRequestAgentService {

    /** Bộ lọc danh sách của Agent. */
    public enum Tab { PENDING, ACCEPTED, HISTORY }

    private final CustomRequestRepository requestRepository;
    private final CustomRequestAssignmentRepository assignmentRepository;
    private final CustomRequestAssembler assembler;
    private final CustomRequestLifecycle lifecycle;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<CustomRequestResponses.Summary> list(Long agentId, Tab tab, Pageable pageable) {
        Set<AssignmentStatus> statuses = switch (tab == null ? Tab.PENDING : tab) {
            case PENDING -> EnumSet.of(AssignmentStatus.PENDING);
            case ACCEPTED -> EnumSet.of(AssignmentStatus.ACCEPTED);
            case HISTORY -> EnumSet.of(AssignmentStatus.DECLINED, AssignmentStatus.EXPIRED, AssignmentStatus.REVOKED,
                    AssignmentStatus.OVERDUE);
        };
        Specification<CustomRequestAssignment> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("agent").get("id"), agentId), root.get("status").in(statuses));
        var page = assignmentRepository.findAll(spec, StablePaging.of(pageable));
        return PageResponse.of(page, assembler.toAgentSummaries(page.getContent(), agentId));
    }

    @Transactional(readOnly = true)
    public CustomRequestResponses.Detail get(Long agentId, Long requestId) {
        CustomRequestAssignment mine = requireMine(agentId, requestId);
        CustomRequest request = mine.getRequest();
        return assembler.toDetail(request, CustomRequestAssembler.Viewer.AGENT, mine, false, canRespond(mine, request));
    }

    @Transactional
    public CustomRequestResponses.Detail accept(Long agentId, Long requestId) {
        LocalDateTime now = LocalDateTime.now(clock);
        CustomRequest request = lockRequest(requestId);
        CustomRequestAssignment mine = requirePending(agentId, request, now);
        mine.setStatus(AssignmentStatus.ACCEPTED);
        mine.setRespondedAt(now);
        request.setStatus(CustomRequestStatus.IN_PROGRESS);
        request.setAcceptedAt(now);
        request.setProposalDeadline(now.plusHours(CustomRequestRules.PROPOSAL_HOURS));
        request.setDeadlineReminded(false);
        request.setLastActivityAt(now);

        String companyName = assembler.companyName(mine.getAgent());
        String path = "/account/requests/" + request.getId();
        eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(request.getCustomer().getId(),
                WebNotifications.customRequestAccepted(request.getId(), request.getCode(), companyName, CustomRequestRules.PROPOSAL_HOURS)));
        eventPublisher.publishEvent(new NotificationEvents.UserEmailEvent(request.getCustomer().getEmail(),
                EmailTemplates.customRequestAccepted(request.getCode(), companyName, CustomRequestRules.PROPOSAL_HOURS, lifecycle.url(path))));
        return assembler.toDetail(request, CustomRequestAssembler.Viewer.AGENT, mine, false, false);
    }

    @Transactional
    public CustomRequestResponses.Detail decline(Long agentId, Long requestId, String reason) {
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Vui lòng nhập lý do từ chối");
        LocalDateTime now = LocalDateTime.now(clock);
        CustomRequest request = lockRequest(requestId);
        CustomRequestAssignment mine = requirePending(agentId, request, now);
        mine.setStatus(AssignmentStatus.DECLINED);
        mine.setDeclineReason(reason.trim());
        mine.setRespondedAt(now);
        lifecycle.backToQueue(request);
        eventPublisher.publishEvent(new NotificationEvents.AdminWebEvent(WebNotifications.customRequestNeedsReassign(
                request.getId(), request.getCode(), assembler.companyName(mine.getAgent()) + " đã từ chối")));
        return assembler.toDetail(request, CustomRequestAssembler.Viewer.AGENT, mine, false, false);
    }

    private static boolean canRespond(CustomRequestAssignment mine, CustomRequest request) {
        return mine.getStatus() == AssignmentStatus.PENDING && request.getStatus() == CustomRequestStatus.WAITING_AGENT;
    }

    private CustomRequest lockRequest(Long requestId) {
        return requestRepository.findByIdForUpdate(requestId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu"));
    }

    /** Lần giao gần nhất cho Agent này (Agent chỉ xem được yêu cầu từng giao cho mình). */
    private CustomRequestAssignment requireMine(Long agentId, Long requestId) {
        return assignmentRepository.findFirstByRequestIdAndAgentIdOrderByIdDesc(requestId, agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu"));
    }

    private CustomRequestAssignment requirePending(Long agentId, CustomRequest request, LocalDateTime now) {
        CustomRequestAssignment mine = requireMine(agentId, request.getId());
        if (!canRespond(mine, request)) {
            throw new IllegalStateException("Yêu cầu không còn chờ bạn phản hồi");
        }
        if (!mine.getDeadline().isAfter(now)) {
            throw new IllegalStateException("Đã quá " + CustomRequestRules.ACCEPT_HOURS + " giờ để nhận yêu cầu này");
        }
        return mine;
    }
}
