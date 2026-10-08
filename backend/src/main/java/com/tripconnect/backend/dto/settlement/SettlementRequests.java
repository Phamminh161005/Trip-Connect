package com.tripconnect.backend.dto.settlement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public final class SettlementRequests {

    private SettlementRequests() {
    }

    public record Dispute(
            @NotBlank(message = "Vui lòng ghi rõ đơn nào sai và vì sao")
            @Size(max = 2000, message = "Tối đa 2000 ký tự") String reason) {
    }

    public enum Decision { ADJUST, REJECT }

    /**
     * Admin xử lý khiếu nại: ADJUST (thêm khoản điều chỉnh, gửi Agent xác nhận lại) hoặc REJECT (bác, chuyển sang chờ thanh toán).
     */
    public record Resolve(
            @NotNull(message = "Vui lòng chọn cách xử lý") Decision decision,
            @NotBlank(message = "Vui lòng ghi nội dung trả lời Agent")
            @Size(max = 2000, message = "Tối đa 2000 ký tự") String resolution,
            @Size(max = 20, message = "Tối đa 20 khoản điều chỉnh") List<@Valid AdjustmentInput> adjustments) {
    }

    public record AdjustmentInput(
            @NotNull(message = "Vui lòng nhập số tiền")
            @Min(value = -1_000_000_000, message = "Số tiền không hợp lệ")
            @Max(value = 1_000_000_000, message = "Số tiền không hợp lệ") Long amount,
            @NotBlank(message = "Vui lòng ghi lý do điều chỉnh")
            @Size(max = 500, message = "Tối đa 500 ký tự") String reason) {
    }
}
