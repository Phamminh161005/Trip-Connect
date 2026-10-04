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

/** Agent xem và trả lời đánh giá các tour của mình. */
@RestController
@RequestMapping("/api/agent/reviews")
@RequiredArgsConstructor
public class AgentReviewController {

    private final ReviewModerationService moderationService;

    @GetMapping
    public PageResponse<ReviewResponses.Managed> list(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) Long tourId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Boolean replied,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return moderationService.listForAgent(currentUser.userId(), new Filter(tourId, rating, replied, null, null), pageable);
    }

    @PutMapping("/{id}/reply")
    public ReviewResponses.Managed reply(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id,
                                         @Valid @RequestBody ReviewRequests.Reply request) {
        return moderationService.reply(Responder.agent(currentUser.userId()), id, request.getReply());
    }
}
