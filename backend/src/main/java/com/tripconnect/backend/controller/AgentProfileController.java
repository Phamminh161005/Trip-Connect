package com.tripconnect.backend.controller;

import com.tripconnect.backend.dto.*;
import com.tripconnect.backend.enums.AgentDocumentType;
import com.tripconnect.backend.security.AuthenticatedUser;
import com.tripconnect.backend.service.agent.AgentChangeRequestService;
import com.tripconnect.backend.service.agent.AgentProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Hồ sơ kinh doanh của Agent đang đăng nhập (chỉ vai trò AGENT — xem SecurityConfig). */
@RestController
@RequestMapping("/api/agent/profile")
@RequiredArgsConstructor
public class AgentProfileController {

    private final AgentProfileService agentProfileService;
    private final AgentChangeRequestService changeRequestService;

    @GetMapping
    public AgentProfileResponse getMyProfile(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return agentProfileService.getMyProfile(currentUser.userId());
    }

    /** Sửa thông tin pháp lý — chỉ khi hồ sơ Nháp hoặc Cần bổ sung. */
    @PutMapping
    public AgentProfileResponse updateMyProfile(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                @Valid @RequestBody UpdateAgentProfileRequest request) {
        return agentProfileService.updateMyProfile(currentUser.userId(), request);
    }

    /** Số giấy phép lữ hành (nhập cạnh ô tải file giấy phép) — chỉ khi hồ sơ Nháp hoặc Cần bổ sung. */
    @PatchMapping("/business-license")
    public AgentProfileResponse updateBusinessLicense(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                      @Valid @RequestBody UpdateBusinessLicenseRequest request) {
        return agentProfileService.updateBusinessLicense(currentUser.userId(), request.getBusinessLicense());
    }

    /** Sửa khu vực phụ trách + loại hình thế mạnh — được sửa ở mọi trạng thái. */
    @PutMapping("/expertise")
    public AgentProfileResponse updateExpertise(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                @Valid @RequestBody UpdateAgentExpertiseRequest request) {
        return agentProfileService.updateExpertise(currentUser.userId(), request);
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AgentDocumentResponse> uploadDocument(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                                @RequestParam("type") AgentDocumentType type,
                                                                @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(agentProfileService.uploadDocument(currentUser.userId(), type, file));
    }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<Void> deleteDocument(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                               @PathVariable Long documentId) {
        agentProfileService.deleteDocument(currentUser.userId(), documentId);
        return ResponseEntity.noContent().build();
    }

    /** Link xem giấy tờ, tự hết hạn sau 5 phút. */
    @GetMapping("/documents/{documentId}/url")
    public TemporaryUrlResponse getDocumentUrl(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                               @PathVariable Long documentId) {
        return agentProfileService.getDocumentUrl(currentUser.userId(), documentId);
    }

    /** Nộp (hoặc nộp lại) hồ sơ để Admin duyệt. */
    @PostMapping("/submit")
    public AgentProfileResponse submit(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return agentProfileService.submit(currentUser.userId());
    }

    @PatchMapping("/accepting-requests")
    public AgentProfileResponse setAcceptingRequests(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                     @Valid @RequestBody AcceptingRequestsRequest request) {
        return agentProfileService.setAcceptingRequests(currentUser.userId(), request.getAccepting());
    }

    // ===================== Yêu cầu cập nhật hồ sơ (sau khi đã duyệt) =====================

    @PostMapping(value = "/change-requests", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AgentChangeRequestResponse> createChangeRequest(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @ModelAttribute AgentChangeRequestForm form) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(changeRequestService.create(currentUser.userId(), form));
    }

    @GetMapping("/change-requests")
    public List<AgentChangeRequestResponse> listMyChangeRequests(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return changeRequestService.listMine(currentUser.userId());
    }

    @DeleteMapping("/change-requests/{requestId}")
    public ResponseEntity<Void> cancelChangeRequest(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                    @PathVariable Long requestId) {
        changeRequestService.cancel(currentUser.userId(), requestId);
        return ResponseEntity.noContent().build();
    }
}
