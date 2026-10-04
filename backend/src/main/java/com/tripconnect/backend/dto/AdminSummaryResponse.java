package com.tripconnect.backend.dto;

/** Số liệu cho trang Tổng quan và số việc đang chờ trên menu của Admin (gộp 1 lần gọi). */
public record AdminSummaryResponse(
        long pendingAgentProfiles,
        long pendingChangeRequests,
        long totalUsers,
        long approvedAgents,
        long pendingTours,
        /* Đơn cần Admin hoàn tiền thủ công (VNPay báo lỗi) */
        long manualRefunds
) {
}
