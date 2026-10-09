package com.tripconnect.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Một lượt xem trang tour — dữ liệu cho gợi ý tour cá nhân hóa. */
@Entity
@Table(name = "tour_views")
@Getter
@Setter
@NoArgsConstructor
public class TourView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tourId;

    private Long userId;

    @Column(length = 64)
    private String visitorId;

    @Column(nullable = false)
    private LocalDateTime viewedAt;
}
