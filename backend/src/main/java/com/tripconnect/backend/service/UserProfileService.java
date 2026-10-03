package com.tripconnect.backend.service;

import com.tripconnect.backend.dto.UpdateMyProfileRequest;
import com.tripconnect.backend.dto.UserProfileResponse;
import com.tripconnect.backend.entity.AgentProfile;
import com.tripconnect.backend.entity.User;
import com.tripconnect.backend.enums.AgentStatus;
import com.tripconnect.backend.enums.UserRole;
import com.tripconnect.backend.exception.ResourceNotFoundException;
import com.tripconnect.backend.repository.AgentProfileRepository;
import com.tripconnect.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Người dùng xem / sửa thông tin cá nhân của chính mình. */
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final AgentProfileRepository agentProfileRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(Long userId) {
        return toResponse(requireUser(userId));
    }

    @Transactional
    public UserProfileResponse updateMyProfile(Long userId, UpdateMyProfileRequest request) {
        User user = requireUser(userId);
        user.setFullName(request.getFullName().trim());
        user.setPhone(request.getPhone().trim());
        return toResponse(user);
    }

    private UserProfileResponse toResponse(User user) {
        AgentStatus agentStatus = user.getRole() != UserRole.AGENT ? null
                : agentProfileRepository.findByUserId(user.getId()).map(AgentProfile::getStatus).orElse(null);
        return new UserProfileResponse(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(),
                user.getRole(), agentStatus);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
    }
}
