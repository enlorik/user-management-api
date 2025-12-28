package com.empress.usermanagementapi.entity;

import jakarta.persistence.*;

@Entity
public class EmailVerificationToken extends BaseTokenEntity {

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    private User user;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
