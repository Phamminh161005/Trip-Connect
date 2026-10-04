package com.tripconnect.backend.controller;

import com.tripconnect.backend.dto.review.ReviewRequests;
import com.tripconnect.backend.dto.review.ReviewResponses;
import com.tripconnect.backend.security.AuthenticatedUser;
import com.tripconnect.backend.service.review.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Đánh giá tour.
 *  - GET /api/tours/{tourId}/reviews(/summary): công khai (SecurityConfig cho phép GET /api/tours/**).
 *  - Còn lại: khách đánh giá đơn của mình.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/tours/{tourId}/reviews")
    public ReviewResponses.PublicPage listForTour(@PathVariable Long tourId,
                                                  @RequestParam(required = false) Long before,
                                                  @RequestParam(defaultValue = "10") int size,
                                                  @RequestParam(required = false) Integer rating) {
        return reviewService.listPublic(tourId, before, size, rating);
    }

    @GetMapping("/tours/{tourId}/reviews/summary")
    public ReviewResponses.Summary summary(@PathVariable Long tourId) {
        return reviewService.summary(tourId);
    }

    /** Đánh giá của khách cho đơn này; chưa đánh giá -> 204. */
    @GetMapping("/bookings/{bookingId}/review")
    public ResponseEntity<ReviewResponses.Mine> getMine(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                        @PathVariable Long bookingId) {
        return reviewService.getMine(currentUser.userId(), bookingId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/bookings/{bookingId}/review")
    public ResponseEntity<ReviewResponses.Mine> create(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                       @PathVariable Long bookingId,
                                                       @Valid @RequestBody ReviewRequests.Write request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(currentUser.userId(), bookingId, request));
    }

    @PutMapping("/reviews/{id}")
    public ReviewResponses.Mine update(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id,
                                       @Valid @RequestBody ReviewRequests.Write request) {
        return reviewService.update(currentUser.userId(), id, request);
    }

    @PostMapping(value = "/reviews/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReviewResponses.Image> uploadImage(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                             @PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.uploadImage(currentUser.userId(), id, file));
    }

    @DeleteMapping("/reviews/{id}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                            @PathVariable Long id, @PathVariable Long imageId) {
        reviewService.deleteImage(currentUser.userId(), id, imageId);
        return ResponseEntity.noContent().build();
    }
}
