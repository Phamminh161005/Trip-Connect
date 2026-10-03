package com.tripconnect.backend.service;

import com.tripconnect.backend.enums.UserRole;
import com.tripconnect.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEmailListener {

    private final EmailService emailService;
    private final UserRepository userRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserEmail(NotificationEvents.UserEmailEvent event) {
        sendSafely(event.toEmail(), event.email());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAdminEmail(NotificationEvents.AdminEmailEvent event) {
        for (String adminEmail : userRepository.findActiveEmailsByRole(UserRole.ADMIN)) {
            sendSafely(adminEmail, event.email());
        }
    }

    private void sendSafely(String to, EmailTemplates.Email email) {
        try {
            emailService.send(to, email.subject(), email.body());
        } catch (Exception e) {
            log.warn("Gửi email '{}' tới {} thất bại: {}", email.subject(), EmailService.maskEmail(to), e.getMessage());
        }
    }
}
