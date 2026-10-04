package com.tripconnect.backend.repository;

import com.tripconnect.backend.entity.TourImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TourImageRepository extends JpaRepository<TourImage, Long> {

    List<TourImage> findByTourIdOrderBySortOrderAscIdAsc(Long tourId);

    Optional<TourImage> findByIdAndTourId(Long id, Long tourId);

    long countByTourId(Long tourId);

    /** Chỉ lấy mã file (không nạp entity) — dùng khi xóa tour để dọn file trên Cloudinary. */
    @Query("select i.publicId from TourImage i where i.tour.id = :tourId")
    List<String> findPublicIdsByTourId(@Param("tourId") Long tourId);

    @Query("select coalesce(max(i.sortOrder), -1) from TourImage i where i.tour.id = :tourId")
    int findMaxSortOrder(@Param("tourId") Long tourId);

    /** Ảnh bìa (sortOrder nhỏ nhất) của nhiều tour trong 1 câu query — dùng cho danh sách tour. */
    @Query("""
            select i from TourImage i
            where i.tour.id in :tourIds
              and i.sortOrder = (select min(i2.sortOrder) from TourImage i2 where i2.tour.id = i.tour.id)
            """)
    List<TourImage> findCoverImages(@Param("tourIds") Collection<Long> tourIds);
}
