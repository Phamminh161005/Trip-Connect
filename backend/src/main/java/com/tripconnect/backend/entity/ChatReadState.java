package com.tripconnect.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Một người đã đọc tới tin nào trong một cuộc trò chuyện. */
@Entity
@Table(name = "chat_read_states")
@Getter
@Setter
@NoArgsConstructor
public class ChatReadState {

    @EmbeddedId
    private Key id;

    @Column(nullable = false)
    private long lastReadMessageId;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {

        private Long requestId;

        private Long agentId;

        private Long userId;
    }
}
