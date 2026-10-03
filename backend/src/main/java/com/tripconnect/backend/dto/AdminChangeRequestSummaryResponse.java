package com.tripconnect.backend.dto;

import com.tripconnect.backend.enums.ChangeRequestStatus;

import java.time.LocalDateTime;

/** Một dòng trong danh sách yêu cầu cập nhật hồ sơ ở trang Admin. */
public record AdminChangeRequestSummaryResponse(
        Long id,
        Long agentProfileId,
        String currentCompanyName,
        String agentEmail,
        ChangeRequestStatus status,
        LocalDateTime createdAt
) {
}
