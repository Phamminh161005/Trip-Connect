package com.tripconnect.backend.repository;

import com.tripconnect.backend.entity.SearchLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {

    List<SearchLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<SearchLog> findByIdAndUserId(Long id, Long userId);

    /** Khách vừa đăng nhập: gộp lịch sử tìm kiếm lúc chưa đăng nhập vào tài khoản. */
    @Modifying
    @Query("update SearchLog s set s.userId = :userId where s.visitorId = :visitorId and s.userId is null")
    int claimVisitorLogs(@Param("visitorId") String visitorId, @Param("userId") Long userId);

    @Modifying
    @Query("delete from SearchLog s where s.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);

    /** Xóa một mục "tìm kiếm gần đây" = xóa mọi lần tìm cùng từ khóa + điểm đến đó. */
    @Modifying
    @Query("""
            delete from SearchLog s where s.userId = :userId
              and coalesce(s.keyword, '') = coalesce(:keyword, '')
              and coalesce(s.destinationId, 0) = coalesce(:destinationId, 0)
            """)
    int deleteSameSearch(@Param("userId") Long userId, @Param("keyword") String keyword,
                         @Param("destinationId") Long destinationId);

    @Modifying
    @Query("delete from SearchLog s where s.createdAt < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);
}
