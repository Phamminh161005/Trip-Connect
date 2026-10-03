package com.tripconnect.backend.controller;

import com.tripconnect.backend.dto.UpdateMyProfileRequest;
import com.tripconnect.backend.dto.UserProfileResponse;
import com.tripconnect.backend.security.AuthenticatedUser;
import com.tripconnect.backend.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Thông tin cá nhân của người dùng đang đăng nhập (xem: GET /api/auth/me). */
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService userProfileService;

    @PutMapping
    public UserProfileResponse updateMyProfile(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                               @Valid @RequestBody UpdateMyProfileRequest request) {
        return userProfileService.updateMyProfile(currentUser.userId(), request);
    }
}
