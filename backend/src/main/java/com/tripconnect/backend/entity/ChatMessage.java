package com.tripconnect.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Một tin nhắn giữa khách và đơn vị tổ chức. Cuộc trò chuyện = (requestId, agentId). Gửi rồi không sửa / xóa. */
@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long requestId;

    @Column(nullable = false)
    private Long agentId;

    @Column(nullable = false)
    private Long senderId;

    /** Nguyên văn; null khi tin chỉ có ảnh. */
    @Column(columnDefinition = "TEXT")
    private String body;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ChatMessageImage> images = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
