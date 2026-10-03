package com.tripconnect.backend.repository;

import com.tripconnect.backend.entity.AgentProfileChangeRequest;
import com.tripconnect.backend.enums.ChangeRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentProfileChangeRequestRepository extends JpaRepository<AgentProfileChangeRequest, Long> {

    boolean existsByAgentProfileIdAndStatus(Long profileId, ChangeRequestStatus status);

    long countByStatus(ChangeRequestStatus status);

    Optional<AgentProfileChangeRequest> findFirstByAgentProfileIdAndStatus(Long profileId, ChangeRequestStatus status);

    List<AgentProfileChangeRequest> findByAgentProfileIdOrderByCreatedAtDesc(Long profileId);

    Optional<AgentProfileChangeRequest> findByIdAndAgentProfileId(Long id, Long profileId);

    @EntityGraph(attributePaths = {"agentProfile", "agentProfile.user"})
    Page<AgentProfileChangeRequest> findByStatus(ChangeRequestStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"agentProfile", "agentProfile.user"})
    Optional<AgentProfileChangeRequest> findWithProfileById(Long id);
}
