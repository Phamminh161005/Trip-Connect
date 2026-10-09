package com.tripconnect.backend.repository;

import com.tripconnect.backend.entity.AiConversation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {

    List<AiConversation> findByUserIdOrderByUpdatedAtDesc(Long userId, Pageable pageable);

    Optional<AiConversation> findByIdAndUserId(Long id, Long userId);
}
