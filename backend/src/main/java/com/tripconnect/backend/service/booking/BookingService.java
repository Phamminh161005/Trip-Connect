package com.tripconnect.backend.service.booking;

import com.tripconnect.backend.dto.PageResponse;
import com.tripconnect.backend.dto.booking.BookingResponses;
import com.tripconnect.backend.dto.booking.CreateBookingRequest;
import com.tripconnect.backend.dto.booking.PassengerRequest;
import com.tripconnect.backend.entity.*;
import com.tripconnect.backend.enums.*;
import com.tripconnect.backend.exception.ForbiddenException;
import com.tripconnect.backend.exception.ResourceNotFoundException;
import com.tripconnect.backend.repository.*;
import com.tripconnect.backend.service.tour.TourBookingStats;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Khách hàng (và Agent — kế thừa quyền khách) đặt tour, thanh toán, xem và hủy đơn của mình. */
@Service
@RequiredArgsConstructor
public class BookingService {

    /** Một khách giữ tối đa ngần này đơn chờ thanh toán cùng lúc (chống giữ chỗ ảo). */
    static final int MAX_PENDING_PER_CUSTOMER = 3;
    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyMMdd");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final BookingRepository bookingRepository;
    private final TourDepartureRepository departureRepository;
    private final UserRepository userRepository;
    private final AgentProfileRepository agentProfileRepository;
    private final TourBookingStats bookingStats;
    private final PaymentService paymentService;
    private final BookingCancellation cancellation;
    private final BookingAssembler assembler;
    private final BookingSettings settings;
    private final Clock clock;

    // ===================== Đặt tour =====================

