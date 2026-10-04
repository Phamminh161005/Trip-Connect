package com.tripconnect.backend.controller;

import com.tripconnect.backend.dto.PageResponse;
import com.tripconnect.backend.dto.review.ReviewRequests;
import com.tripconnect.backend.dto.review.ReviewResponses;
import com.tripconnect.backend.security.AuthenticatedUser;
import com.tripconnect.backend.service.review.ReviewModerationService;
import com.tripconnect.backend.service.review.ReviewModerationService.Filter;
import com.tripconnect.backend.service.review.ReviewModerationService.Responder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Admin: xem mọi đánh giá, ẩn / hiện lại đánh giá vi phạm, trả lời đánh giá tour của TripConnect. */
@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ReviewModerationService moderationService;

    @GetMapping
    public PageResponse<ReviewResponses.Managed> list(
            @RequestParam(required = false) Long tourId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Boolean replied,
            @RequestParam(required = false) Boolean hidden,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return moderationService.listForAdmin(new Filter(tourId, rating, replied, hidden, q), pageable);
    }

    @PostMapping("/{id}/hide")
    public ReviewResponses.Managed hide(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id,
                                        @Valid @RequestBody ReviewRequests.Hide request) {
        return moderationService.hide(currentUser.userId(), id, request.getReason());
    }

    @PostMapping("/{id}/unhide")
    public ReviewResponses.Managed unhide(@PathVariable Long id) {
        return moderationService.unhide(id);
    }

    /** Chỉ đánh giá tour của TripConnect (tour của Agent do Agent trả lời). */
    @PutMapping("/{id}/reply")
    public ReviewResponses.Managed reply(@PathVariable Long id, @Valid @RequestBody ReviewRequests.Reply request) {
        return moderationService.reply(Responder.platform(), id, request.getReply());
    }
}
