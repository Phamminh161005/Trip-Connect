-- =====================================================================
-- V13: Danh sách hành khách được bổ sung sau khi đặt (hạn chót: trước ngày đi 3 ngày).
--  Đơn tour quốc tế chưa nhập đủ được nhắc một lần qua email trước hạn.
-- =====================================================================

ALTER TABLE bookings ADD COLUMN passenger_list_reminded boolean NOT NULL DEFAULT false;
