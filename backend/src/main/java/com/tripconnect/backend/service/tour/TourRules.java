package com.tripconnect.backend.service.tour;

/** Các giới hạn nghiệp vụ của tour (Frontend có hằng số tương ứng). */
public final class TourRules {

    private TourRules() {
    }

    public static final int MAX_CATEGORIES = 3;
    public static final int MAX_DESTINATIONS = 20;
    public static final int MAX_DURATION_DAYS = 30;
    public static final int MIN_HIGHLIGHTS = 3;
    public static final int MAX_HIGHLIGHTS = 6;
    public static final int MAX_SERVICE_ITEMS = 30;

    public static final int MIN_IMAGES = 3;
    public static final int MAX_IMAGES = 10;

    /** Lịch khởi hành mới phải cách hôm nay ít nhất ngần này ngày. */
    public static final int MIN_DEPARTURE_LEAD_DAYS = 3;
    public static final int MAX_CAPACITY = 100;
    public static final long MIN_ADULT_PRICE = 10_000;
    public static final long MAX_PRICE = 1_000_000_000;
}
