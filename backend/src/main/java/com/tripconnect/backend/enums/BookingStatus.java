package com.tripconnect.backend.enums;

/**
 * Vòng đời đơn đặt tour:
 * <pre>
 *  PENDING_PAYMENT --thanh toán VNPay thành công--> PAID --3 ngày sau ngày về--> COMPLETED
 *        \--quá 15 phút / khách hủy--> CANCELLED      \--khách / Agent / Admin hủy--> CANCELLED (+ hoàn tiền)
 * </pre>
 * Tour đoàn không có bước "Đã xác nhận": thanh toán xong là xác nhận.
 */
public enum BookingStatus {
    PENDING_PAYMENT,
    PAID,
    COMPLETED,
    CANCELLED
}