    @Transactional
    public BookingResponses.Created create(Long userId, String role, CreateBookingRequest request, String ipAddr) {
        if (UserRole.ADMIN.name().equals(role)) {
            throw new ForbiddenException("Tài khoản quản trị không đặt tour");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();

        // Khóa lịch khởi hành: các lượt đặt cùng lịch xếp hàng -> đếm chỗ luôn đúng, không bán vượt
        TourDeparture departure = departureRepository.findByIdForUpdate(request.getDepartureId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch khởi hành"));
        Tour tour = departure.getTour();
        requireBookable(tour, departure, today);
        if (!tour.isPlatformTour() && tour.getAgent().getId().equals(userId)) {
            throw new IllegalArgumentException("Bạn không thể đặt tour do chính mình tổ chức");
        }

        bookingRepository.findFirstByCustomerIdAndDepartureIdAndStatusAndHoldExpiresAtAfter(
                userId, departure.getId(), BookingStatus.PENDING_PAYMENT, now).ifPresent(existing -> {
            throw new IllegalStateException("Bạn đang có đơn " + existing.getCode()
                    + " chờ thanh toán cho lịch này. Hãy thanh toán hoặc hủy đơn đó trước");
        });
        if (bookingRepository.countByCustomerIdAndStatusAndHoldExpiresAtAfter(userId, BookingStatus.PENDING_PAYMENT, now)
                >= MAX_PENDING_PER_CUSTOMER) {
            throw new IllegalStateException("Bạn đang có quá nhiều đơn chờ thanh toán. Hãy hoàn tất hoặc hủy bớt");
        }

        Booking booking = new Booking();
        setTravellers(booking, request.getAdults(), request.getChildren(), request.getInfants());
        replacePassengers(booking, request.getPassengers(), departure.getStartDate(), tour.isInternational());
        int free = departure.getCapacity() - bookingStats.seatsBooked(departure.getId());
        if (booking.seats() > free) {
            throw new IllegalStateException(free <= 0 ? "Lịch khởi hành này đã hết chỗ"
                    : "Lịch khởi hành chỉ còn " + free + " chỗ (trẻ sơ sinh dưới 2 tuổi không tính chỗ)");
        }

        booking.setCode(newCode(today));
        booking.setCustomer(userRepository.getReferenceById(userId));
        booking.setTour(tour);
        booking.setDeparture(departure);
        booking.setAgent(tour.getAgent());
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setAdultPrice(departure.getAdultPrice());
        booking.setChildPrice(departure.getChildPrice());
        long total = booking.getAdults() * departure.getAdultPrice() + booking.getChildren() * departure.getChildPrice();
        booking.setTotalAmount(total);
        BigDecimal rate = tour.isPlatformTour() ? BigDecimal.ZERO : settings.getCommissionRate();
        booking.setCommissionRate(rate);
        booking.setCommissionAmount(BigDecimal.valueOf(total).multiply(rate).setScale(0, RoundingMode.HALF_UP).longValue());
        booking.setContactName(request.getContactName().trim());
        booking.setContactPhone(request.getContactPhone().trim());
        booking.setContactEmail(request.getContactEmail().trim().toLowerCase(Locale.ROOT));
        booking.setNote(request.getNote() == null || request.getNote().isBlank() ? null : request.getNote().trim());
        booking.setRefundFullDays((short) settings.getRefundFullDays());
        booking.setRefundPartialDays((short) settings.getRefundPartialDays());
        booking.setRefundPartialPercent((short) settings.getRefundPartialPercent());
        booking.setHoldExpiresAt(now.plusMinutes(settings.getHoldMinutes()));
        bookingRepository.save(booking);

        String paymentUrl = paymentService.createPaymentUrl(booking, ipAddr);
        return new BookingResponses.Created(assembler.toDetail(booking, BookingAssembler.Viewer.CUSTOMER), paymentUrl);
    }

    /** Tạo lại link thanh toán (khách hủy ở VNPay / đóng tab) — chỉ khi còn hạn giữ chỗ. */
    @Transactional
    public String pay(Long userId, Long bookingId, String ipAddr) {
        Booking booking = requireMineForUpdate(userId, bookingId);
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Đơn không ở trạng thái chờ thanh toán");
        }
        if (!booking.getHoldExpiresAt().isAfter(LocalDateTime.now(clock))) {
            throw new IllegalStateException("Đơn đã hết thời gian giữ chỗ, vui lòng đặt lại");
        }
        return paymentService.createPaymentUrl(booking, ipAddr);
    }

    /** Khách sửa danh sách hành khách (gõ sai tên, ngày sinh...) — tới hết hạn chót trước ngày đi. */
    @Transactional
    public BookingResponses.Detail updatePassengers(Long userId, Long bookingId, List<PassengerRequest> passengers) {
        Booking booking = requireMineForUpdate(userId, bookingId);
        LocalDateTime now = LocalDateTime.now(clock);
        boolean pendingAlive = booking.getStatus() == BookingStatus.PENDING_PAYMENT && booking.getHoldExpiresAt().isAfter(now);
        if (booking.getStatus() != BookingStatus.PAID && !pendingAlive) {
            throw new IllegalStateException("Đơn không ở trạng thái được cập nhật danh sách hành khách");
        }
        LocalDate startDate = booking.getDeparture().getStartDate();
        if (!BookingRules.passengerListOpen(startDate, now.toLocalDate())) {
            throw new IllegalStateException("Đã quá hạn cập nhật danh sách hành khách (hết ngày "
                    + BookingRules.passengerListDeadline(startDate).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    + "). Vui lòng liên hệ đơn vị tổ chức");
        }
        replacePassengers(booking, passengers, startDate, booking.getTour().isInternational());
        return assembler.toDetail(booking, BookingAssembler.Viewer.CUSTOMER);
    }

    // ===================== Xem đơn =====================

    @Transactional(readOnly = true)
    public PageResponse<BookingResponses.Summary> listMine(Long userId, BookingStatus status, Pageable pageable) {
        Specification<Booking> spec = (root, query, cb) -> cb.equal(root.get("customer").get("id"), userId);
        if (status != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        var page = bookingRepository.findAll(spec, StablePaging.of(pageable));
        return PageResponse.of(page, assembler.toSummaries(page));
    }

    @Transactional(readOnly = true)
    public BookingResponses.Detail getMine(Long userId, Long bookingId) {
        return assembler.toDetail(requireMine(userId, bookingId), BookingAssembler.Viewer.CUSTOMER);
    }

    // ===================== Hủy đơn =====================

    @Transactional(readOnly = true)
    public BookingResponses.CancellationQuote quote(Long userId, Long bookingId) {
        return quote(requireMine(userId, bookingId));
    }

    /** Khách tự hủy: chưa trả tiền -> hủy luôn; đã trả -> hoàn theo chính sách chụp lại trong đơn. */
    @Transactional
    public BookingResponses.Detail cancel(Long userId, Long bookingId, String reason) {
        Booking booking = requireMineForUpdate(userId, bookingId);
        BookingResponses.CancellationQuote quote = quote(booking);
        if (!quote.cancellable()) throw new IllegalStateException(quote.explanation());
        String text = reason == null || reason.isBlank() ? "Khách hủy đơn" : "Khách hủy đơn: " + reason.trim();
        cancellation.cancel(booking, CancelledBy.CUSTOMER, text, quote.refundAmount(), true);
        return assembler.toDetail(booking, BookingAssembler.Viewer.CUSTOMER);
    }

    private BookingResponses.CancellationQuote quote(Booking booking) {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = booking.getDeparture().getStartDate();
        return switch (booking.getStatus()) {
            case PENDING_PAYMENT -> new BookingResponses.CancellationQuote(true, 0, 0, 0,
                    java.time.temporal.ChronoUnit.DAYS.between(today, start), "Đơn chưa thanh toán nên hủy không mất phí.");
            case PAID -> {
                if (!start.isAfter(today)) {
                    yield new BookingResponses.CancellationQuote(false, booking.getTotalAmount(), 0, 0, 0,
                            "Tour đã khởi hành nên không thể hủy trên hệ thống. Vui lòng liên hệ đơn vị tổ chức.");
                }
                BookingRules.RefundQuote q = BookingRules.customerRefund(booking, start, today);
                String explanation = q.percent() == 100
                        ? "Hủy trước ngày đi " + q.daysBeforeDeparture() + " ngày: được hoàn 100%."
                        : q.percent() > 0
                        ? "Hủy trước ngày đi " + q.daysBeforeDeparture() + " ngày: được hoàn " + q.percent() + "%."
                        : "Hủy sát ngày đi (còn " + q.daysBeforeDeparture() + " ngày): không được hoàn tiền theo chính sách.";
                yield new BookingResponses.CancellationQuote(true, booking.getTotalAmount(), q.percent(), q.amount(),
                        q.daysBeforeDeparture(), explanation);
            }
            default -> new BookingResponses.CancellationQuote(false, 0, 0, 0, 0, "Đơn không ở trạng thái có thể hủy.");
        };
    }

    // ===================== Tiện ích =====================

    /** Tour đang bán, đơn vị tổ chức còn hoạt động, lịch đang mở bán và chưa khởi hành. */
    private void requireBookable(Tour tour, TourDeparture departure, LocalDate today) {
        boolean agentOk = tour.isPlatformTour() || (tour.getAgent().isActive()
                && agentProfileRepository.findByUserId(tour.getAgent().getId())
                .map(p -> p.getStatus() == AgentStatus.APPROVED).orElse(false));
        if (tour.getStatus() != TourStatus.PUBLISHED || !agentOk) {
            throw new IllegalStateException("Tour hiện không nhận đặt chỗ");
        }
        if (departure.getStatus() != DepartureStatus.OPEN || !departure.getStartDate().isAfter(today)) {
            throw new IllegalStateException("Lịch khởi hành này không còn nhận đặt chỗ");
        }
    }

    private void setTravellers(Booking booking, int adults, int children, int infants) {
        if (adults < 1) throw new IllegalArgumentException("Đơn cần ít nhất 1 người lớn (từ 12 tuổi)");
        if (infants > adults) throw new IllegalArgumentException("Mỗi trẻ sơ sinh dưới 2 tuổi cần đi cùng một người lớn");
        if (adults + children + infants > settings.getMaxTravellers()) {
            throw new IllegalArgumentException("Mỗi đơn tối đa " + settings.getMaxTravellers()
                    + " khách. Đoàn đông hơn vui lòng gửi yêu cầu thiết kế tour riêng");
        }
        booking.setAdults((short) adults);
        booking.setChildren((short) children);
        booking.setInfants((short) infants);
    }

    /**
     * Thay danh sách hành khách: phải đủ số khách, và số người từng loại (tính theo tuổi vào ngày đi)
     * đúng bằng số đã đặt — giá đã chốt theo số này.
     */
    private void replacePassengers(Booking booking, List<PassengerRequest> requests, LocalDate startDate, boolean international) {
        if (requests.size() != booking.travellers()) {
            throw new IllegalArgumentException("Vui lòng nhập đủ thông tin " + booking.travellers() + " hành khách");
        }
        Map<PassengerType, Integer> counts = new EnumMap<>(PassengerType.class);
        List<BookingPassenger> passengers = new ArrayList<>();
        for (PassengerRequest p : requests) {
            String name = p.getFullName().trim();
            if (!p.getDateOfBirth().isBefore(startDate)) {
                throw new IllegalArgumentException("Ngày sinh của " + name + " không hợp lệ");
            }
            String passport = p.getPassportNumber() == null || p.getPassportNumber().isBlank()
                    ? null : p.getPassportNumber().trim().toUpperCase(Locale.ROOT);
            if (international && passport == null) {
                throw new IllegalArgumentException("Tour quốc tế cần số hộ chiếu của " + name);
            }
            PassengerType type = BookingRules.classify(p.getDateOfBirth(), startDate);
            counts.merge(type, 1, Integer::sum);
            BookingPassenger passenger = new BookingPassenger();
            passenger.setBooking(booking);
            passenger.setFullName(name);
            passenger.setDateOfBirth(p.getDateOfBirth());
            passenger.setType(type);
            passenger.setPassportNumber(passport);
            passengers.add(passenger);
        }
        requireWithin(counts, PassengerType.ADULT, booking.getAdults(), "người lớn (từ 12 tuổi)");
        requireWithin(counts, PassengerType.CHILD, booking.getChildren(), "trẻ em (2-11 tuổi)");
        requireWithin(counts, PassengerType.INFANT, booking.getInfants(), "trẻ sơ sinh (dưới 2 tuổi)");
        booking.getPassengers().clear();
        booking.getPassengers().addAll(passengers);
    }

    private static void requireWithin(Map<PassengerType, Integer> counts, PassengerType type, int booked, String label) {
        int listed = counts.getOrDefault(type, 0);
        if (listed > booked) {
            throw new IllegalArgumentException("Danh sách có " + listed + " " + label + " nhưng đơn đặt " + booked
                    + ". Loại khách tính theo tuổi vào ngày khởi hành — vui lòng kiểm tra lại ngày sinh");
        }
    }

    /** "TC" + ngày đặt + 6 số ngẫu nhiên — khó đoán, không lộ số lượng đơn. */
    private String newCode(LocalDate today) {
        for (int i = 0; i < 10; i++) {
            String code = "TC" + today.format(CODE_DATE) + String.format("%06d", RANDOM.nextInt(1_000_000));
            if (!bookingRepository.existsByCode(code)) return code;
        }
        throw new IllegalStateException("Không tạo được mã đơn, vui lòng thử lại");
    }

    private Booking requireMine(Long userId, Long bookingId) {
        return bookingRepository.findById(bookingId)
                .filter(b -> b.getCustomer().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt tour"));
    }

    private Booking requireMineForUpdate(Long userId, Long bookingId) {
        return bookingRepository.findByIdForUpdate(bookingId)
                .filter(b -> b.getCustomer().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt tour"));
    }
}
