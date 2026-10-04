package com.tripconnect.backend.service.review;

import com.tripconnect.backend.entity.Booking;
import com.tripconnect.backend.entity.Review;
import com.tripconnect.backend.enums.BookingStatus;

import java.time.LocalDateTime;
import java.util.Arrays;

/** Quy tắc đánh giá (không truy cập DB). */
public final class ReviewRules {

    private ReviewRules() {
    }

    /** Viết được trong ngần này ngày sau khi đơn hoàn thành. */
    public static final int WRITE_WINDOW_DAYS = 60;
    /** Sửa được trong ngần này ngày sau khi gửi. */
    public static final int EDIT_DAYS = 7;
    public static final int MAX_IMAGES = 5;

    public static boolean canWrite(Booking booking, LocalDateTime now) {
        return booking.getStatus() == BookingStatus.COMPLETED && booking.getCompletedAt() != null
                && !now.isAfter(booking.getCompletedAt().plusDays(WRITE_WINDOW_DAYS));
    }

    public static LocalDateTime editableUntil(Review review) {
        return review.getCreatedAt().plusDays(EDIT_DAYS);
    }

    /** Bị ẩn thì không sửa nữa (tránh sửa để "lách" quyết định của Admin). */
    public static boolean canEdit(Review review, LocalDateTime now) {
        return !review.isHidden() && now.isBefore(editableUntil(review));
    }

    /** "Phạm Văn Minh" -> "Phạm V. Minh"; giữ họ và tên, tên đệm chỉ lấy chữ cái đầu. */
    public static String displayName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "Khách hàng";
        String[] words = fullName.trim().split("\\s+");
        if (words.length <= 2) return String.join(" ", words);
        String middle = Arrays.stream(words, 1, words.length - 1)
                .map(w -> w.substring(0, 1).toUpperCase() + ".")
                .reduce((a, b) -> a + " " + b).orElse("");
        return words[0] + " " + middle + " " + words[words.length - 1];
    }
}
