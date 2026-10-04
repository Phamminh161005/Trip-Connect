package com.tripconnect.backend.service;

import com.tripconnect.backend.enums.NotificationType;
import com.tripconnect.backend.service.NotificationEvents.WebMessage;

import java.time.LocalDate;

/**
 * Nội dung thông báo trên web (ngắn gọn, khác email) — gom một chỗ như {@link EmailTemplates}.
 * Link là đường dẫn trang web; khu vực quản lý tour là /agent hoặc /admin (tour của TripConnect).
 */
public final class WebNotifications {

    private WebNotifications() {
    }

    private static String countdown(long daysLeft) {
        return daysLeft == 1 ? "khởi hành ngày mai" : "còn " + daysLeft + " ngày nữa là khởi hành";
    }

    // ===== Khách =====

    public static WebMessage bookingPaid(Long bookingId, String code, String tourTitle, LocalDate startDate) {
        return new WebMessage(NotificationType.BOOKING_PAID, "Đặt tour thành công - đơn " + code,
                tourTitle + " · khởi hành " + EmailTemplates.day(startDate), "/account/bookings/" + bookingId);
    }

    public static WebMessage bookingCancelled(Long bookingId, String code, String tourTitle, long refundAmount) {
        return new WebMessage(NotificationType.BOOKING_CANCELLED, "Đơn " + code + " đã bị hủy",
                tourTitle + (refundAmount > 0 ? " · hoàn " + EmailTemplates.money(refundAmount) : ""),
                "/account/bookings/" + bookingId);
    }

    public static WebMessage refundCompleted(Long bookingId, String code, long amount) {
        return new WebMessage(NotificationType.REFUND_COMPLETED, "Đã hoàn tiền đơn " + code,
                "TripConnect đã hoàn " + EmailTemplates.money(amount) + ". Thời gian tiền về tùy ngân hàng.",
                "/account/bookings/" + bookingId);
    }

    public static WebMessage tripReminder(Long bookingId, String tourTitle, long daysLeft, String meetingTime,
                                          String meetingPoint) {
        return new WebMessage(NotificationType.TRIP_REMINDER, "Chuyến đi của bạn " + countdown(daysLeft),
                tourTitle + " · tập trung " + meetingTime + " tại " + meetingPoint, "/account/bookings/" + bookingId);
    }

    /** Đơn vừa hoàn thành: mời đánh giá (link mở sẵn khung viết đánh giá). */
    public static WebMessage reviewInvite(Long bookingId, String tourTitle) {
        return new WebMessage(NotificationType.REVIEW_INVITE, "Chuyến đi thế nào? Hãy đánh giá tour",
                tourTitle + " · nhận xét của bạn giúp các khách khác chọn tour", "/account/bookings/" + bookingId + "?review=1");
    }

    public static WebMessage reviewReplied(Long bookingId, String tourTitle) {
        return new WebMessage(NotificationType.REVIEW_REPLIED, "Đơn vị tổ chức đã trả lời đánh giá của bạn", tourTitle,
                "/account/bookings/" + bookingId);
    }

    public static WebMessage reviewHidden(Long bookingId, String tourTitle, String reason) {
        return new WebMessage(NotificationType.REVIEW_HIDDEN, "Đánh giá của bạn đã bị ẩn",
                tourTitle + " · lý do: " + reason, "/account/bookings/" + bookingId);
    }

    public static WebMessage reviewUnhidden(Long bookingId, String tourTitle) {
        return new WebMessage(NotificationType.REVIEW_UNHIDDEN, "Đánh giá của bạn đã được hiện lại",
                tourTitle + " · đánh giá hiển thị lại trên trang tour", "/account/bookings/" + bookingId);
    }

    // ===== Agent =====

    public static WebMessage newBooking(Long bookingId, String code, String tourTitle, LocalDate startDate, int travellers,
                                        long amount) {
        return new WebMessage(NotificationType.NEW_BOOKING, "Đơn đặt mới " + code,
                tourTitle + " · " + EmailTemplates.day(startDate) + " · " + travellers + " khách · "
                        + EmailTemplates.money(amount),
                "/agent/bookings/" + bookingId);
    }

    public static WebMessage tourApproved(Long tourId, String tourTitle) {
        return new WebMessage(NotificationType.TOUR_APPROVED, "Tour đã được duyệt", tourTitle, "/agent/tours/" + tourId);
    }

    public static WebMessage tourNeedsRevision(Long tourId, String tourTitle, String reason) {
        return new WebMessage(NotificationType.TOUR_NEEDS_REVISION, "Tour cần chỉnh sửa: " + tourTitle,
                "Lý do: " + reason, "/agent/tours/" + tourId);
    }

    public static WebMessage tourSuspended(Long tourId, String tourTitle, String reason) {
        return new WebMessage(NotificationType.TOUR_SUSPENDED, "Tour bị đình chỉ: " + tourTitle,
                "Lý do: " + reason, "/agent/tours/" + tourId);
    }

