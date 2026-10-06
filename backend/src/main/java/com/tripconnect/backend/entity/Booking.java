package com.tripconnect.backend.entity;

import com.tripconnect.backend.enums.BookingStatus;
import com.tripconnect.backend.enums.CancelledBy;
import com.tripconnect.backend.enums.RefundStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Đơn đặt tour cho một lịch khởi hành. Giá, hoa hồng, chính sách hoàn tiền được chụp lại lúc đặt.
 * Vòng đời: xem {@link BookingStatus}.
 */
@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mã đơn hiển thị cho khách, vd TC261003482915. */
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departure_id", nullable = false)
    private TourDeparture departure;

    /** Agent tổ chức tour lúc đặt; null = tour của TripConnect. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private User agent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Column(nullable = false)
    private short adults;

    @Column(nullable = false)
    private short children;

    @Column(nullable = false)
    private short infants;

    @Column(nullable = false)
    private long adultPrice;

    @Column(nullable = false)
    private long childPrice;

    @Column(nullable = false)
    private long totalAmount;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal commissionRate;

    @Column(nullable = false)
    private long commissionAmount;

    @Column(nullable = false, length = 100)
    private String contactName;

    @Column(nullable = false, length = 20)
    private String contactPhone;

    @Column(nullable = false, length = 254)
    private String contactEmail;

    @Column(columnDefinition = "TEXT")
    private String note;

    // ----- Chính sách hoàn tiền áp dụng cho đơn này -----

    @Column(nullable = false)
    private short refundFullDays;

    @Column(nullable = false)
    private short refundPartialDays;

    @Column(nullable = false)
    private short refundPartialPercent;

    // ----- Mốc thời gian -----

    @Column(nullable = false)
    private LocalDateTime holdExpiresAt;

    private LocalDateTime paidAt;

    private LocalDateTime completedAt;

    private LocalDateTime cancelledAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CancelledBy cancelledBy;

    @Column(columnDefinition = "TEXT")
    private String cancelReason;

    @Column(nullable = false)
    private long refundAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RefundStatus refundStatus = RefundStatus.NONE;

    /** Lần nhắc lịch khởi hành gần nhất đã gửi khách: 0 chưa, 1 trước 3 ngày, 2 trước 1 ngày. */
    @Column(nullable = false)
    private short reminderStage;

    // ----- Trả 2 lần (tour riêng) -----

    /** 0 = trả một lần. */
    @Column(nullable = false)
    private long depositAmount;

    private LocalDateTime depositPaidAt;

    /** Hạn trả phần còn lại (hết ngày này). */
    private java.time.LocalDate balanceDueDate;

    /** Khách đã dùng lượt gia hạn 3 ngày. */
    @Column(nullable = false)
    private boolean balanceExtended;

    /** 0 chưa nhắc, 1 đã nhắc đặt cọc, 2 nhắc trả nốt trước 3 ngày, 3 trước 1 ngày. */
    @Column(nullable = false)
    private short paymentReminderStage;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<BookingPassenger> passengers = new ArrayList<>();

    /** Chặn 2 thao tác đổi trạng thái cùng lúc (vd khách hủy đúng lúc VNPay báo đã thanh toán). */
    @Version
    private long version;

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

    /** Số chỗ chiếm trên lịch khởi hành (em bé dưới 2 tuổi ngồi cùng người lớn, không tính chỗ). */
    public int seats() {
        return adults + children;
    }

    public int travellers() {
        return adults + children + infants;
    }

    public boolean paysInTwoParts() {
        return depositAmount > 0;
    }

    /** Số tiền khách đã trả tới hiện tại (giới hạn trên của khoản hoàn). */
    public long paidAmount() {
        return switch (status) {
            case DEPOSIT_PAID -> depositAmount;
            case PAID, COMPLETED -> totalAmount;
            default -> 0;
        };
    }

    /** Lần thanh toán tiếp theo: cọc hoặc phần còn lại (đơn trả 2 lần), toàn bộ (đơn thường). */
    public long nextPaymentAmount() {
        if (status == BookingStatus.DEPOSIT_PAID) return totalAmount - depositAmount;
        return paysInTwoParts() ? depositAmount : totalAmount;
    }
}
