package com.tripconnect.backend.job;

import com.tripconnect.backend.repository.SearchLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Lịch sử tìm kiếm chỉ giữ 180 ngày (không lưu dữ liệu hành vi lâu hơn mức cần cho gợi ý tour). */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchLogCleanupJob {

    static final int RETENTION_DAYS = 180;

    private final SearchLogRepository searchLogRepository;

    // 03:00 mỗi ngày (giờ Việt Nam)
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void deleteOldLogs() {
        int deleted = searchLogRepository.deleteOlderThan(LocalDateTime.now().minusDays(RETENTION_DAYS));
        if (deleted > 0) {
            log.info("Đã xóa {} lượt tìm kiếm cũ hơn {} ngày", deleted, RETENTION_DAYS);
        }
    }
}
