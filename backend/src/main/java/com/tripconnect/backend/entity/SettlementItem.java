package com.tripconnect.backend.entity;

import com.tripconnect.backend.enums.SettlementItemKind;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Một đơn trong bảng đối soát (số tiền chụp lại lúc lập). */
@Entity
@Table(name = "settlement_items")
@Getter
@Setter
@NoArgsConstructor
public class SettlementItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", nullable = false)
    private Settlement settlement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementItemKind kind;

    /** Lúc đơn hoàn thành / bị hủy. */
    @Column(nullable = false)
    private LocalDateTime eventAt;

    @Column(nullable = false)
    private long paidAmount;

    @Column(nullable = false)
    private long refundedAmount;

    @Column(nullable = false)
    private long retainedAmount;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal commissionRate;

    @Column(nullable = false)
    private long commissionAmount;

    @Column(nullable = false)
    private long payoutAmount;
}
