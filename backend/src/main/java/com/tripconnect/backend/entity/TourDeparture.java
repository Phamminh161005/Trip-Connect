package com.tripconnect.backend.entity;

import com.tripconnect.backend.enums.DepartureStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Một lịch khởi hành của tour: ngày đi, số chỗ, giá (VNĐ). */
@Entity
@Table(name = "tour_departures")
@Getter
@Setter
@NoArgsConstructor
public class TourDeparture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private int capacity;

    /** Giá người lớn, VNĐ. */
    @Column(nullable = false)
    private long adultPrice;

    /** Giá trẻ em 2-11 tuổi, VNĐ (em bé dưới 2 tuổi miễn phí). */
    @Column(nullable = false)
    private long childPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DepartureStatus status;

    @Column(columnDefinition = "TEXT")
    private String cancelReason;

    /** Lần nhắc gần nhất đã gửi đơn vị tổ chức (danh sách khách): 0 chưa, 1 trước 3 ngày, 2 trước 1 ngày. */
    @Column(nullable = false)
    private short organizerReminderStage;

    /** Đã nhắc đơn vị tổ chức lịch này ít khách (trước 7 ngày) chưa. */
    @Column(nullable = false)
    private boolean lowBookingReminded;

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

    /** Ngày về = ngày đi + (số ngày của tour - 1). */
    public LocalDate endDate(int durationDays) {
        return startDate.plusDays(durationDays - 1L);
    }
}
