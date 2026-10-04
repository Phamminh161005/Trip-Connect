package com.tripconnect.backend.service.tour;

import com.tripconnect.backend.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Số chỗ đã đặt của lịch khởi hành. Các quy tắc "đã có khách thì không được đổi ngày/giá, không được xóa"
 * đều hỏi qua lớp này.
 * Chỗ đã đặt = đơn đã thanh toán / hoàn thành + đơn đang chờ thanh toán còn hạn giữ chỗ (em bé không tính chỗ).
 */
@Component
@RequiredArgsConstructor
public class TourBookingStats {

    private final BookingRepository bookingRepository;

    /** @return departureId -> số chỗ đã đặt (lịch không có trong map = 0 chỗ) */
    public Map<Long, Integer> seatsBooked(Collection<Long> departureIds) {
        if (departureIds.isEmpty()) return Map.of();
        Map<Long, Integer> result = new HashMap<>();
        for (Object[] row : bookingRepository.sumSeatsByDeparture(departureIds, LocalDateTime.now())) {
            result.put((Long) row[0], ((Number) row[1]).intValue());
        }
        return result;
    }

    public int seatsBooked(Long departureId) {
        return seatsBooked(List.of(departureId)).getOrDefault(departureId, 0);
    }

    /** Có đơn còn hiệu lực (đang giữ chỗ / đã thanh toán) — chặn đổi số ngày của tour. */
    public boolean tourHasActiveBookings(Long tourId) {
        return bookingRepository.tourHasActiveBookings(tourId, LocalDateTime.now());
    }

    /** Từng có đơn (kể cả đã hủy) — không xóa được tour vì phải giữ lịch sử đơn hàng, hoàn tiền, đối soát. */
    public boolean tourHasBookings(Long tourId) {
        return bookingRepository.existsByTourId(tourId);
    }

    public boolean departureHasBookings(Long departureId) {
        return bookingRepository.existsByDepartureId(departureId);
    }
}
