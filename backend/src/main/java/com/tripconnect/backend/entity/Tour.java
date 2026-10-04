package com.tripconnect.backend.entity;

import com.tripconnect.backend.enums.AccommodationType;
import com.tripconnect.backend.enums.TourStatus;
import com.tripconnect.backend.enums.TransportMode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Tour đoàn công khai. Giá và số chỗ nằm ở từng lịch khởi hành ({@link TourDeparture}). */
@Entity
@Table(name = "tours")
@Getter
@Setter
@NoArgsConstructor
public class Tour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Agent tổ chức tour; null = tour của TripConnect (Admin tạo). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private User agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(nullable = false, length = 150)
    private String title;

    @ManyToMany
    @JoinTable(name = "tour_category_links",
            joinColumns = @JoinColumn(name = "tour_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<TourCategory> categories = new HashSet<>();

    /** Nơi khởi hành — luôn là một Tỉnh/Thành của Việt Nam. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departure_location_id", nullable = false)
    private Location departureLocation;

    /** Tỉnh/Thành Việt Nam hoặc quốc gia (nước ngoài chỉ chọn tới cấp quốc gia). */
    @ManyToMany
    @JoinTable(name = "tour_destinations",
            joinColumns = @JoinColumn(name = "tour_id"),
            inverseJoinColumns = @JoinColumn(name = "location_id"))
    private Set<Location> destinations = new HashSet<>();

    @Column(nullable = false)
    private short durationDays;

    @Column(nullable = false)
    private short durationNights;

    /** Tự tính từ điểm đến: có ít nhất một điểm đến ngoài Việt Nam. */
    @Column(name = "is_international", nullable = false)
    private boolean international;

    // Các danh sách chỉ để hiển thị -> lưu mảng text[] của PostgreSQL, giữ đúng thứ tự Agent nhập
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private List<String> highlights = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "included_services", nullable = false, columnDefinition = "text[]")
    private List<String> includedServices = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "excluded_services", nullable = false, columnDefinition = "text[]")
    private List<String> excludedServices = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** Chữ thường không dấu của tên, điểm đến, loại hình, điểm nổi bật — chỉ dùng để tìm kiếm (xem SearchText). */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String searchText = "";

    @ElementCollection
    @CollectionTable(name = "tour_transport_modes", joinColumns = @JoinColumn(name = "tour_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 20)
    private Set<TransportMode> transportModes = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccommodationType accommodationType;

    @Column(nullable = false)
    private String meetingPoint;

    @Column(nullable = false)
    private LocalTime meetingTime;

    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayNumber ASC")
    private List<TourItineraryDay> itineraryDays = new ArrayList<>();

    /** Ảnh đầu tiên (sortOrder nhỏ nhất) là ảnh bìa. Thêm/xóa ảnh qua repository riêng. */
    @OneToMany(mappedBy = "tour")
    @OrderBy("sortOrder ASC, id ASC")
    private List<TourImage> images = new ArrayList<>();

    // ----- File chương trình tour (PDF, không bắt buộc, lưu private) -----

    private String itineraryFilePublicId;

    @Column(length = 20)
    private String itineraryFileFormat;

    private Long itineraryFileSize;

    private String itineraryFileName;

    // ----- Trạng thái & duyệt -----

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TourStatus status;

    /** Lý do Admin yêu cầu chỉnh sửa / đình chỉ. */
    @Column(columnDefinition = "TEXT")
    private String statusReason;

    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    /** Lần đầu được công khai. */
    private LocalDateTime publishedAt;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    @Column(nullable = false)
    private int ratingCount;

    /** Chặn 2 thao tác đổi trạng thái cùng lúc (vd Admin duyệt đúng lúc Agent rút lại). */
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

    /** Tour của TripConnect (Admin tạo), không có Agent. */
    public boolean isPlatformTour() {
        return agent == null;
    }

    public boolean hasItineraryFile() {
        return itineraryFilePublicId != null;
    }
}
