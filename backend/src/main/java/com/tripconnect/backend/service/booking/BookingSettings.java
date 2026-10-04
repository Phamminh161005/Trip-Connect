package com.tripconnect.backend.service.booking;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Các con số nghiệp vụ của đặt tour (application.properties: app.booking.*). */
@Getter
@Component
public class BookingSettings {

    private final BigDecimal commissionRate;
    private final int maxTravellers;
    private final int holdMinutes;
    private final int refundFullDays;
    private final int refundPartialDays;
    private final int refundPartialPercent;

    public BookingSettings(@Value("${app.booking.commission-rate}") BigDecimal commissionRate,
                           @Value("${app.booking.max-travellers}") int maxTravellers,
                           @Value("${app.booking.hold-minutes}") int holdMinutes,
                           @Value("${app.booking.refund-full-days}") int refundFullDays,
                           @Value("${app.booking.refund-partial-days}") int refundPartialDays,
                           @Value("${app.booking.refund-partial-percent}") int refundPartialPercent) {
        this.commissionRate = commissionRate;
        this.maxTravellers = maxTravellers;
        this.holdMinutes = holdMinutes;
        this.refundFullDays = refundFullDays;
        this.refundPartialDays = refundPartialDays;
        this.refundPartialPercent = refundPartialPercent;
    }
}
