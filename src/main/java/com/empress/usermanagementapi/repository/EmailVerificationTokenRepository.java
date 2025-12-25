package com.empress.usermanagementapi.repository;

import com.empress.usermanagementapi.entity.EmailVerificationToken;
import com.empress.usermanagementapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationTokenRepository
        extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);

    Optional<EmailVerificationToken> findByUser(User user);

    @Modifying
    @Transactional
    @Query("DELETE FROM EmailVerificationToken e WHERE e.expiryDate < :dateTime")
    int deleteByExpiryDateBefore(@Param("dateTime") LocalDateTime dateTime);
}
