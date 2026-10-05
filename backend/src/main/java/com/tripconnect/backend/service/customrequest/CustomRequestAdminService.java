package com.tripconnect.backend.service.customrequest;

import com.tripconnect.backend.dto.PageResponse;
import com.tripconnect.backend.dto.customrequest.CustomRequestResponses;
import com.tripconnect.backend.entity.CustomRequest;
import com.tripconnect.backend.entity.CustomRequestAssignment;
import com.tripconnect.backend.enums.AssignmentStatus;
import com.tripconnect.backend.enums.CustomRequestStatus;
import com.tripconnect.backend.exception.ResourceNotFoundException;
import com.tripconnect.backend.repository.*;
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
import java.util.List;

/** Admin: xem yêu cầu, xem Agent gợi ý, giao việc, đóng yêu cầu. */
@Service
@RequiredArgsConstructor
public class CustomRequestAdminService {

    private final CustomRequestRepository requestRepository;
    private final CustomRequestAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final CandidateFinder candidateFinder;
    private final CustomRequestAssembler assembler;
    private final CustomRequestLifecycle lifecycle;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<CustomRequestResponses.Summary> list(CustomRequestStatus status, String q, Pageable pageable) {
        Specification<CustomRequest> spec = (root, query, cb) -> cb.conjunction();
        if (status != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        if (q != null && !q.isBlank()) {
            String pattern = SearchPatterns.contains(q);
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("code")), pattern, SearchPatterns.ESCAPE),
                    cb.like(cb.lower(root.get("customer").get("fullName")), pattern, SearchPatterns.ESCAPE),
                    cb.like(cb.lower(root.get("customer").get("email")), pattern, SearchPatterns.ESCAPE)));
        }
        var page = requestRepository.findAll(spec, StablePaging.of(pageable));
        return PageResponse.of(page, assembler.toSummaries(page.getContent(), CustomRequestAssembler.Viewer.ADMIN));
    }

    @Transactional(readOnly = true)
    public CustomRequestResponses.Detail get(Long requestId) {
        CustomRequest request = require(requestId);
        return assembler.toDetail(request, CustomRequestAssembler.Viewer.ADMIN, null,
                CustomRequestRules.OPEN_STATUSES.contains(request.getStatus()), false);
    }

    @Transactional(readOnly = true)
    public List<CustomRequestResponses.Candidate> candidates(Long requestId) {
        return candidateFinder.find(require(requestId));
    }

    @Transactional
    public CustomRequestResponses.Detail assign(Long adminId, Long requestId, Long agentId) {
        CustomRequest request = requestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu"));
        if (request.getStatus() != CustomRequestStatus.NEW) {
            throw new IllegalStateException("Chỉ giao được yêu cầu đang chờ giao");
        }
        // Kiểm tra lại điều kiện ngay lúc giao (Agent có thể vừa tắt nhận yêu cầu / vừa nhận đủ việc)
        CustomRequestResponses.Candidate candidate = candidateFinder.find(request).stream()
                .filter(c -> c.agentId().equals(agentId)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Đơn vị này hiện không đủ điều kiện nhận yêu cầu, vui lòng chọn đơn vị khác"));

        LocalDateTime now = LocalDateTime.now(clock);
        CustomRequestAssignment assignment = new CustomRequestAssignment();
        assignment.setRequest(request);
        assignment.setAgent(userRepository.getReferenceById(agentId));
        assignment.setAssignedBy(userRepository.getReferenceById(adminId));
        assignment.setStatus(AssignmentStatus.PENDING);
        assignment.setMatchScore(candidate.totalScore());
        assignment.setDeadline(now.plusHours(CustomRequestRules.ACCEPT_HOURS));
        assignment.setCreatedAt(now);
        assignmentRepository.save(assignment);

        request.setStatus(CustomRequestStatus.WAITING_AGENT);
        request.setAgent(assignment.getAgent());
        request.setAssignedAt(now);

        String destinations = CustomRequestAssembler.destinationsText(request);
        String path = "/agent/requests/" + request.getId();
        eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(agentId, WebNotifications.customRequestAssigned(
                request.getId(), request.getCode(), destinations, CustomRequestRules.ACCEPT_HOURS)));
        eventPublisher.publishEvent(new NotificationEvents.UserEmailEvent(userRepository.findById(agentId).orElseThrow().getEmail(),
                EmailTemplates.customRequestAssigned(request.getCode(), destinations, assignment.getDeadline(), lifecycle.url(path))));
        return assembler.toDetail(request, CustomRequestAssembler.Viewer.ADMIN, null, true, false);
    }

    @Transactional
    public CustomRequestResponses.Detail close(Long requestId, String reason) {
        CustomRequest request = requestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu"));
        if (!CustomRequestRules.OPEN_STATUSES.contains(request.getStatus())) {
            throw new IllegalStateException("Yêu cầu đã kết thúc");
        }
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Vui lòng nhập lý do đóng yêu cầu");
        lifecycle.finish(request, CustomRequestStatus.CLOSED, reason.trim());
        return assembler.toDetail(request, CustomRequestAssembler.Viewer.ADMIN, null, false, false);
    }

    private CustomRequest require(Long requestId) {
        return requestRepository.findById(requestId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu"));
    }
}
