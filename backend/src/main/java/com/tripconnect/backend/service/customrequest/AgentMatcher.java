package com.tripconnect.backend.service.customrequest;

import com.tripconnect.backend.entity.Location;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * Chấm điểm mức phù hợp của Agent với một yêu cầu (tài liệu nghiệp vụ):
 * tổng = 60% khớp địa điểm + 40% điểm đánh giá đã chuẩn hóa.
 * <ul>
 *   <li>Mỗi điểm đến: Agent phục vụ đúng nơi đó = 100; phục vụ nơi khác cùng quốc gia = 60; không = 0.
 *       Điểm địa điểm = trung bình các điểm đến.</li>
 *   <li>Điểm đánh giá r (1–5) -> (r − 1) / 4 × 100. Agent chưa có đánh giá tính 60 (khoảng 3,4 sao),
 *       để Agent mới vẫn có cơ hội được giao.</li>
 * </ul>
 */
public final class AgentMatcher {

    private AgentMatcher() {
    }

    static final double LOCATION_WEIGHT = 0.6;
    static final double RATING_WEIGHT = 0.4;
    static final double SAME_LOCATION = 100;
    static final double SAME_COUNTRY = 60;
    /** Tương đương khoảng 3,4 sao. */
    static final double NO_RATING = 60;

    public static double locationScore(Collection<Location> destinations, Collection<Location> serviceAreas) {
        if (destinations.isEmpty()) return 0;
        double sum = 0;
        for (Location destination : destinations) {
            sum += destinationScore(destination, serviceAreas);
        }
        return sum / destinations.size();
    }

    private static double destinationScore(Location destination, Collection<Location> serviceAreas) {
        double best = 0;
        for (Location area : serviceAreas) {
            if (area.getId().equals(destination.getId())) return SAME_LOCATION;
            if (area.getCountry() != null && area.getCountry().equalsIgnoreCase(destination.getCountry())) best = SAME_COUNTRY;
        }
        return best;
    }

    public static double ratingScore(BigDecimal rating) {
        if (rating == null) return NO_RATING;
        return (rating.doubleValue() - 1) / 4 * 100;
    }

    /** Làm tròn 1 chữ số thập phân. */
    public static BigDecimal total(double locationScore, double ratingScore) {
        return BigDecimal.valueOf(LOCATION_WEIGHT * locationScore + RATING_WEIGHT * ratingScore).setScale(1, RoundingMode.HALF_UP);
    }
}
