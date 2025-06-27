package com.example.webdemo.beans;

import java.sql.Timestamp;

// 审计日志实体类，对应数据库 audit_logs 表
public class AuditLog {
    // 日志ID，自增主键
    private int logId;
    // 操作用户ID，可为null
    private Integer userId;
    // 用户名
    private String username;
    // 操作类型
    private String actionType;
    // 目标实体（如users、appointments等）
    private String targetEntity;
    // 目标实体ID
    private Integer targetEntityId;
    // 操作详情
    private String details;
    // 操作IP
    private String ipAddress;
    // 日志时间
    private Timestamp logTimestamp;
    // HMAC-SM3值（可选）
    private String hmacSm3Hash;

    public AuditLog() {
    }

    public int getLogId() { return logId; }
    public void setLogId(int logId) { this.logId = logId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getTargetEntity() { return targetEntity; }
    public void setTargetEntity(String targetEntity) { this.targetEntity = targetEntity; }

    public Integer getTargetEntityId() { return targetEntityId; }
    public void setTargetEntityId(Integer targetEntityId) { this.targetEntityId = targetEntityId; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public Timestamp getLogTimestamp() { return logTimestamp; }
    public void setLogTimestamp(Timestamp logTimestamp) { this.logTimestamp = logTimestamp; }

    public String getHmacSm3Hash() { return hmacSm3Hash; }
    public void setHmacSm3Hash(String hmacSm3Hash) { this.hmacSm3Hash = hmacSm3Hash; }

    @Override
    public String toString() {
        return "AuditLog{" +
                "logId=" + logId +
                ", userId=" + userId +
                ", username='" + username + '\'' +
                ", actionType='" + actionType + '\'' +
                ", targetEntity='" + targetEntity + '\'' +
                ", targetEntityId=" + targetEntityId +
                ", details='" + details + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", logTimestamp=" + logTimestamp +
                ", hmacSm3Hash='" + hmacSm3Hash + '\'' +
                '}';
    }
}
