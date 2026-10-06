package com.tripconnect.backend.realtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Đẩy sự kiện xuống hộp thư riêng /user/queue/events của từng người. Đợi transaction commit xong mới đẩy
 * (rollback thì không đẩy) — tránh người nhận thấy dữ liệu chưa / không được lưu. Người không trực tuyến thì bỏ qua:
 * khi mở lại trang, dữ liệu được tải bằng API.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimePublisher {

    public enum Type { CHAT_MESSAGE, CHAT_READ, NOTIFICATION }

    /** Gói tin gửi xuống trình duyệt. */
    public record Event(Type type, Object data) {
    }

    private final SimpMessagingTemplate messagingTemplate;

    public void publish(Long userId, Type type, Object data) {
        publish(List.of(userId), type, data);
    }

    public void publish(Collection<Long> userIds, Type type, Object data) {
        if (userIds.isEmpty()) return;
        Set<Long> recipients = Set.copyOf(userIds);
        Event event = new Event(type, data);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(recipients, event);
                }
            });
        } else {
            send(recipients, event);
        }
    }

    private void send(Set<Long> userIds, Event event) {
        for (Long userId : userIds) {
            try {
                messagingTemplate.convertAndSendToUser(String.valueOf(userId), WebSocketConfig.USER_QUEUE, event);
            } catch (RuntimeException e) {
                log.warn("Không đẩy được sự kiện {} cho user {}: {}", event.type(), userId, e.getMessage());
            }
        }
    }
}
