package com.tripconnect.backend.repository;

import com.tripconnect.backend.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    /** Thu hồi token nếu nó còn hiệu lực. Trả về 0 nếu token đã bị thu hồi trước đó (bị dùng lại). */
    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.id = :id and r.revoked = false")
    int revokeIfActive(@Param("id") Long id);

    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.user.id = :userId and r.revoked = false")
    int revokeAllByUserId(@Param("userId") Long userId);
}
