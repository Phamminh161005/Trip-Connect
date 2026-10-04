package com.tripconnect.backend.service.tour;

import com.tripconnect.backend.entity.Tour;
import com.tripconnect.backend.entity.User;
import com.tripconnect.backend.enums.TourStatus;
import com.tripconnect.backend.exception.ForbiddenException;
import com.tripconnect.backend.exception.ResourceNotFoundException;
import com.tripconnect.backend.repository.TourRepository;
import com.tripconnect.backend.security.AgentAccessGuard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TourAccessTest {

    private static final TourActor AGENT = new TourActor(7L, false);
    private static final TourActor OTHER_AGENT = new TourActor(8L, false);
    private static final TourActor ADMIN = new TourActor(1L, true);

    @Mock private TourRepository tourRepository;
    @Mock private AgentAccessGuard agentAccessGuard;

    @InjectMocks private TourAccess access;

    @Test
    void agentEditsPublishedTour_tourGoesBackToDraftForReview() {
        Tour tour = agentTour(TourStatus.PUBLISHED);
        tour.setStatusReason("lý do cũ");

        access.onContentChanged(tour, AGENT);

        assertThat(tour.getStatus()).isEqualTo(TourStatus.DRAFT);
        assertThat(tour.getStatusReason()).isNull();
    }

    @Test
    void agentEditsHiddenTour_alsoNeedsReview() {
        Tour tour = agentTour(TourStatus.HIDDEN);
        access.onContentChanged(tour, AGENT);
        assertThat(tour.getStatus()).isEqualTo(TourStatus.DRAFT);
    }

    @Test
    void adminEditsPlatformTour_staysPublished() {
        Tour tour = new Tour();
        tour.setStatus(TourStatus.PUBLISHED);
        access.onContentChanged(tour, ADMIN);
        assertThat(tour.getStatus()).isEqualTo(TourStatus.PUBLISHED);
    }

    @Test
    void pendingOrSuspendedTour_contentIsLocked() {
        assertThatThrownBy(() -> access.requireContentEditable(agentTour(TourStatus.PENDING_APPROVAL)))
                .hasMessageContaining("rút lại");
        assertThatThrownBy(() -> access.requireContentEditable(agentTour(TourStatus.SUSPENDED)))
                .hasMessageContaining("đình chỉ");
    }

    @Test
    void agentOpensAnotherAgentsTour_looksLikeNotFound() {
        when(tourRepository.findById(10L)).thenReturn(Optional.of(agentTour(TourStatus.PUBLISHED)));

        assertThatThrownBy(() -> access.requireManageable(10L, OTHER_AGENT))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void adminCannotEditAgentTour() {
        when(tourRepository.findById(10L)).thenReturn(Optional.of(agentTour(TourStatus.PUBLISHED)));

        assertThatThrownBy(() -> access.requireManageable(10L, ADMIN))
                .isInstanceOf(ForbiddenException.class);
    }

    private static Tour agentTour(TourStatus status) {
        User agent = new User();
        agent.setId(AGENT.userId());
        Tour tour = new Tour();
        tour.setId(10L);
        tour.setAgent(agent);
        tour.setStatus(status);
        return tour;
    }
}
