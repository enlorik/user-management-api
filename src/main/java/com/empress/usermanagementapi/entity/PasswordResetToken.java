package com.empress.usermanagementapi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class PasswordResetToken extends BaseTokenEntity {

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    private User user;

    public PasswordResetToken() {}

    public PasswordResetToken(String token, User user, LocalDateTime expiryDate) {
        this.setToken(token);
        this.user = user;
        this.setExpiryDate(expiryDate);
        this.setUsed(false);
    }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
