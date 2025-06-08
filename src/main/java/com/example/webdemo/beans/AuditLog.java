package com.example.webdemo.beans;

import java.sql.Timestamp;

public class AuditLog {
    private int logId;
    private String userId; // Changed from int to String
    private String username; // To store username, especially if userId is not immediately available
    private String actionType; // E.g., "Login", "ViewAppointment", "ApproveAppointment"
    private String actionDetails; // E.g., "User logged in successfully", "Viewed appointment ID: 123"
    private Timestamp actionTime;
    private String clientIp;

    public AuditLog() {
    }

    // Getters and Setters
    public int getLogId() { return logId; }
    public void setLogId(int logId) { this.logId = logId; }

    public String getUserId() { return userId; } // Changed return type
    public void setUserId(String userId) { this.userId = userId; } // Changed parameter type

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getActionDetails() {
        return actionDetails;
    }

    public void setActionDetails(String actionDetails) {
        this.actionDetails = actionDetails;
    }

    public Timestamp getActionTime() {
        return actionTime;
    }

    public void setActionTime(Timestamp actionTime) {
        this.actionTime = actionTime;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    @Override
    public String toString() {
        return "AuditLog{" +
                "logId=" + logId +
                ", userId=\'" + userId + '\'' + // Updated for String
                ", username=\'" + (username != null ? username.replace("'", "\\'") : "null") + '\'' +
                ", actionType=\'" + (actionType != null ? actionType.replace("'", "\\'") : "null") + '\'' +
                ", actionDetails=\'" + (actionDetails != null ? actionDetails.replace("'", "\\'") : "null") + '\'' +
                ", actionTime=" + actionTime +
                ", clientIp='" + clientIp + '\'' +
                '}';
    }
}
