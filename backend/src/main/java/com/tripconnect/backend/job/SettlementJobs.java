package com.tripconnect.backend.job;

import com.tripconnect.backend.service.settlement.SettlementGenerator;
import com.tripconnect.backend.service.settlement.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/** Việc định kỳ của đối soát hoa hồng. */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementJobs {

    private final SettlementGenerator generator;
    private final SettlementService settlementService;
    private final Clock clock;

    /** 03:00 ngày 1 hằng tháng: lập bảng đối soát tháng trước cho từng Agent có phát sinh. */
    @Scheduled(cron = "0 0 3 1 * *", zone = "Asia/Ho_Chi_Minh")
    public void generateMonthly() {
        int created = generator.generate(LocalDate.now(clock).withDayOfMonth(1).atStartOfDay());
        log.info("Đã lập {} bảng đối soát tháng trước", created);
    }

    /** Mỗi giờ (phút 20): Agent quá hạn phản hồi -> tự xác nhận. */
    @Scheduled(cron = "0 20 * * * *", zone = "Asia/Ho_Chi_Minh")
    public void autoConfirm() {
        int confirmed = settlementService.autoConfirmOverdue();
        if (confirmed > 0) log.info("{} bảng đối soát tự xác nhận vì quá hạn", confirmed);
    }
}
