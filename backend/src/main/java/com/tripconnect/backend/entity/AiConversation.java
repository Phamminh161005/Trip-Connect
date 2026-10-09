package com.tripconnect.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Một cuộc trò chuyện với trợ lý AI của người đã đăng nhập. */
@Entity
@Table(name = "ai_conversations")
@Getter
@Setter
@NoArgsConstructor
public class AiConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    /** Lấy từ câu hỏi đầu tiên. */
    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
