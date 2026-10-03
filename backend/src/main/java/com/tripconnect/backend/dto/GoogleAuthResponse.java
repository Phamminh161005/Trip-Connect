package com.tripconnect.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GoogleAuthResponse {
    private boolean needsRoleSelection;
    private String email;
    private String fullName;
    private LoginResponse loginResponse; // null nếu needsRoleSelection = true
}