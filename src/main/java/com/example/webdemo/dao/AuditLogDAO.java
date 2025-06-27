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
import javax.sql.DataSource;

public class AuditLogDAO {
    private DataSource dataSource;

    public AuditLogDAO() {
        // 使用默认数据源
        this.dataSource = DBUtils.getDataSource();
    }

    public AuditLogDAO(DataSource dataSource) {
        // 允许注入数据源，方便测试
        this.dataSource = dataSource;
    }

    public boolean createLog(AuditLog log) {
        String sql = "INSERT INTO audit_logs (user_id, username, action_type, target_entity, target_entity_id, details, ip_address, log_timestamp) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (log.getUserId() == null) {
                pstmt.setNull(1, java.sql.Types.INTEGER);
            } else {
                pstmt.setInt(1, log.getUserId());
            }
            pstmt.setString(2, log.getUsername());
            pstmt.setString(3, log.getActionType());
            
            // 设置目标实体和ID（可能为null）
            if (log.getTargetEntity() != null) {
                pstmt.setString(4, log.getTargetEntity());
            } else {
                pstmt.setNull(4, java.sql.Types.VARCHAR);
            }
            
            if (log.getTargetEntityId() != null) {
                pstmt.setInt(5, log.getTargetEntityId());
            } else {
                pstmt.setNull(5, java.sql.Types.INTEGER);
            }
            
            pstmt.setString(6, log.getDetails());
            pstmt.setString(7, log.getIpAddress());
            pstmt.setTimestamp(8, log.getLogTimestamp() != null ? log.getLogTimestamp() : new Timestamp(System.currentTimeMillis()));
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取符合条件的审计日志（支持分页）
     * 
     * @param username 用户名（模糊匹配）
     * @param actionType 操作类型（完全匹配）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param offset 分页偏移量
     * @param limit 每页记录数
     * @return 审计日志列表
     */
    public List<AuditLog> getFilteredLogs(String username, String actionType, Timestamp startDate, Timestamp endDate, int offset, int limit) {
        List<AuditLog> logs = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM audit_logs WHERE 1=1 ");
        
        if (startDate != null) {
            sqlBuilder.append("AND log_timestamp >= ? ");
        }
        if (endDate != null) {
            sqlBuilder.append("AND log_timestamp <= ? ");
        }
        if (username != null && !username.isEmpty()) {
            sqlBuilder.append("AND username LIKE ? ");
        }
        if (actionType != null && !actionType.isEmpty()) {
            sqlBuilder.append("AND action_type = ? ");
        }
        
        sqlBuilder.append("ORDER BY log_timestamp DESC LIMIT ? OFFSET ?");
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            int paramIndex = 1;
            
            if (startDate != null) {
                pstmt.setTimestamp(paramIndex++, startDate);
            }
            if (endDate != null) {
                pstmt.setTimestamp(paramIndex++, endDate);
            }
            if (username != null && !username.isEmpty()) {
                pstmt.setString(paramIndex++, "%" + username + "%");
            }
            if (actionType != null && !actionType.isEmpty()) {
                pstmt.setString(paramIndex++, actionType);
            }
            
            pstmt.setInt(paramIndex++, limit);
            pstmt.setInt(paramIndex++, offset);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(extractAuditLogFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }

    /**
     * 统计符合条件的审计日志总数
     */
    public int countFilteredLogs(String username, String actionType, Timestamp startDate, Timestamp endDate) {
        StringBuilder sqlBuilder = new StringBuilder("SELECT COUNT(*) FROM audit_logs WHERE 1=1 ");
        
        if (startDate != null) {
            sqlBuilder.append("AND log_timestamp >= ? ");
        }
        if (endDate != null) {
            sqlBuilder.append("AND log_timestamp <= ? ");
        }
        if (username != null && !username.isEmpty()) {
            sqlBuilder.append("AND username LIKE ? ");
        }
        if (actionType != null && !actionType.isEmpty()) {
            sqlBuilder.append("AND action_type = ? ");
        }
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            int paramIndex = 1;
            
            if (startDate != null) {
                pstmt.setTimestamp(paramIndex++, startDate);
            }
            if (endDate != null) {
                pstmt.setTimestamp(paramIndex++, endDate);
            }
            if (username != null && !username.isEmpty()) {
                pstmt.setString(paramIndex++, "%" + username + "%");
            }
            if (actionType != null && !actionType.isEmpty()) {
                pstmt.setString(paramIndex++, actionType);
            }
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 获取所有审计日志（不分页，谨慎使用）
     */
    public List<AuditLog> getLogs(Timestamp startTime, Timestamp endTime, String username, String actionType) {
        List<AuditLog> logs = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM audit_logs WHERE 1=1 ");
        if (startTime != null) {
            sqlBuilder.append("AND log_timestamp >= ? ");
        }
        if (endTime != null) {
            sqlBuilder.append("AND log_timestamp <= ? ");
        }
        if (username != null && !username.isEmpty()) {
            sqlBuilder.append("AND username LIKE ? ");
        }
        if (actionType != null && !actionType.isEmpty()) {
            sqlBuilder.append("AND action_type = ? ");
        }
        sqlBuilder.append("ORDER BY log_timestamp DESC");
        try (Connection conn = dataSource.getConnection();
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
                    logs.add(extractAuditLogFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }
    
    /**
     * 从ResultSet中提取AuditLog对象
     */
    private AuditLog extractAuditLogFromResultSet(ResultSet rs) throws SQLException {
        AuditLog log = new AuditLog();
        log.setLogId(rs.getInt("log_id"));
        
        int uid = rs.getInt("user_id");
        log.setUserId(rs.wasNull() ? null : uid);
        
        log.setUsername(rs.getString("username"));
        log.setActionType(rs.getString("action_type"));
        
        log.setTargetEntity(rs.getString("target_entity"));
        
        int targetId = rs.getInt("target_entity_id");
        log.setTargetEntityId(rs.wasNull() ? null : targetId);
        
        log.setDetails(rs.getString("details"));
        log.setLogTimestamp(rs.getTimestamp("log_timestamp"));
        log.setIpAddress(rs.getString("ip_address"));
        return log;
    }
}
