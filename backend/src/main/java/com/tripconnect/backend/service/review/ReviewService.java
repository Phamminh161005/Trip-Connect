package com.tripconnect.backend.service.review;

import com.tripconnect.backend.dto.review.ReviewRequests;
import com.tripconnect.backend.dto.review.ReviewResponses;
import com.tripconnect.backend.entity.Booking;
import com.tripconnect.backend.entity.Review;
import com.tripconnect.backend.entity.ReviewImage;
import com.tripconnect.backend.exception.ResourceNotFoundException;
import com.tripconnect.backend.repository.BookingRepository;
import com.tripconnect.backend.repository.ReviewRepository;
import com.tripconnect.backend.service.NotificationEvents;
import com.tripconnect.backend.service.WebNotifications;
import com.tripconnect.backend.storage.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Khách viết / sửa đánh giá cho đơn đã hoàn thành; trang tour xem đánh giá công khai. */
@Service
@RequiredArgsConstructor
public class ReviewService {

    static final int MAX_PAGE_SIZE = 30;

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final RatingCalculator ratingCalculator;
    private final ReviewAssembler assembler;
    private final FileStorageService fileStorageService;
    private final FileValidator fileValidator;
    private final TransactionalFileCleanup fileCleanup;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    // ===================== Trang tour (công khai) =====================

    @Transactional(readOnly = true)
    public ReviewResponses.PublicPage listPublic(Long tourId, Long before, int size, Integer rating) {
        int limit = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        Short ratingFilter = rating == null ? null : rating.shortValue();
        List<Review> rows = reviewRepository.findVisiblePage(tourId, before, ratingFilter, PageRequest.of(0, limit + 1));
        boolean hasMore = rows.size() > limit;
        List<Review> page = hasMore ? rows.subList(0, limit) : rows;
        return new ReviewResponses.PublicPage(assembler.toPublic(page), hasMore ? page.get(page.size() - 1).getId() : null);
    }

    @Transactional(readOnly = true)
    public ReviewResponses.Summary summary(Long tourId) {
        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int stars = 5; stars >= 1; stars--) distribution.put(stars, 0L);
        long count = 0, total = 0;
        for (Object[] row : reviewRepository.countVisibleByRating(tourId)) {
            int stars = ((Number) row[0]).intValue();
            long n = ((Number) row[1]).longValue();
            distribution.put(stars, n);
            count += n;
            total += stars * n;
        }
        BigDecimal average = count == 0 ? null
                : BigDecimal.valueOf(total).divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        return new ReviewResponses.Summary(average, (int) count, distribution);
    }

    // ===================== Khách =====================

    @Transactional(readOnly = true)
    public Optional<ReviewResponses.Mine> getMine(Long userId, Long bookingId) {
        requireOwnBooking(userId, bookingId);
        return reviewRepository.findByBookingId(bookingId).map(r -> assembler.toMine(r, LocalDateTime.now(clock)));
    }

    @Transactional
    public ReviewResponses.Mine create(Long userId, Long bookingId, ReviewRequests.Write request) {
        LocalDateTime now = LocalDateTime.now(clock);
        // Khóa đơn: 2 lần bấm gửi cùng lúc không tạo 2 đánh giá
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .filter(b -> b.getCustomer().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt tour"));
        if (reviewRepository.findByBookingId(bookingId).isPresent()) {
            throw new IllegalStateException("Bạn đã đánh giá chuyến đi này");
        }
        if (!ReviewRules.canWrite(booking, now)) {
            throw new IllegalStateException("Chỉ đánh giá được chuyến đi đã hoàn thành, trong vòng "
                    + ReviewRules.WRITE_WINDOW_DAYS + " ngày");
        }

        Review review = new Review();
        review.setBooking(booking);
        review.setTour(booking.getTour());
        review.setAgent(booking.getAgent());
        review.setCustomer(booking.getCustomer());
        review.setRating(request.getRating().shortValue());
        review.setComment(request.getComment().trim());
        review.setCreatedAt(now);
        reviewRepository.save(review);

        Long agentId = booking.getAgent() == null ? null : booking.getAgent().getId();
        ratingCalculator.recalculate(booking.getTour().getId(), agentId);
        var message = WebNotifications.newReview(agentId == null, booking.getTour().getTitle(), review.getRating());
        eventPublisher.publishEvent(agentId == null
                ? new NotificationEvents.AdminWebEvent(message)
                : new NotificationEvents.UserWebEvent(agentId, message));
        return assembler.toMine(review, now);
    }

    @Transactional
    public ReviewResponses.Mine update(Long userId, Long reviewId, ReviewRequests.Write request) {
        LocalDateTime now = LocalDateTime.now(clock);
        Review review = requireEditable(userId, reviewId, now);
        boolean ratingChanged = review.getRating() != request.getRating();
        review.setRating(request.getRating().shortValue());
        review.setComment(request.getComment().trim());
        review.setUpdatedAt(now);
        if (ratingChanged) {
            ratingCalculator.recalculate(review.getTour().getId(), review.getAgent() == null ? null : review.getAgent().getId());
        }
        return assembler.toMine(review, now);
    }

    @Transactional
    public ReviewResponses.Image uploadImage(Long userId, Long reviewId, MultipartFile file) {
        LocalDateTime now = LocalDateTime.now(clock);
        Review review = requireEditable(userId, reviewId, now);
        if (review.getImages().size() >= ReviewRules.MAX_IMAGES) {
            throw new IllegalStateException("Mỗi đánh giá có tối đa " + ReviewRules.MAX_IMAGES + " ảnh");
        }
        fileValidator.validate(file, FileRule.IMAGE);
        StoredFile stored = fileStorageService.upload(file, "reviews/" + reviewId, FileVisibility.PUBLIC);
        fileCleanup.deleteOnRollback(stored.publicId(), FileVisibility.PUBLIC);

        ReviewImage image = new ReviewImage();
        image.setReview(review);
        image.setPublicId(stored.publicId());
        image.setFormat(stored.format());
        image.setSizeBytes(stored.sizeBytes());
        image.setSortOrder(review.getImages().stream().mapToInt(ReviewImage::getSortOrder).max().orElse(0) + 1);
        image.setUploadedAt(now);
        review.getImages().add(image);
        reviewRepository.flush();
        return assembler.toImage(image);
    }

    @Transactional
    public void deleteImage(Long userId, Long reviewId, Long imageId) {
        Review review = requireEditable(userId, reviewId, LocalDateTime.now(clock));
        ReviewImage image = review.getImages().stream().filter(i -> i.getId().equals(imageId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ảnh"));
        review.getImages().remove(image);
        fileCleanup.deleteAfterCommit(image.getPublicId(), FileVisibility.PUBLIC);
    }

    // ===================== Tiện ích =====================

    private void requireOwnBooking(Long userId, Long bookingId) {
        bookingRepository.findById(bookingId)
                .filter(b -> b.getCustomer().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt tour"));
    }

    /** Khóa đánh giá của chính khách, còn trong hạn sửa. */
    private Review requireEditable(Long userId, Long reviewId, LocalDateTime now) {
        Review review = reviewRepository.findByIdForUpdate(reviewId)
                .filter(r -> r.getCustomer().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá"));
        if (!ReviewRules.canEdit(review, now)) {
            throw new IllegalStateException(review.isHidden() ? "Đánh giá đã bị ẩn nên không sửa được"
                    : "Chỉ sửa được đánh giá trong " + ReviewRules.EDIT_DAYS + " ngày sau khi gửi");
        }
        return review;
    }
}
