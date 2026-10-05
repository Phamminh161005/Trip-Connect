package com.tripconnect.backend.entity;

import com.tripconnect.backend.enums.AccommodationType;
import com.tripconnect.backend.enums.ProposalStatus;
import com.tripconnect.backend.enums.TransportMode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Một phiên bản đề xuất (lịch trình + báo giá) Agent gửi cho yêu cầu tour riêng. Gửi rồi thì không sửa nội dung. */
@Entity
@Table(name = "custom_proposals")
@Getter
@Setter
@NoArgsConstructor
public class CustomProposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private CustomRequest request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private User agent;

    /** 1, 2, 3... theo từng Agent. */
    @Column(nullable = false)
    private short versionNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProposalStatus status;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private short durationDays;

    @Column(nullable = false)
    private short durationNights;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccommodationType accommodationType;

    @ElementCollection
    @CollectionTable(name = "custom_proposal_transport_modes", joinColumns = @JoinColumn(name = "proposal_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 20)
    private Set<TransportMode> transportModes = new HashSet<>();

    @Column(nullable = false)
    private String meetingPoint;

    @Column(nullable = false)
    private LocalTime meetingTime;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "included_services", nullable = false, columnDefinition = "text[]")
    private List<String> includedServices = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "excluded_services", nullable = false, columnDefinition = "text[]")
    private List<String> excludedServices = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayNumber ASC")
    private List<CustomProposalDay> days = new ArrayList<>();

    @Column(nullable = false)
    private long adultPrice;

    @Column(nullable = false)
    private long childPrice;

    @Column(nullable = false)
    private long totalPrice;

    @Column(nullable = false)
    private long depositAmount;

    @Column(columnDefinition = "TEXT")
    private String agentMessage;

    @Column(columnDefinition = "TEXT")
    private String customerFeedback;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean expiryReminded;

    private LocalDateTime respondedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
