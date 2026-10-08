package com.tripconnect.backend.controller;

import com.tripconnect.backend.dto.PageResponse;
import com.tripconnect.backend.dto.TemporaryUrlResponse;
import com.tripconnect.backend.dto.settlement.SettlementRequests;
import com.tripconnect.backend.dto.settlement.SettlementResponses;
import com.tripconnect.backend.enums.SettlementStatus;
import com.tripconnect.backend.security.AuthenticatedUser;
import com.tripconnect.backend.service.settlement.SettlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Agent: bảng đối soát hoa hồng của mình. */
@RestController
@RequestMapping("/api/agent/settlements")
@RequiredArgsConstructor
public class AgentSettlementController {

    private final SettlementService settlementService;

    @GetMapping
    public PageResponse<SettlementResponses.Summary> list(@AuthenticationPrincipal AuthenticatedUser user,
                                                          @RequestParam(required = false) SettlementStatus status,
                                                          @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                                                          Pageable pageable) {
        return settlementService.list(user.userId(), status, pageable);
    }

    @GetMapping("/{id}")
    public SettlementResponses.Detail get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return settlementService.get(user.userId(), id);
    }

    @PostMapping("/{id}/confirm")
    public SettlementResponses.Detail confirm(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return settlementService.confirm(user.userId(), id);
    }

    @PostMapping("/{id}/dispute")
    public SettlementResponses.Detail dispute(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                              @Valid @RequestBody SettlementRequests.Dispute body) {
        return settlementService.dispute(user.userId(), id, body.reason());
    }

    @GetMapping("/{id}/receipt-url")
    public TemporaryUrlResponse receiptUrl(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return settlementService.receiptUrl(user.userId(), id);
    }
}
