package com.tripconnect.backend.dto.booking;

import com.tripconnect.backend.enums.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Các DTO trả về của module đặt tour. */
public final class BookingResponses {

    private BookingResponses() {
    }

    /** Một dòng trong danh sách đơn (khách / Agent / Admin). */
    public record Summary(
            Long id,
            String code,
            BookingStatus status,
            RefundStatus refundStatus,
            Long tourId,
            String tourTitle,
            String coverImageUrl,
            LocalDate startDate,
            LocalDate endDate,
            int adults,
            int children,
            int infants,
            long totalAmount,
            long refundAmount,
            String customerName,
            String customerEmail,
            LocalDateTime holdExpiresAt,
            LocalDateTime createdAt
    ) {
    }

    public record Passenger(Long id, String fullName, LocalDate dateOfBirth, PassengerType type, String passportNumber) {
    }

    public record PaymentView(Long id, String txnRef, long amount, PaymentStatus status, String bankCode,
                              String transactionNo, String responseCode, LocalDateTime createdAt) {
    }

    public record RefundView(Long id, long amount, RefundRecordStatus status, String reason, String message,
                             LocalDateTime processedAt, LocalDateTime createdAt) {
    }

    /** Chính sách hoàn tiền áp dụng cho đơn (đã chụp lại lúc đặt). */
    public record RefundPolicy(int fullRefundDays, int partialRefundDays, int partialRefundPercent) {
    }

    public record Detail(
            Long id,
            String code,
            BookingStatus status,
            Long tourId,
            String tourTitle,
            String coverImageUrl,
            boolean international,
            String providerName,
            Long departureId,
            LocalDate startDate,
            LocalDate endDate,
            String meetingPoint,
            String meetingTime,
            int adults,
            int children,
            int infants,
            long adultPrice,
            long childPrice,
            long totalAmount,
            /* Chỉ hiện với Admin / Agent (phần TripConnect giữ lại) */
            BigDecimal commissionRate,
            Long commissionAmount,
            String contactName,
            String contactPhone,
            String contactEmail,
            String note,
            List<Passenger> passengers,
            /* Khách sửa được danh sách hành khách tới hết ngày này */
            LocalDate passengerListDeadline,
            boolean canEditPassengers,
            RefundPolicy refundPolicy,
            LocalDateTime holdExpiresAt,
            LocalDateTime paidAt,
            LocalDateTime completedAt,
            LocalDateTime cancelledAt,
            CancelledBy cancelledBy,
            String cancelReason,
            long refundAmount,
            RefundStatus refundStatus,
            List<PaymentView> payments,
            List<RefundView> refunds,
            /* Khách: được thanh toán tiếp / được hủy không */
            boolean canPay,
            boolean canCancel,
            LocalDateTime createdAt
    ) {
    }

    /** Kết quả đặt tour: đơn vừa tạo + link chuyển sang VNPay. */
    public record Created(Detail booking, String paymentUrl) {
    }

    /** Xem trước số tiền được hoàn nếu hủy ngay bây giờ. */
    public record CancellationQuote(boolean cancellable, long paidAmount, int refundPercent, long refundAmount,
                                    long daysBeforeDeparture, String explanation) {
    }

    /** Kết quả trang quay về sau khi thanh toán VNPay. */
    public record PaymentResult(boolean success, Long bookingId, String bookingCode, BookingStatus bookingStatus,
                                String message) {
    }

    /** Danh sách đoàn của một lịch khởi hành (Agent chuẩn bị xe, khách sạn, bảo hiểm). */
    public record Manifest(Long departureId, String tourTitle, LocalDate startDate, LocalDate endDate, int capacity,
                           int seatsBooked, List<ManifestBooking> bookings) {
    }

    public record ManifestBooking(Long bookingId, String code, BookingStatus status, String contactName,
                                  String contactPhone, String contactEmail, String note,
                                  int adults, int children, int infants, List<Passenger> passengers) {
    }
}
