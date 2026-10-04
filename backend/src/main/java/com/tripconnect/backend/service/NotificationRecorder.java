package com.tripconnect.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Lưu thông báo trên web. Listener đồng bộ (không @Async, không AFTER_COMMIT) nên chạy ngay trong
 * transaction của nghiệp vụ đã phát sự kiện.
 */
@Component
@RequiredArgsConstructor
public class NotificationRecorder {

    private final NotificationService notificationService;

    @EventListener
    public void onUser(NotificationEvents.UserWebEvent event) {
        notificationService.record(List.of(event.userId()), event.message());
    }

    @EventListener
    public void onAdmins(NotificationEvents.AdminWebEvent event) {
        notificationService.recordForAdmins(event.message());
    }
}
