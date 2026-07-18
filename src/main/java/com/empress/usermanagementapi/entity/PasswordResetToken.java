package com.empress.usermanagementapi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class PasswordResetToken extends BaseTokenEntity {

    public PasswordResetToken() {}

    public PasswordResetToken(String tokenHash, User user, LocalDateTime expiryDate) {
        this.setTokenHash(tokenHash);
        this.setUser(user);
        this.setExpiryDate(expiryDate);
        this.setUsed(false);
    }
}
