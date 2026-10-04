package com.tripconnect.backend.controller;

import com.tripconnect.backend.dto.PageResponse;
import com.tripconnect.backend.dto.booking.BookingResponses;
import com.tripconnect.backend.dto.booking.ManualRefundRequest;
import com.tripconnect.backend.dto.tour.ReasonRequest;
import com.tripconnect.backend.enums.BookingStatus;
import com.tripconnect.backend.enums.RefundStatus;
import com.tripconnect.backend.security.AuthenticatedUser;
import com.tripconnect.backend.service.booking.BookingManagementService;
import com.tripconnect.backend.service.booking.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Admin tra cứu mọi đơn đặt tour, hủy đơn vì bất khả kháng, xử lý hoàn tiền thủ công. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminBookingController {

    private final BookingManagementService managementService;
    private final RefundService refundService;

    /** Ví dụ: ?refundStatus=MANUAL_REQUIRED (đơn cần hoàn tiền thủ công) hoặc ?q=TC2610 */
    @GetMapping("/bookings")
    public PageResponse<BookingResponses.Summary> list(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) RefundStatus refundStatus,
            @RequestParam(required = false) Long tourId,
            @RequestParam(required = false) Long departureId,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return managementService.list(new BookingManagementService.Filter(null, status, refundStatus, tourId,
                departureId, q), pageable);
    }

    @GetMapping("/bookings/{id}")
    public BookingResponses.Detail get(@PathVariable Long id) {
        return managementService.get(id, null);
    }

    @GetMapping("/bookings/departures/{departureId}/manifest")
    public BookingResponses.Manifest manifest(@PathVariable Long departureId) {
        return managementService.manifest(departureId, null);
    }

    /** Hủy vì bất khả kháng — khách được hoàn 100%. */
    @PostMapping("/bookings/{id}/cancel")
    public BookingResponses.Detail cancel(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
        return managementService.cancelByAdmin(id, request.getReason());
    }

    /** Thử hoàn tự động qua VNPay lại. */
    @PostMapping("/refunds/{id}/retry")
    public ResponseEntity<Void> retryRefund(@PathVariable Long id) {
        refundService.retry(id);
        return ResponseEntity.ok().build();
    }

    /** Xác nhận đã chuyển khoản hoàn tiền thủ công. */
    @PostMapping("/refunds/{id}/mark-done")
    public ResponseEntity<Void> markRefundDone(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id,
                                               @Valid @RequestBody(required = false) ManualRefundRequest request) {
        refundService.markManualDone(id, currentUser.userId(), request == null ? null : request.getNote());
        return ResponseEntity.ok().build();
    }
}
