-- =====================================================================
-- V20: Tour riêng từ yêu cầu thiết kế tour + thanh toán đặt cọc
--  - Khách đồng ý đề xuất -> tạo tour PRIVATE (không hiện ở tìm kiếm, không sửa được) + 1 lịch + 1 đơn cho khách.
--  - Đơn tour riêng trả 2 lần: cọc (deposit_amount, trong 48 giờ) rồi phần còn lại trước balance_due_date
--    (trước ngày đi 7 ngày trong nước / 15 ngày quốc tế; khách được tự gia hạn 1 lần thêm 3 ngày).
--    deposit_amount = 0: trả một lần như tour thường (kể cả tour riêng sát ngày đi).
--  - payments.purpose: giao dịch trả cho phần nào.
-- =====================================================================

ALTER TABLE tours DROP CONSTRAINT tours_status_check;
ALTER TABLE tours ADD CONSTRAINT tours_status_check CHECK (status IN
    ('DRAFT', 'PENDING_APPROVAL', 'NEEDS_REVISION', 'PUBLISHED', 'HIDDEN', 'SUSPENDED', 'PRIVATE'));
ALTER TABLE tours ADD COLUMN custom_request_id bigint REFERENCES custom_requests (id);
CREATE UNIQUE INDEX uq_tours_custom_request ON tours (custom_request_id) WHERE custom_request_id IS NOT NULL;

ALTER TABLE bookings DROP CONSTRAINT bookings_status_check;
ALTER TABLE bookings ADD CONSTRAINT bookings_status_check CHECK (status IN
    ('PENDING_PAYMENT', 'DEPOSIT_PAID', 'PAID', 'COMPLETED', 'CANCELLED'));
ALTER TABLE bookings
    ADD COLUMN deposit_amount         bigint   NOT NULL DEFAULT 0,
    ADD COLUMN deposit_paid_at        timestamp(6),
    ADD COLUMN balance_due_date       date,
    ADD COLUMN balance_extended       boolean  NOT NULL DEFAULT false,
    -- Nhắc thanh toán đã gửi: 0 chưa, 1 nhắc đặt cọc, 2 nhắc trả nốt trước 3 ngày, 3 trước 1 ngày
    ADD COLUMN payment_reminder_stage smallint NOT NULL DEFAULT 0,
    ADD CONSTRAINT bookings_deposit_check CHECK (deposit_amount >= 0 AND deposit_amount <= total_amount);

-- Job: quá hạn / sắp tới hạn trả phần còn lại
CREATE INDEX idx_bookings_balance_due ON bookings (balance_due_date) WHERE status = 'DEPOSIT_PAID';

ALTER TABLE payments ADD COLUMN purpose varchar(20) NOT NULL DEFAULT 'FULL';
ALTER TABLE payments ADD CONSTRAINT payments_purpose_check CHECK (purpose IN ('FULL', 'DEPOSIT', 'BALANCE'));
