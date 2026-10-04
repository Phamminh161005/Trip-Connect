package com.tripconnect.backend.repository;

import com.tripconnect.backend.entity.TourDeparture;
import com.tripconnect.backend.enums.DepartureStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TourDepartureRepository extends JpaRepository<TourDeparture, Long> {

    List<TourDeparture> findByTourIdOrderByStartDateAsc(Long tourId);

    Optional<TourDeparture> findByIdAndTourId(Long id, Long tourId);

    /**
     * Khóa lịch khởi hành khi đặt chỗ: 2 khách đặt cùng lúc phải xếp hàng, người sau đếm chỗ SAU KHI người trước
     * đã giữ chỗ -> không bán vượt số chỗ. Nạp luôn tour (cần tên, trạng thái, Agent).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from TourDeparture d join fetch d.tour t left join fetch t.agent where d.id = :id")
    Optional<TourDeparture> findByIdForUpdate(@Param("id") Long id);

    /** Đã có lịch (chưa hủy) trùng ngày khởi hành chưa. {@code excludeId} = lịch đang sửa (null khi thêm mới). */
    @Query("""
            select count(d) > 0 from TourDeparture d
            where d.tour.id = :tourId and d.startDate = :startDate and d.status <> :cancelled
              and (:excludeId is null or d.id <> :excludeId)
            """)
    boolean existsActiveOnDate(@Param("tourId") Long tourId,
                               @Param("startDate") LocalDate startDate,
                               @Param("cancelled") DepartureStatus cancelled,
                               @Param("excludeId") Long excludeId);

    /** Giá thấp nhất + số lịch đang mở bán của nhiều tour — dùng cho danh sách tour ("Từ 3.490.000đ"). */
    @Query("""
            select d.tour.id as tourId, min(d.adultPrice) as minAdultPrice, count(d) as openCount
            from TourDeparture d
            where d.tour.id in :tourIds and d.status = :open and d.startDate > :today
            group by d.tour.id
            """)
    List<OpenDepartureStats> findOpenStats(@Param("tourIds") Collection<Long> tourIds,
                                           @Param("open") DepartureStatus open,
                                           @Param("today") LocalDate today);

    /** Lịch chưa hủy, khởi hành trong khoảng [from, to], có khách đã thanh toán và chưa nhắc đơn vị tổ chức đủ 2 lần. */
    @Query("""
            select d.id from TourDeparture d
            where d.startDate between :from and :to and d.status <> com.tripconnect.backend.enums.DepartureStatus.CANCELLED
              and d.organizerReminderStage < 2
              and exists (select 1 from Booking b where b.departure = d
                          and b.status = com.tripconnect.backend.enums.BookingStatus.PAID)
            order by d.id
            """)
    List<Long> findIdsForOrganizerReminder(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Lịch chưa hủy, khởi hành trong khoảng [from, to], chưa nhắc ít khách và đã bán dưới {@code percent}% số chỗ. */
    @Query("""
            select d.id from TourDeparture d
            where d.startDate between :from and :to and d.status <> com.tripconnect.backend.enums.DepartureStatus.CANCELLED
              and d.lowBookingReminded = false
              and (select coalesce(sum(b.adults + b.children), 0) from Booking b where b.departure = d
                   and b.status = com.tripconnect.backend.enums.BookingStatus.PAID) * 100 < d.capacity * :percent
            order by d.id
            """)
    List<Long> findIdsForLowBookingReminder(@Param("from") LocalDate from, @Param("to") LocalDate to,
                                            @Param("percent") int percent);

    interface OpenDepartureStats {
        Long getTourId();

        Long getMinAdultPrice();

        Long getOpenCount();
    }
}
