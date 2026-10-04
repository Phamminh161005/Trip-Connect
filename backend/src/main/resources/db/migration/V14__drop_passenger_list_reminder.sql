-- =====================================================================
-- V14: Danh sách hành khách bắt buộc nhập đủ ngay lúc đặt tour -> không còn email nhắc bổ sung (bỏ cột của V13)
-- =====================================================================

ALTER TABLE bookings DROP COLUMN passenger_list_reminded;
