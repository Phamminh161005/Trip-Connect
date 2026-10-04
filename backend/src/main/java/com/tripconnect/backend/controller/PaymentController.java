package com.tripconnect.backend.controller;

import com.tripconnect.backend.dto.booking.BookingResponses;
import com.tripconnect.backend.service.booking.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Nhận kết quả thanh toán từ VNPay — không cần đăng nhập (tin cậy nhờ chữ ký HMAC, không nhờ phiên đăng nhập).
 *  - /return: Frontend chuyển nguyên các tham số VNPay gắn trên link quay về.
 *  - /ipn: máy chủ VNPay gọi thẳng (cần địa chỉ public; chạy trên máy thì dùng ngrok).
 */
@RestController
@RequestMapping("/api/payments/vnpay")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/return")
    public BookingResponses.PaymentResult handleReturn(@RequestParam Map<String, String> params) {
        return paymentService.handleReturn(params);
    }

    /** Trả lời đúng định dạng VNPay yêu cầu, vd RspCode=00, Message=Confirm Success. */
    @GetMapping("/ipn")
    public Map<String, String> handleIpn(@RequestParam Map<String, String> params) {
        PaymentService.Outcome outcome = paymentService.handleIpn(params);
        return Map.of("RspCode", outcome.rspCode(), "Message", outcome.message());
    }
}
