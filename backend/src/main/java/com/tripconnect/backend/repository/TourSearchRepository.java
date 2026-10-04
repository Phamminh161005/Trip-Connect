package com.tripconnect.backend.repository;

import com.tripconnect.backend.dto.search.TourSearchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Tìm tour cho khách bằng SQL thuần: cần lọc theo LỊCH KHỞI HÀNH (ngày, giá) và sắp xếp theo giá thấp nhất /
 * ngày đi gần nhất. Chỉ trả về id + số liệu tính được;
 * thông tin hiển thị nạp sau bằng JPA (TourSearchService).
 *
 * Tour "tìm thấy được" = đang bán (PUBLISHED), của TripConnect hoặc của Agent còn hoạt động + hồ sơ đã duyệt,
 * và có ít nhất một lịch ĐANG MỞ BÁN, CHƯA KHỞI HÀNH, khớp bộ lọc ngày / giá.
 */
@Repository
@RequiredArgsConstructor
public class TourSearchRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /** Một tour khớp bộ lọc + giá thấp nhất / ngày đi sớm nhất trong các lịch khớp. */
    public record Hit(long tourId, long minPrice, LocalDate nextDate, int departureCount) {
    }

    public record Page(List<Hit> hits, long total) {
    }

    /** Điểm đến + số tour tìm thấy được ở đó + một tour tiêu biểu (lấy ảnh bìa). */
    public record DestinationCount(long locationId, long tourCount, long sampleTourId) {
    }

    // Phần chung: tour tìm thấy được + lịch đang mở bán của nó
    private static final String VISIBLE_TOURS = """
            FROM tours t
            JOIN tour_departures d ON d.tour_id = t.id AND d.status = 'OPEN' AND d.start_date > :today
            LEFT JOIN users u ON u.id = t.agent_id
            LEFT JOIN agent_profiles p ON p.user_id = t.agent_id
            WHERE t.status = 'PUBLISHED'
              AND (t.agent_id IS NULL OR (u.is_active AND p.status = 'APPROVED'))
            """;

    public Page search(TourSearchRequest r, List<String> keywordTokens, LocalDate today) {
        MapSqlParameterSource params = new MapSqlParameterSource("today", today);
        StringBuilder where = new StringBuilder();

        // Lọc trên lịch khởi hành: cùng một lịch phải khớp cả ngày lẫn giá
        if (r.getDateFrom() != null) {
            where.append(" AND d.start_date >= :dateFrom");
            params.addValue("dateFrom", r.getDateFrom());
        }
        if (r.getDateTo() != null) {
            where.append(" AND d.start_date <= :dateTo");
            params.addValue("dateTo", r.getDateTo());
        }
        if (r.getPriceMin() != null) {
            where.append(" AND d.adult_price >= :priceMin");
            params.addValue("priceMin", r.getPriceMin());
        }
        if (r.getPriceMax() != null) {
            where.append(" AND d.adult_price <= :priceMax");
            params.addValue("priceMax", r.getPriceMax());
        }

        // Lọc trên tour
        for (int i = 0; i < keywordTokens.size(); i++) {
            // Khớp ĐẦU TỪ: "ha" khớp "ha long", "ha noi" nhưng không khớp "thanh", "kham pha".
            // Token chỉ gồm a-z, 0-9 (SearchText.normalize) nên không cần thoát ký tự % _
            where.append(" AND (' ' || t.search_text) LIKE :kw").append(i);
            params.addValue("kw" + i, "% " + keywordTokens.get(i) + "%");
        }
        if (r.getDestinationId() != null) {
            where.append(" AND EXISTS (SELECT 1 FROM tour_destinations td WHERE td.tour_id = t.id AND td.location_id = :destinationId)");
            params.addValue("destinationId", r.getDestinationId());
        }
        if (r.getDepartureLocationId() != null) {
            where.append(" AND t.departure_location_id = :departureLocationId");
            params.addValue("departureLocationId", r.getDepartureLocationId());
        }
        if (r.getDurationMin() != null) {
            where.append(" AND t.duration_days >= :durationMin");
            params.addValue("durationMin", r.getDurationMin());
        }
        if (r.getDurationMax() != null) {
            where.append(" AND t.duration_days <= :durationMax");
            params.addValue("durationMax", r.getDurationMax());
        }
        if (r.getCategoryIds() != null && !r.getCategoryIds().isEmpty()) {
            where.append(" AND EXISTS (SELECT 1 FROM tour_category_links c WHERE c.tour_id = t.id AND c.category_id IN (:categoryIds))");
            params.addValue("categoryIds", r.getCategoryIds());
        }
        if (r.getInternational() != null) {
            where.append(" AND t.is_international = :international");
            params.addValue("international", r.getInternational());
        }

        String sql = """
                WITH matched AS (
                    SELECT t.id, MIN(d.adult_price) AS min_price, MIN(d.start_date) AS next_date, COUNT(*) AS departure_count
                """ + VISIBLE_TOURS + where + """
                    GROUP BY t.id
                )
                SELECT m.id, m.min_price, m.next_date, m.departure_count, COUNT(*) OVER () AS total
                FROM matched m
                JOIN tours t ON t.id = m.id
                ORDER BY\s""" + orderBy(r.getSort()) + """
                , t.id DESC
                LIMIT :limit OFFSET :offset
                """;
        params.addValue("limit", r.getSize());
        params.addValue("offset", (long) r.getPage() * r.getSize());

        List<Hit> hits = new ArrayList<>();
        long[] total = {0};
        jdbc.query(sql, params, rs -> {
            hits.add(new Hit(rs.getLong("id"), rs.getLong("min_price"), rs.getObject("next_date", LocalDate.class),
                    rs.getInt("departure_count")));
            total[0] = rs.getLong("total");
        });
        // Trang vượt quá số kết quả thì không có dòng nào để đọc tổng -> đếm lại
        if (hits.isEmpty() && r.getPage() > 0) {
            total[0] = count(where.toString(), params);
        }
        return new Page(hits, total[0]);
    }

    /** Điểm đến có nhiều tour tìm thấy được nhất (khối "Điểm đến nổi bật" ở trang chủ). */
    public List<DestinationCount> popularDestinations(LocalDate today, int limit) {
        String sql = """
                SELECT td.location_id, COUNT(DISTINCT t.id) AS tour_count, MAX(t.id) AS sample_tour_id
                """ + VISIBLE_TOURS.replace("FROM tours t", "FROM tours t JOIN tour_destinations td ON td.tour_id = t.id") + """
                GROUP BY td.location_id
                ORDER BY tour_count DESC, td.location_id
                LIMIT :limit
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("today", today).addValue("limit", limit);
        return jdbc.query(sql, params, (rs, i) ->
                new DestinationCount(rs.getLong("location_id"), rs.getLong("tour_count"), rs.getLong("sample_tour_id")));
    }

    private long count(String where, MapSqlParameterSource params) {
        Long value = jdbc.queryForObject("SELECT COUNT(DISTINCT t.id) " + VISIBLE_TOURS + where, params, Long.class);
        return value == null ? 0 : value;
    }

    private static String orderBy(TourSearchRequest.Sort sort) {
        return switch (sort) {
            case PRICE_ASC -> "m.min_price ASC";
            case PRICE_DESC -> "m.min_price DESC";
            case DEPARTURE_SOON -> "m.next_date ASC";
            case NEWEST -> "t.published_at DESC NULLS LAST";
            case RATING -> "t.rating DESC NULLS LAST, t.rating_count DESC";
            // Có đánh giá tốt lên trước, sau đó tour mới công khai
            case RECOMMENDED -> "(t.rating_count > 0) DESC, t.rating DESC NULLS LAST, t.published_at DESC NULLS LAST";
        };
    }
}
