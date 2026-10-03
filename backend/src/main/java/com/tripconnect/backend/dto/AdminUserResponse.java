package com.tripconnect.backend.dto;

import com.tripconnect.backend.enums.UserRole;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        UserRole role,
        boolean active,
        boolean emailVerified,
        boolean googleLinked,
        String deactivatedReason,
        LocalDateTime deactivatedAt,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        /** Id hồ sơ đối tác (chỉ có với Agent) — để trang Admin mở thẳng hồ sơ. */
        Long agentProfileId
) {
}
