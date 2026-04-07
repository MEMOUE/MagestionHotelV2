package com.mghbackend.repository;

import com.mghbackend.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenAndUsedFalse(String token);

    Optional<PasswordResetToken> findByEmailAndAccountTypeAndUsedFalse(String email, String accountType);

    /** Supprime les tokens expirés (nettoyage) */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    /** Invalide les anciens tokens non utilisés pour cet email */
    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.used = true WHERE t.email = :email AND t.accountType = :accountType AND t.used = false")
    void invalidateExistingTokens(@Param("email") String email, @Param("accountType") String accountType);
}