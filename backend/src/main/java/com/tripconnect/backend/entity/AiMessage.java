package com.tripconnect.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Một tin trong cuộc trò chuyện với trợ lý (câu hỏi của người dùng hoặc câu trả lời). */
@Entity
@Table(name = "ai_messages")
@Getter
@Setter
@NoArgsConstructor
public class AiMessage {

    public enum Role { USER, ASSISTANT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long conversationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** Thẻ tour / gợi ý tour riêng đi kèm câu trả lời (JSON). */
    @Column(columnDefinition = "TEXT")
    private String attachments;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
