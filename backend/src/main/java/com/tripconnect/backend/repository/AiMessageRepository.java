package com.tripconnect.backend.repository;

import com.tripconnect.backend.entity.AiMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AiMessageRepository extends JpaRepository<AiMessage, Long> {

    List<AiMessage> findByConversationIdOrderByIdAsc(Long conversationId);

    /** Các tin gần nhất (mới trước) — đưa vào ngữ cảnh khi hỏi tiếp. */
    @Query("select m from AiMessage m where m.conversationId = :conversationId order by m.id desc")
    List<AiMessage> findRecent(@Param("conversationId") Long conversationId, Pageable pageable);
}
