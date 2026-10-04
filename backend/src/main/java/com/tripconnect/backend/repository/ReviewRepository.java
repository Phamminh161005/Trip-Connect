package com.tripconnect.backend.repository;

import com.tripconnect.backend.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    Optional<Review> findByBookingId(Long bookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Review r where r.id = :id")
    Optional<Review> findByIdForUpdate(@Param("id") Long id);

    @Query("select r.booking.id from Review r where r.booking.id in :bookingIds")
    List<Long> findReviewedBookingIds(@Param("bookingIds") Collection<Long> bookingIds);

    /** Trang tour: đánh giá đang hiện, mới nhất trước; {@code before} = con trỏ (null = trang đầu). */
    @EntityGraph(attributePaths = {"customer", "booking", "booking.departure"})
    @Query("""
            select r from Review r
            where r.tour.id = :tourId and r.hidden = false
              and (:before is null or r.id < :before)
              and (:rating is null or r.rating = :rating)
            order by r.id desc
            """)
    List<Review> findVisiblePage(@Param("tourId") Long tourId, @Param("before") Long before,
                                 @Param("rating") Short rating, Pageable limit);

    /** [số sao, số lượt] của các đánh giá đang hiện. */
    @Query("select r.rating, count(r) from Review r where r.tour.id = :tourId and r.hidden = false group by r.rating")
    List<Object[]> countVisibleByRating(@Param("tourId") Long tourId);

    /** [trung bình, số lượt] đánh giá đang hiện của tour. */
    @Query("select avg(r.rating), count(r) from Review r where r.tour.id = :tourId and r.hidden = false")
    List<Object[]> visibleStatsByTour(@Param("tourId") Long tourId);

    /** [trung bình, số lượt] đánh giá đang hiện trên mọi tour của Agent. */
    @Query("select avg(r.rating), count(r) from Review r where r.agent.id = :agentId and r.hidden = false")
    List<Object[]> visibleStatsByAgent(@Param("agentId") Long agentId);

    @Override
    @EntityGraph(attributePaths = {"tour", "customer", "booking"})
    Page<Review> findAll(Specification<Review> spec, Pageable pageable);
}
