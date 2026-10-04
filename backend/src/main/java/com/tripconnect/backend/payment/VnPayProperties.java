package com.tripconnect.backend.payment;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Cấu hình VNPay (application.properties + .env). Thiếu mã TMN / chuỗi bí mật thì chức năng thanh toán báo lỗi rõ ràng. */
@Getter
@Component
public class VnPayProperties {

    private final String tmnCode;
    private final String hashSecret;
    private final String payUrl;
    private final String apiUrl;
    /** Trang Frontend nhận kết quả sau khi khách thanh toán xong. */
    private final String returnUrl;

    public VnPayProperties(@Value("${vnpay.tmn-code}") String tmnCode,
                           @Value("${vnpay.hash-secret}") String hashSecret,
                           @Value("${vnpay.pay-url}") String payUrl,
                           @Value("${vnpay.api-url}") String apiUrl,
                           @Value("${app.frontend-url}") String frontendUrl) {
        this.tmnCode = tmnCode == null ? "" : tmnCode.trim();
        this.hashSecret = hashSecret == null ? "" : hashSecret.trim();
        this.payUrl = payUrl;
        this.apiUrl = apiUrl;
        this.returnUrl = frontendUrl.replaceAll("/+$", "") + "/payment/vnpay-return";
    }

    public boolean isConfigured() {
        return !tmnCode.isEmpty() && !hashSecret.isEmpty();
    }
}
