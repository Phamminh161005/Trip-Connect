package com.tripconnect.backend.enums;

/** Trạng thái yêu cầu thiết kế tour riêng. */
public enum CustomRequestStatus {
    /** Chờ Admin giao cho Agent (mới gửi, hoặc Agent trước đã từ chối / hết hạn). */
    NEW,
    /** Đã giao, chờ Agent nhận trong 48 giờ. */
    WAITING_AGENT,
    /** Agent đã nhận: soạn đề xuất, khách xem và yêu cầu chỉnh sửa. */
    IN_PROGRESS,
    /** Khách đã đồng ý một đề xuất (tiếp theo: tạo tour riêng và đặt cọc). */
    AGREED,
    CANCELLED,
    CLOSED
}
