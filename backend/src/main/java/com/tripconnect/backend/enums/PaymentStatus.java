package com.tripconnect.backend.enums;

public enum PaymentStatus {
    /** Đã tạo link, khách chưa thanh toán xong. */
    PENDING,
    SUCCESS,
    /** Khách hủy / thẻ lỗi / VNPay từ chối. */
    FAILED,
    /** Hết hạn giữ chỗ mà không nhận được kết quả thành công. */
    EXPIRED
}
