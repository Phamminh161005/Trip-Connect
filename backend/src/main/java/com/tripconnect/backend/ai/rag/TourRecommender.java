package com.tripconnect.backend.ai.rag;

import com.tripconnect.backend.ai.VectorMath;
import com.tripconnect.backend.dto.search.SearchResponses;
import com.tripconnect.backend.dto.search.TourSearchRequest;
import com.tripconnect.backend.entity.TourView;
import com.tripconnect.backend.enums.TourStatus;
import com.tripconnect.backend.repository.TourRepository;
import com.tripconnect.backend.repository.TourViewRepository;
import com.tripconnect.backend.service.search.SearchHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Gợi ý tour dựa trên véc-tơ nội dung:
 * - "Tour tương tự": tour gần véc-tơ của tour đang xem nhất.
 * - "Gợi ý cho bạn": trung bình véc-tơ các tour người đó xem gần đây (xem nhiều / mới xem nặng ký hơn).
 * Chỉ gợi ý tour còn lịch đặt được.
 */
@Service
@RequiredArgsConstructor
public class TourRecommender {

    /** Xem lại cùng tour trong khoảng này chỉ tính một lượt. */
    static final int VIEW_DEDUP_MINUTES = 30;
    /** Sở thích tính trên các tour xem trong ngần này ngày, tối đa ngần này tour. */
    static final int PROFILE_DAYS = 90;
    static final int PROFILE_TOURS = 15;
    /** Tour xem trước đó một bậc thì trọng số nhân với hệ số này. */
    static final double RECENCY_DECAY = 0.85;
    static final int CANDIDATES = 200;

    private final TourViewRepository viewRepository;
    private final TourRepository tourRepository;
    private final TourCandidates candidates;
    private final VectorIndex vectorIndex;
    private final Clock clock;

    /** Ghi một lượt xem trang tour (không ghi được người xem thì bỏ qua). */
    @Transactional
    public void recordView(Long tourId, Long userId, String visitorId) {
        String visitor = SearchHistoryService.isValidVisitorId(visitorId) ? visitorId : null;
        if (userId == null && visitor == null) return;
        boolean published = tourRepository.findById(tourId).map(t -> t.getStatus() == TourStatus.PUBLISHED).orElse(false);
        if (!published) return;
        LocalDateTime now = LocalDateTime.now(clock);
        if (viewRepository.viewedSince(tourId, userId, visitor, now.minusMinutes(VIEW_DEDUP_MINUTES))) return;

        TourView view = new TourView();
        view.setTourId(tourId);
        view.setUserId(userId);
        view.setVisitorId(userId == null ? visitor : null);
        view.setViewedAt(now);
        viewRepository.save(view);
    }

    /** Khách vãng lai vừa đăng nhập: lượt xem trên máy đó thành của tài khoản. */
    @Transactional
    public int claimViews(Long userId, String visitorId) {
        if (!SearchHistoryService.isValidVisitorId(visitorId)) return 0;
        return viewRepository.claim(visitorId, userId);
    }

    public List<SearchResponses.TourCard> similar(Long tourId, int limit) {
        Optional<float[]> vector = vectorIndex.tourVector(tourId);
        if (vector.isEmpty()) return List.of();
        return rank(vector.get(), Set.of(tourId), limit);
    }

    /** Rỗng nếu chưa xem tour nào (trang chủ khi đó ẩn mục này). */
    @Transactional(readOnly = true)
    public List<SearchResponses.TourCard> forViewer(Long userId, String visitorId, int limit) {
        String visitor = SearchHistoryService.isValidVisitorId(visitorId) ? visitorId : null;
        if (userId == null && visitor == null) return List.of();
        List<Object[]> recent = viewRepository.countRecentByViewer(userId, userId == null ? visitor : null,
                LocalDateTime.now(clock).minusDays(PROFILE_DAYS), PageRequest.of(0, PROFILE_TOURS));

        List<float[]> vectors = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        double recency = 1;
        for (Object[] row : recent) {
            Long id = ((Number) row[0]).longValue();
            long views = ((Number) row[1]).longValue();
            seen.add(id);
            Optional<float[]> v = vectorIndex.tourVector(id);
            if (v.isPresent()) {
                vectors.add(v.get());
                // Xem lại nhiều lần thì quan tâm hơn, nhưng không để một tour lấn át (log)
                weights.add(recency * (1 + Math.log(views)));
            }
            recency *= RECENCY_DECAY;
        }
        if (vectors.isEmpty()) return List.of();
        return rank(VectorMath.weightedMean(vectors, weights), seen, limit);
    }

    private List<SearchResponses.TourCard> rank(float[] query, Set<Long> exclude, int limit) {
        Map<Long, SearchResponses.TourCard> pool = candidates.find(new TourSearchRequest(), CANDIDATES).cards().stream()
                .filter(c -> !exclude.contains(c.id()))
                .collect(Collectors.toMap(SearchResponses.TourCard::id, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        return vectorIndex.nearestTours(query, pool.keySet(), limit, 0).stream()
                .map(hit -> pool.get(hit.item()))
                .toList();
    }
}
