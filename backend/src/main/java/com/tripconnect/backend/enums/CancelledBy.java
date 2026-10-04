package com.tripconnect.backend.enums;

/** Ai hủy đơn. SYSTEM = tự hủy vì quá hạn giữ chỗ / thanh toán trễ khi đã hết chỗ. */
public enum CancelledBy {
    CUSTOMER,
    AGENT,
    ADMIN,
    SYSTEM
}
