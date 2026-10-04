package com.tripconnect.backend.service;

import com.tripconnect.backend.enums.NotificationType;

/**
 * Các sự kiện thông báo. Service chỉ "phát" sự kiện:
 * <ul>
 *   <li>Email: {@link NotificationEmailListener} gửi ở luồng riêng SAU KHI transaction commit thành công.</li>
 *   <li>Thông báo trên web: {@link NotificationRecorder} lưu NGAY trong transaction đang chạy —
 *       thao tác thất bại (rollback) thì thông báo cũng không còn.</li>
 * </ul>
 */
public final class NotificationEvents {

    private NotificationEvents() {
    }

    /** Email cho một người dùng cụ thể. */
    public record UserEmailEvent(String toEmail, EmailTemplates.Email email) {
    }

    /** Email cho tất cả Admin đang hoạt động. */
    public record AdminEmailEvent(EmailTemplates.Email email) {
    }

    /** Nội dung một thông báo trên web. */
    public record WebMessage(NotificationType type, String title, String body, String link) {
    }

    /** Thông báo trên web cho một người dùng. */
    public record UserWebEvent(Long userId, WebMessage message) {
    }

    /** Thông báo trên web cho tất cả Admin đang hoạt động (mỗi Admin một bản). */
    public record AdminWebEvent(WebMessage message) {
    }
}
