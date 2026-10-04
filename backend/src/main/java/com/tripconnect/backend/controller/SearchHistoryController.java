package com.tripconnect.backend.controller;

import com.tripconnect.backend.dto.search.SearchResponses;
import com.tripconnect.backend.security.AuthenticatedUser;
import com.tripconnect.backend.service.search.SearchHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Lịch sử tìm kiếm của người đang đăng nhập ("Tìm kiếm gần đây"). */
@RestController
@RequestMapping("/api/me/search-history")
@RequiredArgsConstructor
public class SearchHistoryController {

    private final SearchHistoryService historyService;

    @GetMapping
    public List<SearchResponses.RecentSearch> recent(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return historyService.recent(currentUser.userId());
    }

    /** Gọi ngay sau khi đăng nhập: gộp lịch sử lúc chưa đăng nhập (theo mã khách) vào tài khoản. */
    @PostMapping("/claim")
    public Map<String, Integer> claim(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                      @RequestHeader(value = TourController.VISITOR_HEADER, required = false) String visitorId) {
        return Map.of("claimed", historyService.claim(currentUser.userId(), visitorId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        historyService.delete(currentUser.userId(), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clear(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        historyService.clear(currentUser.userId());
        return ResponseEntity.noContent().build();
    }
}
