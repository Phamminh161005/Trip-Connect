package com.tripconnect.backend.repository;

import com.tripconnect.backend.entity.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {

    /** Ảnh của nhiều đánh giá một lượt (tránh truy vấn từng đánh giá khi hiện danh sách). */
    @Query("select i from ReviewImage i where i.review.id in :reviewIds order by i.review.id, i.sortOrder")
    List<ReviewImage> findByReviewIds(@Param("reviewIds") Collection<Long> reviewIds);
}
