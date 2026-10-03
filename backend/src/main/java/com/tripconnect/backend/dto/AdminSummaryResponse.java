package com.tripconnect.backend.dto;

/** Số liệu cho trang Tổng quan và số việc đang chờ trên menu của Admin (gộp 1 lần gọi). */
public record AdminSummaryResponse(
        long pendingAgentProfiles,
        long pendingChangeRequests,
        long totalUsers,
        long approvedAgents
) {
}
