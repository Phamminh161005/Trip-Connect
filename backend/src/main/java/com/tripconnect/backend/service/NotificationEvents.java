package com.tripconnect.backend.service;

/**
 * Các sự kiện gửi email thông báo. Service chỉ "phát" sự kiện; {@link NotificationEmailListener}
 * gửi email ở luồng riêng SAU KHI transaction commit thành công.
 */
public final class NotificationEvents {

    private NotificationEvents() {
    }

    /** Gửi cho một người dùng cụ thể. */
    public record UserEmailEvent(String toEmail, EmailTemplates.Email email) {
    }

    /** Gửi cho tất cả Admin đang hoạt động. */
    public record AdminEmailEvent(EmailTemplates.Email email) {
    }
}
