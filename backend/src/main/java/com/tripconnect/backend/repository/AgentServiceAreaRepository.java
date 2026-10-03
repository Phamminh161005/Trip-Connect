package com.tripconnect.backend.repository;

import com.tripconnect.backend.entity.AgentServiceArea;
import com.tripconnect.backend.entity.AgentServiceAreaId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AgentServiceAreaRepository extends JpaRepository<AgentServiceArea, AgentServiceAreaId> {

    @EntityGraph(attributePaths = "location")
    List<AgentServiceArea> findByAgentId(Long agentId);

    @Modifying
    @Query("delete from AgentServiceArea a where a.agent.id = :agentId")
    void deleteByAgentId(@Param("agentId") Long agentId);
}
