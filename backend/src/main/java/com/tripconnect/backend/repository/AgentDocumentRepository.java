package com.tripconnect.backend.repository;

import com.tripconnect.backend.entity.AgentDocument;
import com.tripconnect.backend.enums.AgentDocumentStatus;
import com.tripconnect.backend.enums.AgentDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AgentDocumentRepository extends JpaRepository<AgentDocument, Long> {

    List<AgentDocument> findByAgentProfileIdAndStatusOrderByUploadedAtAsc(Long profileId, AgentDocumentStatus status);

    List<AgentDocument> findByAgentProfileIdAndTypeAndStatus(Long profileId, AgentDocumentType type,
                                                            AgentDocumentStatus status);

    long countByAgentProfileIdAndTypeAndStatusIn(Long profileId, AgentDocumentType type,
                                                 Collection<AgentDocumentStatus> statuses);

    List<AgentDocument> findByChangeRequestIdOrderByUploadedAtAsc(Long changeRequestId);

    Optional<AgentDocument> findByIdAndAgentProfileId(Long id, Long profileId);
}
