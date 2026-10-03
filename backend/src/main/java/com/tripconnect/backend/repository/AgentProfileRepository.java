package com.tripconnect.backend.repository;

import com.tripconnect.backend.entity.AgentProfile;
import com.tripconnect.backend.enums.AgentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AgentProfileRepository extends JpaRepository<AgentProfile, Long>, JpaSpecificationExecutor<AgentProfile> {

    Optional<AgentProfile> findByUserId(Long userId);

    List<AgentProfile> findByUserIdIn(Collection<Long> userIds);

    @EntityGraph(attributePaths = "user")
    Optional<AgentProfile> findWithUserById(Long id);

    // Tránh N+1 khi hiển thị danh sách
    @Override
    @EntityGraph(attributePaths = "user")
    Page<AgentProfile> findAll(Specification<AgentProfile> spec, Pageable pageable);

    long countByStatus(AgentStatus status);

    /** Tắt "Đang nhận yêu cầu" của Agent không đăng nhập kể từ {@code threshold}. */
    @Modifying
    @Query("""
            update AgentProfile p set p.isAcceptingRequests = false
            where p.isAcceptingRequests = true
              and p.user.id in (select u.id from User u where u.lastLoginAt is null or u.lastLoginAt < :threshold)
            """)
    int disableAcceptingForInactiveAgents(@Param("threshold") LocalDateTime threshold);
}
