package com.tripconnect.backend.dto.notification;

import com.tripconnect.backend.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.List;

public final class NotificationResponses {

    private NotificationResponses() {
    }

    public record Item(Long id, NotificationType type, String title, String body, String link, boolean read,
                       LocalDateTime createdAt) {
    }

    /** Một trang thông báo; nextCursor = null khi đã hết (truyền lại vào ?before= để lấy trang sau). */
    public record Page(List<Item> items, Long nextCursor) {
    }

    public record UnreadCount(long count) {
    }
}
