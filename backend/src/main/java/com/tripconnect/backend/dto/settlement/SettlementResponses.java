package com.tripconnect.backend.dto.settlement;

import com.tripconnect.backend.enums.SettlementDisputeStatus;
import com.tripconnect.backend.enums.SettlementItemKind;
import com.tripconnect.backend.enums.SettlementStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class SettlementResponses {

    private SettlementResponses() {
    }

    /** @param periodLabel vd "Tháng 10/2026" hoặc "Đến 07/10/2026 15:30" (Admin lập giữa tháng) */
    public record Summary(Long id, String code, Long agentId, String agentName, String periodLabel, LocalDateTime cutoffAt,
                          SettlementStatus status, int itemCount, long retainedAmount, long commissionAmount,
                          long adjustmentAmount, long payoutAmount, LocalDateTime confirmDeadline, LocalDateTime paidAt,
                          LocalDateTime createdAt) {
    }

    public record Item(Long id, Long bookingId, String bookingCode, String tourTitle, LocalDate startDate, String customerName,
                       SettlementItemKind kind, LocalDateTime eventAt, long paidAmount, long refundedAmount, long retainedAmount,
                       BigDecimal commissionRate, long commissionAmount, long payoutAmount) {
    }

    public record Adjustment(Long id, long amount, String reason, String createdByName, LocalDateTime createdAt) {
    }

    public record Dispute(Long id, String reason, SettlementDisputeStatus status, String resolution, String resolvedByName,
                          LocalDateTime resolvedAt, LocalDateTime createdAt) {
    }

    /** Tài khoản nhận tiền: chụp lại sau khi trả; trước đó là tài khoản hiện tại trong hồ sơ Agent (null nếu chưa có). */
    public record BankAccount(String bin, String bankName, String accountNumber, String accountHolder) {
    }

    public record Detail(Summary summary, List<Item> items, List<Adjustment> adjustments, List<Dispute> disputes,
                         BankAccount bankAccount, LocalDateTime confirmedAt, boolean autoConfirmed, String paidByName,
                         String transactionRef, boolean hasReceipt,
                         /* Agent */
                         boolean canConfirm, boolean canDispute,
                         /* Admin */
                         boolean canResolve, boolean canPay) {
    }
}
