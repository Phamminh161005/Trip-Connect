package com.tripconnect.backend.dto;

/** Admin xem yêu cầu cập nhật: đặt cạnh thông tin hiện tại để so sánh trước khi duyệt. */
public record AdminChangeRequestDetailResponse(
        AgentChangeRequestResponse changeRequest,
        AgentProfileResponse currentProfile
) {
}
