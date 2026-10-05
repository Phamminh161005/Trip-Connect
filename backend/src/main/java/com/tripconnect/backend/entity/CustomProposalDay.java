package com.tripconnect.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Lịch trình một ngày trong đề xuất tour riêng. */
@Entity
@Table(name = "custom_proposal_days")
@Getter
@Setter
@NoArgsConstructor
public class CustomProposalDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id", nullable = false)
    private CustomProposal proposal;

    /** Bắt đầu từ 1. */
    @Column(nullable = false)
    private short dayNumber;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean hasBreakfast;

    @Column(nullable = false)
    private boolean hasLunch;

    @Column(nullable = false)
    private boolean hasDinner;

    private String accommodation;
}
