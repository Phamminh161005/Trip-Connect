package com.tripconnect.backend.service.review;

import com.tripconnect.backend.entity.Booking;
import com.tripconnect.backend.entity.Review;
import com.tripconnect.backend.enums.BookingStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewRulesTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 4, 9, 0);

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Phạm Văn Minh        | Phạm V. Minh",
            "Nguyễn Thị Thu Hà    | Nguyễn T. T. Hà",
            "Trần An              | Trần An",
            "Lê                   | Lê",
    })
    void displayNameKeepsFamilyAndGivenName(String fullName, String expected) {
        assertThat(ReviewRules.displayName(fullName)).isEqualTo(expected);
    }

    @Test
    void displayNameFallsBackWhenEmpty() {
        assertThat(ReviewRules.displayName(null)).isEqualTo("Khách hàng");
    }

    @Test
    void canWriteOnlyCompletedBookingsWithinSixtyDays() {
        Booking booking = new Booking();
        booking.setStatus(BookingStatus.PAID);
        assertThat(ReviewRules.canWrite(booking, NOW)).isFalse();

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(NOW.minusDays(60));
        assertThat(ReviewRules.canWrite(booking, NOW)).isTrue();
        booking.setCompletedAt(NOW.minusDays(60).minusMinutes(1));
        assertThat(ReviewRules.canWrite(booking, NOW)).isFalse();
    }

    @Test
    void canEditWithinSevenDaysUnlessHidden() {
        Review review = new Review();
        review.setCreatedAt(NOW.minusDays(6));
        assertThat(ReviewRules.canEdit(review, NOW)).isTrue();

        review.setHidden(true);
        assertThat(ReviewRules.canEdit(review, NOW)).isFalse();

        review.setHidden(false);
        review.setCreatedAt(NOW.minusDays(7));
        assertThat(ReviewRules.canEdit(review, NOW)).isFalse();
    }

    @Test
    void ratingStatsRoundToTwoDecimals_andNoReviewsMeansNoRating() {
        assertThat(RatingCalculator.toStats(List.<Object[]>of(new Object[]{4.666666, 3L})))
                .isEqualTo(new RatingCalculator.Stats(new BigDecimal("4.67"), 3));
        assertThat(RatingCalculator.toStats(List.<Object[]>of(new Object[]{null, 0L})))
                .isEqualTo(new RatingCalculator.Stats(null, 0));
    }
}
