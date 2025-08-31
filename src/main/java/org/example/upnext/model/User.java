package org.example.upnext.model;

import java.time.OffsetDateTime;

public class User {
    private Long userId;
    private String username;
    private String email;
    private String passwordHash;
    private String globalRole;   // ADMIN, MANAGER, MEMBER
    private String status;       // ACTIVE, SUSPENDED
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public User() {}
    public User(Long userId, String username, String email, String passwordHash, String globalRole, String status) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.globalRole = globalRole;
        this.status = status;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getGlobalRole() {
        return globalRole;
    }

    public void setGlobalRole(String globalRole) {
        this.globalRole = globalRole;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
