package com.tripconnect.backend.enums;

/** Kết quả một lần giao yêu cầu cho Agent. */
public enum AssignmentStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    /** Quá 48 giờ không phản hồi. */
    EXPIRED,
    /** Yêu cầu bị hủy / đóng khi Agent chưa phản hồi. */
    REVOKED,
    /** Đã nhận nhưng quá hạn gửi đề xuất -> yêu cầu được giao lại. */
    OVERDUE
}
