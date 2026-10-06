package com.tripconnect.backend.controller;

import com.tripconnect.backend.dto.PageResponse;
import com.tripconnect.backend.dto.booking.BookingResponses;
import com.tripconnect.backend.dto.booking.CancelBookingRequest;
import com.tripconnect.backend.dto.booking.CreateBookingRequest;
import com.tripconnect.backend.dto.booking.UpdatePassengersRequest;
import com.tripconnect.backend.enums.BookingStatus;
import com.tripconnect.backend.security.AuthenticatedUser;
import com.tripconnect.backend.security.RateLimiter;
import com.tripconnect.backend.service.booking.BookingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

/** Đơn đặt tour của người đang đăng nhập (khách hàng, Agent). */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private static final Duration TEN_MINUTES = Duration.ofMinutes(10);

    private final BookingService bookingService;
    private final RateLimiter rateLimiter;

    /** Đặt tour: giữ chỗ 15 phút + trả link VNPay để chuyển khách sang thanh toán. */
    @PostMapping
    public ResponseEntity<BookingResponses.Created> create(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                           @Valid @RequestBody CreateBookingRequest request,
                                                           HttpServletRequest http) {
        rateLimiter.check("booking-create", String.valueOf(currentUser.userId()), 10, TEN_MINUTES);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.create(currentUser.userId(), currentUser.role(), request, http.getRemoteAddr()));
    }

    @GetMapping
    public PageResponse<BookingResponses.Summary> listMine(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) BookingStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return bookingService.listMine(currentUser.userId(), status, pageable);
    }

    @GetMapping("/{id}")
    public BookingResponses.Detail get(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        return bookingService.getMine(currentUser.userId(), id);
    }

    /** Thanh toán tiếp (tạo link VNPay mới) khi đơn còn hạn giữ chỗ. */
    @PostMapping("/{id}/pay")
    public Map<String, String> pay(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id,
                                   HttpServletRequest http) {
        rateLimiter.check("booking-pay", String.valueOf(currentUser.userId()), 20, TEN_MINUTES);
        return Map.of("paymentUrl", bookingService.pay(currentUser.userId(), id, http.getRemoteAddr()));
    }

    /** Nhập / sửa danh sách hành khách (tới hạn chót trước ngày đi). */
    @PutMapping("/{id}/passengers")
    public BookingResponses.Detail updatePassengers(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                    @PathVariable Long id,
                                                    @Valid @RequestBody UpdatePassengersRequest request) {
        return bookingService.updatePassengers(currentUser.userId(), id, request.getPassengers());
    }

    /** Tour riêng: gia hạn trả phần còn lại thêm 3 ngày (một lần). */
    @PostMapping("/{id}/extend-balance")
    public BookingResponses.Detail extendBalance(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        return bookingService.extendBalance(currentUser.userId(), id);
    }

    /** Xem trước số tiền được hoàn nếu hủy ngay bây giờ. */
    @GetMapping("/{id}/cancellation-quote")
    public BookingResponses.CancellationQuote quote(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                    @PathVariable Long id) {
        return bookingService.quote(currentUser.userId(), id);
    }

    @PostMapping("/{id}/cancel")
    public BookingResponses.Detail cancel(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id,
                                          @Valid @RequestBody(required = false) CancelBookingRequest request) {
        return bookingService.cancel(currentUser.userId(), id, request == null ? null : request.getReason());
    }
}
