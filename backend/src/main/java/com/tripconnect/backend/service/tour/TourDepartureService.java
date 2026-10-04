package com.tripconnect.backend.service.tour;

import com.tripconnect.backend.dto.tour.DepartureRequest;
import com.tripconnect.backend.dto.tour.TourResponses;
import com.tripconnect.backend.entity.Tour;
import com.tripconnect.backend.entity.TourDeparture;
import com.tripconnect.backend.enums.CancelledBy;
import com.tripconnect.backend.enums.DepartureStatus;
import com.tripconnect.backend.enums.TourStatus;
import com.tripconnect.backend.exception.ResourceNotFoundException;
import com.tripconnect.backend.repository.TourDepartureRepository;
import com.tripconnect.backend.service.booking.BookingCancellation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Lịch khởi hành. Không cần Admin duyệt lại (Admin duyệt NỘI DUNG tour; lịch chỉ là ngày + số chỗ + giá),
 * thay vào đó hệ thống tự kiểm tra: ngày đi đủ xa, giá hợp lệ, lịch đã có khách thì không đổi ngày/giá.
 */
@Service
@RequiredArgsConstructor
public class TourDepartureService {

    private final TourDepartureRepository departureRepository;
    private final TourAccess access;
    private final TourAssembler assembler;
    private final TourBookingStats bookingStats;
    private final BookingCancellation bookingCancellation;
    private final Clock clock;

    @Transactional
    public TourResponses.Departure add(TourActor actor, Long tourId, DepartureRequest request) {
        Tour tour = access.requireManageable(tourId, actor);
        requireNotSuspended(tour);
        validatePrices(request);
        requireFarEnough(request.getStartDate());
        requireDateFree(tour, request.getStartDate(), null);

        TourDeparture departure = new TourDeparture();
        departure.setTour(tour);
        departure.setStartDate(request.getStartDate());
        departure.setCapacity(request.getCapacity());
        departure.setAdultPrice(request.getAdultPrice());
        departure.setChildPrice(request.getChildPrice());
        departure.setStatus(DepartureStatus.OPEN);
        return toResponse(departureRepository.save(departure), 0);
    }

    /** Lịch đã có khách: chỉ được tăng số chỗ, không đổi ngày / giá (khách đã trả tiền theo giá cũ). */
    @Transactional
    public TourResponses.Departure update(TourActor actor, Long tourId, Long departureId, DepartureRequest request) {
        Tour tour = access.requireManageable(tourId, actor);
        requireNotSuspended(tour);
        TourDeparture departure = requireDeparture(tourId, departureId);
        requireChangeable(departure);
        validatePrices(request);

        int booked = bookingStats.seatsBooked(departure.getId());
        boolean dateChanged = !request.getStartDate().equals(departure.getStartDate());
        boolean priceChanged = request.getAdultPrice() != departure.getAdultPrice()
                || request.getChildPrice() != departure.getChildPrice();
        if (booked > 0 && (dateChanged || priceChanged)) {
            throw new IllegalStateException("Lịch đã có khách đặt nên không thể đổi ngày hoặc giá. "
                    + "Bạn có thể ngừng bán lịch này và thêm lịch mới");
        }
        if (request.getCapacity() < booked) {
            throw new IllegalArgumentException("Số chỗ không được ít hơn số khách đã đặt (" + booked + ")");
        }
        if (dateChanged) {
            requireFarEnough(request.getStartDate());
            requireDateFree(tour, request.getStartDate(), departure.getId());
        }

        departure.setStartDate(request.getStartDate());
        departure.setCapacity(request.getCapacity());
        departure.setAdultPrice(request.getAdultPrice());
        departure.setChildPrice(request.getChildPrice());
        return toResponse(departure, booked);
    }

    /** Ngừng bán thêm — khách đã đặt vẫn đi bình thường. */
    @Transactional
    public TourResponses.Departure close(TourActor actor, Long tourId, Long departureId) {
        access.requireManageable(tourId, actor);
        TourDeparture departure = requireDeparture(tourId, departureId);
        requireChangeable(departure);
        if (departure.getStatus() != DepartureStatus.OPEN) {
            throw new IllegalStateException("Lịch khởi hành không ở trạng thái đang mở bán");
        }
        departure.setStatus(DepartureStatus.CLOSED);
        return toResponse(departure, bookingStats.seatsBooked(departure.getId()));
    }

