package com.tripconnect.backend.service.review;

import com.tripconnect.backend.entity.*;
import com.tripconnect.backend.repository.ReviewRepository;
import com.tripconnect.backend.repository.UserRepository;
import com.tripconnect.backend.service.NotificationEvents;
import com.tripconnect.backend.service.review.ReviewModerationService.Responder;
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
class ReviewModerationServiceTest {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 4, 9, 0);
    private static final long AGENT_ID = 9L;

    @Mock private ReviewRepository reviewRepository;
    @Mock private UserRepository userRepository;
    @Mock private RatingCalculator ratingCalculator;
    @Mock private ReviewAssembler assembler;
    @Mock private ApplicationEventPublisher eventPublisher;

    private ReviewModerationService service;
    private Review review;

    @BeforeEach
    void setUp() {
        service = new ReviewModerationService(reviewRepository, userRepository, ratingCalculator, assembler, eventPublisher,
                Clock.fixed(NOW.atZone(VN).toInstant(), VN));
        User agent = new User();
        agent.setId(AGENT_ID);
        User customer = new User();
        customer.setId(5L);
        Tour tour = new Tour();
        tour.setId(3L);
        tour.setTitle("Sa Pa 3N2Đ");
        Booking booking = new Booking();
        booking.setId(1L);
        review = new Review();
        review.setId(20L);
        review.setTour(tour);
        review.setAgent(agent);
        review.setCustomer(customer);
        review.setBooking(booking);
        lenient().when(reviewRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(review));
        lenient().when(assembler.toManaged(any())).thenReturn(List.of(mock(com.tripconnect.backend.dto.review.ReviewResponses.Managed.class)));
    }

    @Test
    void agentRepliesToOwnTourReview_customerNotifiedOnlyOnFirstReply() {
        service.reply(Responder.agent(AGENT_ID), 20L, "  Cảm ơn anh/chị!  ");
        service.reply(Responder.agent(AGENT_ID), 20L, "Cảm ơn anh/chị đã góp ý!");

        assertThat(review.getReply()).isEqualTo("Cảm ơn anh/chị đã góp ý!");
        assertThat(review.getRepliedAt()).isEqualTo(NOW);
        verify(eventPublisher, times(1)).publishEvent(any(NotificationEvents.UserWebEvent.class));
    }

    @Test
    void cannotReplyToSomeoneElsesReview_norFromAdminOnAgentTour() {
        assertThatThrownBy(() -> service.reply(Responder.agent(77L), 20L, "x")).hasMessageContaining("Không tìm thấy");
        assertThatThrownBy(() -> service.reply(Responder.platform(), 20L, "x")).hasMessageContaining("Không tìm thấy");
    }

    @Test
    void hideExcludesFromRatings_thenUnhideRestores_customerAndAgentToldBothTimes() {
        ArgumentCaptor<Object> events = ArgumentCaptor.forClass(Object.class);
        service.hide(1L, 20L, " Nội dung quảng cáo ");

        assertThat(review.isHidden()).isTrue();
        assertThat(review.getHiddenReason()).isEqualTo("Nội dung quảng cáo");
        verify(ratingCalculator).recalculate(3L, AGENT_ID);
        assertThatThrownBy(() -> service.reply(Responder.agent(AGENT_ID), 20L, "x")).hasMessageContaining("bị ẩn");

        service.unhide(20L);
        assertThat(review.isHidden()).isFalse();
        assertThat(review.getHiddenReason()).isNull();
        verify(ratingCalculator, times(2)).recalculate(3L, AGENT_ID);
        verify(eventPublisher, times(4)).publishEvent(events.capture());
        assertThat(events.getAllValues()).extracting(e -> ((NotificationEvents.UserWebEvent) e).userId())
                .containsExactly(5L, AGENT_ID, 5L, AGENT_ID);
    }

    @Test
    void hidingPlatformTourReviewDoesNotNotifyAnyAgent() {
        review.setAgent(null);

        service.hide(1L, 20L, "Spam");

        verify(eventPublisher, times(1)).publishEvent(any(NotificationEvents.UserWebEvent.class));
    }
}
