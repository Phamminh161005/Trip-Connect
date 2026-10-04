package com.tripconnect.backend.enums;

/** Tình trạng hoàn tiền của đơn. */
public enum RefundStatus {
    /** Không có khoản hoàn (chưa thanh toán, hoặc hủy sát ngày không được hoàn). */
    NONE,
    /** Đang gửi yêu cầu hoàn qua VNPay. */
    PENDING,
    REFUNDED,
    /** VNPay báo lỗi — chờ Admin hoàn thủ công. */
    MANUAL_REQUIRED
}
