package com.tripconnect.backend.repository;

import com.tripconnect.backend.entity.Payment;
import com.tripconnect.backend.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** Khóa giao dịch khi xử lý kết quả: Return URL và IPN đến cùng lúc thì chỉ một bên ghi nhận. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.txnRef = :txnRef")
    Optional<Payment> findByTxnRefForUpdate(@Param("txnRef") String txnRef);

    @Query("select p.booking.id from Payment p where p.txnRef = :txnRef")
    Optional<Long> findBookingIdByTxnRef(@Param("txnRef") String txnRef);

    List<Payment> findByBookingIdOrderByIdDesc(Long bookingId);

    List<Payment> findByBookingIdAndStatus(Long bookingId, PaymentStatus status);

    /** [bookingId, tổng tiền đã thu thành công] */
    @org.springframework.data.jpa.repository.Query("select p.booking.id, sum(p.amount) from Payment p where p.booking.id in :ids "
            + "and p.status = com.tripconnect.backend.enums.PaymentStatus.SUCCESS group by p.booking.id")
    List<Object[]> sumSuccessByBookingIds(@org.springframework.data.repository.query.Param("ids") java.util.Collection<Long> ids);

    Optional<Payment> findFirstByBookingIdAndStatus(Long bookingId, PaymentStatus status);

    long countByBookingId(Long bookingId);
}
