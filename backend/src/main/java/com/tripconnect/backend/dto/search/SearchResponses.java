package com.tripconnect.backend.dto.search;

import com.tripconnect.backend.dto.LocationResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Các DTO trả về của tìm kiếm tour. */
public final class SearchResponses {

    private SearchResponses() {
    }

    /** Thẻ tour ở trang tìm kiếm / trang chủ. Giá và ngày tính trên các lịch khớp bộ lọc. */
    public record TourCard(
            Long id,
            String title,
            String coverImageUrl,
            int durationDays,
            int durationNights,
            String departureLocation,
            List<String> destinations,
            boolean international,
            /* 2 điểm nổi bật đầu tiên */
            List<String> highlights,
            BigDecimal rating,
            int ratingCount,
            /* "TripConnect" với tour của nền tảng */
            String providerName,
            long minPrice,
            LocalDate nextDepartureDate,
            int departureCount
    ) {
    }

    public record PopularDestination(LocationResponse location, long tourCount, String coverImageUrl) {
    }

    /** Một mục "tìm kiếm gần đây" (các lần tìm trùng từ khóa + điểm đến được gộp làm một). */
    public record RecentSearch(
            Long id,
            String keyword,
            LocationResponse destination,
            LocalDate dateFrom,
            LocalDate dateTo,
            LocalDateTime searchedAt
    ) {
    }
}
