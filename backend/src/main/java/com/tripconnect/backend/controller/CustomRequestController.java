package com.tripconnect.backend.controller;

import com.tripconnect.backend.dto.PageResponse;
import com.tripconnect.backend.dto.customrequest.CustomRequestRequests;
import com.tripconnect.backend.dto.customrequest.CustomRequestResponses;
import com.tripconnect.backend.security.AuthenticatedUser;
import com.tripconnect.backend.security.RateLimiter;
import com.tripconnect.backend.service.customrequest.CustomProposalService;
import com.tripconnect.backend.service.customrequest.CustomRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

/** Yêu cầu thiết kế tour riêng của khách hàng đang đăng nhập. */
@RestController
@RequestMapping("/api/custom-requests")
@RequiredArgsConstructor
public class CustomRequestController {

    private final CustomRequestService requestService;
    private final CustomProposalService proposalService;
    private final RateLimiter rateLimiter;

    @PostMapping
    public ResponseEntity<CustomRequestResponses.Detail> create(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                                @Valid @RequestBody CustomRequestRequests.Create request) {
        rateLimiter.check("custom-request-create", String.valueOf(currentUser.userId()), 5, Duration.ofHours(1));
        return ResponseEntity.status(HttpStatus.CREATED).body(requestService.create(currentUser.userId(), currentUser.role(), request));
    }

    @GetMapping
    public PageResponse<CustomRequestResponses.Summary> listMine(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return requestService.listMine(currentUser.userId(), pageable);
    }

    @GetMapping("/{id}")
    public CustomRequestResponses.Detail get(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        return requestService.getMine(currentUser.userId(), id);
    }

    @PostMapping("/{id}/cancel")
    public CustomRequestResponses.Detail cancel(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id,
                                                @Valid @RequestBody(required = false) CustomRequestRequests.Reason body) {
        return requestService.cancel(currentUser.userId(), id, body == null ? null : body.getReason());
    }

    @PostMapping("/{id}/proposals/accept")
    public CustomRequestResponses.Detail acceptProposal(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id,
                                                        @Valid @RequestBody CustomRequestRequests.ProposalResponse body) {
        return proposalService.accept(currentUser.userId(), id, body.getProposalId());
    }

    @PostMapping("/{id}/proposals/revise")
    public CustomRequestResponses.Detail requestRevision(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id,
                                                         @Valid @RequestBody CustomRequestRequests.ProposalResponse body) {
        return proposalService.requestRevision(currentUser.userId(), id, body.getProposalId(), body.getFeedback());
    }
}
