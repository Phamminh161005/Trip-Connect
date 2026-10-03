package com.tripconnect.backend.repository;

import com.tripconnect.backend.entity.AgentSpecialty;
import com.tripconnect.backend.entity.AgentSpecialtyId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AgentSpecialtyRepository extends JpaRepository<AgentSpecialty, AgentSpecialtyId> {

    @EntityGraph(attributePaths = "category")
    List<AgentSpecialty> findByAgentId(Long agentId);

    @Modifying
    @Query("delete from AgentSpecialty s where s.agent.id = :agentId")
    void deleteByAgentId(@Param("agentId") Long agentId);
}
