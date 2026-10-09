package com.tripconnect.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/** Véc-tơ ý nghĩa của một tour đang bán (tìm tour theo mô tả, gợi ý tour tương tự). */
@Entity
@Table(name = "tour_embeddings")
@Getter
@Setter
@NoArgsConstructor
public class TourEmbedding {

    @Id
    private Long tourId;

    @Column(nullable = false, length = 60)
    private String model;

    @Column(nullable = false, length = 64)
    private String contentHash;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "real[]")
    private float[] vector;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
