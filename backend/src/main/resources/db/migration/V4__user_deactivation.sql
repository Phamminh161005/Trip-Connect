-- =====================================================================
-- V4: Admin vô hiệu hóa tài khoản người dùng
-- Khác với khóa tạm thời do sai mật khẩu (locked_until): cái này do Admin quyết định, không tự mở.
-- =====================================================================

ALTER TABLE users ADD COLUMN is_active          boolean NOT NULL DEFAULT true;
ALTER TABLE users ADD COLUMN deactivated_reason text;
ALTER TABLE users ADD COLUMN deactivated_at     timestamp(6);
ALTER TABLE users ADD COLUMN updated_at         timestamp(6);

-- Phục vụ trang Admin: lọc theo vai trò / trạng thái, sắp xếp theo ngày tạo
CREATE INDEX idx_users_role_active ON users (role, is_active);
CREATE INDEX idx_users_created_at ON users (created_at);
