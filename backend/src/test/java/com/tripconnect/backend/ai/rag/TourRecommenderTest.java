package com.tripconnect.backend.ai.rag;

import com.tripconnect.backend.ai.VectorMath;
import com.tripconnect.backend.dto.search.SearchResponses;
import com.tripconnect.backend.entity.Tour;
import com.tripconnect.backend.entity.TourView;
import com.tripconnect.backend.enums.TourStatus;
import com.tripconnect.backend.repository.TourRepository;
import com.tripconnect.backend.repository.TourViewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/** Gợi ý tour: tour tương tự, gợi ý theo lượt xem, ghi lượt xem. */
@ExtendWith(MockitoExtension.class)
class TourRecommenderTest {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 7, 10, 0);
    private static final String VISITOR = "11111111-2222-4333-8444-555555555555";

    @Mock private TourViewRepository viewRepository;
    @Mock private TourRepository tourRepository;
    @Mock private TourCandidates candidates;

    private final VectorIndex index = new VectorIndex();
    private TourRecommender recommender;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(VN).toInstant(), VN);
        recommender = new TourRecommender(viewRepository, tourRepository, candidates, index, clock);
        // Véc-tơ 2 chiều: tour 1, 2 "biển"; tour 3, 4 "núi"; tour 5 ở giữa
        index.replaceTours(Map.of(
                1L, vec(1, 0), 2L, vec(0.95f, 0.05f),
                3L, vec(0, 1), 4L, vec(0.05f, 0.95f),
                5L, vec(0.5f, 0.5f)));
        lenient().when(candidates.find(any(), anyInt())).thenReturn(new TourCandidates.Result(
                List.of(card(1), card(2), card(3), card(4), card(5)), 5));
    }

    @Test
    void similarExcludesTheTourItselfAndRanksByCloseness() {
        assertThat(recommender.similar(1L, 3)).extracting(SearchResponses.TourCard::id).containsExactly(2L, 5L, 4L);
        assertThat(recommender.similar(99L, 3)).isEmpty();
    }

    @Test
    void forViewerFollowsRecentInterestsAndSkipsSeenTours() {
        // Gần đây xem tour núi (3) nhiều lần, trước đó xem tour biển (1) một lần
        when(viewRepository.countRecentByViewer(isNull(), eq(VISITOR), eq(NOW.minusDays(TourRecommender.PROFILE_DAYS)), any()))
                .thenReturn(List.of(new Object[]{3L, 4L}, new Object[]{1L, 1L}));

        List<SearchResponses.TourCard> result = recommender.forViewer(null, VISITOR, 2);

        assertThat(result).extracting(SearchResponses.TourCard::id).containsExactly(4L, 5L);
    }

    @Test
    void forViewerUsesAccountWhenSignedInAndIsEmptyWithoutHistory() {
        when(viewRepository.countRecentByViewer(eq(5L), isNull(), any(), any())).thenReturn(List.of());

        assertThat(recommender.forViewer(5L, VISITOR, 4)).isEmpty();
        assertThat(recommender.forViewer(null, "không hợp lệ", 4)).isEmpty();
        assertThat(recommender.forViewer(null, null, 4)).isEmpty();
    }

    @Test
    void recordsViewOfPublishedTourOncePerWindow() {
        Tour tour = new Tour();
        tour.setStatus(TourStatus.PUBLISHED);
        when(tourRepository.findById(1L)).thenReturn(Optional.of(tour));
        when(viewRepository.viewedSince(1L, null, VISITOR, NOW.minusMinutes(TourRecommender.VIEW_DEDUP_MINUTES)))
                .thenReturn(false, true);

        recommender.recordView(1L, null, VISITOR);
        recommender.recordView(1L, null, VISITOR);

        ArgumentCaptor<TourView> saved = ArgumentCaptor.forClass(TourView.class);
        verify(viewRepository, times(1)).save(saved.capture());
        assertThat(saved.getValue().getVisitorId()).isEqualTo(VISITOR);
        assertThat(saved.getValue().getUserId()).isNull();
        assertThat(saved.getValue().getViewedAt()).isEqualTo(NOW);
    }

    @Test
    void ignoresViewsOfUnpublishedToursOrUnknownViewers() {
        Tour hidden = new Tour();
        hidden.setStatus(TourStatus.HIDDEN);
        when(tourRepository.findById(2L)).thenReturn(Optional.of(hidden));

        recommender.recordView(2L, 5L, null);
        recommender.recordView(1L, null, "bad id");

        verify(viewRepository, never()).save(any());
    }

    private static float[] vec(float a, float b) {
        return VectorMath.normalize(new float[]{a, b});
    }

    private static SearchResponses.TourCard card(long id) {
        return new SearchResponses.TourCard(id, "Tour " + id, null, 3, 2, "Hà Nội", List.of(), false,
                List.of(), null, 0, "TripConnect", 1_000_000, LocalDate.of(2026, 11, 1), 1);
    }
}
