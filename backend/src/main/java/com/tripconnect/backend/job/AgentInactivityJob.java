package com.tripconnect.backend.job;

import com.tripconnect.backend.repository.AgentProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Nghiệp vụ 1.6: tự tắt "Đang nhận yêu cầu" nếu Agent không đăng nhập quá 14 ngày,
 * để hệ thống không phân bổ yêu cầu cho Agent không còn hoạt động.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentInactivityJob {

    static final int INACTIVE_DAYS = 14;

    private final AgentProfileRepository agentProfileRepository;

    // Chạy lúc 02:00 mỗi ngày (giờ Việt Nam) — thời điểm ít người dùng
    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void disableInactiveAgents() {
        int affected = agentProfileRepository.disableAcceptingForInactiveAgents(
                LocalDateTime.now().minusDays(INACTIVE_DAYS));
        if (affected > 0) {
            log.info("Đã tắt 'Đang nhận yêu cầu' của {} Agent không đăng nhập quá {} ngày", affected, INACTIVE_DAYS);
        }
    }
}
