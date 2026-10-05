package com.tripconnect.backend.repository;

import com.tripconnect.backend.entity.CustomRequestAssignment;
import com.tripconnect.backend.enums.AssignmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CustomRequestAssignmentRepository
        extends JpaRepository<CustomRequestAssignment, Long>, JpaSpecificationExecutor<CustomRequestAssignment> {

    @EntityGraph(attributePaths = {"agent", "assignedBy"})
    List<CustomRequestAssignment> findByRequestIdOrderByIdAsc(Long requestId);

    Optional<CustomRequestAssignment> findFirstByRequestIdAndStatus(Long requestId, AssignmentStatus status);

    Optional<CustomRequestAssignment> findFirstByRequestIdAndAgentIdOrderByIdDesc(Long requestId, Long agentId);

    /** Agent đã từng được giao yêu cầu này (không giao lại). */
    @Query("select distinct a.agent.id from CustomRequestAssignment a where a.request.id = :requestId")
    List<Long> findAgentIdsByRequestId(@Param("requestId") Long requestId);

    @Query("select a.request.id from CustomRequestAssignment a where a.status = com.tripconnect.backend.enums.AssignmentStatus.PENDING "
            + "and a.deadline < :now order by a.deadline")
    List<Long> findRequestIdsWithExpiredAssignment(@Param("now") LocalDateTime now);

    @Override
    @EntityGraph(attributePaths = {"request", "request.departureLocation", "request.customer"})
    Page<CustomRequestAssignment> findAll(Specification<CustomRequestAssignment> spec, Pageable pageable);
}
