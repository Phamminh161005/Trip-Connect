package com.tripconnect.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Lịch trình một ngày của tour. */
@Entity
@Table(name = "tour_itinerary_days")
@Getter
@Setter
@NoArgsConstructor
public class TourItineraryDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

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

    /** Nơi nghỉ đêm đó (ngày cuối thường để trống). */
    private String accommodation;
}
