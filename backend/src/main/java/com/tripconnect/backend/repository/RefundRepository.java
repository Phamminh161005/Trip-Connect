package com.tripconnect.backend.repository;

import com.tripconnect.backend.entity.Refund;
import com.tripconnect.backend.enums.RefundRecordStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    List<Refund> findByBookingIdOrderByIdDesc(Long bookingId);

    /** [bookingId, tổng tiền phải hoàn] (kể cả khoản đang xử lý / chờ hoàn thủ công) */
    @Query("select r.booking.id, sum(r.amount) from Refund r where r.booking.id in :ids group by r.booking.id")
    List<Object[]> sumByBookingIds(@Param("ids") java.util.Collection<Long> ids);

    boolean existsByBookingIdAndStatusAndIdNot(Long bookingId, com.tripconnect.backend.enums.RefundRecordStatus status, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Refund r join fetch r.booking join fetch r.payment where r.id = :id")
    Optional<Refund> findByIdForUpdate(@Param("id") Long id);

    long countByStatus(RefundRecordStatus status);
}
