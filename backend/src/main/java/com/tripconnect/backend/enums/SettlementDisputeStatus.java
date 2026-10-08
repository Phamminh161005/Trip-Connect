package com.tripconnect.backend.enums;

public enum SettlementDisputeStatus {
    OPEN,
    /** Admin đã thêm khoản điều chỉnh, gửi Agent xác nhận lại. */
    ADJUSTED,
    REJECTED
}
