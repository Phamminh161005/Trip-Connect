package com.tripconnect.backend.service.agent;

import com.tripconnect.backend.dto.AgentDocumentResponse;
import com.tripconnect.backend.dto.AgentProfileResponse;
import com.tripconnect.backend.dto.TemporaryUrlResponse;
import com.tripconnect.backend.dto.UpdateAgentExpertiseRequest;
import com.tripconnect.backend.dto.UpdateAgentProfileRequest;
import com.tripconnect.backend.entity.AgentDocument;
import com.tripconnect.backend.entity.AgentProfile;
import com.tripconnect.backend.entity.User;
import com.tripconnect.backend.enums.AgentDocumentStatus;
import com.tripconnect.backend.enums.AgentDocumentType;
import com.tripconnect.backend.enums.AgentStatus;
import com.tripconnect.backend.exception.ForbiddenException;
import com.tripconnect.backend.exception.ResourceNotFoundException;
import com.tripconnect.backend.repository.AgentDocumentRepository;
import com.tripconnect.backend.repository.AgentProfileRepository;
import com.tripconnect.backend.service.EmailTemplates;
import com.tripconnect.backend.service.NotificationEvents;
import com.tripconnect.backend.service.WebNotifications;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Nghiệp vụ hồ sơ Agent phía Agent: xem, sửa (khi Nháp / Cần bổ sung), upload giấy tờ, nộp hồ sơ,
 * sửa chuyên môn, bật/tắt nhận yêu cầu.
 *
 * Vòng đời trạng thái:  DRAFT --nộp--> PENDING_APPROVAL --duyệt--> APPROVED
 *                                                       \--từ chối--> NEEDS_REVISION --nộp lại--> PENDING_APPROVAL
 */
@Service
@RequiredArgsConstructor
public class AgentProfileService {

    private final AgentProfileRepository profileRepository;
    private final AgentDocumentRepository documentRepository;
    private final AgentProfileCompleteness completeness;
    private final AgentDocumentStorage documentStorage;
    private final AgentExpertiseWriter expertiseWriter;
    private final AgentProfileAssembler assembler;
    private final AgentReferenceResolver referenceResolver;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public AgentProfileResponse getMyProfile(Long userId) {
        return assembler.toResponse(requireProfile(userId));
    }

    @Transactional
    public AgentProfileResponse updateMyProfile(Long userId, UpdateAgentProfileRequest request) {
        AgentProfile profile = requireProfile(userId);
        requireEditable(profile);

        profile.setCompanyName(request.getCompanyName().trim());
        profile.setTaxCode(request.getTaxCode().trim());
        profile.setBusinessLicense(trimToNull(request.getBusinessLicense()));
        profile.setAddressProvince(referenceResolver.requireVietnamProvince(request.getAddressProvinceId()));
        profile.setAddress(request.getAddress().trim());
        profile.setBank(referenceResolver.requireBank(request.getBankBin()));
        profile.setBankAccountNumber(request.getBankAccountNumber());
        profile.setBankAccountHolder(BankAccountHolder.normalize(request.getBankAccountHolder()));
        return assembler.toResponse(profile);
    }

    /** Số giấy phép lữ hành — lưu riêng vì trên giao diện nó nằm cạnh ô tải file giấy phép. */
    @Transactional
    public AgentProfileResponse updateBusinessLicense(Long userId, String businessLicense) {
        AgentProfile profile = requireProfile(userId);
        requireEditable(profile);
        profile.setBusinessLicense(businessLicense.trim());
        return assembler.toResponse(profile);
    }

    /** Khu vực phụ trách + loại hình thế mạnh: không ảnh hưởng tính pháp lý nên Agent tự sửa ở mọi trạng thái. */
    @Transactional
    public AgentProfileResponse updateExpertise(Long userId, UpdateAgentExpertiseRequest request) {
        AgentProfile profile = requireProfile(userId);
        expertiseWriter.replace(profile.getUser(), request.getLocationIds(), request.getCategoryIds());
        // replace() đã clear bộ nhớ đệm của Hibernate -> đọc lại hồ sơ
        return assembler.toResponse(requireProfile(userId));
    }

