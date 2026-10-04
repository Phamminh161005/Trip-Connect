package com.tripconnect.backend.service.review;

import com.tripconnect.backend.dto.PageResponse;
import com.tripconnect.backend.dto.review.ReviewResponses;
import com.tripconnect.backend.entity.Review;
import com.tripconnect.backend.exception.ResourceNotFoundException;
import com.tripconnect.backend.repository.ReviewRepository;
import com.tripconnect.backend.repository.SearchPatterns;
import com.tripconnect.backend.repository.StablePaging;
import com.tripconnect.backend.repository.UserRepository;
import com.tripconnect.backend.service.NotificationEvents;
import com.tripconnect.backend.service.WebNotifications;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Quản lý đánh giá: đơn vị tổ chức trả lời (Agent với tour của mình, Admin với tour của TripConnect);
 * Admin ẩn / hiện lại đánh giá vi phạm.
 */
@Service
@RequiredArgsConstructor
public class ReviewModerationService {

    /** Ai đang trả lời: Agent (agentId) hoặc Admin (agentId = null, chỉ tour của TripConnect). */
    public record Responder(Long agentId) {
        public static Responder agent(Long agentId) {
            return new Responder(agentId);
        }

        public static Responder platform() {
            return new Responder(null);
        }
    }

    public record Filter(Long tourId, Integer rating, Boolean replied, Boolean hidden, String q) {
    }

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final RatingCalculator ratingCalculator;
    private final ReviewAssembler assembler;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    // ===================== Danh sách =====================

    /** Agent: đánh giá các tour của mình (kể cả bị ẩn, để biết lý do). */
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponses.Managed> listForAgent(Long agentId, Filter filter, Pageable pageable) {
        Specification<Review> spec = (root, query, cb) -> cb.equal(root.get("agent").get("id"), agentId);
        return list(spec.and(filterSpec(filter)), pageable);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponses.Managed> listForAdmin(Filter filter, Pageable pageable) {
        return list(filterSpec(filter), pageable);
    }

    private PageResponse<ReviewResponses.Managed> list(Specification<Review> spec, Pageable pageable) {
        var page = reviewRepository.findAll(spec, StablePaging.of(pageable));
        return PageResponse.of(page, assembler.toManaged(page.getContent()));
    }

    private static Specification<Review> filterSpec(Filter f) {
        Specification<Review> spec = (root, query, cb) -> cb.conjunction();
        if (f.tourId() != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("tour").get("id"), f.tourId()));
        if (f.rating() != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("rating"), f.rating().shortValue()));
        if (f.hidden() != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("hidden"), f.hidden()));
        if (f.replied() != null) {
            spec = spec.and((root, query, cb) -> f.replied() ? cb.isNotNull(root.get("reply")) : cb.isNull(root.get("reply")));
        }
        if (f.q() != null && !f.q().isBlank()) {
            String pattern = SearchPatterns.contains(f.q());
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("tour").get("title")), pattern, SearchPatterns.ESCAPE),
                    cb.like(cb.lower(root.get("booking").get("code")), pattern, SearchPatterns.ESCAPE),
                    cb.like(cb.lower(root.get("customer").get("fullName")), pattern, SearchPatterns.ESCAPE)));
        }
        return spec;
    }

    // ===================== Trả lời =====================

    @Transactional
    public ReviewResponses.Managed reply(Responder responder, Long reviewId, String text) {
        Review review = reviewRepository.findByIdForUpdate(reviewId)
                .filter(r -> responder.agentId() == null
                        ? r.getAgent() == null
                        : r.getAgent() != null && r.getAgent().getId().equals(responder.agentId()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá"));
        if (review.isHidden()) throw new IllegalStateException("Đánh giá đã bị ẩn nên không trả lời được");
        boolean firstReply = review.getReply() == null;
        review.setReply(text.trim());
        review.setRepliedAt(LocalDateTime.now(clock));
        if (firstReply) {
            eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(review.getCustomer().getId(),
                    WebNotifications.reviewReplied(review.getBooking().getId(), review.getTour().getTitle())));
        }
        return assembler.toManaged(List.of(review)).get(0);
    }

    // ===================== Ẩn / hiện (Admin) =====================

    @Transactional
    public ReviewResponses.Managed hide(Long adminId, Long reviewId, String reason) {
        Review review = requireForUpdate(reviewId);
        if (review.isHidden()) throw new IllegalStateException("Đánh giá đang bị ẩn");
        review.setHidden(true);
        review.setHiddenReason(reason.trim());
        review.setHiddenAt(LocalDateTime.now(clock));
        review.setHiddenBy(userRepository.getReferenceById(adminId));
        recalculate(review);
        eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(review.getCustomer().getId(),
                WebNotifications.reviewHidden(review.getBooking().getId(), review.getTour().getTitle(), reason.trim())));
        notifyAgent(review, WebNotifications.tourReviewHidden(review.getTour().getTitle(), review.getRating(), reason.trim()));
        return assembler.toManaged(List.of(review)).get(0);
    }

    @Transactional
    public ReviewResponses.Managed unhide(Long reviewId) {
        Review review = requireForUpdate(reviewId);
        if (!review.isHidden()) throw new IllegalStateException("Đánh giá đang hiển thị");
        review.setHidden(false);
        review.setHiddenReason(null);
        review.setHiddenAt(null);
        review.setHiddenBy(null);
        recalculate(review);
        eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(review.getCustomer().getId(),
                WebNotifications.reviewUnhidden(review.getBooking().getId(), review.getTour().getTitle())));
        notifyAgent(review, WebNotifications.tourReviewUnhidden(review.getTour().getTitle(), review.getRating()));
        return assembler.toManaged(List.of(review)).get(0);
    }

    /** Tour của TripConnect thì bỏ qua — chính Admin là người thao tác. */
    private void notifyAgent(Review review, NotificationEvents.WebMessage message) {
        if (review.getAgent() != null) {
            eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(review.getAgent().getId(), message));
        }
    }

    private Review requireForUpdate(Long reviewId) {
        return reviewRepository.findByIdForUpdate(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá"));
    }

    private void recalculate(Review review) {
        // Đẩy thay đổi xuống DB trước khi đếm lại
        reviewRepository.flush();
        ratingCalculator.recalculate(review.getTour().getId(), review.getAgent() == null ? null : review.getAgent().getId());
    }
}
