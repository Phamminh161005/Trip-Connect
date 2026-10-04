package com.tripconnect.backend.controller;

import com.tripconnect.backend.dto.PageResponse;
import com.tripconnect.backend.dto.TemporaryUrlResponse;
import com.tripconnect.backend.dto.tour.ReasonRequest;
import com.tripconnect.backend.dto.tour.TourResponses;
import com.tripconnect.backend.enums.TourStatus;
import com.tripconnect.backend.security.AuthenticatedUser;
import com.tripconnect.backend.service.tour.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Admin: xem mọi tour, duyệt / đình chỉ tour của đối tác.
 * Tạo / sửa / ảnh / lịch khởi hành cho tour của TripConnect: kế thừa từ {@link TourManagementEndpoints}.
 */
@RestController
@RequestMapping("/api/admin/tours")
public class AdminTourController extends TourManagementEndpoints {

    private final TourReviewService reviewService;

    public AdminTourController(TourService tourService, TourMediaService mediaService,
                               TourDepartureService departureService, TourReviewService reviewService) {
        super(tourService, mediaService, departureService);
        this.reviewService = reviewService;
    }

    /**
     * Ví dụ hàng chờ duyệt: ?status=PENDING_APPROVAL&sort=submittedAt,asc
     * provider=AGENT (tour đối tác) | PLATFORM (tour TripConnect) | bỏ trống = tất cả; q tìm theo tên tour / tên công ty.
     */
    @GetMapping
    public PageResponse<TourResponses.Summary> list(
            @RequestParam(required = false) TourStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) TourReviewService.ProviderFilter provider,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return reviewService.list(status, q, provider, pageable);
    }

    @GetMapping("/{id}")
    public TourResponses.Detail get(@PathVariable Long id) {
        return reviewService.get(id);
    }

    @GetMapping("/{id}/itinerary-file/url")
    public TemporaryUrlResponse itineraryFileUrl(@PathVariable Long id) {
        return reviewService.itineraryFileUrl(id);
    }

    // ===================== Duyệt tour của đối tác =====================

    @PostMapping("/{id}/approve")
    public TourResponses.Detail approve(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        return reviewService.approve(id, currentUser.userId());
    }

    @PostMapping("/{id}/request-revision")
    public TourResponses.Detail requestRevision(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                @PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
        return reviewService.requestRevision(id, currentUser.userId(), request.getReason());
    }

    @PostMapping("/{id}/suspend")
    public TourResponses.Detail suspend(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                        @PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
        return reviewService.suspend(id, currentUser.userId(), request.getReason());
    }

    @PostMapping("/{id}/unsuspend")
    public TourResponses.Detail unsuspend(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        return reviewService.unsuspend(id, currentUser.userId());
    }

    // ===================== Tour của TripConnect =====================

    /** Công khai ngay, không qua duyệt (tour phải đủ ít nhất 3 ảnh). */
    @PostMapping("/{id}/publish")
    public TourResponses.Detail publish(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        return tourService.publish(TourActor.of(currentUser), id);
    }
}
