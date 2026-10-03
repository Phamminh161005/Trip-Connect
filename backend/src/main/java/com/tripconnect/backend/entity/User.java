package com.tripconnect.backend.entity;

import com.tripconnect.backend.enums.OtpPurpose;
import com.tripconnect.backend.enums.UserRole;
import org.hibernate.annotations.ColumnDefault;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    @Column(unique = true)
    private String googleId;
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    private LocalDateTime lastLoginAt;

    private LocalDateTime createdAt;


    @Column(length = 64)
    private String otpHash;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private OtpPurpose otpPurpose;

    @ColumnDefault("0")
    @Column(nullable = false)
    private int otpAttempts = 0;

    private LocalDateTime otpSentAt;


    private LocalDateTime otpExpiresAt;

    @Column(nullable = false)
    private boolean emailVerified = false;

    @Column(nullable = false)
    private int failedLoginAttempts = 0;

    private LocalDateTime lockedUntil;

    // ===== Admin vô hiệu hóa tài khoản (khác với lockedUntil: không tự mở) =====

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(columnDefinition = "TEXT")
    private String deactivatedReason;

    private LocalDateTime deactivatedAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
