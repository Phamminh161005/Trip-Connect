package com.tripconnect.backend.service.review;

import com.tripconnect.backend.entity.Booking;
import com.tripconnect.backend.entity.Review;
import com.tripconnect.backend.enums.BookingStatus;
import com.tripconnect.backend.service.DisplayNames;

import java.time.LocalDateTime;

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

    /** Tên người đánh giá hiện trên trang tour, vd "Phạm V. Minh". */
    public static String displayName(String fullName) {
        return DisplayNames.masked(fullName);
    }
}
