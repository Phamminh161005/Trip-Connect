package com.tripconnect.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Một lần tìm kiếm tour (chỉ ghi khi người dùng thật sự lọc / gõ từ khóa ở trang tìm kiếm).
 * Người đăng nhập: userId; khách: visitorId (mã ngẫu nhiên ở trình duyệt). Lưu id, không lưu entity
 * để ghi nhanh và không cần nạp quan hệ.
 */
@Entity
@Table(name = "search_logs")
@Getter
@Setter
@NoArgsConstructor
public class SearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(length = 36)
    private String visitorId;

    @Column(length = 200)
    private String keyword;

    private Long destinationId;

    private Long departureLocationId;

    private LocalDate dateFrom;

    private LocalDate dateTo;

    private Long priceMin;

    private Long priceMax;

    private Short durationMin;

    private Short durationMax;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "bigint[]")
    private List<Long> categoryIds = new ArrayList<>();

    private Boolean international;

    @Column(nullable = false)
    private int resultCount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
