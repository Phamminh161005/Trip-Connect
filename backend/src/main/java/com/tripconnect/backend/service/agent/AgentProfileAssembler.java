package com.tripconnect.backend.service.agent;

import com.tripconnect.backend.dto.*;
import com.tripconnect.backend.entity.AgentDocument;
import com.tripconnect.backend.entity.AgentProfile;
import com.tripconnect.backend.entity.AgentProfileChangeRequest;
import com.tripconnect.backend.entity.Location;
import com.tripconnect.backend.entity.User;
import com.tripconnect.backend.enums.AgentDocumentStatus;
import com.tripconnect.backend.enums.ChangeRequestStatus;
import com.tripconnect.backend.repository.AgentDocumentRepository;
import com.tripconnect.backend.repository.AgentProfileChangeRequestRepository;
import com.tripconnect.backend.repository.AgentServiceAreaRepository;
import com.tripconnect.backend.repository.AgentSpecialtyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Chuyển entity hồ sơ Agent thành DTO trả về cho client (gom dữ liệu từ nhiều bảng).
 * Phải được gọi bên trong transaction vì có truy cập quan hệ LAZY (profile.getUser()).
 */
@Component
@RequiredArgsConstructor
public class AgentProfileAssembler {

    private final AgentServiceAreaRepository serviceAreaRepository;
    private final AgentSpecialtyRepository specialtyRepository;
    private final AgentDocumentRepository documentRepository;
    private final AgentProfileChangeRequestRepository changeRequestRepository;
    private final AgentProfileCompleteness completeness;

    public AgentProfileResponse toResponse(AgentProfile profile) {
        User user = profile.getUser();

        List<LocationResponse> areas = serviceAreaRepository.findByAgentId(user.getId()).stream()
                .map(a -> toLocationResponse(a.getLocation()))
                .toList();
        List<TourCategoryResponse> specialties = specialtyRepository.findByAgentId(user.getId()).stream()
                .map(s -> new TourCategoryResponse(s.getCategory().getId(), s.getCategory().getName()))
                .toList();
        List<AgentDocumentResponse> documents = documentRepository
                .findByAgentProfileIdAndStatusOrderByUploadedAtAsc(profile.getId(), AgentDocumentStatus.ACTIVE)
                .stream().map(AgentProfileAssembler::toDocumentResponse).toList();
        Long pendingChangeRequestId = changeRequestRepository
                .findFirstByAgentProfileIdAndStatus(profile.getId(), ChangeRequestStatus.PENDING)
                .map(AgentProfileChangeRequest::getId)
                .orElse(null);

        return new AgentProfileResponse(
                profile.getId(), user.getId(), user.getFullName(), user.getEmail(), user.getPhone(),
                profile.getCompanyName(), profile.getTaxCode(), profile.getBusinessLicense(),
                toLocationResponse(profile.getAddressProvince()), profile.getAddress(),
                BankResponse.from(profile.getBank()), profile.getBankAccountNumber(), profile.getBankAccountHolder(),
                profile.getStatus(), profile.getRejectionReason(),
                profile.isAcceptingRequests(), profile.getMaxOpenRequests(), profile.getRating(),
                profile.getRatingCount(), profile.getSubmittedAt(), profile.getReviewedAt(), profile.getCreatedAt(),
                areas, specialties, documents, pendingChangeRequestId,
                profile.isEditable() ? completeness.findMissingItems(profile) : List.of()
        );
    }

    public AgentChangeRequestResponse toChangeRequestResponse(AgentProfileChangeRequest request) {
        List<AgentDocumentResponse> documents = documentRepository
                .findByChangeRequestIdOrderByUploadedAtAsc(request.getId()).stream()
                .map(AgentProfileAssembler::toDocumentResponse)
                .toList();
        return new AgentChangeRequestResponse(
                request.getId(), request.getAgentProfile().getId(), request.getStatus(),
                request.getCompanyName(), request.getTaxCode(), request.getBusinessLicense(),
                toLocationResponse(request.getAddressProvince()), request.getAddress(),
                BankResponse.from(request.getBank()), request.getBankAccountNumber(), request.getBankAccountHolder(),
                request.getNote(), request.getRejectionReason(), request.getCreatedAt(),
                request.getReviewedAt(), documents
        );
    }

    private static LocationResponse toLocationResponse(Location location) {
        return location == null ? null
                : new LocationResponse(location.getId(), location.getCountry(), location.getProvince());
    }

    public static AgentProfileSummaryResponse toSummary(AgentProfile profile) {
        User user = profile.getUser();
        return new AgentProfileSummaryResponse(
                profile.getId(), user.getId(), user.getFullName(), user.getEmail(),
                profile.getCompanyName(), profile.getTaxCode(), profile.getStatus(),
                profile.getSubmittedAt(), profile.getCreatedAt()
        );
    }

    public static AdminChangeRequestSummaryResponse toChangeRequestSummary(AgentProfileChangeRequest request) {
        AgentProfile profile = request.getAgentProfile();
        return new AdminChangeRequestSummaryResponse(
                request.getId(), profile.getId(), profile.getCompanyName(), profile.getUser().getEmail(),
                request.getStatus(), request.getCreatedAt()
        );
    }

    public static AgentDocumentResponse toDocumentResponse(AgentDocument document) {
        return new AgentDocumentResponse(
                document.getId(), document.getType(), document.getType().label(), document.getStatus(),
                document.getOriginalFilename(), document.getMimeType(), document.getSizeBytes(),
                document.getUploadedAt()
        );
    }
}
