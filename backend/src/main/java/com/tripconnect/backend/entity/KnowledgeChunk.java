package com.tripconnect.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/** Một đoạn trong kho kiến thức TripConnect (chính sách, hướng dẫn) kèm véc-tơ để trợ lý tra cứu. */
@Entity
@Table(name = "knowledge_chunks")
@Getter
@Setter
@NoArgsConstructor
public class KnowledgeChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String source;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

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
