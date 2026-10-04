package com.tripconnect.backend.service;

import com.tripconnect.backend.dto.AgentProfileResponse;
import com.tripconnect.backend.dto.AgentProfileSummaryResponse;
import com.tripconnect.backend.dto.PageResponse;
import com.tripconnect.backend.dto.TemporaryUrlResponse;
import com.tripconnect.backend.entity.AgentDocument;
import com.tripconnect.backend.entity.AgentProfile;
import com.tripconnect.backend.entity.User;
import com.tripconnect.backend.enums.AgentStatus;
import com.tripconnect.backend.repository.StablePaging;
import com.tripconnect.backend.repository.SearchPatterns;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import com.tripconnect.backend.exception.ResourceNotFoundException;
import com.tripconnect.backend.repository.AgentDocumentRepository;
import com.tripconnect.backend.repository.AgentProfileRepository;
import com.tripconnect.backend.repository.UserRepository;
import com.tripconnect.backend.service.agent.AgentDocumentStorage;
import com.tripconnect.backend.service.agent.AgentProfileAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Admin xem và duyệt hồ sơ Agent. */
@Service
@RequiredArgsConstructor
public class AgentAdminService {

    private final AgentProfileRepository agentProfileRepository;
    private final AgentDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final AgentProfileAssembler assembler;
    private final AgentDocumentStorage documentStorage;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * @param status  null = tất cả trạng thái
     * @param keyword tìm gần đúng theo tên công ty, email hoặc mã số thuế; null = không lọc
     */
    @Transactional(readOnly = true)
    public PageResponse<AgentProfileSummaryResponse> listProfiles(AgentStatus status, String keyword, Pageable pageable) {
        Specification<AgentProfile> spec = (root, query, cb) -> cb.conjunction();
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (keyword != null && !keyword.isBlank()) {
            String pattern = SearchPatterns.contains(keyword);
            spec = spec.and((root, query, cb) -> {
                Join<AgentProfile, User> user = root.join("user");
                return cb.or(
                        cb.like(cb.lower(root.get("companyName")), pattern, SearchPatterns.ESCAPE),
                        cb.like(cb.lower(user.get("email")), pattern, SearchPatterns.ESCAPE),
                        cb.like(root.get("taxCode"), pattern, SearchPatterns.ESCAPE));
            });
        }
        return PageResponse.from(agentProfileRepository.findAll(spec, StablePaging.of(pageable)), AgentProfileAssembler::toSummary);
    }

    @Transactional(readOnly = true)
    public AgentProfileResponse getProfile(Long profileId) {
        return assembler.toResponse(requireProfile(profileId));
    }

    @Transactional(readOnly = true)
    public TemporaryUrlResponse getDocumentUrl(Long profileId, Long documentId) {
        AgentDocument document = documentRepository.findByIdAndAgentProfileId(documentId, profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giấy tờ"));
        return documentStorage.temporaryUrl(document);
    }

    @Transactional
    public void approveAgentProfile(Long profileId, Long adminId) {
        AgentProfile profile = requireProfile(profileId);
        requirePendingApproval(profile);

        profile.setStatus(AgentStatus.APPROVED);
        profile.setRejectionReason(null);
        profile.setAcceptingRequests(true); // theo nghiệp vụ: được duyệt thì mặc định bật "Đang nhận yêu cầu"
        markReviewed(profile, adminId);

        eventPublisher.publishEvent(new NotificationEvents.UserEmailEvent(
                profile.getUser().getEmail(), EmailTemplates.agentProfileApproved()));
        eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(
                profile.getUser().getId(), WebNotifications.agentProfileApproved()));
    }

    @Transactional
    public void rejectAgentProfile(Long profileId, Long adminId, String reason) {
        AgentProfile profile = requireProfile(profileId);
        requirePendingApproval(profile);

        profile.setStatus(AgentStatus.NEEDS_REVISION);
        profile.setRejectionReason(reason.trim());
        markReviewed(profile, adminId);

        eventPublisher.publishEvent(new NotificationEvents.UserEmailEvent(
                profile.getUser().getEmail(), EmailTemplates.agentProfileNeedsRevision(reason.trim())));
        eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(
                profile.getUser().getId(), WebNotifications.agentProfileNeedsRevision(reason.trim())));
    }

    private void markReviewed(AgentProfile profile, Long adminId) {
        profile.setReviewedBy(userRepository.getReferenceById(adminId));
        profile.setReviewedAt(LocalDateTime.now());
    }

    private static void requirePendingApproval(AgentProfile profile) {
        if (profile.getStatus() != AgentStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Chỉ có thể xử lý hồ sơ đang ở trạng thái chờ duyệt");
        }
    }

    private AgentProfile requireProfile(Long profileId) {
        return agentProfileRepository.findWithUserById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ Agent id=" + profileId));
    }
}
