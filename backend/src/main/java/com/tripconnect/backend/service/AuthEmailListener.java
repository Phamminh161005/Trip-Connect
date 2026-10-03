package com.tripconnect.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Gửi email OTP ở luồng riêng, chỉ sau khi transaction đã commit thành công.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEmailListener {

    private final EmailService emailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOtpEmail(OtpEmailEvent event) {
        try {
            switch (event.purpose()) {
                case REGISTER -> emailService.sendOtpEmail(event.email(), event.otp());
                case UNLOCK -> emailService.sendAccountUnlockEmail(event.email(), event.otp());
                case RESET_PASSWORD -> emailService.sendPasswordResetEmail(event.email(), event.otp());
            }
        } catch (Exception e) {
            log.warn("Gửi email OTP ({}) tới {} thất bại: {}", event.purpose(),
                    EmailService.maskEmail(event.email()), e.getMessage());
        }
    }
}
