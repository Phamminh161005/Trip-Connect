package com.tripconnect.backend.entity;

import com.tripconnect.backend.enums.AccommodationType;
import com.tripconnect.backend.enums.CustomRequestStatus;
import com.tripconnect.backend.enums.TransportMode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/** Yêu cầu thiết kế tour riêng của khách. Vòng đời: xem {@link CustomRequestStatus}. */
@Entity
@Table(name = "custom_requests")
@Getter
@Setter
@NoArgsConstructor
public class CustomRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departure_location_id", nullable = false)
    private Location departureLocation;

    @ManyToMany
    @JoinTable(name = "custom_request_destinations",
            joinColumns = @JoinColumn(name = "request_id"),
            inverseJoinColumns = @JoinColumn(name = "location_id"))
    private Set<Location> destinations = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "custom_request_categories",
            joinColumns = @JoinColumn(name = "request_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<TourCategory> categories = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "custom_request_transport_modes", joinColumns = @JoinColumn(name = "request_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 20)
    private Set<TransportMode> transportModes = new HashSet<>();

    /** Khoảng ngày khách có thể khởi hành. */
    @Column(nullable = false)
    private LocalDate earliestStart;

    @Column(nullable = false)
    private LocalDate latestStart;

    @Column(nullable = false)
    private short durationDays;

    @Column(nullable = false)
    private short adults;

    @Column(nullable = false)
    private short children;

    @Column(nullable = false)
    private short infants;

    /** Ngân sách mỗi người, VNĐ (không bắt buộc). */
    private Long budgetMin;

    private Long budgetMax;

    /** null = không yêu cầu */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AccommodationType accommodationType;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CustomRequestStatus status;

    /** Agent đang được giao / đang xử lý. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private User agent;

    private LocalDateTime assignedAt;

    private LocalDateTime acceptedAt;

    /** Hạn Agent gửi đề xuất tiếp theo; null khi đang chờ khách phản hồi. */
    private LocalDateTime proposalDeadline;

    /** Số lần khách đã yêu cầu chỉnh sửa (với Agent hiện tại). */
    @Column(nullable = false)
    private short revisionCount;

    @Column(nullable = false)
    private boolean deadlineReminded;

    /** Lần trao đổi gần nhất (nhận, gửi đề xuất, yêu cầu chỉnh sửa) — để tự đóng yêu cầu bị bỏ dở. */
    private LocalDateTime lastActivityAt;

    private LocalDateTime agreedAt;

    private LocalDateTime closedAt;

    @Column(columnDefinition = "TEXT")
    private String closedReason;

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

    public int travellers() {
        return adults + children + infants;
    }
}
