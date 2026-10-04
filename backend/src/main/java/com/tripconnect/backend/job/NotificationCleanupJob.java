package com.tripconnect.backend.job;

import com.tripconnect.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Thông báo đã đọc quá 60 ngày thì xóa; chưa đọc thì giữ. */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupJob {

    private final NotificationService notificationService;

    // 03:30 mỗi ngày (giờ Việt Nam)
    @Scheduled(cron = "0 30 3 * * *", zone = "Asia/Ho_Chi_Minh")
    public void deleteOldRead() {
        int deleted = notificationService.deleteOldRead();
        if (deleted > 0) {
            log.info("Đã xóa {} thông báo đã đọc lâu", deleted);
        }
    }
}
