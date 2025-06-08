package com.example.webdemo.beans;

import java.util.Date;
import java.sql.Timestamp;

public class User {
    private String userId; // Changed from int to String
    private String username;
    private String passwordHash; // SM3 hashed password
    private String fullName;
    private String departmentId;
    private String phone;
    private String role; // e.g., "SCHOOL_ADMIN", "DEPARTMENT_ADMIN", "AUDIT_ADMIN", "SYSTEM_ADMIN"

    // New fields for security features
    private Date passwordLastChanged;
    private int failedLoginAttempts;
    private Timestamp lockoutTime;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Constructors
    public User() {
    }

    public User(String userId, String username, String passwordHash, String fullName, String departmentId, String phone, String role) { // Changed userId to String
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.departmentId = departmentId;
        this.phone = phone;
        this.role = role;
        this.passwordLastChanged = new Date(); // Set to now on creation
        this.failedLoginAttempts = 0;
        this.lockoutTime = null;
    }

    // Getters and Setters
    public String getUserId() { // Changed return type to String
        return userId;
    }

    public void setUserId(String userId) { // Changed parameter type to String
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Date getPasswordLastChanged() {
        return passwordLastChanged;
    }

    public void setPasswordLastChanged(Date passwordLastChanged) {
        this.passwordLastChanged = passwordLastChanged;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public Timestamp getLockoutTime() {
        return lockoutTime;
    }

    public void setLockoutTime(Timestamp lockoutTime) {
        this.lockoutTime = lockoutTime;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    // toString() method for debugging (optional)
    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' + // Adjusted for String type
                ", username='" + (username != null ? username.replace("'", "\\'") : null) + '\'' +
                ", fullName='" + (fullName != null ? fullName.replace("'", "\\'") : null) + '\'' +
                ", departmentId='" + (departmentId != null ? departmentId.replace("'", "\\'") : null) + '\'' +
                ", phone='" + (phone != null ? phone.replace("'", "\\'") : null) + '\'' +
                ", role='" + (role != null ? role.replace("'", "\\'") : null) + '\'' +
                ", passwordLastChanged=" + passwordLastChanged +
                ", failedLoginAttempts=" + failedLoginAttempts +
                ", lockoutTime=" + lockoutTime +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
