package com.tripconnect.backend.dto;

import com.tripconnect.backend.enums.AgentStatus;

import java.time.LocalDateTime;

/** Một dòng trong danh sách hồ sơ Agent ở trang Admin. */
public record AgentProfileSummaryResponse(
        Long id,
        Long userId,
        String fullName,
        String email,
        String companyName,
        String taxCode,
        AgentStatus status,
        LocalDateTime submittedAt,
        LocalDateTime createdAt
) {
}
