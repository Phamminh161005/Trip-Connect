package com.tripconnect.backend.dto;

import com.tripconnect.backend.enums.AgentStatus;
import com.tripconnect.backend.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserProfileResponse {
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private UserRole role;
    private AgentStatus agentStatus; // null nếu không phải Agent
}
