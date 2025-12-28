package com.empress.usermanagementapi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class PasswordResetToken extends BaseTokenEntity {

    public PasswordResetToken() {}

    public PasswordResetToken(String token, User user, LocalDateTime expiryDate) {
        this.setToken(token);
        this.setUser(user);
        this.setExpiryDate(expiryDate);
        this.setUsed(false);
    }
}
