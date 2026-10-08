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
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/** Admin: đối soát hoa hồng với các đơn vị tổ chức. */
@RestController
@RequestMapping("/api/admin/settlements")
@RequiredArgsConstructor
public class AdminSettlementController {

    private final SettlementService settlementService;

    @GetMapping
    public PageResponse<SettlementResponses.Summary> list(@RequestParam(required = false) SettlementStatus status,
                                                          @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                                                          Pageable pageable) {
        return settlementService.list(null, status, pageable);
    }

    @GetMapping("/{id}")
    public SettlementResponses.Detail get(@PathVariable Long id) {
        return settlementService.get(null, id);
    }

    /** Lập ngay cho các đơn chưa đối soát tới thời điểm hiện tại (ngoài lượt tự động ngày 1 hằng tháng). */
    @PostMapping("/generate")
    public Map<String, Integer> generate() {
        return Map.of("created", settlementService.generateNow());
    }

    @PostMapping("/{id}/resolve")
    public SettlementResponses.Detail resolve(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                              @Valid @RequestBody SettlementRequests.Resolve body) {
        return settlementService.resolve(user.userId(), id, body);
    }

    @PostMapping(value = "/{id}/pay", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SettlementResponses.Detail pay(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                          @RequestParam String transactionRef,
                                          @RequestParam(required = false) MultipartFile receipt) {
        return settlementService.markPaid(user.userId(), id, transactionRef, receipt);
    }

    @GetMapping("/{id}/receipt-url")
    public TemporaryUrlResponse receiptUrl(@PathVariable Long id) {
        return settlementService.receiptUrl(null, id);
    }
}
