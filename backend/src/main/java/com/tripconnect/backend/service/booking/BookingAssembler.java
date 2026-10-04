package com.tripconnect.backend.service.booking;

import com.tripconnect.backend.dto.booking.BookingResponses;
import com.tripconnect.backend.entity.*;
import com.tripconnect.backend.enums.BookingStatus;
import com.tripconnect.backend.repository.AgentProfileRepository;
import com.tripconnect.backend.repository.PaymentRepository;
import com.tripconnect.backend.repository.RefundRepository;
import com.tripconnect.backend.repository.ReviewRepository;
import com.tripconnect.backend.service.review.ReviewRules;
import com.tripconnect.backend.repository.TourImageRepository;
import com.tripconnect.backend.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Chuyển đơn đặt thành DTO. Phải gọi trong transaction (có quan hệ LAZY). */
@Component
@RequiredArgsConstructor
public class BookingAssembler {

    /** Ai đang xem: khách không thấy hoa hồng; Agent / Admin thấy. */
    public enum Viewer { CUSTOMER, AGENT, ADMIN }

    private final TourImageRepository imageRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final AgentProfileRepository agentProfileRepository;
    private final FileStorageService fileStorageService;
    private final ReviewRepository reviewRepository;
    private final Clock clock;

    public List<BookingResponses.Summary> toSummaries(Page<Booking> page) {
        List<Booking> bookings = page.getContent();
        Map<Long, String> covers = coverUrls(bookings.stream().map(b -> b.getTour().getId()).distinct().toList());
        Set<Long> reviewed = bookings.isEmpty() ? Set.of()
                : new HashSet<>(reviewRepository.findReviewedBookingIds(bookings.stream().map(Booking::getId).toList()));
        LocalDateTime now = LocalDateTime.now(clock);
        return bookings.stream().map(b -> new BookingResponses.Summary(
                b.getId(), b.getCode(), b.getStatus(), b.getRefundStatus(),
                b.getTour().getId(), b.getTour().getTitle(), covers.get(b.getTour().getId()),
                b.getDeparture().getStartDate(), b.getDeparture().endDate(b.getTour().getDurationDays()),
                b.getAdults(), b.getChildren(), b.getInfants(), b.getTotalAmount(), b.getRefundAmount(),
                b.getContactName(), b.getCustomer().getEmail(), b.getHoldExpiresAt(),
                !reviewed.contains(b.getId()) && ReviewRules.canWrite(b, now), b.getCreatedAt())).toList();
    }

    public BookingResponses.Detail toDetail(Booking b, Viewer viewer) {
        Tour tour = b.getTour();
        TourDeparture departure = b.getDeparture();
        boolean staff = viewer != Viewer.CUSTOMER;
        LocalDateTime now = LocalDateTime.now(clock);
        boolean pendingAlive = b.getStatus() == BookingStatus.PENDING_PAYMENT && b.getHoldExpiresAt().isAfter(now);
        boolean beforeDeparture = departure.getStartDate().isAfter(LocalDate.now(clock));

        List<BookingResponses.PaymentView> payments = staff
                ? paymentRepository.findByBookingIdOrderByIdDesc(b.getId()).stream()
                .map(p -> new BookingResponses.PaymentView(p.getId(), p.getTxnRef(), p.getAmount(), p.getStatus(),
                        p.getBankCode(), p.getVnpTransactionNo(), p.getVnpResponseCode(), p.getCreatedAt()))
                .toList()
                : List.of();
        Long reviewId = reviewRepository.findByBookingId(b.getId()).map(Review::getId).orElse(null);
        List<BookingResponses.RefundView> refunds = refundRepository.findByBookingIdOrderByIdDesc(b.getId()).stream()
                .map(r -> new BookingResponses.RefundView(r.getId(), r.getAmount(), r.getStatus(), r.getReason(),
                        staff ? r.getMessage() : null, r.getProcessedAt(), r.getCreatedAt()))
                .toList();

        return new BookingResponses.Detail(
                b.getId(), b.getCode(), b.getStatus(),
                tour.getId(), tour.getTitle(), coverUrls(List.of(tour.getId())).get(tour.getId()), tour.isInternational(),
                providerName(tour),
                departure.getId(), departure.getStartDate(), departure.endDate(tour.getDurationDays()),
                tour.getMeetingPoint(), tour.getMeetingTime().toString(),
                b.getAdults(), b.getChildren(), b.getInfants(), b.getAdultPrice(), b.getChildPrice(), b.getTotalAmount(),
                staff ? b.getCommissionRate() : null, staff ? b.getCommissionAmount() : null,
                b.getContactName(), b.getContactPhone(), b.getContactEmail(), b.getNote(),
                b.getPassengers().stream().map(BookingAssembler::toPassenger).toList(),
                BookingRules.passengerListDeadline(departure.getStartDate()),
                viewer == Viewer.CUSTOMER && (pendingAlive || b.getStatus() == BookingStatus.PAID)
                        && BookingRules.passengerListOpen(departure.getStartDate(), LocalDate.now(clock)),
                new BookingResponses.RefundPolicy(b.getRefundFullDays(), b.getRefundPartialDays(), b.getRefundPartialPercent()),
                b.getHoldExpiresAt(), b.getPaidAt(), b.getCompletedAt(), b.getCancelledAt(), b.getCancelledBy(),
                b.getCancelReason(), b.getRefundAmount(), b.getRefundStatus(),
                payments, refunds,
                pendingAlive,
                pendingAlive || (b.getStatus() == BookingStatus.PAID && beforeDeparture),
                reviewId,
                viewer == Viewer.CUSTOMER && reviewId == null && ReviewRules.canWrite(b, now),
                b.getCreatedAt());
    }

    public static BookingResponses.Passenger toPassenger(BookingPassenger p) {
        return new BookingResponses.Passenger(p.getId(), p.getFullName(), p.getDateOfBirth(), p.getType(), p.getPassportNumber());
    }

    private String providerName(Tour tour) {
        if (tour.isPlatformTour()) return "TripConnect";
        return agentProfileRepository.findByUserId(tour.getAgent().getId())
                .map(AgentProfile::getCompanyName).orElse("Đối tác TripConnect");
    }

    private Map<Long, String> coverUrls(Collection<Long> tourIds) {
        if (tourIds.isEmpty()) return Map.of();
        return imageRepository.findCoverImages(tourIds).stream()
                .collect(Collectors.toMap(i -> i.getTour().getId(), Function.identity(), (a, c) -> a.getId() < c.getId() ? a : c))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> fileStorageService.publicUrl(e.getValue().getPublicId(), e.getValue().getFormat())));
    }
}
