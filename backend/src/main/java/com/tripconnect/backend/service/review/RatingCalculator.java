package com.tripconnect.backend.service.review;

import com.tripconnect.backend.entity.Tour;
import com.tripconnect.backend.repository.AgentProfileRepository;
import com.tripconnect.backend.repository.ReviewRepository;
import com.tripconnect.backend.repository.TourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Tính lại điểm trung bình của tour và của Agent từ các đánh giá đang hiện.
 * Khóa dòng tour (rồi hồ sơ Agent) trước khi đếm: 2 đánh giá gửi cùng lúc xếp hàng, người sau đếm cả đánh giá
 * của người trước -> điểm không bị ghi đè sai.
 */
@Component
@RequiredArgsConstructor
public class RatingCalculator {

    private final ReviewRepository reviewRepository;
    private final TourRepository tourRepository;
    private final AgentProfileRepository agentProfileRepository;

    record Stats(BigDecimal average, int count) {
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recalculate(Long tourId, Long agentId) {
        Tour tour = tourRepository.findByIdForUpdate(tourId).orElseThrow();
        Stats tourStats = toStats(reviewRepository.visibleStatsByTour(tourId));
        tour.setRating(tourStats.average());
        tour.setRatingCount(tourStats.count());

        if (agentId != null) {
            agentProfileRepository.findByUserIdForUpdate(agentId).ifPresent(profile -> {
                Stats agentStats = toStats(reviewRepository.visibleStatsByAgent(agentId));
                profile.setRating(agentStats.average());
                profile.setRatingCount(agentStats.count());
            });
        }
    }

    /** Kết quả [avg, count] -> điểm làm tròn 2 chữ số (null khi chưa có đánh giá). */
    static Stats toStats(List<Object[]> rows) {
        Object[] row = rows.isEmpty() ? new Object[]{null, 0L} : rows.get(0);
        int count = row[1] == null ? 0 : ((Number) row[1]).intValue();
        BigDecimal average = count == 0 || row[0] == null ? null
                : BigDecimal.valueOf(((Number) row[0]).doubleValue()).setScale(2, RoundingMode.HALF_UP);
        return new Stats(average, count);
    }
}
