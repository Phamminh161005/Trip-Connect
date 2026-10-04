package com.tripconnect.backend.service.review;

import com.tripconnect.backend.dto.review.ReviewResponses;
import com.tripconnect.backend.entity.Review;
import com.tripconnect.backend.entity.ReviewImage;
import com.tripconnect.backend.repository.ReviewImageRepository;
import com.tripconnect.backend.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Chuyển đánh giá thành DTO. Gọi trong transaction; ảnh của cả danh sách lấy một lượt. */
@Component
@RequiredArgsConstructor
public class ReviewAssembler {

    private final ReviewImageRepository imageRepository;
    private final FileStorageService fileStorageService;

    public ReviewResponses.Image toImage(ReviewImage image) {
        return new ReviewResponses.Image(image.getId(), fileStorageService.publicUrl(image.getPublicId(), image.getFormat()));
    }

    private Map<Long, List<ReviewResponses.Image>> imagesOf(List<Review> reviews) {
        if (reviews.isEmpty()) return Map.of();
        return imageRepository.findByReviewIds(reviews.stream().map(Review::getId).toList()).stream()
                .collect(Collectors.groupingBy(i -> i.getReview().getId(), Collectors.mapping(this::toImage, Collectors.toList())));
    }

    public List<ReviewResponses.Public> toPublic(List<Review> reviews) {
        Map<Long, List<ReviewResponses.Image>> images = imagesOf(reviews);
        return reviews.stream().map(r -> new ReviewResponses.Public(
                r.getId(), r.getRating(), r.getComment(), ReviewRules.displayName(r.getCustomer().getFullName()),
                r.getBooking().getDeparture().getStartDate(), images.getOrDefault(r.getId(), List.of()),
                r.getReply(), r.getRepliedAt(), r.getCreatedAt(),
                r.getUpdatedAt() != null && r.getUpdatedAt().isAfter(r.getCreatedAt()))).toList();
    }

    public ReviewResponses.Mine toMine(Review r, LocalDateTime now) {
        return new ReviewResponses.Mine(r.getId(), r.getBooking().getId(), r.getTour().getId(), r.getRating(),
                r.getComment(), r.getImages().stream().map(this::toImage).toList(), r.getReply(), r.getRepliedAt(),
                r.isHidden(), r.getHiddenReason(), ReviewRules.canEdit(r, now), ReviewRules.editableUntil(r), r.getCreatedAt());
    }

    public List<ReviewResponses.Managed> toManaged(List<Review> reviews) {
        Map<Long, List<ReviewResponses.Image>> images = imagesOf(reviews);
        return reviews.stream().map(r -> new ReviewResponses.Managed(
                r.getId(), r.getTour().getId(), r.getTour().getTitle(), r.getBooking().getId(), r.getBooking().getCode(),
                r.getCustomer().getFullName(), r.getCustomer().getEmail(), r.getBooking().getDeparture().getStartDate(),
                r.getRating(), r.getComment(), images.getOrDefault(r.getId(), List.of()), r.getReply(), r.getRepliedAt(),
                r.isHidden(), r.getHiddenReason(), r.getHiddenAt(), r.getAgent() == null, r.getCreatedAt())).toList();
    }
}
