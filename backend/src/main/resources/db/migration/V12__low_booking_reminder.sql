-- =====================================================================
-- V12: Nhắc đơn vị tổ chức khi lịch còn 7 ngày khởi hành mà đã bán dưới 30% số chỗ (gửi một lần)
-- =====================================================================

ALTER TABLE tour_departures ADD COLUMN low_booking_reminded boolean NOT NULL DEFAULT false;
