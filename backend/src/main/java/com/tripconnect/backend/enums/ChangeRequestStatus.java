package com.tripconnect.backend.enums;

public enum ChangeRequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    /** Agent tự hủy khi còn chờ duyệt. */
    CANCELLED
}
