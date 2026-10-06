package com.tripconnect.backend.enums;

/**
 * Vòng đời đơn đặt tour:
 * <pre>
 *  PENDING_PAYMENT --thanh toán VNPay thành công--> PAID --3 ngày sau ngày về--> COMPLETED
 *        \--quá 15 phút / khách hủy--> CANCELLED      \--khách / Agent / Admin hủy--> CANCELLED (+ hoàn tiền)
 *  Tour riêng trả 2 lần: PENDING_PAYMENT --đặt cọc--> DEPOSIT_PAID --trả phần còn lại--> PAID
 *        (quá 48 giờ chưa cọc / quá hạn trả phần còn lại -> CANCELLED, tiền cọc không hoàn)
 * </pre>
 * Tour đoàn không có bước "Đã xác nhận": thanh toán xong là xác nhận.
 */
public enum BookingStatus {
    PENDING_PAYMENT,
    /** Tour riêng: đã đặt cọc, chờ trả phần còn lại. */
    DEPOSIT_PAID,
    PAID,
    COMPLETED,
    CANCELLED
}
