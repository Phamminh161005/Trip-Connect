package com.tripconnect.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    // Spring ném lỗi này khi email ĐÃ gửi xong, chỉ lỗi ở bước đóng kết nối với máy chủ SMTP
    private static final String CLOSE_CONNECTION_FAILURE = "Failed to close server connection after message sending";

    private final JavaMailSender mailSender;

    public void sendOtpEmail(String toEmail, String otp) {
        send(toEmail, "Mã xác thực tài khoản TripConnect",
                "Mã OTP của bạn là: " + otp + "\nMã có hiệu lực trong 5 phút.");
    }

    public void sendPasswordResetEmail(String toEmail, String otp) {
        send(toEmail, "Đặt lại mật khẩu TripConnect",
                "Mã OTP đặt lại mật khẩu của bạn là: " + otp
                        + "\nMã có hiệu lực trong 5 phút. Nếu bạn không yêu cầu, vui lòng bỏ qua email này.");
    }

    public void sendAccountUnlockEmail(String toEmail, String otp) {
        send(toEmail, "Mã OTP mở khóa tài khoản TripConnect",
                "Mã OTP mở khóa tài khoản của bạn là: " + otp + "\nMã có hiệu lực trong 5 phút.");
    }

    /** Gửi email văn bản thường. Nội dung các thông báo nghiệp vụ nằm ở {@link EmailTemplates}. */
    public void send(String toEmail, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(text);

        try {
            mailSender.send(message);
            log.info("Đã gửi email '{}' tới {}", subject, maskEmail(toEmail));
        } catch (MailSendException e) {
            if (e.getMessage() != null && e.getMessage().startsWith(CLOSE_CONNECTION_FAILURE)) {
                log.warn("Đã gửi email '{}' tới {} nhưng đóng kết nối SMTP bị lỗi (không ảnh hưởng người nhận)",
                        subject, maskEmail(toEmail));
                return;
            }
            throw e;
        }
    }

    /** Che bớt email khi ghi log để log không chứa đầy đủ dữ liệu cá nhân: nguyenvana@gmail.com -> n***@gmail.com */
    static String maskEmail(String email) {
        if (email == null) return null;
        int at = email.indexOf('@');
        if (at <= 0) return "***";
        return email.charAt(0) + "***" + email.substring(at);
    }
}
