package com.tripconnect.backend.service.booking;

import com.tripconnect.backend.entity.Booking;
import com.tripconnect.backend.enums.PassengerType;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;

/** Các quy tắc tính toán thuần (không truy cập DB) — dễ kiểm thử. */
public final class BookingRules {

    private BookingRules() {
    }

    public static final int CHILD_MIN_AGE = 2;
    public static final int ADULT_MIN_AGE = 12;

    /** Loại hành khách theo tuổi VÀO NGÀY KHỞI HÀNH: dưới 2 em bé, 2-11 trẻ em, từ 12 người lớn. */
    public static PassengerType classify(LocalDate dateOfBirth, LocalDate startDate) {
        int age = Period.between(dateOfBirth, startDate).getYears();
        if (age < CHILD_MIN_AGE) return PassengerType.INFANT;
        if (age < ADULT_MIN_AGE) return PassengerType.CHILD;
        return PassengerType.ADULT;
    }

    /** Danh sách hành khách được nhập / sửa tới hết ngày này (trước ngày đi 3 ngày). */
    public static final int PASSENGER_LIST_DEADLINE_DAYS = 3;

    public static LocalDate passengerListDeadline(LocalDate startDate) {
        return startDate.minusDays(PASSENGER_LIST_DEADLINE_DAYS);
    }

    public static boolean passengerListOpen(LocalDate startDate, LocalDate today) {
        return !today.isAfter(passengerListDeadline(startDate));
    }

    public record RefundQuote(int percent, long amount, long daysBeforeDeparture) {
    }

    /**
     * Tiền được hoàn khi KHÁCH tự hủy, theo chính sách chụp lại trong đơn:
     * còn >= refundFullDays ngày -> 100%; >= refundPartialDays -> refundPartialPercent%; sát hơn -> 0.
     */
    public static RefundQuote customerRefund(Booking booking, LocalDate startDate, LocalDate today) {
        long days = ChronoUnit.DAYS.between(today, startDate);
        int percent = days >= booking.getRefundFullDays() ? 100
                : days >= booking.getRefundPartialDays() ? booking.getRefundPartialPercent()
                : 0;
        return new RefundQuote(percent, booking.getTotalAmount() * percent / 100, days);
    }
}
