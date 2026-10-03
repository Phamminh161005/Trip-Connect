package com.tripconnect.backend.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tripconnect.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;

/**
 * Nhớ tạm "tài khoản còn hoạt động không" để bộ lọc JWT kiểm tra ở mọi request mà không phải hỏi DB mỗi lần.
 * Mỗi user tối đa 1 truy vấn / 30 giây; khi Admin khóa hoặc mở khóa thì xóa ngay kết quả đã nhớ.
 * Lưu trong RAM -> chỉ đúng khi chạy 1 instance (nhiều instance: instance khác trễ tối đa TTL).
 */
@Component
@RequiredArgsConstructor
public class UserStatusCache {

    private static final Duration TTL = Duration.ofSeconds(30);

    private final UserRepository userRepository;

    private final Cache<Long, Boolean> activeByUserId = Caffeine.newBuilder()
            .expireAfterWrite(TTL)
            .maximumSize(100_000)
            .build();

    /** User không tồn tại (đã bị xóa) cũng tính là không hoạt động. */
    public boolean isActive(Long userId) {
        if (userId == null) return false;
        return activeByUserId.get(userId, userRepository::existsByIdAndActiveTrue);
    }

    /**
     * Gọi khi trạng thái tài khoản thay đổi. Xóa cả SAU KHI transaction commit: nếu chỉ xóa ngay,
     * một request chen vào trước lúc commit có thể đọc lại giá trị cũ từ DB và nhớ thêm 30 giây.
     */
    public void evict(Long userId) {
        activeByUserId.invalidate(userId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    activeByUserId.invalidate(userId);
                }
            });
        }
    }
}
