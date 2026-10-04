package com.tripconnect.backend.enums;

/**
 * Trạng thái lịch khởi hành. "Đã khởi hành" không lưu mà tính theo ngày (ngày đi đã qua).
 */
public enum DepartureStatus {
    /** Đang mở bán. */
    OPEN,
    /** Ngừng bán thêm (khách đã đặt vẫn đi bình thường). */
    CLOSED,
    /** Hủy chuyến — khách đã đặt được hoàn 100% (xử lý ở module đặt tour). */
    CANCELLED
}
