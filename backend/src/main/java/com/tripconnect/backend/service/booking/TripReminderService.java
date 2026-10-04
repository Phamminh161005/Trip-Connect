package com.tripconnect.backend.service.booking;

import com.tripconnect.backend.entity.Booking;
import com.tripconnect.backend.entity.BookingPassenger;
import com.tripconnect.backend.entity.Tour;
import com.tripconnect.backend.entity.TourDeparture;
import com.tripconnect.backend.entity.User;
import com.tripconnect.backend.enums.BookingStatus;
import com.tripconnect.backend.enums.DepartureStatus;
import com.tripconnect.backend.enums.PassengerType;
import com.tripconnect.backend.repository.AgentProfileRepository;
import com.tripconnect.backend.repository.BookingRepository;
import com.tripconnect.backend.repository.TourDepartureRepository;
import com.tripconnect.backend.service.EmailTemplates;
import com.tripconnect.backend.service.NotificationEvents;
import com.tripconnect.backend.service.WebNotifications;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.LongConsumer;

/**
 * Email nhắc lịch khởi hành, 2 lần: trước 3 ngày và trước 1 ngày.
 * Khách nhận theo từng đơn; đơn vị tổ chức (Agent, hoặc Admin với tour của TripConnect) nhận một email / lịch.
 * Ngoài ra nhắc đơn vị tổ chức một lần khi lịch còn 7 ngày mà bán chưa tới 30% số chỗ.
 */
@Slf4j
@Service
public class TripReminderService {

    static final int FIRST_REMINDER_DAYS = 3;
    static final int LAST_REMINDER_DAYS = 1;
    static final short STAGE_FIRST = 1;
    static final short STAGE_LAST = 2;
    static final int LOW_BOOKING_DAYS = 7;
    /** Lỡ mốc 7 ngày thì gửi bù tới hết ngày này (từ mốc 3 ngày đã có email danh sách khách). */
    static final int LOW_BOOKING_LATEST_DAYS = 4;
    static final int LOW_BOOKING_PERCENT = 30;

    private static final Map<PassengerType, String> PASSENGER_LABELS = Map.of(
            PassengerType.ADULT, "Người lớn", PassengerType.CHILD, "Trẻ em", PassengerType.INFANT, "Trẻ sơ sinh");

    private final BookingRepository bookingRepository;
    private final TourDepartureRepository departureRepository;
    private final AgentProfileRepository agentProfileRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final String frontendUrl;

