package com.tripconnect.backend.service;

import com.tripconnect.backend.dto.notification.NotificationResponses;
import com.tripconnect.backend.entity.Notification;
import com.tripconnect.backend.enums.UserRole;
import com.tripconnect.backend.repository.NotificationRepository;
import com.tripconnect.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/** Thông báo trên web: lưu, xem theo trang (con trỏ), đếm chưa đọc, đánh dấu đã đọc, dọn dẹp. */
@Service
@RequiredArgsConstructor
public class NotificationService {

    static final int MAX_PAGE_SIZE = 50;
    /** Đã đọc quá ngần này ngày thì xóa. */
    static final int KEEP_READ_DAYS = 60;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    /** Tham gia transaction của nghiệp vụ đang chạy (nếu có). */
    @Transactional
    public void record(Collection<Long> userIds, NotificationEvents.WebMessage message) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Notification> notifications = userIds.stream().map(userId -> {
            Notification n = new Notification();
            n.setUserId(userId);
            n.setType(message.type());
            n.setTitle(truncate(message.title(), 200));
            n.setBody(message.body());
            n.setLink(message.link());
            n.setCreatedAt(now);
            return n;
        }).toList();
        notificationRepository.saveAll(notifications);
    }

    @Transactional
    public void recordForAdmins(NotificationEvents.WebMessage message) {
        record(userRepository.findActiveIdsByRole(UserRole.ADMIN), message);
    }

    @Transactional(readOnly = true)
    public NotificationResponses.Page list(Long userId, Long before, int size, boolean unreadOnly) {
        int limit = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        // Lấy dư 1 dòng để biết còn trang sau không
        List<Notification> rows = notificationRepository.findPage(userId, before, unreadOnly, PageRequest.of(0, limit + 1));
        boolean hasMore = rows.size() > limit;
        List<Notification> page = hasMore ? rows.subList(0, limit) : rows;
        return new NotificationResponses.Page(page.stream().map(NotificationService::toItem).toList(),
                hasMore ? page.get(page.size() - 1).getId() : null);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    /** Đánh dấu đã đọc; thông báo của người khác / không tồn tại thì bỏ qua (không lộ là có hay không). */
    @Transactional
    public void markRead(Long userId, Long notificationId) {
        notificationRepository.markRead(notificationId, userId, LocalDateTime.now(clock));
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllRead(userId, LocalDateTime.now(clock));
    }

    @Transactional
    public int deleteOldRead() {
        return notificationRepository.deleteReadBefore(LocalDateTime.now(clock).minusDays(KEEP_READ_DAYS));
    }

    static NotificationResponses.Item toItem(Notification n) {
        return new NotificationResponses.Item(n.getId(), n.getType(), n.getTitle(), n.getBody(), n.getLink(),
                n.getReadAt() != null, n.getCreatedAt());
    }

    private static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}
