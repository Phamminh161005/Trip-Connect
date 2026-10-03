package com.tripconnect.backend.entity;

import com.tripconnect.backend.enums.AgentDocumentStatus;
import com.tripconnect.backend.enums.AgentDocumentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Giấy tờ pháp lý của Agent — file lưu PRIVATE trên Cloudinary, DB chỉ giữ publicId. */
@Entity
@Table(name = "agent_documents")
@Getter
@Setter
@NoArgsConstructor
public class AgentDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_profile_id", nullable = false)
    private AgentProfile agentProfile;

    /** Khác null nếu giấy tờ được nộp kèm một yêu cầu cập nhật hồ sơ. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "change_request_id")
    private AgentProfileChangeRequest changeRequest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AgentDocumentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AgentDocumentStatus status;

    @Column(nullable = false)
    private String publicId;

    @Column(nullable = false, length = 20)
    private String format;

    @Column(nullable = false, length = 100)
    private String mimeType;

    @Column(nullable = false)
    private long sizeBytes;

    private String originalFilename;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
    }
}
