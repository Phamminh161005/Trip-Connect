-- V6: Agent ghi chú lý do thay đổi khi gửi yêu cầu cập nhật hồ sơ (vd "Công ty chuyển trụ sở từ 01/10/2026")
-- giúp Admin hiểu bối cảnh và duyệt nhanh hơn. Không bắt buộc.
ALTER TABLE agent_profile_change_requests ADD COLUMN note text;
