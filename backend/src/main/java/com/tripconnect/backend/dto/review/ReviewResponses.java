package com.tripconnect.backend.dto.review;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class ReviewResponses {

    private ReviewResponses() {
    }

    public record Image(Long id, String url) {
    }

    /** Đánh giá hiện trên trang tour. reviewerName đã rút gọn (vd "Phạm V. Minh"). */
    public record Public(Long id, int rating, String comment, String reviewerName, LocalDate departureDate,
                         List<Image> images, String reply, LocalDateTime repliedAt, LocalDateTime createdAt,
                         boolean edited) {
    }

    /** nextCursor = null khi đã hết. */
    public record PublicPage(List<Public> items, Long nextCursor) {
    }

    /** distribution: số sao -> số lượt (đủ 5 mức, kể cả 0). */
    public record Summary(BigDecimal average, int count, Map<Integer, Long> distribution) {
    }

    /** Đánh giá của chính khách (trang chi tiết đơn). */
    public record Mine(Long id, Long bookingId, Long tourId, int rating, String comment, List<Image> images,
                       String reply, LocalDateTime repliedAt, boolean hidden, String hiddenReason,
                       boolean canEdit, LocalDateTime editableUntil, LocalDateTime createdAt) {
    }

    /** Đánh giá trong trang quản lý (Agent / Admin). */
    public record Managed(Long id, Long tourId, String tourTitle, Long bookingId, String bookingCode,
                          String customerName, String customerEmail, LocalDate departureDate, int rating,
                          String comment, List<Image> images, String reply, LocalDateTime repliedAt,
                          boolean hidden, String hiddenReason, LocalDateTime hiddenAt, boolean platformTour,
                          LocalDateTime createdAt) {
    }
}
