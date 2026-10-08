package com.tripconnect.backend.entity;

import com.tripconnect.backend.enums.SettlementDisputeStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Khiếu nại của Agent về một bảng đối soát và cách Admin xử lý. */
@Entity
@Table(name = "settlement_disputes")
@Getter
@Setter
@NoArgsConstructor
public class SettlementDispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", nullable = false)
    private Settlement settlement;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementDisputeStatus status;

    @Column(columnDefinition = "TEXT")
    private String resolution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    private LocalDateTime resolvedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
