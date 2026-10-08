package com.tripconnect.backend.enums;

/** Vì sao đơn được đối soát. */
public enum SettlementItemKind {
    /** Đơn hoàn thành. */
    COMPLETED,
    /** Đơn bị hủy mà TripConnect vẫn giữ tiền (khách hủy sát ngày, mất cọc). */
    CANCELLED
}
