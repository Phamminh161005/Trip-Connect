package com.tripconnect.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Khoản điều chỉnh Admin thêm khi xử lý khiếu nại (dương: cộng cho Agent, âm: trừ bớt). */
@Entity
@Table(name = "settlement_adjustments")
@Getter
@Setter
@NoArgsConstructor
public class SettlementAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", nullable = false)
    private Settlement settlement;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
