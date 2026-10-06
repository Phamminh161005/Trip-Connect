package com.tripconnect.backend.repository;

import com.tripconnect.backend.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /** Tin cũ hơn :before (null = mới nhất), mới trước. */
    @EntityGraph(attributePaths = "images")
    @Query("""
            select m from ChatMessage m
            where m.requestId = :requestId and m.agentId = :agentId and (:before is null or m.id < :before)
            order by m.id desc
            """)
    List<ChatMessage> findOlder(@Param("requestId") Long requestId, @Param("agentId") Long agentId,
                                @Param("before") Long before, Pageable pageable);

    /** Tin mới hơn :after (tải bù sau khi mất kết nối), cũ trước. */
    @EntityGraph(attributePaths = "images")
    @Query("""
            select m from ChatMessage m
            where m.requestId = :requestId and m.agentId = :agentId and m.id > :after
            order by m.id asc
            """)
    List<ChatMessage> findNewer(@Param("requestId") Long requestId, @Param("agentId") Long agentId,
                                @Param("after") Long after, Pageable pageable);

    /** Số tin người khác gửi mà :userId chưa đọc trong một cuộc trò chuyện. */
    @Query("""
            select count(m) from ChatMessage m
            where m.requestId = :requestId and m.agentId = :agentId and m.senderId <> :userId and m.id > :lastRead
            """)
    long countUnread(@Param("requestId") Long requestId, @Param("agentId") Long agentId,
                     @Param("userId") Long userId, @Param("lastRead") long lastRead);

    /** Các đơn vị từng trò chuyện trong yêu cầu, cũ trước. */
    @Query("select m.agentId from ChatMessage m where m.requestId = :requestId group by m.agentId order by min(m.id)")
    List<Long> findAgentIds(@Param("requestId") Long requestId);

    /**
     * [requestId, số tin chưa đọc] của :userId trên nhiều yêu cầu (danh sách yêu cầu).
     * agentOnly = true: chỉ tính cuộc trò chuyện của chính Agent :userId.
     */
    @Query(value = """
            select m.request_id, count(*) from chat_messages m
            left join chat_read_states r
                   on r.request_id = m.request_id and r.agent_id = m.agent_id and r.user_id = :userId
            where m.request_id in (:requestIds) and m.sender_id <> :userId
              and m.id > coalesce(r.last_read_message_id, 0)
              and (:agentOnly = false or m.agent_id = :userId)
            group by m.request_id
            """, nativeQuery = true)
    List<Object[]> countUnreadByRequest(@Param("requestIds") Collection<Long> requestIds, @Param("userId") Long userId,
                                        @Param("agentOnly") boolean agentOnly);
}
