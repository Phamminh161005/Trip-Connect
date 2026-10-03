package com.tripconnect.backend.controller;

import com.tripconnect.backend.dto.*;
import com.tripconnect.backend.security.AuthenticatedUser;
import com.tripconnect.backend.security.RateLimiter;
import com.tripconnect.backend.service.AuthService;
import com.tripconnect.backend.service.UserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

/*
 * Lưu ý rate limit: /login, /refresh, /logout đi qua route BFF của Next.js nên backend chỉ thấy IP
 * của server Next.js -> KHÔNG giới hạn theo IP ở các endpoint này (sẽ làm khi cấu hình X-Forwarded-For).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Duration ONE_MINUTE = Duration.ofMinutes(1);
    private static final Duration FIFTEEN_MINUTES = Duration.ofMinutes(15);
    private static final Duration ONE_HOUR = Duration.ofHours(1);

    private final AuthService authService;
    private final UserProfileService userProfileService;
    private final RateLimiter rateLimiter;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        rateLimiter.check("register-ip", http.getRemoteAddr(), 5, ONE_HOUR);
        authService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Void> verifyOtp(@Valid @RequestBody VerifyOtpRequest request, HttpServletRequest http) {
        rateLimiter.check("otp-verify-ip", http.getRemoteAddr(), 20, FIFTEEN_MINUTES);
        authService.verifyOtp(request.getEmail(), request.getOtp());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<Void> resendOtp(@Valid @RequestBody ResendOtpRequest request, HttpServletRequest http) {
        checkOtpSendLimit(http, request.getEmail());
        authService.resendOtp(request.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        rateLimiter.check("login-email", request.getEmail(), 10, FIFTEEN_MINUTES);
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenPairResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshAccessToken(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/google")
    public ResponseEntity<GoogleAuthResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request,
                                                          HttpServletRequest http) {
        rateLimiter.check("google-ip", http.getRemoteAddr(), 20, ONE_MINUTE);
        return ResponseEntity.ok(authService.loginWithGoogle(request));
    }

    @PostMapping("/resend-unlock-otp")
    public ResponseEntity<Void> resendUnlockOtp(@Valid @RequestBody ResendOtpRequest request,
                                                HttpServletRequest http) {
        checkOtpSendLimit(http, request.getEmail());
        authService.resendUnlockOtp(request.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/unlock-account")
    public ResponseEntity<Void> unlockAccount(@Valid @RequestBody VerifyOtpRequest request, HttpServletRequest http) {
        rateLimiter.check("otp-verify-ip", http.getRemoteAddr(), 20, FIFTEEN_MINUTES);
        authService.unlockAccount(request.getEmail(), request.getOtp());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(userProfileService.getMyProfile(currentUser.userId()));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(currentUser.userId(), request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                               HttpServletRequest http) {
        checkOtpSendLimit(http, request.getEmail());
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request,
                                              HttpServletRequest http) {
        rateLimiter.check("otp-verify-ip", http.getRemoteAddr(), 20, FIFTEEN_MINUTES);
        authService.resetPassword(request.getEmail(), request.getOtp(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    /** Dùng chung cho mọi endpoint gửi email OTP để chống spam email. */
    private void checkOtpSendLimit(HttpServletRequest http, String email) {
        rateLimiter.check("otp-send-ip", http.getRemoteAddr(), 10, ONE_HOUR);
        rateLimiter.check("otp-send-email", email, 3, FIFTEEN_MINUTES);
    }
}
