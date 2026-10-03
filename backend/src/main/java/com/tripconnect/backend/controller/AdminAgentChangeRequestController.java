package com.tripconnect.backend.controller;

import com.tripconnect.backend.dto.AdminChangeRequestDetailResponse;
import com.tripconnect.backend.dto.AdminChangeRequestSummaryResponse;
import com.tripconnect.backend.dto.PageResponse;
import com.tripconnect.backend.dto.RejectAgentProfileRequest;
import com.tripconnect.backend.enums.ChangeRequestStatus;
import com.tripconnect.backend.security.AuthenticatedUser;
import com.tripconnect.backend.service.agent.AgentChangeRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Admin duyệt yêu cầu cập nhật hồ sơ của Agent đã được duyệt. */
@RestController
@RequestMapping("/api/admin/agent-change-requests")
@RequiredArgsConstructor
public class AdminAgentChangeRequestController {

    private final AgentChangeRequestService changeRequestService;

    /** Mặc định chỉ lấy yêu cầu đang chờ duyệt (PENDING). */
    @GetMapping
    public PageResponse<AdminChangeRequestSummaryResponse> list(
            @RequestParam(required = false) ChangeRequestStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return changeRequestService.list(status, pageable);
    }

    /** Chi tiết yêu cầu + thông tin hồ sơ hiện tại để so sánh. Giấy tờ xem qua API documents/{id}/url của hồ sơ. */
    @GetMapping("/{id}")
    public AdminChangeRequestDetailResponse getDetail(@PathVariable Long id) {
        return changeRequestService.getDetail(id);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                        @PathVariable Long id) {
        changeRequestService.approve(id, currentUser.userId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> reject(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                       @PathVariable Long id,
                                       @Valid @RequestBody RejectAgentProfileRequest request) {
        changeRequestService.reject(id, currentUser.userId(), request.getReason());
        return ResponseEntity.ok().build();
    }
}
