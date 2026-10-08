package com.tripconnect.backend.service.settlement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Các quy tắc tính toán thuần của đối soát. */
public final class SettlementRules {

    private SettlementRules() {
    }

    /** Agent có ngần này ngày để xác nhận / khiếu nại, quá hạn tự xác nhận. */
    public static final int CONFIRM_DAYS = 5;

    public record Amounts(long retained, long commission, long payout) {
    }

    /** Tiền giữ lại = đã thu - đã hoàn; hoa hồng theo tỷ lệ chụp trong đơn (làm tròn đến đồng); Agent nhận phần còn lại. */
    public static Amounts split(long paid, long refunded, BigDecimal commissionRate) {
        long retained = Math.max(0, paid - refunded);
        long commission = BigDecimal.valueOf(retained).multiply(commissionRate).setScale(0, RoundingMode.HALF_UP).longValue();
        return new Amounts(retained, commission, retained - commission);
    }

    /** Chốt đúng 0h ngày đầu tháng -> "Tháng 10/2026" (tháng trước đó); lập giữa chừng -> "Đến 07/10/2026 15:30". */
    public static String periodLabel(LocalDateTime cutoff) {
        if (cutoff.getDayOfMonth() == 1 && cutoff.toLocalTime().equals(java.time.LocalTime.MIDNIGHT)) {
            return "tháng " + cutoff.minusDays(1).format(DateTimeFormatter.ofPattern("MM/yyyy"));
        }
        return "đến " + cutoff.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
    }
}
