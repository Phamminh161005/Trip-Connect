package com.tripconnect.backend.controller;

import com.tripconnect.backend.dto.*;
import com.tripconnect.backend.enums.AgentStatus;
import com.tripconnect.backend.security.AuthenticatedUser;
import com.tripconnect.backend.service.AgentAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Admin xem và duyệt hồ sơ Agent. */
@RestController
@RequestMapping("/api/admin/agent-profiles")
@RequiredArgsConstructor
public class AdminAgentProfileController {

    private final AgentAdminService agentAdminService;

    /**
     * Ví dụ: ?status=PENDING_APPROVAL&q=abc&page=0&size=20&sort=submittedAt,asc
     * Bỏ status = mọi trạng thái; q tìm theo tên công ty / email / mã số thuế.
     */
    @GetMapping
    public PageResponse<AgentProfileSummaryResponse> list(
            @RequestParam(required = false) AgentStatus status,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return agentAdminService.listProfiles(status, q, pageable);
    }

    @GetMapping("/{id}")
    public AgentProfileResponse getProfile(@PathVariable Long id) {
        return agentAdminService.getProfile(id);
    }

    /** Link xem giấy tờ, tự hết hạn sau 5 phút. */
    @GetMapping("/{id}/documents/{documentId}/url")
    public TemporaryUrlResponse getDocumentUrl(@PathVariable Long id, @PathVariable Long documentId) {
        return agentAdminService.getDocumentUrl(id, documentId);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                        @PathVariable Long id) {
        agentAdminService.approveAgentProfile(id, currentUser.userId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> reject(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                       @PathVariable Long id,
                                       @Valid @RequestBody RejectAgentProfileRequest request) {
        agentAdminService.rejectAgentProfile(id, currentUser.userId(), request.getReason());
        return ResponseEntity.ok().build();
    }
}