    public TripReminderService(BookingRepository bookingRepository, TourDepartureRepository departureRepository,
                               AgentProfileRepository agentProfileRepository, ApplicationEventPublisher eventPublisher,
                               TransactionTemplate transactionTemplate, Clock clock,
                               @Value("${app.frontend-url}") String frontendUrl) {
        this.bookingRepository = bookingRepository;
        this.departureRepository = departureRepository;
        this.agentProfileRepository = agentProfileRepository;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    /**
     * Lần nhắc đến hạn với số ngày còn lại tới ngày đi, 0 nếu không có.
     * Trễ lần trước 3 ngày (vd backend tắt) vẫn gửi bù khi còn 2 ngày; lần cuối chỉ gửi đúng hôm trước ngày đi.
     */
    static short dueStage(long daysLeft, short sentStage) {
        if (daysLeft == LAST_REMINDER_DAYS) return sentStage < STAGE_LAST ? STAGE_LAST : 0;
        if (daysLeft > LAST_REMINDER_DAYS && daysLeft <= FIRST_REMINDER_DAYS) return sentStage < STAGE_FIRST ? STAGE_FIRST : 0;
        return 0;
    }

    public void sendDueReminders() {
        LocalDate today = LocalDate.now(clock);
        LocalDate from = today.plusDays(LAST_REMINDER_DAYS);
        LocalDate to = today.plusDays(FIRST_REMINDER_DAYS);
        int customers = runEach(bookingRepository.findPaidIdsForReminder(from, to), id -> remindCustomer(id, today), "đơn");
        int organizers = runEach(departureRepository.findIdsForOrganizerReminder(from, to), id -> remindOrganizer(id, today), "lịch");
        int lowBookings = runEach(departureRepository.findIdsForLowBookingReminder(today.plusDays(LOW_BOOKING_LATEST_DAYS),
                today.plusDays(LOW_BOOKING_DAYS), LOW_BOOKING_PERCENT), id -> remindLowBookings(id, today), "lịch ít khách");
        if (customers + organizers + lowBookings > 0) {
            log.info("Đã xử lý nhắc lịch khởi hành: {} đơn, {} lịch, {} lịch ít khách", customers, organizers, lowBookings);
        }
    }

    private int runEach(List<Long> ids, LongConsumer action, String what) {
        int done = 0;
        for (Long id : ids) {
            try {
                action.accept(id);
                done++;
            } catch (RuntimeException e) {
                log.warn("Nhắc lịch khởi hành ({} id={}) lỗi: {}", what, id, e.getMessage());
            }
        }
        return done;
    }

    void remindCustomer(Long bookingId, LocalDate today) {
        transactionTemplate.executeWithoutResult(status -> {
            // Khóa đơn: khách có thể hủy đúng lúc job chạy
            Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElseThrow();
            if (booking.getStatus() != BookingStatus.PAID) return;
            TourDeparture departure = booking.getDeparture();
            long daysLeft = ChronoUnit.DAYS.between(today, departure.getStartDate());
            short stage = dueStage(daysLeft, booking.getReminderStage());
            if (stage == 0) return;
            booking.setReminderStage(stage);
            // Thanh toán ngay trong đợt nhắc thì email xác nhận vừa gửi đã có đủ thông tin
            LocalDate windowStart = departure.getStartDate().minusDays(stage == STAGE_LAST ? LAST_REMINDER_DAYS : FIRST_REMINDER_DAYS);
            if (booking.getPaidAt() != null && !booking.getPaidAt().toLocalDate().isBefore(windowStart)) return;

            Tour tour = booking.getTour();
            List<String> passengers = booking.getPassengers().stream()
                    .map(p -> p.getFullName() + " (" + PASSENGER_LABELS.get(p.getType()) + ")")
                    .toList();
            eventPublisher.publishEvent(new NotificationEvents.UserEmailEvent(booking.getContactEmail(),
                    EmailTemplates.tripReminder(booking.getCode(), tour.getTitle(), daysLeft, departure.getStartDate(),
                            departure.endDate(tour.getDurationDays()), tour.getMeetingTime().toString(),
                            tour.getMeetingPoint(), passengers, booking.travellers() - passengers.size(),
                            tour.getNotes(), tour.isInternational(),
                            organizerContact(tour), frontendUrl + "/account/bookings/" + booking.getId())));
            eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(booking.getCustomer().getId(),
                    WebNotifications.tripReminder(booking.getId(), tour.getTitle(), daysLeft, tour.getMeetingTime().toString(),
                            tour.getMeetingPoint())));
        });
    }

    void remindOrganizer(Long departureId, LocalDate today) {
        transactionTemplate.executeWithoutResult(status -> {
            TourDeparture departure = departureRepository.findByIdForUpdate(departureId).orElseThrow();
            if (departure.getStatus() == DepartureStatus.CANCELLED) return;
            long daysLeft = ChronoUnit.DAYS.between(today, departure.getStartDate());
            short stage = dueStage(daysLeft, departure.getOrganizerReminderStage());
            if (stage == 0) return;
            List<Booking> bookings = bookingRepository.findWithPassengersByDepartureId(departureId, List.of(BookingStatus.PAID));
            if (bookings.isEmpty()) return;
            departure.setOrganizerReminderStage(stage);

            Tour tour = departure.getTour();
            int adults = 0, children = 0, infants = 0;
            for (Booking b : bookings) {
                adults += b.getAdults();
                children += b.getChildren();
                infants += b.getInfants();
            }
            List<String> lines = bookings.stream().map(TripReminderService::organizerLine).toList();
            String area = tour.isPlatformTour() ? "/admin" : "/agent";
            EmailTemplates.Email email = EmailTemplates.departureReminderForOrganizer(tour.getTitle(), daysLeft,
                    departure.getStartDate(), bookings.size(), adults, children, infants, lines,
                    frontendUrl + area + "/tours/" + tour.getId() + "/departures/" + departure.getId());
            notifyOrganizer(tour, email, WebNotifications.departureReminder(tour.isPlatformTour(), tour.getId(),
                    departure.getId(), tour.getTitle(), departure.getStartDate(), daysLeft, bookings.size(),
                    adults + children + infants));
        });
    }

