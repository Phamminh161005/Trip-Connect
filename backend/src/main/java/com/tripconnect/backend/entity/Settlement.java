package com.tripconnect.backend.entity;

import com.tripconnect.backend.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Bảng đối soát với một Agent: các đơn chưa đối soát phát sinh trước cutoffAt. Vòng đời: {@link SettlementStatus}. */
@Entity
@Table(name = "settlements")
@Getter
@Setter
@NoArgsConstructor
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** vd DS2610000123 */
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private User agent;

    @Column(nullable = false)
    private LocalDateTime cutoffAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementStatus status;

    @Column(nullable = false)
    private int itemCount;

    /** Tổng tiền TripConnect giữ của các đơn. */
    @Column(nullable = false)
    private long retainedAmount;

    @Column(nullable = false)
    private long commissionAmount;

    /** Tổng các khoản điều chỉnh khi xử lý khiếu nại (âm = trừ bớt). */
    @Column(nullable = false)
    private long adjustmentAmount;

    /** Agent thực nhận = giữ lại - hoa hồng + điều chỉnh. */
    @Column(nullable = false)
    private long payoutAmount;

    private LocalDateTime confirmDeadline;

    private LocalDateTime confirmedAt;

    @Column(nullable = false)
    private boolean autoConfirmed;

    private LocalDateTime paidAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_by")
    private User paidBy;

    // ----- Tài khoản nhận tiền, chụp lại lúc trả -----

    @Column(length = 6)
    private String bankBin;

    private String bankName;

    @Column(length = 19)
    private String bankAccountNumber;

    private String bankAccountHolder;

    /** Mã giao dịch ngân hàng Admin nhập khi chuyển khoản. */
    @Column(length = 100)
    private String transactionRef;

    /** Ảnh biên lai (không bắt buộc, lưu riêng tư). */
    private String receiptPublicId;

    @Column(length = 20)
    private String receiptFormat;

    @OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("eventAt ASC, id ASC")
    private List<SettlementItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<SettlementAdjustment> adjustments = new ArrayList<>();

    @OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<SettlementDispute> disputes = new ArrayList<>();

    @Version
    private long version;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /** Tính lại các tổng từ dòng đối soát và khoản điều chỉnh. */
    public void recalculate() {
        itemCount = items.size();
        retainedAmount = items.stream().mapToLong(SettlementItem::getRetainedAmount).sum();
        commissionAmount = items.stream().mapToLong(SettlementItem::getCommissionAmount).sum();
        adjustmentAmount = adjustments.stream().mapToLong(SettlementAdjustment::getAmount).sum();
        payoutAmount = retainedAmount - commissionAmount + adjustmentAmount;
    }
}
