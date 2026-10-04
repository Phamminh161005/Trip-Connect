package com.tripconnect.backend.service.booking;

import com.tripconnect.backend.entity.Booking;
import com.tripconnect.backend.enums.PassengerType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BookingRulesTest {

    private static final LocalDate START = LocalDate.of(2026, 11, 15);

    @Test
    void passengerType_isBasedOnAgeOnDepartureDay() {
        assertThat(BookingRules.classify(LocalDate.of(2025, 11, 16), START)).isEqualTo(PassengerType.INFANT); // 1 tuổi
        assertThat(BookingRules.classify(LocalDate.of(2024, 11, 15), START)).isEqualTo(PassengerType.CHILD);  // tròn 2 tuổi
        assertThat(BookingRules.classify(LocalDate.of(2014, 11, 16), START)).isEqualTo(PassengerType.CHILD);  // 11 tuổi
        assertThat(BookingRules.classify(LocalDate.of(2014, 11, 15), START)).isEqualTo(PassengerType.ADULT);  // tròn 12 tuổi
    }

    @Test
    void customerRefund_followsSnapshotPolicy() {
        Booking booking = new Booking();
        booking.setTotalAmount(10_000_000);
        booking.setRefundFullDays((short) 7);
        booking.setRefundPartialDays((short) 3);
        booking.setRefundPartialPercent((short) 50);

        var sevenDays = BookingRules.customerRefund(booking, START, START.minusDays(7));
        assertThat(sevenDays.percent()).isEqualTo(100);
        assertThat(sevenDays.amount()).isEqualTo(10_000_000);

        var sixDays = BookingRules.customerRefund(booking, START, START.minusDays(6));
        assertThat(sixDays.percent()).isEqualTo(50);
        assertThat(sixDays.amount()).isEqualTo(5_000_000);

        var threeDays = BookingRules.customerRefund(booking, START, START.minusDays(3));
        assertThat(threeDays.percent()).isEqualTo(50);

        var twoDays = BookingRules.customerRefund(booking, START, START.minusDays(2));
        assertThat(twoDays.percent()).isZero();
        assertThat(twoDays.amount()).isZero();
    }
}
