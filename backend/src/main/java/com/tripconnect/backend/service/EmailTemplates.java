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

    // ===== Tour =====

    public static Email tourApproved(String tourTitle) {
        return new Email(
                "Tour đã được duyệt - TripConnect",
                "Tour \"" + tourTitle + "\" đã được duyệt và đang hiển thị với khách hàng.\n"
                        + "Hãy kiểm tra các lịch khởi hành để khách có thể đặt tour.");
    }

    public static Email tourNeedsRevision(String tourTitle, String reason) {
        return new Email(
                "Tour cần chỉnh sửa - TripConnect",
                "Tour \"" + tourTitle + "\" cần chỉnh sửa trước khi được công khai.\nLý do: " + reason
                        + "\nVui lòng cập nhật và gửi duyệt lại.");
    }

    public static Email tourSuspended(String tourTitle, String reason) {
        return new Email(
                "Tour bị đình chỉ - TripConnect",
                "Tour \"" + tourTitle + "\" đã bị quản trị viên đình chỉ và không còn nhận đặt chỗ mới.\nLý do: " + reason
                        + "\nCác lịch khởi hành đã có khách đặt vẫn giữ nguyên. Vui lòng liên hệ bộ phận hỗ trợ nếu cần.");
    }

    public static Email tourUnsuspended(String tourTitle) {
        return new Email(
                "Tour đã được mở lại - TripConnect",
                "Tour \"" + tourTitle + "\" đã được quản trị viên bỏ đình chỉ và hiển thị lại với khách hàng.");
    }

    // ===== Đặt tour =====

    static final java.text.NumberFormat VND = java.text.NumberFormat.getIntegerInstance(java.util.Locale.forLanguageTag("vi-VN"));

    static String money(long amount) {
        return VND.format(amount) + " VNĐ";
    }

    static String day(java.time.LocalDate date) {
        return date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public static Email bookingPaid(String code, String tourTitle, java.time.LocalDate startDate, String meetingPoint,
                                    String meetingTime, int travellers, long amount) {
        return new Email(
                "Đặt tour thành công - " + code + " - TripConnect",
                "Cảm ơn bạn đã đặt tour trên TripConnect!\n\n"
                        + "Mã đơn: " + code + "\n"
                        + "Tour: " + tourTitle + "\n"
                        + "Khởi hành: " + day(startDate) + ", tập trung lúc " + meetingTime + " tại " + meetingPoint + "\n"
                        + "Số khách: " + travellers + "\n"
                        + "Đã thanh toán: " + money(amount) + "\n\n"
                        + "Bạn có thể xem chi tiết đơn trong mục \"Đơn đặt của tôi\".");
    }

    private static String countdown(long daysLeft) {
        return daysLeft == 1 ? "ngày mai" : "còn " + daysLeft + " ngày nữa";
    }

    /**
     * Nhắc khách trước ngày khởi hành.
     *
     * @param passengers mỗi dòng một hành khách, vd "Nguyễn Văn A (Người lớn)"
     * @param organizer  tên + cách liên hệ đơn vị tổ chức
     */
    public static Email tripReminder(String code, String tourTitle, long daysLeft, java.time.LocalDate startDate,
                                     java.time.LocalDate endDate, String meetingTime, String meetingPoint,
                                     java.util.List<String> passengers, int missingPassengers, String notes,
                                     boolean international, String organizer, String bookingUrl) {
        StringBuilder body = new StringBuilder()
                .append("Chuyến đi \"").append(tourTitle).append("\" của bạn khởi hành ").append(countdown(daysLeft)).append(".\n\n")
                .append("Mã đơn: ").append(code).append("\n")
                .append("Thời gian: ").append(day(startDate)).append(" - ").append(day(endDate)).append("\n")
                .append("Tập trung: ").append(meetingTime).append(" ngày ").append(day(startDate))
                .append(" tại ").append(meetingPoint).append("\n\n")
                .append("Hành khách:\n");
        passengers.forEach(p -> body.append("- ").append(p).append("\n"));
        if (missingPassengers > 0) {
            body.append("- Còn ").append(missingPassengers).append(" khách chưa có thông tin\n");
        }
        body.append("\nGiấy tờ cần mang: ").append(international
                ? "hộ chiếu còn hạn ít nhất 6 tháng của tất cả hành khách."
                : "căn cước công dân (trẻ em mang giấy khai sinh).").append("\n");
        if (notes != null && !notes.isBlank()) {
            body.append("\nLưu ý từ đơn vị tổ chức:\n").append(notes.trim()).append("\n");
        }
        body.append("\nĐơn vị tổ chức: ").append(organizer).append("\n")
                .append("Chi tiết đơn: ").append(bookingUrl).append("\n\n")
                .append("Chúc bạn có một chuyến đi vui vẻ!");
        return new Email("Chuyến đi của bạn khởi hành " + countdown(daysLeft) + " - " + code + " - TripConnect",
                body.toString());
    }

    /**
     * Nhắc đơn vị tổ chức chuẩn bị đoàn.
     *
     * @param bookingLines mỗi dòng một đơn, vd "TC... · Nguyễn Văn A · 0900000000 · 2 khách"
     */
    public static Email departureReminderForOrganizer(String tourTitle, long daysLeft, java.time.LocalDate startDate,
                                                      int bookings, int adults, int children, int infants,
                                                      java.util.List<String> bookingLines, String manifestUrl) {
        StringBuilder body = new StringBuilder()
                .append("Lịch khởi hành ngày ").append(day(startDate)).append(" của tour \"").append(tourTitle)
                .append("\" bắt đầu ").append(countdown(daysLeft)).append(".\n\n")
                .append("Số đơn đã thanh toán: ").append(bookings).append("\n")
                .append("Tổng khách: ").append(adults + children + infants)
                .append(" (").append(adults).append(" người lớn, ").append(children).append(" trẻ em, ")
                .append(infants).append(" trẻ sơ sinh)\n\n")
                .append("Danh sách đơn:\n");
        bookingLines.forEach(line -> body.append("- ").append(line).append("\n"));
        body.append("\nDanh sách hành khách đầy đủ: ").append(manifestUrl);
        return new Email("[TripConnect] Lịch " + day(startDate) + " khởi hành " + countdown(daysLeft) + ": " + tourTitle,
                body.toString());
    }

    public static Email departureLowBookings(String tourTitle, long daysLeft, java.time.LocalDate startDate,
                                             long paidSeats, int capacity, int thresholdPercent, String departureUrl) {
        return new Email(
                "[TripConnect] Lịch " + day(startDate) + " còn ít khách: " + tourTitle,
                "Lịch khởi hành ngày " + day(startDate) + " của tour \"" + tourTitle + "\" còn " + daysLeft
                        + " ngày nữa là khởi hành nhưng mới có " + paidSeats + "/" + capacity + " chỗ đã thanh toán (dưới "
                        + thresholdPercent + "% số chỗ).\n\n"
                        + "Bạn có thể:\n"
                        + "- Tiếp tục mở bán nếu vẫn đủ điều kiện tổ chức chuyến đi;\n"
                        + "- Hoặc hủy lịch khởi hành: khách đã thanh toán được hoàn 100% tự động và nhận email thông báo."
                        + " Hủy càng sớm, khách càng dễ sắp xếp kế hoạch khác.\n\n"
                        + "Quản lý lịch khởi hành: " + departureUrl);
    }

    public static Email reviewInvite(String code, String tourTitle, int windowDays, String reviewUrl) {
        return new Email(
                "Chuyến đi \"" + tourTitle + "\" của bạn thế nào? - TripConnect",
                "Cảm ơn bạn đã đồng hành cùng TripConnect trong chuyến đi \"" + tourTitle + "\" (đơn " + code + ").\n\n"
                        + "Hãy dành một phút đánh giá tour — nhận xét của bạn giúp các khách khác chọn được chuyến đi phù hợp"
                        + " và giúp đơn vị tổ chức phục vụ tốt hơn.\n"
                        + "Bạn có thể đánh giá trong " + windowDays + " ngày tới tại: " + reviewUrl);
    }

    // ===== Yêu cầu thiết kế tour riêng =====

    public static Email customRequestAssigned(String code, String destinations, java.time.LocalDateTime deadline, String url) {
        return new Email(
                "[TripConnect] Yêu cầu thiết kế tour mới " + code,
                "Bạn được giao một yêu cầu thiết kế tour riêng: " + destinations + ".\n"
                        + "Vui lòng xem chi tiết và Nhận hoặc Từ chối trước "
                        + deadline.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                        + ". Quá hạn, yêu cầu sẽ được chuyển cho đơn vị khác.\n\n"
                        + "Xem yêu cầu: " + url);
    }

    public static Email customRequestAccepted(String code, String companyName, int proposalHours, String url) {
        return new Email(
                "Yêu cầu " + code + " đã có đơn vị nhận - TripConnect",
                companyName + " đã nhận yêu cầu thiết kế tour " + code + " của bạn và sẽ gửi đề xuất lịch trình, báo giá trong "
                        + proposalHours + " giờ.\n\nTheo dõi yêu cầu: " + url);
    }

    public static Email customProposalReceived(String code, String companyName, int versionNo, String title,
                                               java.time.LocalDate startDate, java.time.LocalDate endDate, long totalPrice,
                                               long depositAmount, java.time.LocalDateTime expiresAt, String url) {
        return new Email(
                (versionNo == 1 ? "Đề xuất tour cho yêu cầu " : "Đề xuất đã chỉnh sửa cho yêu cầu ") + code + " - TripConnect",
                companyName + (versionNo == 1 ? " đã gửi đề xuất lịch trình và báo giá" : " đã gửi bản chỉnh sửa (bản " + versionNo + ")")
                        + " cho yêu cầu thiết kế tour " + code + " của bạn.\n\n"
                        + "Chuyến đi: " + title + "\n"
                        + "Thời gian: " + day(startDate) + " - " + day(endDate) + "\n"
                        + "Tổng chi phí: " + money(totalPrice) + " (đặt cọc " + money(depositAmount) + " khi xác nhận)\n\n"
                        + "Vui lòng xem chi tiết và Đồng ý hoặc Yêu cầu chỉnh sửa trước "
                        + expiresAt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")) + ".\n\n"
                        + "Xem đề xuất: " + url);
    }

    public static Email customProposalAccepted(String code, String customerName, String title, java.time.LocalDate startDate,
                                               long totalPrice, String url) {
        return new Email(
                "[TripConnect] Khách đã đồng ý đề xuất - " + code,
                "Khách " + customerName + " đã đồng ý đề xuất \"" + title + "\" (khởi hành " + day(startDate) + ", tổng "
                        + money(totalPrice) + ") cho yêu cầu " + code + ".\n\n"
                        + "Bước tiếp theo là tạo tour riêng để khách đặt cọc. Chi tiết: " + url);
    }

    public static Email depositPaid(String code, String title, java.time.LocalDate startDate, long deposit, long balance,
                                   java.time.LocalDate dueDate, int travellers) {
        return new Email(
                "Đã nhận tiền cọc - đơn " + code + " - TripConnect",
                "TripConnect đã nhận " + money(deposit) + " tiền cọc cho chuyến đi \"" + title + "\" khởi hành "
                        + day(startDate) + " (đơn " + code + ").\n\n"
                        + "Phần còn lại: " + money(balance) + ", hạn thanh toán hết ngày " + day(dueDate)
                        + ". Bạn được gia hạn 1 lần thêm 3 ngày nếu cần.\n"
                        + "Trước khi thanh toán phần còn lại, vui lòng nhập đầy đủ thông tin " + travellers
                        + " người đi trong mục \"Đơn đặt của tôi\".\n\n"
                        + "Quá hạn mà chưa thanh toán, đơn sẽ bị hủy và tiền cọc không được hoàn lại.");
    }

    public static Email balanceDue(String code, String title, long balance, java.time.LocalDate dueDate, boolean canExtend,
                                   String url) {
        return new Email(
                "Sắp tới hạn thanh toán phần còn lại - đơn " + code + " - TripConnect",
                "Chuyến đi \"" + title + "\" (đơn " + code + ") còn " + money(balance) + " cần thanh toán, hạn hết ngày "
                        + day(dueDate) + ".\n"
                        + (canExtend ? "Nếu cần thêm thời gian, bạn có thể gia hạn 1 lần thêm 3 ngày ngay trong trang đơn.\n" : "")
                        + "Quá hạn mà chưa thanh toán, đơn sẽ bị hủy và tiền cọc không được hoàn lại.\n\n"
                        + "Thanh toán: " + url);
    }

    public static Email settlementCreated(String code, String period, int bookings, long retained, long commission, long payout,
                                          java.time.LocalDate deadline, String url) {
        return new Email(
                "[TripConnect] Bảng đối soát " + period + " - " + code,
                "TripConnect đã lập bảng đối soát " + period + " (" + code + ") gồm " + bookings + " đơn.\n\n"
                        + "Tiền khách đã thanh toán: " + money(retained) + "\n"
                        + "Phí nền tảng: " + money(commission) + "\n"
                        + "Bạn nhận: " + money(payout) + "\n\n"
                        + "Vui lòng kiểm tra và Xác nhận hoặc Khiếu nại trước hết ngày " + day(deadline)
                        + ". Quá hạn, bảng đối soát được tự xác nhận.\n\nXem chi tiết: " + url);
    }

    public static Email settlementPaid(String code, long payout, String bankName, String accountNumber, String transactionRef,
                                       String url) {
        return new Email(
                "[TripConnect] Đã thanh toán đối soát " + code,
                "TripConnect đã chuyển " + money(payout) + " cho bảng đối soát " + code + " vào tài khoản " + bankName + " "
                        + accountNumber + ".\nMã giao dịch: " + transactionRef + "\n\nChi tiết: " + url);
    }

    /** Khách vừa chốt đề xuất: đơn tour riêng đã tạo, cần thanh toán (đặt cọc hoặc toàn bộ). */
    public static Email privateBookingCreated(String code, String title, java.time.LocalDate startDate, long total,
                                              long firstPayment, boolean deposit, java.time.LocalDateTime payBefore, String url) {
        return new Email(
                "Xác nhận tour riêng - đơn " + code + " - TripConnect",
                "Bạn đã chốt chuyến đi \"" + title + "\" khởi hành " + day(startDate) + ". Đơn đặt tour " + code
                        + " đã được tạo với tổng chi phí " + money(total) + ".\n\n"
                        + (deposit ? "Vui lòng đặt cọc " + money(firstPayment) : "Vui lòng thanh toán " + money(firstPayment))
                        + " trước " + payBefore.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                        + ", quá hạn đơn sẽ tự hủy.\n\nThanh toán: " + url);
    }

    public static Email customRequestClosed(String code, String reason, String url) {
        return new Email(
                "Yêu cầu " + code + " đã đóng - TripConnect",
                "Yêu cầu thiết kế tour " + code + " của bạn đã đóng.\nLý do: " + reason
                        + "\n\nBạn có thể gửi yêu cầu mới bất cứ lúc nào. Chi tiết: " + url);
    }

    public static Email bookingCancelled(String code, String tourTitle, String reason, long refundAmount) {
        String refund = refundAmount > 0
                ? "Số tiền hoàn lại: " + money(refundAmount) + ". Tiền sẽ về tài khoản / thẻ đã thanh toán trong vài ngày làm việc."
                : "Đơn không có khoản hoàn tiền theo chính sách hủy tour.";
        return new Email(
                "Đơn " + code + " đã bị hủy - TripConnect",
                "Đơn đặt tour " + code + " (" + tourTitle + ") đã bị hủy.\n"
                        + (reason == null || reason.isBlank() ? "" : "Lý do: " + reason + "\n")
                        + refund);
    }

    public static Email refundCompleted(String code, long amount) {
        return new Email(
                "Đã hoàn tiền đơn " + code + " - TripConnect",
                "TripConnect đã hoàn " + money(amount) + " cho đơn " + code + ".\n"
                        + "Thời gian tiền về tài khoản tùy ngân hàng, thường trong 1-7 ngày làm việc.");
    }

    public static Email manualRefundNeeded(String code, long amount, String error) {
        return new Email(
                "[TripConnect] Cần hoàn tiền thủ công đơn " + code,
                "Hoàn tiền tự động qua VNPay cho đơn " + code + " (" + money(amount) + ") không thành công.\n"
                        + "Lỗi: " + (error == null ? "không rõ" : error) + "\n"
                        + "Vui lòng vào trang quản trị > Đơn đặt tour để hoàn tiền thủ công.");
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
