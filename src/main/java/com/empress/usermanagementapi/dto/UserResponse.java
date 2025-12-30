package com.empress.usermanagementapi.dto;

import com.empress.usermanagementapi.entity.Role;
import com.empress.usermanagementapi.entity.User;

/**
 * Data Transfer Object for User responses in REST API.
 * This DTO excludes sensitive information like passwords and is used
 * to return user data to clients in a safe manner.
 */
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private Role role;
    private boolean verified;

    public UserResponse() {
    }

    /**
     * Create a UserResponse from a User entity.
     */
    public static UserResponse fromEntity(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setVerified(user.isVerified());
        return response;
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }
}
