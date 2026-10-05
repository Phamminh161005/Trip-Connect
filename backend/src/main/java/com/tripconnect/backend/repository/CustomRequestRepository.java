package com.tripconnect.backend.repository;

import com.tripconnect.backend.entity.CustomRequest;
import com.tripconnect.backend.enums.CustomRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CustomRequestRepository extends JpaRepository<CustomRequest, Long>, JpaSpecificationExecutor<CustomRequest> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from CustomRequest r where r.id = :id")
    Optional<CustomRequest> findByIdForUpdate(@Param("id") Long id);

    boolean existsByCode(String code);

    long countByCustomerIdAndStatusIn(Long customerId, Collection<CustomRequestStatus> statuses);

    long countByStatus(CustomRequestStatus status);

    /** [agentId, số yêu cầu đang mở (chờ nhận + đang xử lý)] */
    @Query("""
            select r.agent.id, count(r) from CustomRequest r
            where r.agent.id in :agentIds and r.status in (com.tripconnect.backend.enums.CustomRequestStatus.WAITING_AGENT,
                                                          com.tripconnect.backend.enums.CustomRequestStatus.IN_PROGRESS)
            group by r.agent.id
            """)
    List<Object[]> countOpenByAgent(@Param("agentIds") Collection<Long> agentIds);

    /** Chưa chốt được đề xuất mà ngày khởi hành muộn nhất đã quá sát -> tự đóng. */
    @Query("""
            select r.id from CustomRequest r
            where r.status in (com.tripconnect.backend.enums.CustomRequestStatus.NEW,
                               com.tripconnect.backend.enums.CustomRequestStatus.WAITING_AGENT,
                               com.tripconnect.backend.enums.CustomRequestStatus.IN_PROGRESS)
              and r.latestStart < :before
            order by r.id
            """)
    List<Long> findOpenStartingBefore(@Param("before") LocalDate before);

    /** Đang chờ Agent gửi đề xuất, hạn rơi trước :before (quá hạn: :before = now; sắp hết hạn: chưa nhắc). */
    @Query("""
            select r.id from CustomRequest r
            where r.status = com.tripconnect.backend.enums.CustomRequestStatus.IN_PROGRESS
              and r.proposalDeadline is not null and r.proposalDeadline < :before
              and (:unremindedOnly = false or r.deadlineReminded = false)
            order by r.id
            """)
    List<Long> findProposalDueBefore(@Param("before") LocalDateTime before, @Param("unremindedOnly") boolean unremindedOnly);

    /** Đang chờ khách (không chờ Agent) mà không có trao đổi nào từ :cutoff. */
    @Query("""
            select r.id from CustomRequest r
            where r.status = com.tripconnect.backend.enums.CustomRequestStatus.IN_PROGRESS
              and r.proposalDeadline is null and r.lastActivityAt < :cutoff
            order by r.id
            """)
    List<Long> findInactiveSince(@Param("cutoff") LocalDateTime cutoff);

    @Override
    @EntityGraph(attributePaths = {"customer", "departureLocation", "agent"})
    Page<CustomRequest> findAll(Specification<CustomRequest> spec, Pageable pageable);
}
