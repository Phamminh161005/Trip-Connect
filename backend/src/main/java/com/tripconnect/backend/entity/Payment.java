package com.tripconnect.backend.entity;

import com.tripconnect.backend.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Một lần chuyển khách sang VNPay thanh toán (khách có thể thanh toán lại -> nhiều dòng cho một đơn). */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    /** vnp_TxnRef — duy nhất. */
    @Column(nullable = false, unique = true, length = 40)
    private String txnRef;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    /** vnp_CreateDate đã gửi (yyyyMMddHHmmss, giờ Việt Nam) — VNPay yêu cầu khi tra cứu / hoàn tiền. */
    @Column(nullable = false, length = 14)
    private String vnpCreateDate;

    @Column(length = 30)
    private String vnpTransactionNo;

    @Column(length = 5)
    private String vnpResponseCode;

    @Column(length = 20)
    private String bankCode;

    @Column(length = 14)
    private String payDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

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
