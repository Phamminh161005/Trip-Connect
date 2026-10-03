package com.tripconnect.backend.entity;

import com.tripconnect.backend.enums.ChangeRequestStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Yêu cầu cập nhật thông tin pháp lý của Agent ĐÃ được duyệt.
 * Trong lúc chờ Admin duyệt, Agent vẫn hoạt động bình thường với thông tin cũ.
 * Trường nào = null nghĩa là không thay đổi trường đó.
 */
@Entity
@Table(name = "agent_profile_change_requests")
@Getter
@Setter
@NoArgsConstructor
public class AgentProfileChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_profile_id", nullable = false)
    private AgentProfile agentProfile;

    private String companyName;

    private String taxCode;

    private String businessLicense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_province_id")
    private Location addressProvince;

    @Column(length = 500)
    private String address;

    // Đổi ngân hàng: 3 trường luôn đi cùng nhau (cả 3 = null nghĩa là không đổi tài khoản ngân hàng)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_bin")
    private Bank bank;

    @Column(length = 19)
    private String bankAccountNumber;

    private String bankAccountHolder;

    /** Ghi chú của Agent cho Admin (lý do thay đổi). Không tính là một "thay đổi". */
    @Column(columnDefinition = "TEXT")
    private String note;

    public boolean changesBankAccount() {
        return bank != null;
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChangeRequestStatus status;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    private LocalDateTime reviewedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ChangeRequestStatus.PENDING;
        }
    }
}
