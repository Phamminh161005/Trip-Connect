package com.tripconnect.backend.service.review;

import com.tripconnect.backend.dto.review.ReviewRequests;
import com.tripconnect.backend.entity.*;
import com.tripconnect.backend.enums.BookingStatus;
import com.tripconnect.backend.repository.BookingRepository;
import com.tripconnect.backend.repository.ReviewRepository;
import com.tripconnect.backend.service.NotificationEvents;
import com.tripconnect.backend.storage.FileStorageService;
import com.tripconnect.backend.storage.FileValidator;
import com.tripconnect.backend.storage.TransactionalFileCleanup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 4, 9, 0);
    private static final long CUSTOMER_ID = 5L;
    private static final long AGENT_ID = 9L;

    @Mock private ReviewRepository reviewRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private RatingCalculator ratingCalculator;
    @Mock private ReviewAssembler assembler;
    @Mock private FileStorageService fileStorageService;
    @Mock private FileValidator fileValidator;
    @Mock private TransactionalFileCleanup fileCleanup;
    @Mock private ApplicationEventPublisher eventPublisher;

    private ReviewService service;
    private Booking booking;

    @BeforeEach
    void setUp() {
        service = new ReviewService(reviewRepository, bookingRepository, ratingCalculator, assembler, fileStorageService,
                fileValidator, fileCleanup, eventPublisher, Clock.fixed(NOW.atZone(VN).toInstant(), VN));
        User customer = new User();
        customer.setId(CUSTOMER_ID);
        User agent = new User();
        agent.setId(AGENT_ID);
        Tour tour = new Tour();
        tour.setId(3L);
        tour.setTitle("Sa Pa 3N2Đ");
        booking = new Booking();
        booking.setId(1L);
        booking.setCustomer(customer);
        booking.setAgent(agent);
        booking.setTour(tour);
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(NOW.minusDays(2));
        lenient().when(bookingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));
        lenient().when(reviewRepository.findByBookingId(1L)).thenReturn(Optional.empty());
    }

    private static ReviewRequests.Write request(int rating) {
        ReviewRequests.Write request = new ReviewRequests.Write();
        request.setRating(rating);
        request.setComment("  Hướng dẫn viên nhiệt tình, lịch trình hợp lý.  ");
        return request;
    }

    @Test
    void create_savesReview_recalculatesRatings_andNotifiesAgent() {
        service.create(CUSTOMER_ID, 1L, request(4));

        ArgumentCaptor<Review> saved = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(saved.capture());
        assertThat(saved.getValue().getRating()).isEqualTo((short) 4);
        assertThat(saved.getValue().getComment()).isEqualTo("Hướng dẫn viên nhiệt tình, lịch trình hợp lý.");
        assertThat(saved.getValue().getAgent().getId()).isEqualTo(AGENT_ID);
        verify(ratingCalculator).recalculate(3L, AGENT_ID);
        verify(eventPublisher).publishEvent(any(NotificationEvents.UserWebEvent.class));
    }

    @Test
    void create_platformTourReviewNotifiesAdmins() {
        booking.setAgent(null);

        service.create(CUSTOMER_ID, 1L, request(5));

        verify(ratingCalculator).recalculate(3L, null);
        verify(eventPublisher).publishEvent(any(NotificationEvents.AdminWebEvent.class));
    }

    @Test
    void create_rejectsNotCompletedExpiredDuplicateOrForeignBookings() {
        booking.setStatus(BookingStatus.PAID);
        assertThatThrownBy(() -> service.create(CUSTOMER_ID, 1L, request(5))).hasMessageContaining("đã hoàn thành");

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(NOW.minusDays(61));
        assertThatThrownBy(() -> service.create(CUSTOMER_ID, 1L, request(5))).hasMessageContaining("60 ngày");

        booking.setCompletedAt(NOW.minusDays(1));
        when(reviewRepository.findByBookingId(1L)).thenReturn(Optional.of(new Review()));
        assertThatThrownBy(() -> service.create(CUSTOMER_ID, 1L, request(5))).hasMessageContaining("đã đánh giá");

        assertThatThrownBy(() -> service.create(77L, 1L, request(5))).hasMessageContaining("Không tìm thấy");
        verify(reviewRepository, never()).save(any());
    }

    private Review existingReview(LocalDateTime createdAt) {
        Review review = new Review();
        review.setId(20L);
        review.setBooking(booking);
        review.setTour(booking.getTour());
        review.setAgent(booking.getAgent());
        review.setCustomer(booking.getCustomer());
        review.setRating((short) 3);
        review.setComment("Ổn");
        review.setCreatedAt(createdAt);
        when(reviewRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(review));
        return review;
    }

    @Test
    void update_withinSevenDays_changesReview_andRecalculatesWhenRatingChanges() {
        Review review = existingReview(NOW.minusDays(3));

        service.update(CUSTOMER_ID, 20L, request(5));

        assertThat(review.getRating()).isEqualTo((short) 5);
        assertThat(review.getUpdatedAt()).isEqualTo(NOW);
        verify(ratingCalculator).recalculate(3L, AGENT_ID);
    }

    @Test
    void update_afterSevenDaysOrWhenHidden_isRejected() {
        Review review = existingReview(NOW.minusDays(8));
        assertThatThrownBy(() -> service.update(CUSTOMER_ID, 20L, request(5))).hasMessageContaining("7 ngày");

        review.setCreatedAt(NOW.minusDays(1));
        review.setHidden(true);
        assertThatThrownBy(() -> service.update(CUSTOMER_ID, 20L, request(5))).hasMessageContaining("bị ẩn");
        verifyNoInteractions(ratingCalculator);
    }

    @Test
    void uploadImage_isLimitedToFive() {
        Review review = existingReview(NOW.minusDays(1));
        for (int i = 0; i < ReviewRules.MAX_IMAGES; i++) review.getImages().add(new ReviewImage());

        assertThatThrownBy(() -> service.uploadImage(CUSTOMER_ID, 20L, null)).hasMessageContaining("tối đa 5 ảnh");
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void summaryHasAllFiveLevels_andAverage() {
        when(reviewRepository.countVisibleByRating(3L)).thenReturn(List.of(new Object[]{(short) 5, 3L}, new Object[]{(short) 4, 1L}));

        var summary = service.summary(3L);

        assertThat(summary.count()).isEqualTo(4);
        assertThat(summary.average()).isEqualByComparingTo("4.75");
        assertThat(summary.distribution()).containsEntry(5, 3L).containsEntry(4, 1L).containsEntry(1, 0L).hasSize(5);
    }
}
