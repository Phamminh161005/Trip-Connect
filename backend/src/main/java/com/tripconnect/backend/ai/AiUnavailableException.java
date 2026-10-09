package com.tripconnect.backend.ai;

/** Mô hình AI không phục vụ được lúc này (quá tải, hết hạn mức miễn phí, lỗi mạng, chưa cấu hình). */
public class AiUnavailableException extends RuntimeException {

    /** Hết hạn mức (429) — khác với quá tải tạm thời. */
    private final boolean quotaExceeded;

    public AiUnavailableException(String message, boolean quotaExceeded) {
        super(message);
        this.quotaExceeded = quotaExceeded;
    }

    public boolean isQuotaExceeded() {
        return quotaExceeded;
    }
}
