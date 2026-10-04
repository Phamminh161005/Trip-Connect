package com.tripconnect.backend.repository;

import com.tripconnect.backend.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Phân trang con trỏ: thông báo có id nhỏ hơn {@code before} (null = trang đầu), mới nhất trước. */
    @Query("""
            select n from Notification n
            where n.userId = :userId and (:before is null or n.id < :before)
              and (:unreadOnly = false or n.readAt is null)
            order by n.id desc
            """)
    List<Notification> findPage(@Param("userId") Long userId, @Param("before") Long before,
                                @Param("unreadOnly") boolean unreadOnly, Pageable limit);

    long countByUserIdAndReadAtIsNull(Long userId);

    @Modifying
    @Query("update Notification n set n.readAt = :now where n.id = :id and n.userId = :userId and n.readAt is null")
    int markRead(@Param("id") Long id, @Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("update Notification n set n.readAt = :now where n.userId = :userId and n.readAt is null")
    int markAllRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("delete from Notification n where n.readAt < :before")
    int deleteReadBefore(@Param("before") LocalDateTime before);
}
