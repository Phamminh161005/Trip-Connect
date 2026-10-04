package com.tripconnect.backend.repository;

import com.tripconnect.backend.entity.Tour;
import com.tripconnect.backend.enums.TourStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TourRepository extends JpaRepository<Tour, Long>, JpaSpecificationExecutor<Tour> {

    /** Khóa dòng tour khi thêm/xóa ảnh để 2 lần upload cùng lúc không vượt quá số ảnh tối đa. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Tour t where t.id = :id")
    Optional<Tour> findByIdForUpdate(@Param("id") Long id);

    // Lấy luôn Agent + nơi khởi hành trong cùng câu query (danh sách hiển thị cả hai)
    @Override
    @EntityGraph(attributePaths = {"agent", "departureLocation"})
    Page<Tour> findAll(Specification<Tour> spec, Pageable pageable);

    long countByStatus(TourStatus status);

    List<Tour> findBySearchText(String searchText);

    /** Dữ liệu cho thẻ tour ở trang tìm kiếm (nơi khởi hành, điểm đến, Agent) trong 1 câu query. */
    @EntityGraph(attributePaths = {"agent", "departureLocation", "destinations"})
    List<Tour> findWithCardDataByIdIn(Collection<Long> ids);
}
