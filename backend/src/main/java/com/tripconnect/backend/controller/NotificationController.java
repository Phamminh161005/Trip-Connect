package com.tripconnect.backend.controller;

import com.tripconnect.backend.dto.notification.NotificationResponses;
import com.tripconnect.backend.security.AuthenticatedUser;
import com.tripconnect.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Thông báo trên web của người đang đăng nhập. */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** Mới nhất trước. Trang sau: truyền nextCursor của trang trước vào {@code before}. */
    @GetMapping
    public NotificationResponses.Page list(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                           @RequestParam(required = false) Long before,
                                           @RequestParam(defaultValue = "20") int size,
                                           @RequestParam(defaultValue = "false") boolean unread) {
        return notificationService.list(currentUser.userId(), before, size, unread);
    }

    @GetMapping("/unread-count")
    public NotificationResponses.UnreadCount unreadCount(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return new NotificationResponses.UnreadCount(notificationService.unreadCount(currentUser.userId()));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        notificationService.markRead(currentUser.userId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        notificationService.markAllRead(currentUser.userId());
        return ResponseEntity.noContent().build();
    }
}
