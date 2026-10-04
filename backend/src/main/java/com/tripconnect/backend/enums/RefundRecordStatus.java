package com.tripconnect.backend.enums;

/** Trạng thái một lần hoàn tiền (bảng refunds). */
public enum RefundRecordStatus {
    PENDING,
    SUCCESS,
    /** API VNPay báo lỗi — cần Admin chuyển khoản thủ công. */
    MANUAL_REQUIRED,
    /** Admin đã hoàn thủ công. */
    MANUAL_DONE
}
