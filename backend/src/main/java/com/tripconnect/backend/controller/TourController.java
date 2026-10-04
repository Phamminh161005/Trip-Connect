package com.tripconnect.backend.controller;

import com.tripconnect.backend.dto.PageResponse;
import com.tripconnect.backend.dto.TemporaryUrlResponse;
import com.tripconnect.backend.dto.search.SearchResponses;
import com.tripconnect.backend.dto.search.TourSearchRequest;
import com.tripconnect.backend.dto.tour.TourResponses;
import com.tripconnect.backend.security.AuthenticatedUser;
import com.tripconnect.backend.service.search.SearchHistoryService;
import com.tripconnect.backend.service.search.TourSearchService;
import com.tripconnect.backend.service.tour.PublicTourService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Tour cho khách — không cần đăng nhập. */
@Slf4j
@RestController
@RequestMapping("/api/tours")
@RequiredArgsConstructor
public class TourController {

    /** Mã khách ẩn danh do trình duyệt tự sinh, dùng cho lịch sử tìm kiếm khi chưa đăng nhập. */
    public static final String VISITOR_HEADER = "X-Visitor-Id";

    private final PublicTourService publicTourService;
    private final TourSearchService searchService;
    private final SearchHistoryService historyService;

    /**
     * Tìm tour đang bán. Ví dụ: ?q=ha long&destinationId=14&dateFrom=2026-10-15&priceMax=5000000&sort=PRICE_ASC
     * Có token (đã đăng nhập) hoặc header X-Visitor-Id và track=true thì lần tìm được ghi vào lịch sử.
     */
    @GetMapping
    public PageResponse<SearchResponses.TourCard> search(
            @Valid @ModelAttribute TourSearchRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestHeader(value = VISITOR_HEADER, required = false) String visitorId) {
        PageResponse<SearchResponses.TourCard> result = searchService.search(request);
        try {
            historyService.record(request, currentUser == null ? null : currentUser.userId(), visitorId, result.totalElements());
        } catch (RuntimeException e) {
            // Ghi lịch sử hỏng không được làm hỏng kết quả tìm kiếm
            log.warn("Không ghi được lịch sử tìm kiếm: {}", e.getMessage());
        }
        return result;
    }

    /** Điểm đến có nhiều tour đang bán nhất (trang chủ). */
    @GetMapping("/destinations/popular")
    public List<SearchResponses.PopularDestination> popularDestinations(
            @RequestParam(defaultValue = "8") @Min(1) @Max(20) int limit) {
        return searchService.popularDestinations(limit);
    }

    @GetMapping("/{id}")
    public TourResponses.Detail get(@PathVariable Long id) {
        return publicTourService.get(id);
    }

    /** Link tải file chương trình tour (PDF), tự hết hạn sau 5 phút. */
    @GetMapping("/{id}/itinerary-file/url")
    public TemporaryUrlResponse itineraryFileUrl(@PathVariable Long id) {
        return publicTourService.itineraryFileUrl(id);
    }
}
