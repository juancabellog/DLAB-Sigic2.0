package com.sisgic.repository;

import com.sisgic.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE PasswordResetToken t SET t.usedAt = :usedAt " +
           "WHERE t.userId = :userId AND t.usedAt IS NULL")
    int invalidateUnusedTokensForUser(@Param("userId") Long userId, @Param("usedAt") Instant usedAt);

    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :cutoff OR t.usedAt IS NOT NULL")
    int deleteExpiredOrUsed(@Param("cutoff") Instant cutoff);
}
