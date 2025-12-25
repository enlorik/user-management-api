package com.empress.usermanagementapi.repository;

import com.empress.usermanagementapi.entity.PasswordResetToken;
import com.empress.usermanagementapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    // new: so we can overwrite the same row for the same user
    Optional<PasswordResetToken> findByUser(User user);

    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetToken p WHERE p.expiryDate < :dateTime")
    int deleteByExpiryDateBefore(@Param("dateTime") LocalDateTime dateTime);
}
