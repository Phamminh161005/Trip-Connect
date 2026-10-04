package com.tripconnect.backend.controller;

import com.tripconnect.backend.dto.PageResponse;
import com.tripconnect.backend.dto.TemporaryUrlResponse;
import com.tripconnect.backend.dto.tour.TourResponses;
import com.tripconnect.backend.enums.TourStatus;
import com.tripconnect.backend.security.AuthenticatedUser;
import com.tripconnect.backend.service.tour.TourActor;
import com.tripconnect.backend.service.tour.TourDepartureService;
import com.tripconnect.backend.service.tour.TourMediaService;
import com.tripconnect.backend.service.tour.TourService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Tour của Agent đang đăng nhập (vai trò AGENT, hồ sơ đã được duyệt).
 * Tạo / sửa / ảnh / lịch khởi hành: kế thừa từ {@link TourManagementEndpoints}.
 */
@RestController
@RequestMapping("/api/agent/tours")
public class AgentTourController extends TourManagementEndpoints {

    public AgentTourController(TourService tourService, TourMediaService mediaService,
                               TourDepartureService departureService) {
        super(tourService, mediaService, departureService);
    }

    /** Ví dụ: ?status=PUBLISHED&q=ha long&page=0&size=20 */
    @GetMapping
    public PageResponse<TourResponses.Summary> list(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) TourStatus status,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return tourService.listMine(TourActor.of(currentUser), status, q, pageable);
    }

    @GetMapping("/{id}")
    public TourResponses.Detail get(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        return tourService.getMine(TourActor.of(currentUser), id);
    }

    @GetMapping("/{id}/itinerary-file/url")
    public TemporaryUrlResponse itineraryFileUrl(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                 @PathVariable Long id) {
        return mediaService.itineraryFileUrlForOwner(TourActor.of(currentUser), id);
    }

    /** Gửi Admin duyệt (tour Nháp / Cần chỉnh sửa, đủ ít nhất 3 ảnh). */
    @PostMapping("/{id}/submit")
    public TourResponses.Detail submit(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        return tourService.submit(TourActor.of(currentUser), id);
    }

    /** Rút lại yêu cầu duyệt để sửa tiếp. */
    @PostMapping("/{id}/withdraw")
    public TourResponses.Detail withdraw(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        return tourService.withdraw(TourActor.of(currentUser), id);
    }
}
