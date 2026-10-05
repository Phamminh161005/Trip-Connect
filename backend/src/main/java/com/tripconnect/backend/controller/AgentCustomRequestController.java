package com.tripconnect.backend.controller;

import com.tripconnect.backend.dto.PageResponse;
import com.tripconnect.backend.dto.customrequest.CustomRequestRequests;
import com.tripconnect.backend.dto.customrequest.CustomRequestResponses;
import com.tripconnect.backend.security.AuthenticatedUser;
import com.tripconnect.backend.service.customrequest.CustomProposalService;
import com.tripconnect.backend.service.customrequest.CustomRequestAgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Agent: yêu cầu thiết kế tour được giao. */
@RestController
@RequestMapping("/api/agent/custom-requests")
@RequiredArgsConstructor
public class AgentCustomRequestController {

    private final CustomRequestAgentService agentService;
    private final CustomProposalService proposalService;

    /** tab: PENDING (chờ nhận) | ACCEPTED (đã nhận) | HISTORY (từ chối, hết hạn, bị thu hồi, quá hạn gửi đề xuất) */
    @GetMapping
    public PageResponse<CustomRequestResponses.Summary> list(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) CustomRequestAgentService.Tab tab,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return agentService.list(currentUser.userId(), tab, pageable);
    }

    @GetMapping("/{id}")
    public CustomRequestResponses.Detail get(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        return agentService.get(currentUser.userId(), id);
    }

    @PostMapping("/{id}/accept")
    public CustomRequestResponses.Detail accept(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        return agentService.accept(currentUser.userId(), id);
    }

    @PostMapping("/{id}/decline")
    public CustomRequestResponses.Detail decline(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id,
                                                 @Valid @RequestBody CustomRequestRequests.Reason body) {
        return agentService.decline(currentUser.userId(), id, body.getReason());
    }

    /** Gửi đề xuất (bản đầu hoặc bản chỉnh sửa theo góp ý của khách). */
    @PostMapping("/{id}/proposals")
    public CustomRequestResponses.Detail propose(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id,
                                                 @Valid @RequestBody CustomRequestRequests.Proposal body) {
        return proposalService.submit(currentUser.userId(), id, body);
    }
}
