package com.tripconnect.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Ảnh tour — file PUBLIC trên Cloudinary, DB chỉ giữ publicId. Ảnh có sortOrder nhỏ nhất là ảnh bìa. */
@Entity
@Table(name = "tour_images")
@Getter
@Setter
@NoArgsConstructor
public class TourImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    @Column(nullable = false)
    private String publicId;

    @Column(nullable = false, length = 20)
    private String format;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
    }
}
