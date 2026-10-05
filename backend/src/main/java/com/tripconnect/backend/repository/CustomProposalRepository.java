package com.tripconnect.backend.repository;

import com.tripconnect.backend.entity.CustomProposal;
import com.tripconnect.backend.enums.ProposalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CustomProposalRepository extends JpaRepository<CustomProposal, Long> {

    List<CustomProposal> findByRequestIdOrderByIdAsc(Long requestId);

    List<CustomProposal> findByRequestIdAndAgentIdOrderByIdAsc(Long requestId, Long agentId);

    Optional<CustomProposal> findFirstByRequestIdAndAgentIdOrderByIdDesc(Long requestId, Long agentId);

    Optional<CustomProposal> findFirstByRequestIdAndStatus(Long requestId, ProposalStatus status);

    long countByRequestIdAndAgentId(Long requestId, Long agentId);

    /** Yêu cầu có đề xuất đang chờ khách mà đã quá hạn phản hồi. */
    @Query("""
            select p.request.id from CustomProposal p
            where p.status = com.tripconnect.backend.enums.ProposalStatus.SENT and p.expiresAt < :now
            order by p.request.id
            """)
    List<Long> findRequestIdsWithExpiredProposal(@Param("now") LocalDateTime now);

    /** Đề xuất đang chờ khách, sắp hết hạn và chưa nhắc. */
    @Query("""
            select p.id from CustomProposal p
            where p.status = com.tripconnect.backend.enums.ProposalStatus.SENT
              and p.expiryReminded = false and p.expiresAt < :before
            order by p.id
            """)
    List<Long> findExpiringUnreminded(@Param("before") LocalDateTime before);
}