    /** Agent, hoặc mọi Admin nếu là tour của TripConnect: email + thông báo trên web. */
    private void notifyOrganizer(Tour tour, EmailTemplates.Email email, NotificationEvents.WebMessage message) {
        if (tour.isPlatformTour()) {
            eventPublisher.publishEvent(new NotificationEvents.AdminEmailEvent(email));
            eventPublisher.publishEvent(new NotificationEvents.AdminWebEvent(message));
        } else {
            eventPublisher.publishEvent(new NotificationEvents.UserEmailEvent(tour.getAgent().getEmail(), email));
            eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(tour.getAgent().getId(), message));
        }
    }

    /** "TC... · Nguyễn Văn A · 0900000000 · 2 người lớn, 1 trẻ em: A, B (thiếu thông tin 1 khách)" */
    static String organizerLine(Booking b) {
        StringBuilder line = new StringBuilder(b.getCode()).append(" · ").append(b.getContactName())
                .append(" · ").append(b.getContactPhone()).append(" · ").append(travellersText(b));
        List<String> names = b.getPassengers().stream().map(BookingPassenger::getFullName).toList();
        if (!names.isEmpty()) line.append(": ").append(String.join(", ", names));
        int missing = b.travellers() - names.size();
        if (missing > 0) line.append(names.isEmpty() ? " (chưa có danh sách)" : " (thiếu thông tin " + missing + " khách)");
        return line.toString();
    }

    private static String travellersText(Booking b) {
        List<String> parts = new ArrayList<>();
        parts.add(b.getAdults() + " người lớn");
        if (b.getChildren() > 0) parts.add(b.getChildren() + " trẻ em");
        if (b.getInfants() > 0) parts.add(b.getInfants() + " trẻ sơ sinh");
        return String.join(", ", parts);
    }

    void remindLowBookings(Long departureId, LocalDate today) {
        transactionTemplate.executeWithoutResult(status -> {
            TourDeparture departure = departureRepository.findByIdForUpdate(departureId).orElseThrow();
            if (departure.getStatus() == DepartureStatus.CANCELLED || departure.isLowBookingReminded()) return;
            long daysLeft = ChronoUnit.DAYS.between(today, departure.getStartDate());
            if (daysLeft < LOW_BOOKING_LATEST_DAYS || daysLeft > LOW_BOOKING_DAYS) return;
            // Lịch mới thêm khi đã sát ngày: chưa có khách là đương nhiên, không cần nhắc
            if (!departure.getCreatedAt().toLocalDate().isBefore(departure.getStartDate().minusDays(LOW_BOOKING_DAYS))) {
                departure.setLowBookingReminded(true);
                return;
            }
            // Đủ khách thì chưa đánh dấu: khách hủy bớt trước mốc 4 ngày vẫn còn được nhắc
            long paidSeats = bookingRepository.sumPaidSeats(departureId);
            if (paidSeats * 100 >= (long) departure.getCapacity() * LOW_BOOKING_PERCENT) return;
            departure.setLowBookingReminded(true);

            Tour tour = departure.getTour();
            String area = tour.isPlatformTour() ? "/admin" : "/agent";
            EmailTemplates.Email email = EmailTemplates.departureLowBookings(tour.getTitle(), daysLeft,
                    departure.getStartDate(), paidSeats, departure.getCapacity(), LOW_BOOKING_PERCENT,
                    frontendUrl + area + "/tours/" + tour.getId() + "/departures/" + departure.getId());
            notifyOrganizer(tour, email, WebNotifications.lowBookings(tour.isPlatformTour(), tour.getId(), departure.getId(),
                    tour.getTitle(), departure.getStartDate(), paidSeats, departure.getCapacity()));
        });
    }

    private String organizerContact(Tour tour) {
        if (tour.isPlatformTour()) return "TripConnect";
        User agent = tour.getAgent();
        String name = agentProfileRepository.findByUserId(agent.getId())
                .map(p -> p.getCompanyName()).orElse(agent.getFullName());
        StringBuilder contact = new StringBuilder(name);
        if (agent.getPhone() != null && !agent.getPhone().isBlank()) contact.append(" · ĐT: ").append(agent.getPhone());
        return contact.append(" · Email: ").append(agent.getEmail()).toString();
    }
}
