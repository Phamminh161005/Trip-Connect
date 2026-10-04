-- =====================================================================
-- V11: Email nhắc lịch khởi hành (trước 3 ngày và trước 1 ngày)
--  - bookings.reminder_stage: lần nhắc khách gần nhất đã gửi (0 = chưa, 1 = trước 3 ngày, 2 = trước 1 ngày)
--  - tour_departures.organizer_reminder_stage: tương tự, cho email gửi đơn vị tổ chức (một email / lịch)
--  Lưu lại để job chạy lại / khởi động lại không gửi trùng.
-- =====================================================================

ALTER TABLE bookings ADD COLUMN reminder_stage smallint NOT NULL DEFAULT 0;
ALTER TABLE tour_departures ADD COLUMN organizer_reminder_stage smallint NOT NULL DEFAULT 0;

CREATE INDEX idx_tour_departures_upcoming ON tour_departures (start_date) WHERE status <> 'CANCELLED';
