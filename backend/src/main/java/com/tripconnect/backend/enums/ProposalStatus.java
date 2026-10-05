package com.tripconnect.backend.enums;

/** Trạng thái một phiên bản đề xuất tour riêng. */
public enum ProposalStatus {
    /** Đang chờ khách phản hồi. */
    SENT,
    ACCEPTED,
    /** Khách yêu cầu chỉnh sửa -> Agent gửi phiên bản mới. */
    REVISION_REQUESTED,
    /** Khách hủy yêu cầu khi đang xem đề xuất này. */
    DECLINED,
    /** Khách không phản hồi kịp (vẫn có thể yêu cầu chỉnh sửa nếu còn lượt). */
    EXPIRED,
    /** Yêu cầu bị đóng khi đề xuất đang chờ khách. */
    WITHDRAWN
}
