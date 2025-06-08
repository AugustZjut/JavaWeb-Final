package com.example.webdemo.dao;

import com.example.webdemo.beans.AuditLog;
import com.example.webdemo.util.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.sql.ResultSet;

public class AuditLogDAO {

    public boolean createLog(AuditLog log) {
        String sql = "INSERT INTO audit_logs (user_id, username, action_type, action_details, action_time, client_ip) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, log.getUserId()); // Changed from getInt
            pstmt.setString(2, log.getUsername());
            pstmt.setString(3, log.getActionType());
            pstmt.setString(4, log.getActionDetails());
            pstmt.setTimestamp(5, log.getActionTime() != null ? log.getActionTime() : new Timestamp(System.currentTimeMillis()));
            pstmt.setString(6, log.getClientIp());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace(); // Proper logging
            return false;
        }
    }

    public List<AuditLog> getLogs(Timestamp startTime, Timestamp endTime, String username, String actionType) {
        List<AuditLog> logs = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM audit_logs WHERE 1=1 ");

        if (startTime != null) {
            sqlBuilder.append("AND action_time >= ? ");
        }
        if (endTime != null) {
            sqlBuilder.append("AND action_time <= ? ");
        }
        if (username != null && !username.isEmpty()) {
            sqlBuilder.append("AND username LIKE ? ");
        }
        if (actionType != null && !actionType.isEmpty()) {
            sqlBuilder.append("AND action_type = ? ");
        }
        sqlBuilder.append("ORDER BY action_time DESC");

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            
            int paramIndex = 1;
            if (startTime != null) {
                pstmt.setTimestamp(paramIndex++, startTime);
            }
            if (endTime != null) {
                pstmt.setTimestamp(paramIndex++, endTime);
            }
            if (username != null && !username.isEmpty()) {
                pstmt.setString(paramIndex++, "%" + username + "%");
            }
            if (actionType != null && !actionType.isEmpty()) {
                pstmt.setString(paramIndex++, actionType);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    AuditLog log = new AuditLog();
                    log.setLogId(rs.getInt("log_id"));
                    log.setUserId(rs.getString("user_id")); // Changed from getInt to getString
                    log.setUsername(rs.getString("username"));
                    log.setActionType(rs.getString("action_type"));
                    log.setActionDetails(rs.getString("action_details"));
                    log.setActionTime(rs.getTimestamp("action_time"));
                    log.setClientIp(rs.getString("client_ip"));
                    logs.add(log);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Proper logging
        }
        return logs;
    }
    
    // Optional: Method to add HMAC for log integrity if implemented
    // public boolean createLogWithHmac(AuditLog log, String hmacKey) { ... }
}
