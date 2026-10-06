package com.tripconnect.backend.enums;

/**
 * Vòng đời tour:
 * <pre>
 *  DRAFT --gửi duyệt--> PENDING_APPROVAL --duyệt--> PUBLISHED <--ẩn/hiện--> HIDDEN
 *    ^                    |  \--yêu cầu sửa--> NEEDS_REVISION --gửi lại--> PENDING_APPROVAL
 *    |--rút lại-----------/
 *  PUBLISHED / HIDDEN --Agent sửa nội dung--> DRAFT (tạm ẩn, phải gửi duyệt lại)
 *  PUBLISHED / HIDDEN --Admin đình chỉ--> SUSPENDED --bỏ đình chỉ--> PUBLISHED
 * </pre>
 * Tour của TripConnect (Admin tạo) không qua duyệt: DRAFT/HIDDEN --công khai--> PUBLISHED.
 * PRIVATE: tour riêng tạo từ đề xuất khách đã đồng ý — chỉ khách đó đặt, không hiện ở tìm kiếm, không sửa được.
 */
public enum TourStatus {
    DRAFT,
    PENDING_APPROVAL,
    NEEDS_REVISION,
    /** Đang bán, khách tìm thấy. */
    PUBLISHED,
    /** Agent tự tạm ẩn — không nhận booking mới, lịch đã có khách vẫn khởi hành. */
    HIDDEN,
    /** Admin đình chỉ do vi phạm — Agent không tự mở lại được. */
    SUSPENDED,
    PRIVATE;

    /** Nội dung được sửa (sửa khi đang bán thì tour chuyển về Nháp để duyệt lại). */
    public boolean contentEditable() {
        return this == DRAFT || this == NEEDS_REVISION || this == PUBLISHED || this == HIDDEN;
    }
}
