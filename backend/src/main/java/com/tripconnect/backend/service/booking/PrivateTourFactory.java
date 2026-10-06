package com.tripconnect.backend.service.booking;

import com.tripconnect.backend.entity.*;
import com.tripconnect.backend.enums.BookingStatus;
import com.tripconnect.backend.enums.DepartureStatus;
import com.tripconnect.backend.enums.TourStatus;
import com.tripconnect.backend.repository.BookingRepository;
import com.tripconnect.backend.repository.TourDepartureRepository;
import com.tripconnect.backend.repository.TourRepository;
import com.tripconnect.backend.service.tour.TourContentWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

/**
 * Khách đồng ý đề xuất -> tạo tour riêng (PRIVATE, nội dung đúng như đề xuất) + một lịch khởi hành + đơn đặt của khách.
 * Đơn chờ đặt cọc trong 48 giờ; ngày đi quá gần (không kịp 2 lần thanh toán) thì trả toàn bộ một lần.
 */
@Component
@RequiredArgsConstructor
public class PrivateTourFactory {

    private final TourRepository tourRepository;
    private final TourDepartureRepository departureRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final BookingSettings settings;

    @Transactional(propagation = Propagation.MANDATORY)
    public Booking create(CustomRequest request, CustomProposal proposal, LocalDateTime now) {
        Tour tour = buildTour(request, proposal);
        tourRepository.save(tour);

        TourDeparture departure = new TourDeparture();
        departure.setTour(tour);
        departure.setStartDate(proposal.getStartDate());
        departure.setCapacity(Math.max(1, request.getAdults() + request.getChildren()));
        departure.setAdultPrice(proposal.getAdultPrice());
        departure.setChildPrice(proposal.getChildPrice());
        // Không mở bán: chỉ đơn của khách này
        departure.setStatus(DepartureStatus.CLOSED);
        departureRepository.save(departure);

        User customer = request.getCustomer();
        Booking booking = new Booking();
        booking.setCode(bookingService.newCode(now.toLocalDate()));
        booking.setCustomer(customer);
        booking.setTour(tour);
        booking.setDeparture(departure);
        booking.setAgent(proposal.getAgent());
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setAdults(request.getAdults());
        booking.setChildren(request.getChildren());
        booking.setInfants(request.getInfants());
        booking.setAdultPrice(proposal.getAdultPrice());
        booking.setChildPrice(proposal.getChildPrice());
        booking.setTotalAmount(proposal.getTotalPrice());
        booking.setCommissionRate(settings.getCommissionRate());
        booking.setCommissionAmount(BigDecimal.valueOf(proposal.getTotalPrice()).multiply(settings.getCommissionRate())
                .setScale(0, RoundingMode.HALF_UP).longValue());
        booking.setContactName(customer.getFullName() == null || customer.getFullName().isBlank()
                ? customer.getEmail() : customer.getFullName().trim());
        booking.setContactPhone(customer.getPhone() == null ? "" : customer.getPhone());
        booking.setContactEmail(customer.getEmail().toLowerCase(Locale.ROOT));
        booking.setRefundFullDays((short) settings.getRefundFullDays());
        booking.setRefundPartialDays((short) settings.getRefundPartialDays());
        booking.setRefundPartialPercent((short) settings.getRefundPartialPercent());

        LocalDateTime holdUntil = now.plusHours(BookingRules.DEPOSIT_HOLD_HOURS);
        booking.setHoldExpiresAt(holdUntil);
        LocalDate dueDate = BookingRules.balanceDueDate(proposal.getStartDate(), tour.isInternational());
        if (BookingRules.splitPayment(holdUntil.toLocalDate(), dueDate)) {
            booking.setDepositAmount(proposal.getDepositAmount());
            booking.setBalanceDueDate(dueDate);
        }
        return bookingRepository.save(booking);
    }

    private static Tour buildTour(CustomRequest request, CustomProposal proposal) {
        Tour tour = new Tour();
        tour.setAgent(proposal.getAgent());
        tour.setCreatedBy(proposal.getAgent());
        tour.setStatus(TourStatus.PRIVATE);
        tour.setCustomRequestId(request.getId());
        tour.setTitle(proposal.getTitle());
        tour.setDepartureLocation(request.getDepartureLocation());
        tour.setDestinations(new HashSet<>(request.getDestinations()));
        tour.setCategories(new HashSet<>(request.getCategories()));
        tour.setInternational(request.getDestinations().stream().anyMatch(l -> !l.isVietnam()));
        tour.setDurationDays(proposal.getDurationDays());
        tour.setDurationNights(proposal.getDurationNights());
        tour.setHighlights(new ArrayList<>());
        tour.setTransportModes(new HashSet<>(proposal.getTransportModes()));
        tour.setAccommodationType(proposal.getAccommodationType());
        tour.setMeetingPoint(proposal.getMeetingPoint());
        tour.setMeetingTime(proposal.getMeetingTime());
        tour.setIncludedServices(new ArrayList<>(proposal.getIncludedServices()));
        tour.setExcludedServices(new ArrayList<>(proposal.getExcludedServices()));
        tour.setNotes(proposal.getNotes());
        for (CustomProposalDay day : proposal.getDays()) {
            TourItineraryDay copy = new TourItineraryDay();
            copy.setTour(tour);
            copy.setDayNumber(day.getDayNumber());
            copy.setTitle(day.getTitle());
            copy.setDescription(day.getDescription());
            copy.setHasBreakfast(day.isHasBreakfast());
            copy.setHasLunch(day.isHasLunch());
            copy.setHasDinner(day.isHasDinner());
            copy.setAccommodation(day.getAccommodation());
            tour.getItineraryDays().add(copy);
        }
        tour.setSearchText(TourContentWriter.buildSearchText(tour));
        return tour;
    }
}
