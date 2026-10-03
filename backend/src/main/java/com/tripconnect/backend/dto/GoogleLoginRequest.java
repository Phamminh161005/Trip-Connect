package com.tripconnect.backend.dto;

import com.tripconnect.backend.enums.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleLoginRequest {

    @NotBlank(message = "ID Token không được để trống")
    @Size(max = 4096, message = "ID Token không hợp lệ")
    private String idToken;

    private UserRole role;

    @Valid
    private RegisterRequest.AgentProfileRequest agentProfile;
}