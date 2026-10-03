package com.tripconnect.backend.entity;

import com.tripconnect.backend.enums.AgentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "agent_profiles")
@Getter
@Setter
@NoArgsConstructor
public class AgentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String companyName;

    private String taxCode;

    /** Số giấy phép kinh doanh lữ hành (file giấy tờ nằm ở bảng agent_documents). */
    private String businessLicense;

    /** Tỉnh/Thành của trụ sở (chọn từ danh mục địa điểm). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_province_id")
    private Location addressProvince;

    /** Địa chỉ chi tiết: số nhà, đường, phường/xã. */
    @Column(length = 500)
    private String address;

    // ----- Tài khoản ngân hàng nhận thanh toán (tách riêng để tạo được mã QR VietQR khi quyết toán) -----

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_bin")
    private Bank bank;

    @Column(length = 19)
    private String bankAccountNumber;

    /** Chữ in hoa không dấu, giống cách ngân hàng hiển thị. */
    private String bankAccountHolder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgentStatus status;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    // Chỉ bật khi hồ sơ được duyệt; tự tắt nếu Agent không đăng nhập quá 14 ngày
    @Column(nullable = false)
    private boolean isAcceptingRequests = false;

    @Column(nullable = false)
    private int maxOpenRequests = 5;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    @Column(nullable = false)
    private int ratingCount = 0;

    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.status == null) {
            this.status = AgentStatus.DRAFT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /** Agent chỉ được tự sửa thông tin pháp lý khi hồ sơ chưa được duyệt và không đang chờ duyệt. */
    public boolean isEditable() {
        return status == AgentStatus.DRAFT || status == AgentStatus.NEEDS_REVISION;
    }
}
