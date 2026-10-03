package com.tripconnect.backend.service;

import com.tripconnect.backend.dto.LoginRequest;
import com.tripconnect.backend.dto.LoginResponse;
import com.tripconnect.backend.dto.RegisterRequest;
import com.tripconnect.backend.dto.TokenPairResponse;
import com.tripconnect.backend.dto.GoogleAuthResponse;
import com.tripconnect.backend.dto.GoogleLoginRequest;
import com.tripconnect.backend.entity.*;
import com.tripconnect.backend.enums.AgentStatus;
import com.tripconnect.backend.enums.OtpPurpose;
import com.tripconnect.backend.enums.UserRole;
import com.tripconnect.backend.exception.AccountDisabledException;
import com.tripconnect.backend.exception.AccountLockedException;
import com.tripconnect.backend.exception.EmailAlreadyExistsException;
import com.tripconnect.backend.exception.InvalidCredentialsException;
import com.tripconnect.backend.exception.InvalidOtpException;
import com.tripconnect.backend.exception.InvalidRefreshTokenException;
import com.tripconnect.backend.exception.ResourceNotFoundException;
import com.tripconnect.backend.exception.TooManyRequestsException;
import com.tripconnect.backend.repository.*;
import com.tripconnect.backend.security.HashUtil;
import com.tripconnect.backend.security.JwtUtil;
import com.tripconnect.backend.service.agent.AgentExpertiseWriter;
import com.tripconnect.backend.service.agent.AgentReferenceResolver;
import com.tripconnect.backend.service.agent.BankAccountHolder;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;
    private static final int OTP_VALID_MINUTES = 5;
    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final int OTP_RESEND_COOLDOWN_SECONDS = 60;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final AgentProfileRepository agentProfileRepository;
    private final AgentExpertiseWriter agentExpertiseWriter;
    private final AgentReferenceResolver agentReferenceResolver;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final JwtUtil jwtUtil;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    // Hash giả để login với email không tồn tại vẫn tốn thời gian bcrypt như bình thường
    private String dummyPasswordHash;

    @PostConstruct
    void initDummyPasswordHash() {
        dummyPasswordHash = passwordEncoder.encode("dummy-" + UUID.randomUUID());
    }

    // ===================== ĐĂNG KÝ  =====================

    @Transactional
    public void registerUser(RegisterRequest request) {

        if (request.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("Không thể đăng ký tài khoản với vai trò này");
        }

        if (request.getRole() == UserRole.AGENT && request.getAgentProfile() == null) {
            throw new IllegalArgumentException("Thông tin hồ sơ Agent là bắt buộc khi đăng ký làm Agent");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email đã được sử dụng");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setEmailVerified(false);
        String otp = issueOtp(user, OtpPurpose.REGISTER);

        User savedUser = userRepository.save(user);

        if (request.getRole() == UserRole.AGENT) {
            createAgentProfile(savedUser, request.getAgentProfile());
        }

        eventPublisher.publishEvent(new OtpEmailEvent(savedUser.getEmail(), otp, OtpPurpose.REGISTER));
    }

    /**
     * Hồ sơ Agent mới ở trạng thái Nháp (DRAFT). Agent upload giấy tờ và bấm "Nộp hồ sơ"
     * SAU KHI xác thực email — để chỉ tài khoản đã xác thực mới upload được file.
     */
    private void createAgentProfile(User user, RegisterRequest.AgentProfileRequest req) {
        AgentProfile profile = new AgentProfile();
        profile.setUser(user);
        profile.setStatus(AgentStatus.DRAFT);
        profile.setCompanyName(req.getCompanyName().trim());
        profile.setTaxCode(req.getTaxCode().trim());
        profile.setBusinessLicense(req.getBusinessLicense() == null || req.getBusinessLicense().isBlank()
                ? null : req.getBusinessLicense().trim());
        profile.setAddressProvince(agentReferenceResolver.requireVietnamProvince(req.getAddressProvinceId()));
        profile.setAddress(req.getAddress().trim());
        profile.setBank(agentReferenceResolver.requireBank(req.getBankBin()));
        profile.setBankAccountNumber(req.getBankAccountNumber());
        profile.setBankAccountHolder(BankAccountHolder.normalize(req.getBankAccountHolder()));
        agentProfileRepository.save(profile);

        agentExpertiseWriter.create(user, req.getLocationIds(), req.getCategoryIds());
    }

    private AgentStatus resolveAgentStatus(User user) {
        if (user.getRole() != UserRole.AGENT) {
            return null;
        }
        return agentProfileRepository.findByUserId(user.getId())
                .map(AgentProfile::getStatus)
                .orElse(null);
    }

    // ===================== XÁC THỰC OTP  =====================

    @Transactional(noRollbackFor = InvalidOtpException.class)
    public void verifyOtp(String email, String otpInput) {
        User user = userRepository.findByEmailForUpdate(email)
                .orElseThrow(() -> new InvalidOtpException("Mã OTP không hợp lệ hoặc đã hết hạn"));

        if (user.isEmailVerified()) {
            throw new IllegalStateException("Tài khoản đã được xác thực trước đó");
        }

        validateOtp(user, otpInput, OtpPurpose.REGISTER);

        user.setEmailVerified(true);
        clearOtp(user);
        userRepository.save(user);
    }

    @Transactional
    public void resendOtp(String email) {
        Optional<User> found = userRepository.findByEmailForUpdate(email);
        if (found.isEmpty() || found.get().isEmailVerified()) {
            return; // không tiết lộ email có tồn tại hay không
        }

        User user = found.get();
        ensureOtpCooldownPassed(user);
        String otp = issueOtp(user, OtpPurpose.REGISTER);
        userRepository.save(user);

        eventPublisher.publishEvent(new OtpEmailEvent(user.getEmail(), otp, OtpPurpose.REGISTER));
    }

    // ===================== ĐĂNG NHẬP =====================

    @Transactional(noRollbackFor = {InvalidCredentialsException.class, AccountLockedException.class})
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailForUpdate(request.getEmail()).orElse(null);
        if (user == null) {
            passwordEncoder.matches(request.getPassword(), dummyPasswordHash); // cân bằng thời gian phản hồi
            throw new InvalidCredentialsException("Email hoặc mật khẩu không đúng");
        }

        if (isLocked(user)) {
            throw new AccountLockedException("Tài khoản đang tạm khóa do sai mật khẩu nhiều lần. Thử lại sau "
                    + LOCK_MINUTES + " phút hoặc mở khóa ngay bằng mã OTP đã gửi qua email.");
        }

        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user);
            throw new InvalidCredentialsException("Email hoặc mật khẩu không đúng");
        }

        // Kiểm tra SAU khi đúng mật khẩu, để người lạ không dò được tài khoản nào đang bị vô hiệu hóa
        requireActive(user);

        if (!user.isEmailVerified()) {
            throw new IllegalStateException("Tài khoản chưa được xác thực email");
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return issueTokens(user);
    }

    private LoginResponse issueTokens(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = createRefreshToken(user);
        return new LoginResponse(accessToken, refreshToken, user.getId(),
                user.getFullName(), user.getEmail(), user.getRole(), resolveAgentStatus(user));
    }

    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        if (attempts < MAX_FAILED_ATTEMPTS) {
            user.setFailedLoginAttempts(attempts);
            userRepository.save(user);
            return;
        }

        user.setFailedLoginAttempts(0); // hết thời gian khóa thì đếm lại từ đầu
        user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
        String otp = issueOtp(user, OtpPurpose.UNLOCK);
        userRepository.save(user);
        eventPublisher.publishEvent(new OtpEmailEvent(user.getEmail(), otp, OtpPurpose.UNLOCK));
        throw new AccountLockedException("Sai mật khẩu quá " + MAX_FAILED_ATTEMPTS + " lần. Tài khoản bị khóa "
                + LOCK_MINUTES + " phút, mã OTP mở khóa đã được gửi qua email.");
    }

    // ===================== ĐĂNG NHẬP / ĐĂNG KÝ BẰNG GOOGLE =====================

    @Transactional
    public GoogleAuthResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleIdToken.Payload payload = verifyGoogleIdToken(request.getIdToken());

        if (payload.getEmail() == null || !Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new InvalidCredentialsException("Email của tài khoản Google chưa được xác minh");
        }
        String email = payload.getEmail().trim().toLowerCase(Locale.ROOT);
        String googleId = payload.getSubject();
        String fullName = payload.get("name") instanceof String name
                ? name
                : email.substring(0, email.indexOf('@'));

        Optional<User> existing = userRepository.findByEmailForUpdate(email);
        User user;

        if (existing.isPresent()) {
            user = existing.get();
            requireActive(user);
            linkGoogleAccount(user, googleId);
        } else {
            if (request.getRole() == null) {
                // Tài khoản mới, chưa biết đăng ký vai trò gì -> để Frontend hỏi rồi gọi lại
                return new GoogleAuthResponse(true, email, fullName, null);
            }
            if (request.getRole() == UserRole.ADMIN) {
                throw new IllegalArgumentException("Không thể đăng ký tài khoản với vai trò này");
            }
            if (request.getRole() == UserRole.AGENT && request.getAgentProfile() == null) {
                throw new IllegalArgumentException("Thông tin hồ sơ Agent là bắt buộc khi đăng ký làm Agent");
            }

            user = new User();
            user.setFullName(fullName);
            user.setEmail(email);
            user.setPasswordHash(null);
            user.setRole(request.getRole());
            user.setEmailVerified(true); // Google đã xác minh quyền sở hữu email
            user.setGoogleId(googleId);
            user = userRepository.save(user);

            if (request.getRole() == UserRole.AGENT) {
                createAgentProfile(user, request.getAgentProfile());
            }
        }

        return new GoogleAuthResponse(false, email, fullName, issueTokens(user));
    }

    private void linkGoogleAccount(User user, String googleId) {
        if (user.getGoogleId() != null) {
            if (!user.getGoogleId().equals(googleId)) {
                throw new InvalidCredentialsException("Email này đã liên kết với một tài khoản Google khác");
            }
            return;
        }
        if (!user.isEmailVerified()) {
            // Tài khoản được tạo bằng mật khẩu nhưng chưa ai xác minh email:
            // có thể kẻ xấu đã đăng ký trước bằng email của nạn nhân -> xóa mật khẩu của hắn.
            user.setPasswordHash(null);
            user.setEmailVerified(true);
            clearOtp(user);
        }
        user.setGoogleId(googleId);
        userRepository.save(user);
    }

    private GoogleIdToken.Payload verifyGoogleIdToken(String idTokenString) {
        try {
            GoogleIdToken idToken = googleIdTokenVerifier.verify(idTokenString);
            if (idToken == null) {
                throw new InvalidCredentialsException("Google ID token không hợp lệ hoặc đã hết hạn");
            }
            return idToken.getPayload();
        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            throw new InvalidCredentialsException("Không thể xác minh Google ID token");
        }
    }

    // ===================== REFRESH TOKEN =====================

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public TokenPairResponse refreshAccessToken(String refreshTokenValue) {
        RefreshToken stored = refreshTokenRepository.findByToken(HashUtil.sha256(refreshTokenValue))
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token không hợp lệ"));

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidRefreshTokenException("Refresh token đã hết hạn");
        }

        User user = stored.getUser();
        if (!user.isActive()) {
            throw new InvalidRefreshTokenException("Tài khoản đã bị vô hiệu hóa");
        }
        if (refreshTokenRepository.revokeIfActive(stored.getId()) == 0) {
            // Token này đã được dùng trước đó -> có khả năng bị đánh cắp -> hủy mọi phiên của user
            refreshTokenRepository.revokeAllByUserId(user.getId());
            log.warn("Phát hiện refresh token bị dùng lại, userId={}", user.getId());
            throw new InvalidRefreshTokenException("Phiên đăng nhập không hợp lệ, vui lòng đăng nhập lại");
        }

        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        return new TokenPairResponse(newAccessToken, createRefreshToken(user));
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByToken(HashUtil.sha256(refreshTokenValue))
                .ifPresent(rt -> refreshTokenRepository.revokeIfActive(rt.getId()));
    }

    private String createRefreshToken(User user) {
        String rawToken = generateSecureTokenValue();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(HashUtil.sha256(rawToken)); // DB chỉ lưu bản băm
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000));
        refreshTokenRepository.save(refreshToken);
        return rawToken; // bản gốc chỉ trả cho client
    }

    private String generateSecureTokenValue() {
        byte[] randomBytes = new byte[64];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    // ===================== ĐỔI MẬT KHẨU (khi đã đăng nhập) =====================

    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));

        if (user.getPasswordHash() == null) {
            throw new IllegalStateException(
                    "Tài khoản đăng nhập bằng Google chưa có mật khẩu. Hãy dùng \"Quên mật khẩu\" để tạo mật khẩu.");
        }
        // Trả 400 thay vì 401 để Frontend không hiểu nhầm là token hết hạn
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu cũ không đúng");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu cũ");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        refreshTokenRepository.revokeAllByUserId(user.getId());
    }

    // ===================== MỞ KHÓA TÀI KHOẢN =====================

    @Transactional
    public void resendUnlockOtp(String email) {
        Optional<User> found = userRepository.findByEmailForUpdate(email);
        if (found.isEmpty() || !isLocked(found.get())) {
            return;
        }

        User user = found.get();
        ensureOtpCooldownPassed(user);
        String otp = issueOtp(user, OtpPurpose.UNLOCK);
        userRepository.save(user);

        eventPublisher.publishEvent(new OtpEmailEvent(user.getEmail(), otp, OtpPurpose.UNLOCK));
    }

    @Transactional(noRollbackFor = InvalidOtpException.class)
    public void unlockAccount(String email, String otpInput) {
        User user = userRepository.findByEmailForUpdate(email)
                .orElseThrow(() -> new InvalidOtpException("Mã OTP không hợp lệ hoặc đã hết hạn"));

        validateOtp(user, otpInput, OtpPurpose.UNLOCK);

        user.setLockedUntil(null);
        user.setFailedLoginAttempts(0);
        clearOtp(user);
        userRepository.save(user);
    }

    // ===================== QUÊN MẬT KHẨU =====================

    @Transactional
    public void forgotPassword(String email) {
        Optional<User> found = userRepository.findByEmailForUpdate(email);
        if (found.isEmpty()) {
            return; // luôn trả 200 để không lộ email nào đã đăng ký
        }

        User user = found.get();
        ensureOtpCooldownPassed(user);
        String otp = issueOtp(user, OtpPurpose.RESET_PASSWORD);
        userRepository.save(user);

        eventPublisher.publishEvent(new OtpEmailEvent(user.getEmail(), otp, OtpPurpose.RESET_PASSWORD));
    }

    @Transactional(noRollbackFor = InvalidOtpException.class)
    public void resetPassword(String email, String otpInput, String newPassword) {
        User user = userRepository.findByEmailForUpdate(email)
                .orElseThrow(() -> new InvalidOtpException("Mã OTP không hợp lệ hoặc đã hết hạn"));

        validateOtp(user, otpInput, OtpPurpose.RESET_PASSWORD);

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setEmailVerified(true); // nhận được OTP qua email = đã chứng minh sở hữu email
        user.setLockedUntil(null);
        user.setFailedLoginAttempts(0);
        clearOtp(user);
        userRepository.save(user);
        refreshTokenRepository.revokeAllByUserId(user.getId());
    }

    // ===================== TIỆN ÍCH OTP =====================

    /** Tạo OTP mới, lưu bản băm vào user, trả về OTP gốc để gửi email. */
    private String issueOtp(User user, OtpPurpose purpose) {
        String otp = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        user.setOtpHash(HashUtil.sha256(otp));
        user.setOtpPurpose(purpose);
        user.setOtpAttempts(0);
        user.setOtpSentAt(LocalDateTime.now());
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(OTP_VALID_MINUTES));
        return otp;
    }

    private void ensureOtpCooldownPassed(User user) {
        if (user.getOtpSentAt() != null
                && user.getOtpSentAt().plusSeconds(OTP_RESEND_COOLDOWN_SECONDS).isAfter(LocalDateTime.now())) {
            throw new TooManyRequestsException(
                    "Vui lòng đợi " + OTP_RESEND_COOLDOWN_SECONDS + " giây trước khi yêu cầu mã mới");
        }
    }

    private void validateOtp(User user, String otpInput, OtpPurpose purpose) {
        boolean usable = user.getOtpHash() != null
                && user.getOtpPurpose() == purpose
                && user.getOtpExpiresAt() != null
                && user.getOtpExpiresAt().isAfter(LocalDateTime.now())
                && user.getOtpAttempts() < MAX_OTP_ATTEMPTS;
        if (!usable) {
            throw new InvalidOtpException("Mã OTP không hợp lệ hoặc đã hết hạn, vui lòng yêu cầu mã mới");
        }

        if (!HashUtil.constantTimeEquals(user.getOtpHash(), HashUtil.sha256(otpInput))) {
            user.setOtpAttempts(user.getOtpAttempts() + 1);
            userRepository.save(user);
            int remaining = MAX_OTP_ATTEMPTS - user.getOtpAttempts();
            throw new InvalidOtpException(remaining > 0
                    ? "Mã OTP không đúng, còn " + remaining + " lần thử"
                    : "Nhập sai quá nhiều lần, vui lòng yêu cầu mã mới");
        }
    }

    private void clearOtp(User user) {
        user.setOtpHash(null);
        user.setOtpPurpose(null);
        user.setOtpExpiresAt(null);
        user.setOtpAttempts(0);
    }

    private static void requireActive(User user) {
        if (!user.isActive()) {
            throw new AccountDisabledException("Tài khoản đã bị vô hiệu hóa. Lý do: " + user.getDeactivatedReason());
        }
    }

    private boolean isLocked(User user) {
        return user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now());
    }
}
