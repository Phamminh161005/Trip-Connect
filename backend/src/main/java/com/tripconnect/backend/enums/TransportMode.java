package com.tripconnect.backend.enums;

/** Phương tiện di chuyển chính của tour (chọn nhiều). */
public enum TransportMode {
    BUS("Xe du lịch"),
    PLANE("Máy bay"),
    TRAIN("Tàu hỏa"),
    BOAT("Tàu thủy / Du thuyền"),
    MOTORBIKE("Xe máy"),
    SELF_ARRANGED("Tự túc");

    private final String label;

    TransportMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
