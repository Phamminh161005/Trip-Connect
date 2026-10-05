package com.tripconnect.backend.controller;

import com.tripconnect.backend.dto.PageResponse;
import com.tripconnect.backend.dto.customrequest.CustomRequestRequests;
import com.tripconnect.backend.dto.customrequest.CustomRequestResponses;
import com.tripconnect.backend.enums.CustomRequestStatus;
import com.tripconnect.backend.security.AuthenticatedUser;
import com.tripconnect.backend.service.customrequest.CustomRequestAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Admin: điều phối yêu cầu thiết kế tour — xem gợi ý, giao cho Agent, đóng yêu cầu. */
@RestController
@RequestMapping("/api/admin/custom-requests")
@RequiredArgsConstructor
public class AdminCustomRequestController {

    private final CustomRequestAdminService adminService;

    @GetMapping
    public PageResponse<CustomRequestResponses.Summary> list(
            @RequestParam(required = false) CustomRequestStatus status,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return adminService.list(status, q, pageable);
    }

    @GetMapping("/{id}")
    public CustomRequestResponses.Detail get(@PathVariable Long id) {
        return adminService.get(id);
    }

    /** Agent đủ điều kiện, xếp theo điểm phù hợp (3 đơn vị đầu được đánh dấu gợi ý). */
    @GetMapping("/{id}/candidates")
    public List<CustomRequestResponses.Candidate> candidates(@PathVariable Long id) {
        return adminService.candidates(id);
    }

    @PostMapping("/{id}/assign")
    public CustomRequestResponses.Detail assign(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id,
                                                @Valid @RequestBody CustomRequestRequests.Assign body) {
        return adminService.assign(currentUser.userId(), id, body.getAgentId());
    }

    @PostMapping("/{id}/close")
    public CustomRequestResponses.Detail close(@PathVariable Long id, @Valid @RequestBody CustomRequestRequests.Reason body) {
        return adminService.close(id, body.getReason());
    }
}
