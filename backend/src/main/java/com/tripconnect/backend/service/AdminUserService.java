package com.tripconnect.backend.service;

import com.tripconnect.backend.dto.AdminUserResponse;
import com.tripconnect.backend.dto.PageResponse;
import com.tripconnect.backend.entity.AgentProfile;
import com.tripconnect.backend.entity.User;
import com.tripconnect.backend.enums.UserRole;
import com.tripconnect.backend.exception.ResourceNotFoundException;
import com.tripconnect.backend.repository.StablePaging;
import com.tripconnect.backend.repository.AgentProfileRepository;
import com.tripconnect.backend.repository.RefreshTokenRepository;
import com.tripconnect.backend.repository.SearchPatterns;
import com.tripconnect.backend.repository.UserRepository;
import com.tripconnect.backend.security.UserStatusCache;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Admin quản lý người dùng: tìm kiếm, vô hiệu hóa, kích hoạt lại. */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final AgentProfileRepository agentProfileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserStatusCache userStatusCache;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * @param keyword tìm gần đúng theo họ tên hoặc email (không phân biệt hoa thường); null = không lọc
     * @param role    null = mọi vai trò
     * @param active  null = mọi trạng thái
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> search(String keyword, UserRole role, Boolean active, Pageable pageable) {
        Specification<User> spec = (root, query, cb) -> cb.conjunction();

        if (keyword != null && !keyword.isBlank()) {
            String pattern = SearchPatterns.contains(keyword);
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("fullName")), pattern, SearchPatterns.ESCAPE),
                    cb.like(cb.lower(root.get("email")), pattern, SearchPatterns.ESCAPE)));
        }
        if (role != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("role"), role));
        }
        if (active != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), active));
        }

        Page<User> page = userRepository.findAll(spec, StablePaging.of(pageable));

        // Lấy id hồ sơ đối tác của các Agent trong trang này bằng 1 câu query (không query từng dòng)
        List<Long> agentIds = page.getContent().stream()
                .filter(u -> u.getRole() == UserRole.AGENT).map(User::getId).toList();
        Map<Long, Long> profileIdByUserId = agentIds.isEmpty() ? Map.of()
                : agentProfileRepository.findByUserIdIn(agentIds).stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), AgentProfile::getId));

        return PageResponse.from(page, user -> toResponse(user, profileIdByUserId.get(user.getId())));
    }

    @Transactional
    public AdminUserResponse deactivate(Long targetUserId, Long adminId, String reason) {
        User user = requireUser(targetUserId);
        if (user.getId().equals(adminId)) {
            throw new IllegalArgumentException("Không thể tự vô hiệu hóa tài khoản của chính mình");
        }
        if (user.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("Không thể vô hiệu hóa tài khoản Admin");
        }
        if (!user.isActive()) {
            throw new IllegalStateException("Tài khoản đã bị vô hiệu hóa trước đó");
        }

        user.setActive(false);
        user.setDeactivatedReason(reason.trim());
        user.setDeactivatedAt(LocalDateTime.now());
        // Đăng xuất người đó khỏi mọi thiết bị: thu hồi refresh token + bộ lọc JWT chặn access token còn hạn ngay lập tức
        refreshTokenRepository.revokeAllByUserId(user.getId());
        userStatusCache.evict(user.getId());

        eventPublisher.publishEvent(new NotificationEvents.UserEmailEvent(
                user.getEmail(), EmailTemplates.accountDeactivated(reason.trim())));
        return toResponse(user, agentProfileId(user));
    }

    @Transactional
    public AdminUserResponse activate(Long targetUserId) {
        User user = requireUser(targetUserId);
        if (user.isActive()) {
            throw new IllegalStateException("Tài khoản đang hoạt động bình thường");
        }

        user.setActive(true);
        user.setDeactivatedReason(null);
        user.setDeactivatedAt(null);
        userStatusCache.evict(user.getId());

        eventPublisher.publishEvent(new NotificationEvents.UserEmailEvent(
                user.getEmail(), EmailTemplates.accountReactivated()));
        return toResponse(user, agentProfileId(user));
    }

    private Long agentProfileId(User user) {
        if (user.getRole() != UserRole.AGENT) return null;
        return agentProfileRepository.findByUserId(user.getId()).map(AgentProfile::getId).orElse(null);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng id=" + userId));
    }

    static AdminUserResponse toResponse(User user, Long agentProfileId) {
        return new AdminUserResponse(
                user.getId(), user.getFullName(), user.getEmail(), user.getPhone(), user.getRole(),
                user.isActive(), user.isEmailVerified(), user.getGoogleId() != null,
                user.getDeactivatedReason(), user.getDeactivatedAt(), user.getLastLoginAt(), user.getCreatedAt(),
                agentProfileId
        );
    }
}
