package com.tripconnect.backend.service;

/**
 * Nội dung các email thông báo nghiệp vụ — gom một chỗ để dễ sửa câu chữ,
 * không rải rác trong các service.
 */
public final class EmailTemplates {

    private EmailTemplates() {
    }

    public record Email(String subject, String body) {
    }

    // ===== Gửi Admin =====

    public static Email agentProfileSubmitted(String companyName, String agentEmail, boolean resubmission) {
        String action = resubmission ? "nộp lại" : "nộp";
        return new Email(
                "[TripConnect] Hồ sơ Agent mới cần duyệt: " + companyName,
                "Agent " + companyName + " (" + agentEmail + ") vừa " + action + " hồ sơ kinh doanh.\n"
                        + "Vui lòng vào trang quản trị để kiểm tra và duyệt hồ sơ.");
    }

    public static Email agentChangeRequestSubmitted(String companyName, String agentEmail) {
        return new Email(
                "[TripConnect] Yêu cầu cập nhật hồ sơ Agent: " + companyName,
                "Agent " + companyName + " (" + agentEmail + ") vừa gửi yêu cầu cập nhật thông tin hồ sơ.\n"
                        + "Vui lòng vào trang quản trị để kiểm tra và duyệt.");
    }

    // ===== Gửi Agent =====

    public static Email agentProfileApproved() {
        return new Email(
                "Hồ sơ Agent đã được duyệt - TripConnect",
                "Chúc mừng! Hồ sơ kinh doanh Agent của bạn đã được duyệt.\n"
                        + "Bạn có thể bắt đầu đăng bán tour và nhận yêu cầu tư vấn ngay bây giờ.");
    }

    public static Email agentProfileNeedsRevision(String reason) {
        return new Email(
                "Hồ sơ Agent cần bổ sung - TripConnect",
                "Hồ sơ kinh doanh Agent của bạn cần bổ sung/chỉnh sửa.\nLý do: " + reason
                        + "\nVui lòng cập nhật lại thông tin và nộp lại để được xem xét.");
    }

    public static Email agentChangeRequestApproved() {
        return new Email(
                "Yêu cầu cập nhật hồ sơ đã được duyệt - TripConnect",
                "Yêu cầu cập nhật hồ sơ kinh doanh của bạn đã được duyệt và áp dụng.");
    }

    public static Email agentChangeRequestRejected(String reason) {
        return new Email(
                "Yêu cầu cập nhật hồ sơ bị từ chối - TripConnect",
                "Yêu cầu cập nhật hồ sơ kinh doanh của bạn không được chấp nhận.\nLý do: " + reason
                        + "\nThông tin hồ sơ hiện tại của bạn vẫn được giữ nguyên.");
    }

    // ===== Tài khoản =====

    public static Email accountDeactivated(String reason) {
        return new Email(
                "Tài khoản TripConnect đã bị vô hiệu hóa",
                "Tài khoản của bạn đã bị quản trị viên vô hiệu hóa.\nLý do: " + reason
                        + "\nNếu bạn cho rằng đây là nhầm lẫn, vui lòng liên hệ bộ phận hỗ trợ.");
    }

    public static Email accountReactivated() {
        return new Email(
                "Tài khoản TripConnect đã được kích hoạt lại",
                "Tài khoản của bạn đã được quản trị viên kích hoạt lại. Bạn có thể đăng nhập bình thường.");
    }
}
