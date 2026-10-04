package com.tripconnect.backend.enums;

/** Tiêu chuẩn lưu trú của tour (chọn 1). */
public enum AccommodationType {
    NONE("Không lưu trú (tour trong ngày)"),
    HOMESTAY("Homestay"),
    HOTEL_2_3_STAR("Khách sạn 2-3 sao"),
    HOTEL_4_STAR("Khách sạn 4 sao"),
    HOTEL_5_STAR("Khách sạn 5 sao"),
    RESORT("Resort"),
    CRUISE("Du thuyền");

    private final String label;

    AccommodationType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
