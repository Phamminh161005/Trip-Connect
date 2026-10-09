package com.tripconnect.backend.repository;

import com.tripconnect.backend.entity.TourEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface TourEmbeddingRepository extends JpaRepository<TourEmbedding, Long> {

    /** Bỏ véc-tơ của tour không còn bán (ẩn, đình chỉ, sửa nội dung chờ duyệt...). */
    @Modifying
    @Query("delete from TourEmbedding e where e.tourId not in :keep")
    int deleteAllExcept(@Param("keep") Collection<Long> keep);
}
