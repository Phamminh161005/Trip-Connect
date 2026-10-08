package com.tripconnect.backend.enums;

/**
 * Bảng đối soát:
 * <pre>
 *  PENDING_CONFIRM --Agent xác nhận / quá 5 ngày--> AWAITING_PAYMENT --Admin chuyển khoản--> PAID
 *        \--Agent khiếu nại--> DISPUTED --Admin điều chỉnh--> PENDING_CONFIRM
 *                                       \--Admin bác khiếu nại--> AWAITING_PAYMENT
 * </pre>
 */
public enum SettlementStatus {
    PENDING_CONFIRM,
    DISPUTED,
    AWAITING_PAYMENT,
    PAID
}
