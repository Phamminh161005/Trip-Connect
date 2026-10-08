package com.tripconnect.backend.repository;

import com.tripconnect.backend.entity.Settlement;
import com.tripconnect.backend.enums.SettlementStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long>, JpaSpecificationExecutor<Settlement> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Settlement s where s.id = :id")
    Optional<Settlement> findByIdForUpdate(@Param("id") Long id);

    boolean existsByCode(String code);

    long countByStatusIn(Collection<SettlementStatus> statuses);

    /** Chờ Agent xác nhận mà đã quá hạn -> tự xác nhận. */
    @Query("select s.id from Settlement s where s.status = com.tripconnect.backend.enums.SettlementStatus.PENDING_CONFIRM "
            + "and s.confirmDeadline < :now order by s.id")
    List<Long> findPastConfirmDeadline(@Param("now") LocalDateTime now);

    @Override
    @EntityGraph(attributePaths = "agent")
    Page<Settlement> findAll(Specification<Settlement> spec, Pageable pageable);
}
