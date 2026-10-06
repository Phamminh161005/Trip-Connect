package com.tripconnect.backend.repository;

import com.tripconnect.backend.entity.Booking;
import com.tripconnect.backend.enums.BookingStatus;
import com.tripconnect.backend.enums.RefundStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    /**
     * Đơn còn chiếm chỗ: đã thanh toán / hoàn thành, hoặc đang chờ thanh toán và CHƯA hết hạn giữ chỗ.
     * (Đơn quá hạn nhả chỗ ngay, không cần đợi job đổi trạng thái.)
     */
    String HOLDS_SEATS = """
            (b.status in (com.tripconnect.backend.enums.BookingStatus.DEPOSIT_PAID,
                          com.tripconnect.backend.enums.BookingStatus.PAID, com.tripconnect.backend.enums.BookingStatus.COMPLETED)
             or (b.status = com.tripconnect.backend.enums.BookingStatus.PENDING_PAYMENT and b.holdExpiresAt > :now))
            """;

    @Query("select b.departure.id, sum(b.adults + b.children) from Booking b where b.departure.id in :departureIds and "
            + HOLDS_SEATS + " group by b.departure.id")
    List<Object[]> sumSeatsByDeparture(@Param("departureIds") Collection<Long> departureIds, @Param("now") LocalDateTime now);

    boolean existsByTourId(Long tourId);

    boolean existsByDepartureId(Long departureId);

    @Query("select count(b) > 0 from Booking b where b.tour.id = :tourId and " + HOLDS_SEATS)
    boolean tourHasActiveBookings(@Param("tourId") Long tourId, @Param("now") LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);

    /** Đơn đang chờ thanh toán (còn hạn) của khách cho cùng lịch — tránh giữ chỗ trùng. */
    Optional<Booking> findFirstByCustomerIdAndDepartureIdAndStatusAndHoldExpiresAtAfter(
            Long customerId, Long departureId, BookingStatus status, LocalDateTime now);

    long countByCustomerIdAndStatusAndHoldExpiresAtAfter(Long customerId, BookingStatus status, LocalDateTime now);

    /** Đơn chờ thanh toán đã quá hạn giữ chỗ (job xử lý theo lô). */
    @Query("select b.id from Booking b where b.status = com.tripconnect.backend.enums.BookingStatus.PENDING_PAYMENT "
            + "and b.holdExpiresAt < :before order by b.holdExpiresAt")
    List<Long> findExpiredPendingIds(@Param("before") LocalDateTime before, Pageable pageable);

    /** Đơn đã thanh toán của chuyến đã về từ ngày :lastEndDate trở về trước (ngày về = ngày đi + số ngày - 1). */
    @Query(value = """
            SELECT b.id FROM bookings b
            JOIN tour_departures d ON d.id = b.departure_id
            JOIN tours t ON t.id = b.tour_id
            WHERE b.status = 'PAID' AND d.start_date + (t.duration_days - 1) <= :lastEndDate
            """, nativeQuery = true)
    List<Long> findPaidIdsEndedOnOrBefore(@Param("lastEndDate") LocalDate lastEndDate);

    /** Đơn đã thanh toán, khởi hành trong khoảng [from, to] và chưa nhắc đủ 2 lần. */
    @Query("select b.id from Booking b where b.status = com.tripconnect.backend.enums.BookingStatus.PAID "
            + "and b.departure.startDate between :from and :to and b.reminderStage < 2 order by b.id")
    List<Long> findPaidIdsForReminder(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select coalesce(sum(b.adults + b.children), 0) from Booking b where b.departure.id = :departureId "
            + "and b.status = com.tripconnect.backend.enums.BookingStatus.PAID")
    long sumPaidSeats(@Param("departureId") Long departureId);

    /** Tour riêng đã cọc mà quá hạn trả phần còn lại (hạn là hết ngày balanceDueDate). */
    @Query("select b.id from Booking b where b.status = com.tripconnect.backend.enums.BookingStatus.DEPOSIT_PAID "
            + "and b.balanceDueDate < :today order by b.id")
    List<Long> findOverdueBalanceIds(@Param("today") LocalDate today);

    /** Tour riêng chờ đặt cọc, sắp hết hạn và chưa nhắc. */
    @Query("select b.id from Booking b where b.status = com.tripconnect.backend.enums.BookingStatus.PENDING_PAYMENT "
            + "and b.depositAmount > 0 and b.paymentReminderStage < 1 and b.holdExpiresAt between :now and :before order by b.id")
    List<Long> findDepositDueIds(@Param("now") LocalDateTime now, @Param("before") LocalDateTime before);

    /** Tour riêng đã cọc, hạn trả phần còn lại trong vòng tới :lastDue (nhắc trước 3 ngày / 1 ngày). */
    @Query("select b.id from Booking b where b.status = com.tripconnect.backend.enums.BookingStatus.DEPOSIT_PAID "
            + "and b.balanceDueDate <= :lastDue and b.paymentReminderStage < 3 order by b.id")
    List<Long> findBalanceDueIds(@Param("lastDue") LocalDate lastDue);

    /** Đơn của tour riêng sinh ra từ một yêu cầu thiết kế tour. */
    @Query("select b from Booking b where b.tour.customRequestId = :requestId order by b.id desc")
    List<Booking> findByCustomRequestId(@Param("requestId") Long requestId);

    /** Đơn còn hiệu lực của một lịch (khi Agent hủy chuyến). */
    @Query("select b from Booking b where b.departure.id = :departureId and b.status in :statuses")
    List<Booking> findByDepartureIdAndStatusIn(@Param("departureId") Long departureId,
                                               @Param("statuses") Collection<BookingStatus> statuses);

    /** Danh sách hành khách đã xác nhận của một lịch (Agent chuẩn bị đoàn). */
    @EntityGraph(attributePaths = {"passengers", "customer"})
    @Query("select distinct b from Booking b where b.departure.id = :departureId and b.status in :statuses order by b.id")
    List<Booking> findWithPassengersByDepartureId(@Param("departureId") Long departureId,
                                                  @Param("statuses") Collection<BookingStatus> statuses);

    @Override
    @EntityGraph(attributePaths = {"tour", "departure", "customer"})
    Page<Booking> findAll(Specification<Booking> spec, Pageable pageable);

    long countByRefundStatus(RefundStatus refundStatus);

    boolean existsByCode(String code);
}