    @Transactional
    public AgentDocumentResponse uploadDocument(Long userId, AgentDocumentType type, MultipartFile file) {
        AgentProfile profile = requireProfile(userId);
        requireEditable(profile);

        if (type == AgentDocumentType.OTHER) {
            long otherCount = documentRepository.countByAgentProfileIdAndTypeAndStatusIn(
                    profile.getId(), AgentDocumentType.OTHER, List.of(AgentDocumentStatus.ACTIVE));
            if (otherCount >= AgentDocumentType.MAX_OTHER_DOCUMENTS) {
                throw new IllegalStateException("Chỉ được tải tối đa " + AgentDocumentType.MAX_OTHER_DOCUMENTS
                        + " giấy tờ khác");
            }
        }

        String mimeType = documentStorage.validate(file);

        // Loại chỉ có 1 file (giấy phép, CCCD...): file mới thay file cũ
        if (type.singleFile()) {
            documentStorage.deletePermanently(documentRepository.findByAgentProfileIdAndTypeAndStatus(
                    profile.getId(), type, AgentDocumentStatus.ACTIVE));
        }

        AgentDocument document = documentStorage.store(profile, null, type, AgentDocumentStatus.ACTIVE,
                file, mimeType);
        return AgentProfileAssembler.toDocumentResponse(document);
    }

    @Transactional
    public void deleteDocument(Long userId, Long documentId) {
        AgentProfile profile = requireProfile(userId);
        requireEditable(profile);

        AgentDocument document = documentRepository.findByIdAndAgentProfileId(documentId, profile.getId())
                .filter(d -> d.getStatus() == AgentDocumentStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giấy tờ"));
        documentStorage.deletePermanently(document);
    }

    @Transactional(readOnly = true)
    public TemporaryUrlResponse getDocumentUrl(Long userId, Long documentId) {
        AgentProfile profile = requireProfile(userId);
        AgentDocument document = documentRepository.findByIdAndAgentProfileId(documentId, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giấy tờ"));
        return documentStorage.temporaryUrl(document);
    }

    @Transactional
    public AgentProfileResponse submit(Long userId) {
        AgentProfile profile = requireProfile(userId);
        requireEditable(profile);

        List<String> missing = completeness.findMissingItems(profile);
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Hồ sơ còn thiếu: " + String.join(", ", missing));
        }

        boolean resubmission = profile.getStatus() == AgentStatus.NEEDS_REVISION;
        profile.setStatus(AgentStatus.PENDING_APPROVAL);
        profile.setSubmittedAt(LocalDateTime.now());

        eventPublisher.publishEvent(new NotificationEvents.AdminWebEvent(
                WebNotifications.agentProfileSubmitted(profile.getId(), profile.getCompanyName(), resubmission)));
        return assembler.toResponse(profile);
    }

    @Transactional
    public AgentProfileResponse setAcceptingRequests(Long userId, boolean accepting) {
        AgentProfile profile = requireProfile(userId);
        if (profile.getStatus() != AgentStatus.APPROVED) {
            throw new ForbiddenException("Chỉ Agent đã được duyệt hồ sơ mới có thể nhận yêu cầu tư vấn");
        }
        profile.setAcceptingRequests(accepting);
        return assembler.toResponse(profile);
    }

    // ===================== Tiện ích =====================

    private AgentProfile requireProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ Agent"));
    }

    private static void requireEditable(AgentProfile profile) {
        if (profile.isEditable()) return;
        if (profile.getStatus() == AgentStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Hồ sơ đang chờ duyệt, không thể chỉnh sửa lúc này");
        }
        throw new IllegalStateException(
                "Hồ sơ đã được duyệt. Để thay đổi thông tin pháp lý, vui lòng gửi yêu cầu cập nhật hồ sơ");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
