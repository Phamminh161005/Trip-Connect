package com.tripconnect.backend.controller;

import com.tripconnect.backend.dto.PageResponse;
import com.tripconnect.backend.dto.booking.BookingResponses;
import com.tripconnect.backend.enums.BookingStatus;
import com.tripconnect.backend.security.AgentAccessGuard;
import com.tripconnect.backend.security.AuthenticatedUser;
import com.tripconnect.backend.service.booking.BookingManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Đơn đặt các tour do Agent đang đăng nhập tổ chức. */
@RestController
@RequestMapping("/api/agent/bookings")
@RequiredArgsConstructor
public class AgentBookingController {

    private final BookingManagementService managementService;
    private final AgentAccessGuard agentAccessGuard;

    /** Ví dụ: ?status=PAID&tourId=1&departureId=3&q=TC2610 */
    @GetMapping
    public PageResponse<BookingResponses.Summary> list(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) Long tourId,
            @RequestParam(required = false) Long departureId,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        agentAccessGuard.requireApprovedAgent(currentUser.userId());
        return managementService.list(new BookingManagementService.Filter(currentUser.userId(), status, null, tourId,
                departureId, q), pageable);
    }

    @GetMapping("/{id}")
    public BookingResponses.Detail get(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        agentAccessGuard.requireApprovedAgent(currentUser.userId());
        return managementService.get(id, currentUser.userId());
    }

    /** Danh sách đoàn của một lịch khởi hành (đơn đã thanh toán + hành khách). */
    @GetMapping("/departures/{departureId}/manifest")
    public BookingResponses.Manifest manifest(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                              @PathVariable Long departureId) {
        agentAccessGuard.requireApprovedAgent(currentUser.userId());
        return managementService.manifest(departureId, currentUser.userId());
    }
}
