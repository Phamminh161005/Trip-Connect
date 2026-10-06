package com.tripconnect.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Ảnh đính kèm tin nhắn (public trên Cloudinary). */
@Entity
@Table(name = "chat_message_images")
@Getter
@Setter
@NoArgsConstructor
public class ChatMessageImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private ChatMessage message;

    @Column(nullable = false)
    private String publicId;

    @Column(nullable = false, length = 20)
    private String format;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false)
    private short sortOrder;
}
