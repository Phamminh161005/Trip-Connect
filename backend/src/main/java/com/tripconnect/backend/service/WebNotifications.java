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

    // ===== Yêu cầu thiết kế tour riêng =====

    public static WebMessage customRequestAccepted(Long requestId, String code, String companyName, int proposalHours) {
        return new WebMessage(NotificationType.CUSTOM_REQUEST_ACCEPTED, "Yêu cầu " + code + " đã có đơn vị nhận",
                companyName + " sẽ gửi đề xuất lịch trình trong " + proposalHours + " giờ", "/account/requests/" + requestId);
    }

    public static WebMessage customRequestClosed(Long requestId, String code, String reason) {
        return new WebMessage(NotificationType.CUSTOM_REQUEST_CLOSED, "Yêu cầu " + code + " đã đóng", "Lý do: " + reason,
                "/account/requests/" + requestId);
    }

    public static WebMessage customRequestAssigned(Long requestId, String code, String destinations, int acceptHours) {
        return new WebMessage(NotificationType.CUSTOM_REQUEST_ASSIGNED, "Yêu cầu thiết kế tour mới " + code,
                destinations + " · nhận hoặc từ chối trong " + acceptHours + " giờ", "/agent/requests/" + requestId);
    }

    public static WebMessage customRequestExpired(Long requestId, String code) {
        return new WebMessage(NotificationType.CUSTOM_REQUEST_EXPIRED, "Yêu cầu " + code + " đã hết hạn nhận",
                "Bạn không phản hồi kịp nên yêu cầu được chuyển cho đơn vị khác", "/agent/requests/" + requestId);
    }

    public static WebMessage customRequestCancelled(Long requestId, String code, String reason) {
        return new WebMessage(NotificationType.CUSTOM_REQUEST_CANCELLED, "Yêu cầu " + code + " đã hủy / đóng", reason,
                "/agent/requests/" + requestId);
    }

    /** Khách: đơn vị cũ quá hạn gửi đề xuất, đang tìm đơn vị khác. */
    public static WebMessage customRequestReassigning(Long requestId, String code, String companyName) {
        return new WebMessage(NotificationType.CUSTOM_REQUEST_REASSIGNING, "Yêu cầu " + code + " đang được chuyển đơn vị khác",
                companyName + " chưa gửi được đề xuất đúng hạn. TripConnect đang tìm đơn vị phù hợp khác cho bạn",
                "/account/requests/" + requestId);
    }

    public static WebMessage customProposalReceived(Long requestId, String code, String companyName, int versionNo,
                                                    long totalPrice, int responseDays) {
        return new WebMessage(NotificationType.CUSTOM_PROPOSAL_RECEIVED,
                (versionNo == 1 ? "Bạn có đề xuất tour cho yêu cầu " : "Đề xuất đã chỉnh sửa (bản " + versionNo + ") cho yêu cầu ") + code,
                companyName + " · tổng " + EmailTemplates.money(totalPrice) + " · phản hồi trong " + responseDays + " ngày",
                "/account/requests/" + requestId);
    }

    public static WebMessage customProposalExpiring(Long requestId, String code) {
        return new WebMessage(NotificationType.CUSTOM_PROPOSAL_EXPIRING, "Đề xuất cho yêu cầu " + code + " sắp hết hạn",
                "Còn khoảng 1 ngày để đồng ý hoặc yêu cầu chỉnh sửa", "/account/requests/" + requestId);
    }

    public static WebMessage customProposalExpiredForCustomer(Long requestId, String code, boolean canRevise, int inactiveDays) {
        return new WebMessage(NotificationType.CUSTOM_PROPOSAL_EXPIRED, "Đề xuất cho yêu cầu " + code + " đã hết hạn",
                (canRevise ? "Bạn vẫn có thể yêu cầu đơn vị chỉnh sửa và gửi lại. " : "")
                        + "Yêu cầu sẽ tự đóng nếu không có trao đổi thêm trong " + inactiveDays + " ngày kể từ lần gần nhất",
                "/account/requests/" + requestId);
    }

    public static WebMessage customProposalExpiredForAgent(Long requestId, String code) {
        return new WebMessage(NotificationType.CUSTOM_PROPOSAL_EXPIRED, "Đề xuất cho yêu cầu " + code + " đã hết hạn",
                "Khách chưa phản hồi trong thời hạn", "/agent/requests/" + requestId);
    }

    public static WebMessage customProposalRevisionRequested(Long requestId, String code, int round, int maxRounds, int hours) {
        return new WebMessage(NotificationType.CUSTOM_PROPOSAL_REVISION_REQUESTED, "Khách yêu cầu chỉnh sửa đề xuất - " + code,
                "Lần chỉnh sửa " + round + "/" + maxRounds + " · gửi bản mới trong " + hours + " giờ", "/agent/requests/" + requestId);
    }

    public static WebMessage customProposalAccepted(Long requestId, String code, String title) {
        return new WebMessage(NotificationType.CUSTOM_PROPOSAL_ACCEPTED, "Khách đã đồng ý đề xuất - " + code, title,
                "/agent/requests/" + requestId);
    }

    public static WebMessage customProposalDueSoon(Long requestId, String code, java.time.LocalDateTime deadline) {
        return new WebMessage(NotificationType.CUSTOM_PROPOSAL_DUE_SOON, "Sắp hết hạn gửi đề xuất - " + code,
                "Hạn chót " + deadline.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                        + ". Quá hạn, yêu cầu sẽ được chuyển cho đơn vị khác", "/agent/requests/" + requestId);
    }

    public static WebMessage customProposalOverdue(Long requestId, String code) {
        return new WebMessage(NotificationType.CUSTOM_PROPOSAL_OVERDUE, "Đã quá hạn gửi đề xuất - " + code,
                "Yêu cầu được chuyển lại để TripConnect giao cho đơn vị khác", "/agent/requests/" + requestId);
    }

    public static WebMessage customRequestNew(Long requestId, String code, String destinations) {
        return new WebMessage(NotificationType.CUSTOM_REQUEST_NEW, "Yêu cầu thiết kế tour mới " + code,
                destinations + " · cần giao cho đơn vị tổ chức", "/admin/requests/" + requestId);
    }

    /**
     * Agent từ chối / không phản hồi kịp / quá hạn gửi đề xuất -> Admin cần giao lại.
     *
     * @param what vd "Công ty A đã từ chối"
     */
    public static WebMessage customRequestNeedsReassign(Long requestId, String code, String what) {
        return new WebMessage(NotificationType.CUSTOM_REQUEST_DECLINED, "Yêu cầu " + code + " cần giao lại", what,
                "/admin/requests/" + requestId);
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
