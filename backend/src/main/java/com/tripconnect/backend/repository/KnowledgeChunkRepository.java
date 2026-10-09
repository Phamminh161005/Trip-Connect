package com.tripconnect.backend.repository;

import com.tripconnect.backend.entity.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, Long> {
}
