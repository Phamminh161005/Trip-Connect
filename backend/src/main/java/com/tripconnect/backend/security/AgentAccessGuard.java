package com.tripconnect.backend.security;

import com.tripconnect.backend.entity.AgentProfile;
import com.tripconnect.backend.enums.AgentStatus;
import com.tripconnect.backend.exception.ForbiddenException;
import com.tripconnect.backend.repository.AgentProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Chặn các chức năng chỉ dành cho Agent ĐÃ ĐƯỢC DUYỆT (đăng bán tour, nhận yêu cầu tư vấn...).
 * Kiểm tra từ DB chứ không dựa vào token, vì trạng thái hồ sơ có thể đổi sau khi token được cấp.
 */
@Component
@RequiredArgsConstructor
public class AgentAccessGuard {

    private final AgentProfileRepository agentProfileRepository;

    public AgentProfile requireApprovedAgent(Long userId) {
        AgentProfile profile = agentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ForbiddenException("Chức năng này chỉ dành cho Agent"));
        if (profile.getStatus() != AgentStatus.APPROVED) {
            throw new ForbiddenException("Hồ sơ Agent của bạn chưa được duyệt nên chưa thể dùng chức năng này");
        }
        return profile;
    }
}
