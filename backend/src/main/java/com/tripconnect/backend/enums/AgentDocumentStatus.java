package com.tripconnect.backend.enums;

public enum AgentDocumentStatus {
    /** Đang được dùng cho hồ sơ. */
    ACTIVE,
    /** Nằm trong yêu cầu cập nhật hồ sơ, chờ Admin duyệt. */
    PENDING,
    /** Đã bị thay bằng giấy tờ mới — giữ lại để tra cứu. */
    ARCHIVED
}
