package com.tripconnect.backend.service.tour;

import com.tripconnect.backend.entity.Tour;
import com.tripconnect.backend.entity.User;
import com.tripconnect.backend.enums.TourStatus;
import com.tripconnect.backend.repository.*;
import com.tripconnect.backend.service.NotificationEvents;
import com.tripconnect.backend.storage.TransactionalFileCleanup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TourServiceTest {

    private static final TourActor AGENT = new TourActor(7L, false);
    private static final TourActor ADMIN = new TourActor(1L, true);

    @Mock private TourRepository tourRepository;
    @Mock private TourImageRepository imageRepository;
    @Mock private UserRepository userRepository;
    @Mock private AgentProfileRepository agentProfileRepository;
    @Mock private TourAccess access;
    @Mock private TourContentWriter contentWriter;
    @Mock private TourAssembler assembler;
    @Mock private TourBookingStats bookingStats;
    @Mock private TransactionalFileCleanup fileCleanup;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private TourService service;

    @Test
    void submit_completeDraft_goesPendingAndNotifiesAdmins() {
        Tour tour = agentTour(TourStatus.DRAFT);
        when(access.requireManageable(10L, AGENT)).thenReturn(tour);
        when(assembler.missingItems(any(), anyLong())).thenReturn(List.of());
        when(agentProfileRepository.findByUserId(AGENT.userId())).thenReturn(Optional.empty());

        service.submit(AGENT, 10L);

        assertThat(tour.getStatus()).isEqualTo(TourStatus.PENDING_APPROVAL);
        assertThat(tour.getSubmittedAt()).isNotNull();
        verify(eventPublisher).publishEvent(any(NotificationEvents.AdminWebEvent.class));
    }

    @Test
    void submit_missingImages_isRejected() {
        Tour tour = agentTour(TourStatus.DRAFT);
        when(access.requireManageable(10L, AGENT)).thenReturn(tour);
        when(assembler.missingItems(any(), anyLong())).thenReturn(List.of("Cần ít nhất 3 ảnh tour (hiện có 1)"));

        assertThatThrownBy(() -> service.submit(AGENT, 10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("3 ảnh");
        assertThat(tour.getStatus()).isEqualTo(TourStatus.DRAFT);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void platformTour_isPublishedDirectlyWithoutReview() {
        assertThatThrownBy(() -> service.submit(ADMIN, 10L)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.publish(AGENT, 10L)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void delete_tourWithBookings_isRejected() {
        Tour tour = agentTour(TourStatus.HIDDEN);
        when(access.requireManageable(10L, AGENT)).thenReturn(tour);
        when(bookingStats.tourHasBookings(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(AGENT, 10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Tạm ẩn");
        verify(tourRepository, never()).delete(any(Tour.class));
    }

    @Test
    void delete_suspendedTour_isRejected() {
        when(access.requireManageable(10L, AGENT)).thenReturn(agentTour(TourStatus.SUSPENDED));

        assertThatThrownBy(() -> service.delete(AGENT, 10L)).hasMessageContaining("đình chỉ");
        verify(tourRepository, never()).delete(any(Tour.class));
    }

    private static Tour agentTour(TourStatus status) {
        User agent = new User();
        agent.setId(AGENT.userId());
        agent.setEmail("agent@example.com");
        Tour tour = new Tour();
        tour.setId(10L);
        tour.setTitle("Hạ Long 2N1Đ");
        tour.setAgent(agent);
        tour.setStatus(status);
        return tour;
    }
}
