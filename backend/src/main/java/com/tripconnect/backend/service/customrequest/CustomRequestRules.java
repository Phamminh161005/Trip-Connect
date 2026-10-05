package com.tripconnect.backend.service.customrequest;

import com.tripconnect.backend.entity.CustomProposal;
import com.tripconnect.backend.entity.CustomRequest;
import com.tripconnect.backend.enums.CustomRequestStatus;
import com.tripconnect.backend.enums.ProposalStatus;

import java.util.EnumSet;
import java.util.Set;

/** Các mốc và giới hạn của yêu cầu thiết kế tour riêng. */
public final class CustomRequestRules {

    private CustomRequestRules() {
    }

    /** Ngày khởi hành sớm nhất phải cách hôm nay ít nhất ngần này ngày (đủ thời gian giao, nhận, đề xuất, chốt). */
    public static final int MIN_LEAD_DAYS = 14;
    /** Khoảng "có thể khởi hành từ ... đến ..." tối đa. */
    public static final int MAX_START_WINDOW_DAYS = 60;
    public static final int MAX_DURATION_DAYS = 30;
    public static final int MAX_TRAVELLERS = 100;
    public static final int MAX_DESTINATIONS = 10;
    /** Một khách giữ tối đa ngần này yêu cầu đang mở (chống gửi tràn lan). */
    public static final int MAX_OPEN_PER_CUSTOMER = 3;

    /** Agent phải nhận / từ chối trong ngần này giờ kể từ lúc được giao. */
    public static final int ACCEPT_HOURS = 48;
    /** Sau khi nhận, Agent gửi đề xuất đầu tiên trong ngần này giờ. */
    public static final int PROPOSAL_HOURS = 72;
    /** Khách yêu cầu chỉnh sửa -> Agent gửi bản mới trong ngần này giờ. */
    public static final int REVISION_HOURS = 48;
    /** Khách có ngần này ngày để đồng ý hoặc yêu cầu chỉnh sửa một đề xuất. */
    public static final int RESPONSE_DAYS = 3;
    public static final int MAX_REVISIONS = 5;
    /** Đang chờ khách mà không có trao đổi nào trong ngần này ngày -> tự đóng. */
    public static final int INACTIVE_CLOSE_DAYS = 5;
    /** Nhắc trước hạn (Agent gửi đề xuất / khách phản hồi) ngần này giờ. */
    public static final int REMIND_BEFORE_HOURS = 24;
    /**
     * Ngày khởi hành trong đề xuất phải cách hôm nay ít nhất ngần này ngày (đủ thời gian đặt cọc, chuẩn bị).
     * Chưa chốt mà ngày khởi hành muộn nhất còn ít hơn ngần này ngày -> tự đóng.
     */
    public static final int AUTO_CLOSE_START_WITHIN_DAYS = 7;
    public static final int MIN_PROPOSAL_LEAD_DAYS = AUTO_CLOSE_START_WITHIN_DAYS;
    public static final int DEPOSIT_PERCENT = 30;
    public static final long MIN_ADULT_PRICE = 10_000;
    public static final long MAX_PRICE = 1_000_000_000;

    /** Số gợi ý Admin thấy nổi bật (tài liệu nghiệp vụ: Top 3). */
    public static final int TOP_SUGGESTIONS = 3;
    public static final int MAX_CANDIDATES = 10;

    public static final Set<CustomRequestStatus> OPEN_STATUSES =
            EnumSet.of(CustomRequestStatus.NEW, CustomRequestStatus.WAITING_AGENT, CustomRequestStatus.IN_PROGRESS);

    /** Tiền cọc làm tròn lên hàng nghìn, không vượt tổng tiền. */
    public static long deposit(long total) {
        long raw = (total * DEPOSIT_PERCENT + 99) / 100;
        return Math.min(total, (raw + 999) / 1000 * 1000);
    }

    /** Agent đang phụ trách và tới lượt gửi đề xuất (bản đầu hoặc bản chỉnh sửa). */
    public static boolean awaitingAgent(CustomRequest r) {
        return r.getStatus() == CustomRequestStatus.IN_PROGRESS && r.getProposalDeadline() != null;
    }

    /** Khách được yêu cầu chỉnh sửa đề xuất mới nhất (đang chờ hoặc vừa hết hạn) khi còn lượt. */
    public static boolean canRequestRevision(CustomRequest r, CustomProposal latest) {
        return r.getStatus() == CustomRequestStatus.IN_PROGRESS && latest != null
                && (latest.getStatus() == ProposalStatus.SENT || latest.getStatus() == ProposalStatus.EXPIRED)
                && r.getRevisionCount() < MAX_REVISIONS;
    }

    public static boolean canAcceptProposal(CustomRequest r, CustomProposal latest) {
        return r.getStatus() == CustomRequestStatus.IN_PROGRESS && latest != null && latest.getStatus() == ProposalStatus.SENT;
    }
}
