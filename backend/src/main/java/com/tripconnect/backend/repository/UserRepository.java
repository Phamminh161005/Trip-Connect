package com.tripconnect.backend.repository;

import com.tripconnect.backend.entity.User;
import com.tripconnect.backend.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.email = :email")
    Optional<User> findByEmailForUpdate(@Param("email") String email);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Dùng cho bộ lọc JWT: tài khoản còn hoạt động không (không tồn tại = false). */
    boolean existsByIdAndActiveTrue(Long id);

    @Query("select u.email from User u where u.role = :role and u.active = true")
    List<String> findActiveEmailsByRole(@Param("role") UserRole role);

    @Query("select u.id from User u where u.role = :role and u.active = true")
    List<Long> findActiveIdsByRole(@Param("role") UserRole role);
}
