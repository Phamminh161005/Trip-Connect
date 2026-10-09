package com.tripconnect.backend.repository;

import com.tripconnect.backend.entity.TourView;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TourViewRepository extends JpaRepository<TourView, Long> {

    /** [tourId, số lượt xem] gần đây của một người (đăng nhập) hoặc một máy (mã khách). */
    @Query("""
            select v.tourId, count(v) from TourView v
            where ((:userId is not null and v.userId = :userId) or (:visitorId is not null and v.visitorId = :visitorId))
              and v.viewedAt > :since
            group by v.tourId order by max(v.viewedAt) desc
            """)
    List<Object[]> countRecentByViewer(@Param("userId") Long userId, @Param("visitorId") String visitorId,
                                       @Param("since") LocalDateTime since, Pageable pageable);

    /** Đã ghi lượt xem này gần đây chưa (bấm F5 liên tục không tính nhiều lần). */
    @Query("""
            select count(v) > 0 from TourView v
            where v.tourId = :tourId and v.viewedAt > :since
              and ((:userId is not null and v.userId = :userId) or (:visitorId is not null and v.visitorId = :visitorId))
            """)
    boolean viewedSince(@Param("tourId") Long tourId, @Param("userId") Long userId, @Param("visitorId") String visitorId,
                        @Param("since") LocalDateTime since);

    @Modifying
    @Query("delete from TourView v where v.viewedAt < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);

    /** Khách vãng lai đăng nhập: gán lượt xem của máy đó cho tài khoản. */
    @Modifying
    @Query("update TourView v set v.userId = :userId where v.visitorId = :visitorId and v.userId is null")
    int claim(@Param("visitorId") String visitorId, @Param("userId") Long userId);
}
