package com.tripconnect.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Lý do Admin từ chối — dùng chung cho hồ sơ Agent và yêu cầu cập nhật hồ sơ. */
@Getter
@Setter
public class RejectAgentProfileRequest {

    @NotBlank(message = "Lý do từ chối không được để trống")
    @Size(max = 1000, message = "Lý do tối đa 1000 ký tự")
    private String reason;
}
