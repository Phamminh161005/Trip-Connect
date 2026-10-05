package com.tripconnect.backend.job;

import com.tripconnect.backend.service.customrequest.CustomRequestLifecycle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Việc định kỳ của yêu cầu thiết kế tour riêng. */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomRequestJobs {

    private final CustomRequestLifecycle lifecycle;

    /** Mỗi 10 phút: các hạn tính theo giờ (nhận yêu cầu, gửi đề xuất, khách phản hồi) và nhắc trước hạn. */
    @Scheduled(fixedDelay = 600_000, initialDelay = 60_000)
    public void processDeadlines() {
        report("lần giao yêu cầu hết hạn 48 giờ", lifecycle.expireOverdueAssignments());
        report("yêu cầu chuyển lại vì Agent quá hạn gửi đề xuất", lifecycle.dropOverdueAgents());
        report("đề xuất hết hạn phản hồi", lifecycle.expireUnansweredProposals());
        report("yêu cầu tự đóng vì không có trao đổi", lifecycle.closeInactive());
        report("Agent được nhắc hạn gửi đề xuất", lifecycle.remindAgentsDueSoon());
        report("khách được nhắc phản hồi đề xuất", lifecycle.remindCustomersExpiring());
    }

    /** 02:30 mỗi ngày: chưa chốt được mà sắp tới ngày khởi hành -> đóng yêu cầu, báo khách. */
    @Scheduled(cron = "0 30 2 * * *", zone = "Asia/Ho_Chi_Minh")
    public void closeStaleRequests() {
        report("yêu cầu tự đóng vì sát ngày khởi hành", lifecycle.closeOpenNearStart());
    }

    private static void report(String what, int count) {
        if (count > 0) log.info("{} {}", count, what);
    }
}