    public static WebMessage tourUnsuspended(Long tourId, String tourTitle) {
        return new WebMessage(NotificationType.TOUR_UNSUSPENDED, "Tour đã được mở lại", tourTitle, "/agent/tours/" + tourId);
    }

    public static WebMessage agentProfileApproved() {
        return new WebMessage(NotificationType.AGENT_PROFILE_APPROVED, "Hồ sơ kinh doanh đã được duyệt",
                "Bạn có thể bắt đầu đăng bán tour.", "/agent/profile");
    }

    public static WebMessage agentProfileNeedsRevision(String reason) {
        return new WebMessage(NotificationType.AGENT_PROFILE_NEEDS_REVISION, "Hồ sơ kinh doanh cần bổ sung",
                "Lý do: " + reason, "/agent/profile");
    }

    public static WebMessage changeRequestApproved() {
        return new WebMessage(NotificationType.CHANGE_REQUEST_APPROVED, "Yêu cầu cập nhật hồ sơ đã được duyệt",
                "Thông tin mới đã được áp dụng.", "/agent/profile#change-requests");
    }

    public static WebMessage changeRequestRejected(String reason) {
        return new WebMessage(NotificationType.CHANGE_REQUEST_REJECTED, "Yêu cầu cập nhật hồ sơ bị từ chối",
                "Lý do: " + reason, "/agent/profile#change-requests");
    }

    // ===== Đơn vị tổ chức (Agent / Admin) =====

    private static String departureLink(boolean platformTour, Long tourId, Long departureId) {
        return (platformTour ? "/admin" : "/agent") + "/tours/" + tourId + "/departures/" + departureId;
    }

    public static WebMessage newReview(boolean platformTour, String tourTitle, int rating) {
        return new WebMessage(NotificationType.NEW_REVIEW, "Đánh giá mới " + rating + "★", tourTitle,
                (platformTour ? "/admin" : "/agent") + "/reviews");
    }

    /** Báo Agent: đánh giá tour của mình bị Admin ẩn (điểm trung bình thay đổi). */
    public static WebMessage tourReviewHidden(String tourTitle, int rating, String reason) {
        return new WebMessage(NotificationType.TOUR_REVIEW_HIDDEN, "Một đánh giá " + rating + "★ đã bị ẩn",
                tourTitle + " · lý do: " + reason, "/agent/reviews");
    }

    public static WebMessage tourReviewUnhidden(String tourTitle, int rating) {
        return new WebMessage(NotificationType.TOUR_REVIEW_UNHIDDEN, "Một đánh giá " + rating + "★ đã được hiện lại",
                tourTitle + " · đánh giá hiển thị lại trên trang tour", "/agent/reviews");
    }

    public static WebMessage departureReminder(boolean platformTour, Long tourId, Long departureId, String tourTitle,
                                               LocalDate startDate, long daysLeft, int bookings, int travellers) {
        return new WebMessage(NotificationType.DEPARTURE_REMINDER,
                "Lịch " + EmailTemplates.day(startDate) + " " + countdown(daysLeft),
                tourTitle + " · " + bookings + " đơn · " + travellers + " khách",
                departureLink(platformTour, tourId, departureId));
    }

    public static WebMessage lowBookings(boolean platformTour, Long tourId, Long departureId, String tourTitle,
                                         LocalDate startDate, long paidSeats, int capacity) {
        return new WebMessage(NotificationType.LOW_BOOKINGS, "Lịch " + EmailTemplates.day(startDate) + " còn ít khách",
                tourTitle + " · mới bán " + paidSeats + "/" + capacity + " chỗ",
                departureLink(platformTour, tourId, departureId));
    }

    // ===== Admin =====

    public static WebMessage agentProfileSubmitted(Long profileId, String companyName, boolean resubmission) {
        return new WebMessage(NotificationType.AGENT_PROFILE_SUBMITTED,
                (resubmission ? "Hồ sơ Agent nộp lại: " : "Hồ sơ Agent mới: ") + companyName,
                "Chờ duyệt", "/admin/agents/" + profileId);
    }

    public static WebMessage changeRequestSubmitted(Long requestId, String companyName) {
        return new WebMessage(NotificationType.CHANGE_REQUEST_SUBMITTED, "Yêu cầu cập nhật hồ sơ: " + companyName,
                "Chờ duyệt", "/admin/change-requests/" + requestId);
    }

    public static WebMessage tourSubmitted(Long tourId, String tourTitle, String companyName, boolean updateOfPublishedTour) {
        return new WebMessage(NotificationType.TOUR_SUBMITTED,
                (updateOfPublishedTour ? "Tour sửa nội dung cần duyệt lại: " : "Tour mới cần duyệt: ") + tourTitle,
                "Đối tác " + companyName, "/admin/tours/" + tourId);
    }

    public static WebMessage manualRefundNeeded(Long bookingId, String code, long amount) {
        return new WebMessage(NotificationType.MANUAL_REFUND_NEEDED, "Cần hoàn tiền thủ công đơn " + code,
                "VNPay hoàn tự động không thành công · " + EmailTemplates.money(amount), "/admin/bookings/" + bookingId);
    }
}
