package com.example.webdemo.beans;

import java.util.Date;
import java.sql.Timestamp;

// 管理员实体类，对应数据库 users 表
public class User {
    // 用户ID，自增主键
    private int userId;
    // 用户名，唯一
    private String username;
    // 密码哈希（SM3加密）
    private String passwordHash;
    // 真实姓名
    private String fullName;
    // 所属部门ID，可为null
    private Integer departmentId;
    // 联系电话（加密存储）
    private String phoneNumber;
    // 角色（SCHOOL_ADMIN、DEPARTMENT_ADMIN、AUDIT_ADMIN、SYSTEM_ADMIN）
    private String role;
    // 密码最后修改时间
    private Timestamp passwordLastChanged;
    // 登录失败次数
    private int failedLoginAttempts;
    // 账户锁定截止时间
    private Timestamp lockoutTime;
    // 创建时间
    private Timestamp createdAt;
    // 更新时间
    private Timestamp updatedAt;
    // 是否需要强制修改密码（TRUE: 是, FALSE: 否）
    private boolean passwordChangeRequired;
    // 是否有权限管理公众预约
    private boolean canManagePublicAppointments;
    // 是否有权限管理公务预约
    private boolean canManageOfficialAppointments;

    public User() {
    }

    // 省略构造方法，可按需添加

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public Integer getDepartmentId() { return departmentId; }
    public void setDepartmentId(Integer departmentId) { this.departmentId = departmentId; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Timestamp getPasswordLastChanged() { return passwordLastChanged; }
    public void setPasswordLastChanged(Timestamp passwordLastChanged) { this.passwordLastChanged = passwordLastChanged; }

    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }

    public Timestamp getLockoutTime() { return lockoutTime; }
    public void setLockoutTime(Timestamp lockoutTime) { this.lockoutTime = lockoutTime; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public boolean isPasswordChangeRequired() { return passwordChangeRequired; }
    public void setPasswordChangeRequired(boolean passwordChangeRequired) { this.passwordChangeRequired = passwordChangeRequired; }

    public boolean isCanManagePublicAppointments() { return canManagePublicAppointments; }
    public void setCanManagePublicAppointments(boolean canManagePublicAppointments) {
        this.canManagePublicAppointments = canManagePublicAppointments;
    }

    public boolean isCanManageOfficialAppointments() {
        return canManageOfficialAppointments;
    }

    public void setCanManageOfficialAppointments(boolean canManageOfficialAppointments) {
        this.canManageOfficialAppointments = canManageOfficialAppointments;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", departmentId=" + departmentId +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", role='" + role + '\'' +
                ", passwordLastChanged=" + passwordLastChanged +
                ", failedLoginAttempts=" + failedLoginAttempts +
                ", lockoutTime=" + lockoutTime +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
