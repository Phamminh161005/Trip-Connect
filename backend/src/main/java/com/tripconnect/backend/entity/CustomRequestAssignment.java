package com.tripconnect.backend.entity;

import com.tripconnect.backend.enums.AssignmentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Một lần Admin giao yêu cầu cho Agent và kết quả (nhận / từ chối / hết hạn). */
@Entity
@Table(name = "custom_request_assignments")
@Getter
@Setter
@NoArgsConstructor
public class CustomRequestAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private CustomRequest request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private User agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by", nullable = false)
    private User assignedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssignmentStatus status;

    @Column(precision = 5, scale = 1)
    private BigDecimal matchScore;

    @Column(nullable = false)
    private LocalDateTime deadline;

    @Column(columnDefinition = "TEXT")
    private String declineReason;

    private LocalDateTime respondedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
