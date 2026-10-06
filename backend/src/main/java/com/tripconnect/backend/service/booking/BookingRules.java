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

    // ----- Tour riêng: đặt cọc rồi trả phần còn lại -----

    /** Khách có ngần này giờ để đặt cọc sau khi đồng ý đề xuất. */
    public static final int DEPOSIT_HOLD_HOURS = 48;
    /** Trả phần còn lại trước ngày đi ngần này ngày. */
    public static final int BALANCE_DAYS_DOMESTIC = 7;
    public static final int BALANCE_DAYS_INTERNATIONAL = 15;
    /** Khách được tự gia hạn một lần. */
    public static final int BALANCE_EXTENSION_DAYS = 3;
    /** Hạn trả phần còn lại phải cách hạn đặt cọc ít nhất ngần này ngày, sát hơn thì trả một lần. */
    public static final int MIN_DAYS_BETWEEN_PAYMENTS = 3;

    public static LocalDate balanceDueDate(LocalDate startDate, boolean international) {
        return startDate.minusDays(international ? BALANCE_DAYS_INTERNATIONAL : BALANCE_DAYS_DOMESTIC);
    }

    /** Còn đủ thời gian giữa hạn cọc và hạn trả nốt thì chia 2 lần; không thì trả toàn bộ một lần. */
    public static boolean splitPayment(LocalDate depositDeadline, LocalDate balanceDueDate) {
        return !balanceDueDate.isBefore(depositDeadline.plusDays(MIN_DAYS_BETWEEN_PAYMENTS));
    }

    /** Hạn mới khi gia hạn: thêm 3 ngày nhưng vẫn trước ngày đi. */
    public static LocalDate extendedDueDate(LocalDate dueDate, LocalDate startDate) {
        LocalDate extended = dueDate.plusDays(BALANCE_EXTENSION_DAYS);
        LocalDate latest = startDate.minusDays(1);
        return extended.isAfter(latest) ? latest : extended;
    }

    public record RefundQuote(int percent, long amount, long daysBeforeDeparture) {
    }

    /**
     * Tiền được hoàn khi KHÁCH tự hủy đơn đã thanh toán đủ, theo chính sách chụp lại trong đơn:
     * còn >= refundFullDays ngày -> 100%; >= refundPartialDays -> refundPartialPercent%; sát hơn -> 0.
     */
    public static RefundQuote customerRefund(Booking booking, LocalDate startDate, LocalDate today) {
        long days = ChronoUnit.DAYS.between(today, startDate);
        int percent = days >= booking.getRefundFullDays() ? 100
                : days >= booking.getRefundPartialDays() ? booking.getRefundPartialPercent()
                : 0;
        // Tour riêng: tiền cọc không hoàn, phần đã trả thêm hoàn theo tỷ lệ
        long base = booking.getTotalAmount() - booking.getDepositAmount();
        return new RefundQuote(percent, base * percent / 100, days);
    }
}
