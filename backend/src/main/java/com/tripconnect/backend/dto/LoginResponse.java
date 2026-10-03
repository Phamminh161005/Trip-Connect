package com.tripconnect.backend.dto;

import com.tripconnect.backend.enums.AgentStatus;
import com.tripconnect.backend.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private Long userId;
    private String fullName;
    private String email;
    private UserRole role;
    private AgentStatus agentStatus; // null nếu không phải Agent
}