    @Transactional
    public TourResponses.Departure reopen(TourActor actor, Long tourId, Long departureId) {
        Tour tour = access.requireManageable(tourId, actor);
        requireNotSuspended(tour);
        TourDeparture departure = requireDeparture(tourId, departureId);
        if (departure.getStatus() != DepartureStatus.CLOSED) {
            throw new IllegalStateException("Chỉ mở bán lại được lịch đang ngừng bán");
        }
        requireFarEnough(departure.getStartDate());
        departure.setStatus(DepartureStatus.OPEN);
        return toResponse(departure, bookingStats.seatsBooked(departure.getId()));
    }

    /**
     * Hủy chuyến (không đủ khách, thời tiết...). Mọi đơn còn hiệu lực bị hủy theo: khách đã thanh toán được
     * hoàn 100% (lỗi phía đơn vị tổ chức) và nhận email.
     */
    @Transactional
    public TourResponses.Departure cancel(TourActor actor, Long tourId, Long departureId, String reason) {
        access.requireManageable(tourId, actor);
        // Khóa lịch: không cho khách đặt thêm đúng lúc đang hủy chuyến
        TourDeparture departure = departureRepository.findByIdForUpdate(departureId)
                .filter(d -> d.getTour().getId().equals(tourId))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch khởi hành"));
        requireChangeable(departure);
        departure.setStatus(DepartureStatus.CANCELLED);
        departure.setCancelReason(reason.trim());
        bookingCancellation.cancelAllForDeparture(departure.getId(), actor.admin() ? CancelledBy.ADMIN : CancelledBy.AGENT, reason.trim());
        return toResponse(departure, bookingStats.seatsBooked(departure.getId()));
    }

    /** Xóa hẳn — chỉ khi chưa từng có đơn đặt (có đơn thì dùng Hủy chuyến để còn hoàn tiền và giữ lịch sử). */
    @Transactional
    public void delete(TourActor actor, Long tourId, Long departureId) {
        access.requireManageable(tourId, actor);
        TourDeparture departure = requireDeparture(tourId, departureId);
        if (bookingStats.departureHasBookings(departure.getId())) {
            throw new IllegalStateException("Lịch đã có khách đặt nên không thể xóa. Hãy dùng chức năng Hủy chuyến");
        }
        departureRepository.delete(departure);
    }

    // ===================== Quy tắc =====================

    private static void validatePrices(DepartureRequest request) {
        if (request.getChildPrice() > request.getAdultPrice()) {
            throw new IllegalArgumentException("Giá trẻ em không được cao hơn giá người lớn");
        }
    }

    private void requireFarEnough(LocalDate startDate) {
        LocalDate earliest = LocalDate.now(clock).plusDays(TourRules.MIN_DEPARTURE_LEAD_DAYS);
        if (startDate.isBefore(earliest)) {
            throw new IllegalArgumentException("Ngày khởi hành phải cách hôm nay ít nhất "
                    + TourRules.MIN_DEPARTURE_LEAD_DAYS + " ngày");
        }
    }

    private void requireDateFree(Tour tour, LocalDate startDate, Long excludeId) {
        if (departureRepository.existsActiveOnDate(tour.getId(), startDate, DepartureStatus.CANCELLED, excludeId)) {
            throw new IllegalArgumentException("Tour đã có lịch khởi hành ngày này");
        }
    }

    /** Lịch đã hủy hoặc đã khởi hành thì không thao tác được nữa. */
    private void requireChangeable(TourDeparture departure) {
        if (departure.getStatus() == DepartureStatus.CANCELLED) {
            throw new IllegalStateException("Lịch khởi hành đã bị hủy");
        }
        if (!departure.getStartDate().isAfter(LocalDate.now(clock))) {
            throw new IllegalStateException("Lịch khởi hành đã tới ngày đi nên không thể thay đổi");
        }
    }

    private static void requireNotSuspended(Tour tour) {
        if (tour.getStatus() == TourStatus.SUSPENDED) {
            throw new IllegalStateException("Tour đang bị đình chỉ nên không thể thêm hoặc mở bán lịch khởi hành");
        }
    }

    private TourDeparture requireDeparture(Long tourId, Long departureId) {
        return departureRepository.findByIdAndTourId(departureId, tourId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch khởi hành"));
    }

    private TourResponses.Departure toResponse(TourDeparture departure, int seatsBooked) {
        return assembler.toDeparture(departure, departure.getTour().getDurationDays(), seatsBooked);
    }
}
