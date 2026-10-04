package com.tripconnect.backend.service.booking;

import com.tripconnect.backend.dto.PageResponse;
import com.tripconnect.backend.dto.booking.BookingResponses;
import com.tripconnect.backend.entity.Booking;
import com.tripconnect.backend.entity.Tour;
import com.tripconnect.backend.entity.TourDeparture;
import com.tripconnect.backend.enums.BookingStatus;
import com.tripconnect.backend.enums.CancelledBy;
import com.tripconnect.backend.enums.RefundStatus;
import com.tripconnect.backend.exception.ResourceNotFoundException;
import com.tripconnect.backend.repository.BookingRepository;
import com.tripconnect.backend.repository.SearchPatterns;
import com.tripconnect.backend.repository.StablePaging;
import com.tripconnect.backend.repository.TourDepartureRepository;
import com.tripconnect.backend.service.tour.TourBookingStats;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Đơn đặt tour nhìn từ phía đơn vị tổ chức / quản trị:
 *  - Agent: đơn của các tour mình tổ chức + danh sách đoàn từng lịch khởi hành.
 *  - Admin: mọi đơn, hủy đơn vì bất khả kháng (hoàn 100%).
 */
@Service
@RequiredArgsConstructor
public class BookingManagementService {

    /** Bộ lọc danh sách. agentId != null = chỉ đơn của Agent đó. */
    public record Filter(Long agentId, BookingStatus status, RefundStatus refundStatus, Long tourId, Long departureId,
                         String keyword) {
    }

    private final BookingRepository bookingRepository;
    private final TourDepartureRepository departureRepository;
    private final TourBookingStats bookingStats;
    private final BookingCancellation cancellation;
    private final BookingAssembler assembler;

    @Transactional(readOnly = true)
    public PageResponse<BookingResponses.Summary> list(Filter filter, Pageable pageable) {
        Specification<Booking> spec = (root, query, cb) -> cb.conjunction();
        if (filter.agentId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("agent").get("id"), filter.agentId()));
        }
        if (filter.status() != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), filter.status()));
        if (filter.refundStatus() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("refundStatus"), filter.refundStatus()));
        }
        if (filter.tourId() != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("tour").get("id"), filter.tourId()));
        if (filter.departureId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("departure").get("id"), filter.departureId()));
        }
        if (filter.keyword() != null && !filter.keyword().isBlank()) {
            String pattern = SearchPatterns.contains(filter.keyword());
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("code")), pattern, SearchPatterns.ESCAPE),
                    cb.like(cb.lower(root.get("contactName")), pattern, SearchPatterns.ESCAPE),
                    cb.like(cb.lower(root.get("contactEmail")), pattern, SearchPatterns.ESCAPE),
                    cb.like(root.get("contactPhone"), pattern, SearchPatterns.ESCAPE),
                    cb.like(cb.lower(root.get("tour").get("title")), pattern, SearchPatterns.ESCAPE)));
        }
        var page = bookingRepository.findAll(spec, StablePaging.of(pageable));
        return PageResponse.of(page, assembler.toSummaries(page));
    }

    /** @param agentId null = Admin (xem mọi đơn) */
    @Transactional(readOnly = true)
    public BookingResponses.Detail get(Long bookingId, Long agentId) {
        Booking booking = bookingRepository.findById(bookingId)
                .filter(b -> agentId == null || (b.getAgent() != null && b.getAgent().getId().equals(agentId)))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt tour"));
        return assembler.toDetail(booking, agentId == null ? BookingAssembler.Viewer.ADMIN : BookingAssembler.Viewer.AGENT);
    }

    /**
     * Danh sách đoàn của một lịch khởi hành (chỉ đơn đã thanh toán / hoàn thành).
     *
     * @param agentId null = Admin; Admin xem được mọi lịch
     */
    @Transactional(readOnly = true)
    public BookingResponses.Manifest manifest(Long departureId, Long agentId) {
        TourDeparture departure = departureRepository.findById(departureId)
                .filter(d -> agentId == null || (!d.getTour().isPlatformTour() && d.getTour().getAgent().getId().equals(agentId)))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch khởi hành"));
        Tour tour = departure.getTour();
        List<BookingResponses.ManifestBooking> bookings = bookingRepository
                .findWithPassengersByDepartureId(departureId, List.of(BookingStatus.PAID, BookingStatus.COMPLETED)).stream()
                .map(b -> new BookingResponses.ManifestBooking(b.getId(), b.getCode(), b.getStatus(), b.getContactName(),
                        b.getContactPhone(), b.getContactEmail(), b.getNote(),
                        b.getAdults(), b.getChildren(), b.getInfants(),
                        b.getPassengers().stream().map(BookingAssembler::toPassenger).toList()))
                .toList();
        return new BookingResponses.Manifest(departure.getId(), tour.getTitle(), departure.getStartDate(),
                departure.endDate(tour.getDurationDays()), departure.getCapacity(),
                bookingStats.seatsBooked(departure.getId()), bookings);
    }

    /** Admin hủy đơn vì bất khả kháng (thiên tai, dịch bệnh, đơn vị tổ chức vi phạm...) — hoàn 100%. */
    @Transactional
    public BookingResponses.Detail cancelByAdmin(Long bookingId, String reason) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt tour"));
        cancellation.cancel(booking, CancelledBy.ADMIN, reason.trim(), booking.getTotalAmount(), true);
        return assembler.toDetail(booking, BookingAssembler.Viewer.ADMIN);
    }
}
