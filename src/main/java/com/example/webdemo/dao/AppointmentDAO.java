package com.example.webdemo.dao;

import com.example.webdemo.beans.AccompanyingPerson;
import com.example.webdemo.beans.Appointment;
import com.example.webdemo.util.CryptoUtils;
import com.example.webdemo.util.DBUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.text.SimpleDateFormat;
import java.util.UUID;
import java.util.HashMap;

public class AppointmentDAO {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentDAO.class);
    private DataSource dataSource;
    private static String sm4KeyHex;

    private static final String SQL_INSERT_APPOINTMENT = "INSERT INTO appointments (campus, entry_datetime, applicant_organization, applicant_name, applicant_id_card, applicant_phone, transport_mode, license_plate, official_visit_department_id, official_visit_contact_person, visit_reason, appointment_type, status, application_date, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
    private static final String SQL_SELECT_BY_ID = "SELECT * FROM appointments WHERE appointment_id = ?";
    private static final String SQL_SELECT_BY_TYPE = "SELECT * FROM appointments WHERE appointment_type = ? ORDER BY application_date DESC";
    private static final String SQL_UPDATE_STATUS = "UPDATE appointments SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE appointment_id = ?";
    private static final String SQL_SELECT_ACCOMPANYING = "SELECT * FROM accompanying_persons WHERE appointment_id = ?";

    static {
        try (InputStream input = DBUtils.class.getClassLoader().getResourceAsStream("db.properties")) {
            Properties prop = new Properties();
            if (input == null) {
                logger.error("无法找到 db.properties 文件");
                throw new RuntimeException("无法找到 db.properties 文件");
            } else {
                prop.load(input);
                sm4KeyHex = prop.getProperty("sm4.keyHex");

                // 验证 SM4 密钥 是否正确配置
                if (sm4KeyHex == null || sm4KeyHex.isEmpty() || sm4KeyHex.equals("YOUR_SM4_KEY_HEX_HERE")) {
                    logger.error("SM4 密钥未正确配置在 db.properties 中。加解密将会失败。");
                    throw new RuntimeException("SM4 密钥未正确配置。加解密将会失败。");
                } else {
                    logger.info("已成功加载 SM4 密钥");
                }
            }
        } catch (IOException ex) {
            logger.error("加载 db.properties 时出错", ex);
            throw new RuntimeException("加载配置文件时出错", ex);
        }
    }


    public AppointmentDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // Updated createAppointment to take a single Appointment object which includes accompanying persons
    public boolean createAppointment(Appointment appointment) throws SQLException {
        String sqlAppointment = "INSERT INTO appointments (campus, entry_datetime, applicant_organization, applicant_name, applicant_id_card, applicant_phone, transport_mode, license_plate, official_visit_department_id, official_visit_contact_person, visit_reason, appointment_type, status, application_date, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        String sqlAccompanyingPerson = "INSERT INTO accompanying_persons (person_id, appointment_id, name, id_card, phone) VALUES (?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement pstmtAppointment = null;
        PreparedStatement pstmtAccompanying = null;

        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false); // Start transaction

            pstmtAppointment = conn.prepareStatement(sqlAppointment);
            pstmtAppointment.setString(1, appointment.getCampus());
            pstmtAppointment.setTimestamp(2, appointment.getEntryDatetime());
            pstmtAppointment.setString(3, appointment.getApplicantOrganization());
            pstmtAppointment.setString(4, appointment.getApplicantName()); // 不加密姓名
            pstmtAppointment.setString(5, CryptoUtils.encryptSM4(appointment.getApplicantIdCard(), sm4KeyHex));
            pstmtAppointment.setString(6, CryptoUtils.encryptSM4(appointment.getApplicantPhone(), sm4KeyHex));
            pstmtAppointment.setString(7, appointment.getTransportMode());
            pstmtAppointment.setString(8, appointment.getLicensePlate());
            if (appointment.getOfficialVisitDepartmentId() != null) {
                pstmtAppointment.setInt(9, appointment.getOfficialVisitDepartmentId());
            } else {
                pstmtAppointment.setNull(9, Types.INTEGER);
            }
            pstmtAppointment.setString(10, appointment.getOfficialVisitContactPerson());
            pstmtAppointment.setString(11, appointment.getVisitReason());
            pstmtAppointment.setString(12, appointment.getAppointmentType());
            pstmtAppointment.setString(13, appointment.getStatus());
            pstmtAppointment.setTimestamp(14, appointment.getApplicationDate());

            int affectedRows = pstmtAppointment.executeUpdate();

            if (affectedRows == 0) {
                conn.rollback();
                return false;
            }

            if (appointment.getAccompanyingPersons() != null && !appointment.getAccompanyingPersons().isEmpty()) {
                pstmtAccompanying = conn.prepareStatement(sqlAccompanyingPerson);
                for (AccompanyingPerson person : appointment.getAccompanyingPersons()) {
                    pstmtAccompanying.setInt(1, person.getAccompanyingPersonId()); // Use String ID
                    pstmtAccompanying.setInt(2, appointment.getAppointmentId());
                    // Encrypt PII for accompanying persons - now all using SM4
                    pstmtAccompanying.setString(3, CryptoUtils.encryptSM4(person.getName(), sm4KeyHex));
                    pstmtAccompanying.setString(4, CryptoUtils.encryptSM4(person.getIdCard(), sm4KeyHex));
                    pstmtAccompanying.setString(5, CryptoUtils.encryptSM4(person.getPhone(), sm4KeyHex));
                    pstmtAccompanying.addBatch();
                }
                pstmtAccompanying.executeBatch();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            e.printStackTrace(); 
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e; // Re-throw to be handled by servlet
        } catch (Exception e) { // Catch crypto exceptions
            e.printStackTrace();
            if (conn != null) conn.rollback();
            // Consider wrapping in a custom exception or re-throwing SQLException
            throw new SQLException("Encryption/Decryption failed during appointment creation.", e);
        }
        finally {
            try {
                if (pstmtAppointment != null) pstmtAppointment.close();
                if (pstmtAccompanying != null) pstmtAccompanying.close();
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public List<Appointment> getAppointmentsByApplicantIdCard(String applicantIdCard) throws SQLException {
        List<Appointment> appointments = new ArrayList<>();
        String encryptedApplicantIdCard;
        try {
             encryptedApplicantIdCard = CryptoUtils.encryptSM4(applicantIdCard, sm4KeyHex);
        } catch (Exception e) {
            throw new SQLException("Failed to encrypt ID card for search.", e);
        }

        String sql = "SELECT * FROM appointments WHERE applicant_id_card = ? ORDER BY entry_datetime DESC"; // Renamed appointment_time to entry_datetime

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, encryptedApplicantIdCard); // Search by encrypted ID
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Appointment app = mapResultSetToAppointment(rs, true); // Decrypt PII
                    // Double check after decryption if needed, though DB should have matched.
                    // Ensure getApplicantIdCard() returns the decrypted value for comparison
                    if (app.getApplicantIdCard() != null && app.getApplicantIdCard().equals(applicantIdCard)) {
                         appointments.add(app);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) { // Crypto exceptions
            e.printStackTrace();
            throw new SQLException("Decryption failed while fetching appointments.", e);
        }
        return appointments;
    }
    
    // Method to get appointments by type (e.g., "Official", "Public")
    // This method will decrypt PII using the DAO's internally managed keys.
    public List<Appointment> getAppointmentsByType(String appointmentType) throws SQLException {
        List<Appointment> appointments = new ArrayList<>();
        // String sql = "SELECT * FROM appointments WHERE appointment_type = ? ORDER BY submission_time DESC";
        String sql = "SELECT * FROM appointments WHERE appointment_type = ? ORDER BY application_date DESC"; // Corrected to application_date

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, appointmentType);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    appointments.add(mapResultSetToAppointment(rs, true)); // Decrypt PII
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) { // Catch crypto exceptions from mapResultSetToAppointment
            e.printStackTrace();
            throw new SQLException("Decryption failed while fetching appointments by type.", e);
        }
        return appointments;
    }

    public Appointment getAppointmentById(int appointmentId) throws SQLException {
        String sql = "SELECT * FROM appointments WHERE appointment_id = ?";
        Appointment appointment = null;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, appointmentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAppointment(rs, true); // Decrypt PII
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) { // Crypto exceptions
             e.printStackTrace();
            throw new SQLException("Decryption failed while fetching appointment by ID.", e);
        }
        return null;
    }

    public List<AccompanyingPerson> getAccompanyingPersonsByAppointmentId(int appointmentId, boolean decryptPII) throws SQLException { // Changed from String to int
        List<AccompanyingPerson> persons = new ArrayList<>();
        String sql = "SELECT * FROM accompanying_persons WHERE appointment_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, appointmentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    AccompanyingPerson person = new AccompanyingPerson();
                    person.setAccompanyingPersonId(rs.getInt("accompanying_person_id")); // 修正列名与数据库表一致
                    person.setAppointmentId(rs.getInt("appointment_id")); // Use String ID
                    // Decrypt PII data based on the flag
                    if (decryptPII) {
                        // 获取加密的敏感字段
                        String name = rs.getString("name");
                        String idCard = rs.getString("id_card");
                        String phone = rs.getString("phone");
                        
                        // 默认使用原始值，即使未能解密也至少有数据供显示
                        person.setName(name);
                        person.setIdCard(idCard);
                        person.setPhone(phone);
                        
                        // 尝试解密数据，如果失败则保留加密值但不添加"解密失败"标记
                        if (sm4KeyHex != null && !sm4KeyHex.isEmpty()) {
                            try {
                                if (name != null) {
                                    String decryptedName = CryptoUtils.decryptSM4(name, sm4KeyHex);
                                    // 检查解密是否成功
                                    // 简单检查解密是否成功 - 假设加密数据一定会包含'='，而有效的姓名不会包含
                                    if (decryptedName != null && !decryptedName.contains("=")) {
                                        person.setName(decryptedName);
                                    } else {
                                        logger.warn("随行人员姓名解密可能失败，使用原始值: {}", name);
                                    }
                                }
                            } catch (Exception e) {
                                logger.error("随行人员姓名解密失败: {} - {}", name, e.getMessage());
                            }
                            
                            try {
                                if (idCard != null) {
                                    String decryptedIdCard = CryptoUtils.decryptSM4(idCard, sm4KeyHex);
                                    // 检查解密是否成功 - 简单判断不含"="字符
                                    if (decryptedIdCard != null && !decryptedIdCard.contains("=")) {
                                        person.setIdCard(decryptedIdCard);
                                    } else {
                                        logger.warn("随行人员身份证解密可能失败，使用原始值: {}", idCard);
                                    }
                                }
                            } catch (Exception e) {
                                logger.error("随行人员身份证解密失败: {} - {}", idCard, e.getMessage());
                            }
                            
                            try {
                                if (phone != null) {
                                    String decryptedPhone = CryptoUtils.decryptSM4(phone, sm4KeyHex);
                                    // 检查解密是否成功 - 简单判断不含"="字符
                                    if (decryptedPhone != null && !decryptedPhone.contains("=")) {
                                        person.setPhone(decryptedPhone);
                                    } else {
                                        logger.warn("随行人员手机号解密可能失败，使用原始值: {}", phone);
                                    }
                                }
                            } catch (Exception e) {
                                logger.error("随行人员手机号解密失败: {} - {}", phone, e.getMessage());
                            }
                        } else {
                            logger.error("SM4密钥未配置或为空，无法进行随行人员信息解密");
                        }
                    } else {
                        // Store raw encrypted data if not decrypting
                        person.setName(rs.getString("name"));
                        person.setIdCard(rs.getString("id_card"));
                        person.setPhone(rs.getString("phone"));
                    }
                    persons.add(person);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
        return persons;
    }

    public boolean updateAppointmentStatus(int appointmentId, String status) throws SQLException {
        String sql = "UPDATE appointments SET status = ?, updated_at = NOW() WHERE appointment_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, appointmentId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public boolean updateAppointmentStatus(int appointmentId, String status, Integer approvedBy) throws SQLException {
        // Also clears rejection_reason when updating status without providing one.
        logger.info("执行不带驳回理由的状态更新，appointmentId={}, status={}, approvedBy={}", appointmentId, status, approvedBy);
        String sql = "UPDATE appointments SET status = ?, approved_by_user_id = ?, approval_datetime = NOW(), rejection_reason = NULL, updated_at = NOW() WHERE appointment_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            if (approvedBy != null) {
                pstmt.setInt(2, approvedBy);
                logger.info("批准人ID设置为: {}", approvedBy);
            } else {
                pstmt.setNull(2, Types.INTEGER);
                logger.info("批准人ID设置为NULL");
            }
            pstmt.setInt(3, appointmentId);
            logger.info("准备执行SQL: {}", sql.replace("?", "_?_"));
            int rowsAffected = pstmt.executeUpdate();
            logger.info("状态更新执行结果: 影响行数={}", rowsAffected);
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("更新预约状态失败，appointmentId={}", appointmentId, e);
            throw e;
        }
    }

    public boolean updateAppointmentStatus(int appointmentId, String status, Integer approvedBy, String rejectionReason) throws SQLException {
        logger.info("执行带驳回理由的状态更新，appointmentId={}, status={}, approvedBy={}, rejectionReason={}", appointmentId, status, approvedBy, rejectionReason);
        String sql = "UPDATE appointments SET status = ?, approved_by_user_id = ?, approval_datetime = NOW(), rejection_reason = ?, updated_at = NOW() WHERE appointment_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            if (approvedBy != null) {
                pstmt.setInt(2, approvedBy);
                logger.info("批准人ID设置为: {}", approvedBy);
            } else {
                pstmt.setNull(2, Types.INTEGER);
                logger.info("批准人ID设置为NULL");
            }
            pstmt.setString(3, rejectionReason);
            logger.info("驳回理由设置为: {}", rejectionReason);
            pstmt.setInt(4, appointmentId);
            logger.info("准备执行SQL: {}", sql.replace("?", "_?_"));
            int rowsAffected = pstmt.executeUpdate();
            logger.info("状态更新执行结果: 影响行数={}", rowsAffected);
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("更新预约状态失败，appointmentId={}", appointmentId, e);
            throw e;
        }
    }

    // mapResultSetToAppointment now includes a flag to control decryption
    private Appointment mapResultSetToAppointment(ResultSet rs, boolean decryptPII) throws Exception {
        Appointment app = new Appointment();
        app.setAppointmentId(rs.getInt("appointment_id"));
        app.setCampus(rs.getString("campus"));
        app.setEntryDatetime(rs.getTimestamp("entry_datetime"));
        app.setApplicantOrganization(rs.getString("applicant_organization"));
        if (decryptPII) {
            // 获取加密的敏感字段
            String applicantName = rs.getString("applicant_name");
            String applicantIdCard = rs.getString("applicant_id_card");
            String applicantPhone = rs.getString("applicant_phone");
            
            // 默认使用原始值，即使未能解密也至少有数据供显示
            app.setApplicantName(applicantName);
            app.setApplicantIdCard(applicantIdCard);
            app.setApplicantPhone(applicantPhone);
            
            // 尝试解密数据，如果失败则保留加密值但不添加"解密失败"标记
            if (sm4KeyHex != null && !sm4KeyHex.isEmpty()) {
                try {
                    if (applicantName != null) {
                        String decryptedName = CryptoUtils.decryptSM4(applicantName, sm4KeyHex);
                        // 检查解密是否成功（Base64编码的密文通常包含'='字符）
                        // 简单检查解密是否成功 - 假设加密数据一定会包含'='，而有效的姓名不会包含
                        if (decryptedName != null && !decryptedName.contains("=")) {
                            app.setApplicantName(decryptedName);
                        } else {
                            logger.warn("姓名解密可能失败，使用原始值: {}", applicantName);
                        }
                    }
                } catch (Exception e) {
                    logger.error("姓名解密失败: {} - {}", applicantName, e.getMessage());
                }
                
                try {
                    if (applicantIdCard != null) {
                        String decryptedIdCard = CryptoUtils.decryptSM4(applicantIdCard, sm4KeyHex);
                        // 检查解密是否成功 - 简单判断不含"="字符
                        if (decryptedIdCard != null && !decryptedIdCard.contains("=")) {
                            app.setApplicantIdCard(decryptedIdCard);
                        } else {
                            logger.warn("身份证解密可能失败，使用原始值: {}", applicantIdCard);
                        }
                    }
                } catch (Exception e) {
                    logger.error("身份证解密失败: {} - {}", applicantIdCard, e.getMessage());
                }
                
                try {
                    if (applicantPhone != null) {
                        String decryptedPhone = CryptoUtils.decryptSM4(applicantPhone, sm4KeyHex);
                        // 检查解密是否成功 - 简单判断不含"="字符
                        if (decryptedPhone != null && !decryptedPhone.contains("=")) {
                            app.setApplicantPhone(decryptedPhone);
                        } else {
                            logger.warn("手机号解密可能失败，使用原始值: {}", applicantPhone);
                        }
                    }
                } catch (Exception e) {
                    logger.error("手机号解密失败: {} - {}", applicantPhone, e.getMessage());
                }
            } else {
                logger.error("SM4密钥未配置或为空，无法进行解密");
            }
        } else {
            app.setApplicantName(rs.getString("applicant_name"));
            app.setApplicantIdCard(rs.getString("applicant_id_card"));
            app.setApplicantPhone(rs.getString("applicant_phone"));
        }
        app.setTransportMode(rs.getString("transport_mode"));
        app.setLicensePlate(rs.getString("license_plate"));
        int deptId = rs.getInt("official_visit_department_id");
        app.setOfficialVisitDepartmentId(rs.wasNull() ? null : deptId);
        app.setOfficialVisitContactPerson(rs.getString("official_visit_contact_person"));
        app.setVisitReason(rs.getString("visit_reason"));
        app.setAppointmentType(rs.getString("appointment_type"));
        app.setStatus(rs.getString("status"));
        app.setApplicationDate(rs.getTimestamp("application_date"));
        app.setCreatedAt(rs.getTimestamp("created_at"));
        app.setUpdatedAt(rs.getTimestamp("updated_at"));
        
        // 获取随行人员信息
        int appointmentId = rs.getInt("appointment_id");
        app.setAccompanyingPersons(getAccompanyingPersonsByAppointmentId(appointmentId, decryptPII));
        return app;
    }

    // ================ 官方预约管理相关方法 ================

    /**
     * 获取官方预约列表（分页）
     * @param departmentId 如果不为null，则只查询该部门的预约
     */
    public List<Appointment> getOfficialAppointments(int page, int pageSize, Integer departmentId) throws SQLException, Exception {
        List<Appointment> appointments = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM appointments WHERE appointment_type = 'OFFICIAL' ");
        if (departmentId != null) {
            sql.append("AND official_visit_department_id = ? ");
        }
        sql.append("ORDER BY application_date DESC LIMIT ? OFFSET ?");
        
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (departmentId != null) {
                pstmt.setInt(paramIndex++, departmentId);
            }
            pstmt.setInt(paramIndex++, pageSize);
            pstmt.setInt(paramIndex++, (page - 1) * pageSize);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    appointments.add(mapResultSetToAppointment(rs, true));
                }
            }
        } catch (Exception e) {
            logger.error("获取公务预约列表时出错", e);
            throw new SQLException("获取公务预约列表时出错.", e);
        }
        return appointments;
    }

    /**
     * 获取官方预约总数
     * @param departmentId 如果不为null，则只统计该部门的预约
     */
    public int getOfficialAppointmentCount(Integer departmentId) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM appointments WHERE appointment_type = 'OFFICIAL' ");
        if (departmentId != null) {
            sql.append("AND official_visit_department_id = ?");
        }
        
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            if (departmentId != null) {
                pstmt.setInt(1, departmentId);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * 搜索官方预约
     */
    public List<Appointment> searchOfficialAppointments(
            String applicationDateStart, String applicationDateEnd,
            String appointmentDateStart, String appointmentDateEnd,
            String campus, String applicantOrganization, String applicantName, String idCard, 
            Integer visitDepartmentId, String visitContactPerson, String status, 
            Integer currentUserDepartmentId, int page, int pageSize) throws SQLException {
        
        StringBuilder sql = new StringBuilder("SELECT * FROM appointments WHERE appointment_type = 'OFFICIAL'");
        List<Object> params = new ArrayList<>();

        try {
            // 权限控制：部门管理员只能搜索自己部门的
            if (currentUserDepartmentId != null) {
                sql.append(" AND official_visit_department_id = ?");
                params.add(currentUserDepartmentId);
            }
    
            // 构建查询条件 - 现在传入applicantOrganization参数
            buildSearchQuery(sql, params, applicationDateStart, applicationDateEnd, appointmentDateStart, appointmentDateEnd, campus, applicantOrganization, applicantName, idCard, status);
    
            if (visitDepartmentId != null && visitDepartmentId > 0) {
                sql.append(" AND official_visit_department_id = ?");
                params.add(visitDepartmentId);
            }
            if (visitContactPerson != null && !visitContactPerson.trim().isEmpty()) {
                sql.append(" AND official_visit_contact_person LIKE ?");
                params.add("%" + visitContactPerson + "%");
            }
    
            sql.append(" ORDER BY application_date DESC LIMIT ? OFFSET ?");
            params.add(pageSize);
            params.add((page - 1) * pageSize);
    
            List<Appointment> appointments = new ArrayList<>();
            try (Connection conn = DBUtils.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                
                setStatementParams(pstmt, params);
    
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        appointments.add(mapResultSetToAppointment(rs, true));
                    }
                }
            }
            return appointments;
        } catch (Exception e) {
            logger.error("搜索公务预约时出错: {}", e.getMessage(), e);
            throw new SQLException("搜索公务预约时出错: " + e.getMessage(), e);
        }
    }

    /**
     * 获取搜索官方预约总数
     */
    public int getSearchOfficialAppointmentCount(
            String applicationDateStart, String applicationDateEnd,
            String appointmentDateStart, String appointmentDateEnd,
            String campus, String applicantOrganization, String applicantName, String idCard, 
            Integer visitDepartmentId, String visitContactPerson, String status, 
            Integer currentUserDepartmentId) throws SQLException {

        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM appointments WHERE appointment_type = 'OFFICIAL'");
        List<Object> params = new ArrayList<>();

        try {
            if (currentUserDepartmentId != null) {
                sql.append(" AND official_visit_department_id = ?");
                params.add(currentUserDepartmentId);
            }
    
            // 构建查询条件 - 现在传入applicantOrganization参数
            buildSearchQuery(sql, params, applicationDateStart, applicationDateEnd, appointmentDateStart, appointmentDateEnd, campus, applicantOrganization, applicantName, idCard, status);
    
            if (visitDepartmentId != null && visitDepartmentId > 0) {
                sql.append(" AND official_visit_department_id = ?");
                params.add(visitDepartmentId);
            }
            if (visitContactPerson != null && !visitContactPerson.trim().isEmpty()) {
                sql.append(" AND official_visit_contact_person LIKE ?");
                params.add("%" + visitContactPerson + "%");
            }
    
            try (Connection conn = DBUtils.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                
                setStatementParams(pstmt, params);
    
                try (ResultSet rs = pstmt.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        } catch (Exception e) {
            logger.error("获取公务预约搜索总数时出错: {}", e.getMessage(), e);
            throw new SQLException("获取公务预约搜索总数时出错: " + e.getMessage(), e);
        }
    }


    // ================ 公众预约管理相关方法 ================

    /**
     * 获取公众预约列表（分页）
     */
    public List<Appointment> getPublicAppointments(int page, int pageSize) throws SQLException, Exception {
        List<Appointment> appointments = new ArrayList<>();
        String sql = "SELECT * FROM appointments WHERE appointment_type = 'PUBLIC' " +
                    "ORDER BY application_date DESC LIMIT ? OFFSET ?";
        
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, pageSize);
            pstmt.setInt(2, (page - 1) * pageSize);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    appointments.add(mapResultSetToAppointment(rs, true));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) { // Crypto exceptions
            e.printStackTrace();
            throw new SQLException("Decryption failed while fetching public appointments.", e);
        }
        return appointments;
    }

    /**
     * 获取公众预约总数
     */
    public int getPublicAppointmentCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM appointments WHERE appointment_type = 'PUBLIC'";
        
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * 搜索公众预约
     */
    public List<Appointment> searchPublicAppointments(
            String applicationDateStart, String applicationDateEnd,
            String appointmentDateStart, String appointmentDateEnd,
            String campus, String organization, String applicantName, 
            String idCard, String status, int page, int pageSize) throws SQLException {
        
        StringBuilder sql = new StringBuilder("SELECT * FROM appointments WHERE appointment_type = 'PUBLIC'");
        List<Object> params = new ArrayList<>();

        try {
            // 构建查询条件
            buildSearchQuery(sql, params, applicationDateStart, applicationDateEnd, appointmentDateStart, appointmentDateEnd, campus, organization, applicantName, idCard, status);
            
            sql.append(" ORDER BY application_date DESC LIMIT ? OFFSET ?");
            params.add(pageSize);
            params.add((page - 1) * pageSize);
            
            List<Appointment> appointments = new ArrayList<>();
            try (Connection conn = DBUtils.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                
                setStatementParams(pstmt, params);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        appointments.add(mapResultSetToAppointment(rs, true));
                    }
                }
            }
            return appointments;
        } catch (Exception e) {
            logger.error("搜索公众预约时出错", e);
            throw new SQLException("搜索公众预约时出错: " + e.getMessage(), e);
        }
    }

    /**
     * 获取搜索公众预约总数
     */
    public int getSearchPublicAppointmentCount(
            String applicationDateStart, String applicationDateEnd,
            String appointmentDateStart, String appointmentDateEnd,
            String campus, String organization, String applicantName, 
            String idCard, String status) throws SQLException {
        
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM appointments WHERE appointment_type = 'PUBLIC'");
        List<Object> params = new ArrayList<>();

        try {
            // 构建查询条件
            buildSearchQuery(sql, params, applicationDateStart, applicationDateEnd, appointmentDateStart, appointmentDateEnd, campus, organization, applicantName, idCard, status);
            
            try (Connection conn = DBUtils.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                
                setStatementParams(pstmt, params);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        } catch (Exception e) {
            logger.error("获取公众预约搜索总数时出错", e);
            throw new SQLException("获取公众预约搜索总数时出错: " + e.getMessage(), e);
        }
    }

    // ================= 统计相关方法 =================

    public Map<String, Integer> getMonthlyPublicAppointmentStatistics(String year, String month, String campus) throws SQLException {
        Map<String, Integer> stats = new HashMap<>();
        StringBuilder sql = new StringBuilder("SELECT TO_CHAR(application_date, 'YYYY-MM-DD') as day, COUNT(*) as count FROM appointments WHERE appointment_type = 'PUBLIC'");

        List<Object> params = new ArrayList<>();
        if (year != null && !year.isEmpty()) {
            sql.append(" AND EXTRACT(YEAR FROM application_date) = ?");
            params.add(Integer.parseInt(year));
        }
        if (month != null && !month.isEmpty()) {
            sql.append(" AND EXTRACT(MONTH FROM application_date) = ?");
            params.add(Integer.parseInt(month));
        }
        if (campus != null && !campus.isEmpty()) {
            sql.append(" AND campus = ?");
            params.add(campus);
        }

        sql.append(" GROUP BY day ORDER BY day");

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            setStatementParams(pstmt, params);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    stats.put(rs.getString("day"), rs.getInt("count"));
                }
            }
        } catch (Exception e) {
            logger.error("获取公众预约月度统计数据时出错", e);
            throw new SQLException("获取公众预约月度统计数据时出错", e);
        }
        return stats;
    }

    public Map<String, Integer> getCampusPublicAppointmentStatistics(String year, String month) throws SQLException {
        Map<String, Integer> stats = new HashMap<>();
        StringBuilder sql = new StringBuilder("SELECT campus, COUNT(*) as count FROM appointments WHERE appointment_type = 'PUBLIC'");
        List<Object> params = new ArrayList<>();

        if (year != null && !year.isEmpty()) {
            sql.append(" AND EXTRACT(YEAR FROM application_date) = ?");
            params.add(Integer.parseInt(year));
        }
        if (month != null && !month.isEmpty()) {
            sql.append(" AND EXTRACT(MONTH FROM application_date) = ?");
            params.add(Integer.parseInt(month));
        }

        sql.append(" GROUP BY campus ORDER BY campus");

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            setStatementParams(pstmt, params);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    stats.put(rs.getString("campus"), rs.getInt("count"));
                }
            }
        } catch (Exception e) {
            logger.error("获取公众预约校区统计数据时出错", e);
            throw new SQLException("获取公众预约校区统计数据时出错", e);
        }
        return stats;
    }

    public Map<String, Integer> getStatusPublicAppointmentStatistics(String year, String month, String campus) throws SQLException {
        Map<String, Integer> stats = new HashMap<>();
        StringBuilder sql = new StringBuilder("SELECT status, COUNT(*) as count FROM appointments WHERE appointment_type = 'PUBLIC'");
        List<Object> params = new ArrayList<>();

        if (year != null && !year.isEmpty()) {
            sql.append(" AND EXTRACT(YEAR FROM application_date) = ?");
            params.add(Integer.parseInt(year));
        }
        if (month != null && !month.isEmpty()) {
            sql.append(" AND EXTRACT(MONTH FROM application_date) = ?");
            params.add(Integer.parseInt(month));
        }
        if (campus != null && !campus.isEmpty()) {
            sql.append(" AND campus = ?");
            params.add(campus);
        }

        sql.append(" GROUP BY status ORDER BY status");

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            setStatementParams(pstmt, params);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    stats.put(rs.getString("status"), rs.getInt("count"));
                }
            }
        } catch (Exception e) {
            logger.error("获取公众预约状态统计数据时出错", e);
            throw new SQLException("获取公众预约状态统计数据时出错", e);
        }
        return stats;
    }

    // ================= 公务预约统计相关方法 =================

    /**
     * 获取公务预约的月度统计信息
     */
    public Map<String, Object> getMonthlyOfficialAppointmentStatistics(String year, String month, String campus, String visitDepartmentId) throws SQLException {
        Map<String, Object> stats = new HashMap<>();
        
        // 按日统计
        Map<String, Integer> dailyCounts = new HashMap<>();
        StringBuilder sql = new StringBuilder("SELECT TO_CHAR(application_date, 'YYYY-MM-DD') as day, COUNT(*) as count FROM appointments WHERE appointment_type = 'OFFICIAL'");

        List<Object> params = new ArrayList<>();
        if (year != null && !year.isEmpty()) {
            sql.append(" AND EXTRACT(YEAR FROM application_date) = ?");
            params.add(Integer.parseInt(year));
        }
        if (month != null && !month.isEmpty()) {
            sql.append(" AND EXTRACT(MONTH FROM application_date) = ?");
            params.add(Integer.parseInt(month));
        }
        if (campus != null && !campus.isEmpty()) {
            sql.append(" AND campus = ?");
            params.add(campus);
        }
        if (visitDepartmentId != null && !visitDepartmentId.isEmpty()) {
            sql.append(" AND official_visit_department_id = ?");
            params.add(Integer.parseInt(visitDepartmentId));
        }

        sql.append(" GROUP BY day ORDER BY day");

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            setStatementParams(pstmt, params);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    dailyCounts.put(rs.getString("day"), rs.getInt("count"));
                }
            }
        } catch (Exception e) {
            logger.error("获取公务预约月度统计数据时出错", e);
            throw new SQLException("获取公务预约月度统计数据时出错", e);
        }
        stats.put("dailyCounts", dailyCounts);
        
        // 按状态统计
        stats.put("statusCounts", getStatusOfficialAppointmentStatistics(year, month, campus, visitDepartmentId));
        
        // 按校区统计
        stats.put("campusCounts", getCampusOfficialAppointmentStatistics(year, month, visitDepartmentId));
        
        // 按部门统计
        stats.put("departmentCounts", getDepartmentOfficialAppointmentStatistics(year, month, campus));
        
        // 总数
        int totalCount = dailyCounts.values().stream().mapToInt(Integer::intValue).sum();
        stats.put("totalCount", totalCount);
        
        return stats;
    }

    /**
     * 获取公务预约的校区统计信息
     */
    public Map<String, Integer> getCampusOfficialAppointmentStatistics(String year, String month, String visitDepartmentId) throws SQLException {
        Map<String, Integer> stats = new HashMap<>();
        StringBuilder sql = new StringBuilder("SELECT campus, COUNT(*) as count FROM appointments WHERE appointment_type = 'OFFICIAL'");
        List<Object> params = new ArrayList<>();

        if (year != null && !year.isEmpty()) {
            sql.append(" AND EXTRACT(YEAR FROM application_date) = ?");
            params.add(Integer.parseInt(year));
        }
        if (month != null && !month.isEmpty()) {
            sql.append(" AND EXTRACT(MONTH FROM application_date) = ?");
            params.add(Integer.parseInt(month));
        }
        if (visitDepartmentId != null && !visitDepartmentId.isEmpty()) {
            sql.append(" AND official_visit_department_id = ?");
            params.add(Integer.parseInt(visitDepartmentId));
        }

        sql.append(" GROUP BY campus ORDER BY campus");

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            setStatementParams(pstmt, params);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    stats.put(rs.getString("campus"), rs.getInt("count"));
                }
            }
        } catch (Exception e) {
            logger.error("获取公务预约校区统计数据时出错", e);
            throw new SQLException("获取公务预约校区统计数据时出错", e);
        }
        return stats;
    }

    /**
     * 获取公务预约的状态统计信息
     */
    public Map<String, Integer> getStatusOfficialAppointmentStatistics(String year, String month, String campus, String visitDepartmentId) throws SQLException {
        Map<String, Integer> stats = new HashMap<>();
        StringBuilder sql = new StringBuilder("SELECT status, COUNT(*) as count FROM appointments WHERE appointment_type = 'OFFICIAL'");
        List<Object> params = new ArrayList<>();

        if (year != null && !year.isEmpty()) {
            sql.append(" AND EXTRACT(YEAR FROM application_date) = ?");
            params.add(Integer.parseInt(year));
        }
        if (month != null && !month.isEmpty()) {
            sql.append(" AND EXTRACT(MONTH FROM application_date) = ?");
            params.add(Integer.parseInt(month));
        }
        if (campus != null && !campus.isEmpty()) {
            sql.append(" AND campus = ?");
            params.add(campus);
        }
        if (visitDepartmentId != null && !visitDepartmentId.isEmpty()) {
            sql.append(" AND official_visit_department_id = ?");
            params.add(Integer.parseInt(visitDepartmentId));
        }

        sql.append(" GROUP BY status ORDER BY status");

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            setStatementParams(pstmt, params);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    stats.put(rs.getString("status"), rs.getInt("count"));
                }
            }
        } catch (Exception e) {
            logger.error("获取公务预约状态统计数据时出错", e);
            throw new SQLException("获取公务预约状态统计数据时出错", e);
        }
        return stats;
    }

    /**
     * 获取公务预约的部门统计信息
     */
    public Map<String, Integer> getDepartmentOfficialAppointmentStatistics(String year, String month, String campus) throws SQLException {
        Map<String, Integer> stats = new HashMap<>();
        StringBuilder sql = new StringBuilder(
            "SELECT d.department_name, COUNT(a.*) as count " +
            "FROM appointments a " +
            "JOIN departments d ON a.official_visit_department_id = d.department_id " +
            "WHERE a.appointment_type = 'OFFICIAL'"
        );
        List<Object> params = new ArrayList<>();

        if (year != null && !year.isEmpty()) {
            sql.append(" AND EXTRACT(YEAR FROM a.application_date) = ?");
            params.add(Integer.parseInt(year));
        }
        if (month != null && !month.isEmpty()) {
            sql.append(" AND EXTRACT(MONTH FROM a.application_date) = ?");
            params.add(Integer.parseInt(month));
        }
        if (campus != null && !campus.isEmpty()) {
            sql.append(" AND a.campus = ?");
            params.add(campus);
        }

        sql.append(" GROUP BY d.department_name ORDER BY count DESC");

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            setStatementParams(pstmt, params);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    stats.put(rs.getString("department_name"), rs.getInt("count"));
                }
            }
        } catch (Exception e) {
            logger.error("获取公务预约部门统计数据时出错", e);
            throw new SQLException("获取公务预约部门统计数据时出错", e);
        }
        return stats;
    }

    // ================= 辅助方法 =================

    /**
     * 构建搜索查询的通用部分
     */
    private void buildSearchQuery(StringBuilder sql, List<Object> params, 
                                String applicationDateStart, String applicationDateEnd,
                                String appointmentDateStart, String appointmentDateEnd,
                                String campus, String organization, String applicantName, 
                                String idCard, String status) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        
        if (applicationDateStart != null && !applicationDateStart.trim().isEmpty()) {
            try {
                java.util.Date parsedDate = dateFormat.parse(applicationDateStart);
                sql.append(" AND application_date::DATE >= ?");
                params.add(new java.sql.Date(parsedDate.getTime()));
            } catch (Exception e) {
                logger.warn("无效的申请开始日期格式: {}", applicationDateStart);
                // 如果日期格式不正确，我们跳过这个条件而不是抛出异常
            }
        }
        if (applicationDateEnd != null && !applicationDateEnd.trim().isEmpty()) {
            try {
                java.util.Date parsedDate = dateFormat.parse(applicationDateEnd);
                sql.append(" AND application_date::DATE <= ?");
                params.add(new java.sql.Date(parsedDate.getTime()));
            } catch (Exception e) {
                logger.warn("无效的申请结束日期格式: {}", applicationDateEnd);
                // 如果日期格式不正确，我们跳过这个条件而不是抛出异常
            }
        }
        if (appointmentDateStart != null && !appointmentDateStart.trim().isEmpty()) {
            try {
                java.util.Date parsedDate = dateFormat.parse(appointmentDateStart);
                sql.append(" AND entry_datetime::DATE >= ?");
                params.add(new java.sql.Date(parsedDate.getTime()));
            } catch (Exception e) {
                logger.warn("无效的预约开始日期格式: {}", appointmentDateStart);
                // 如果日期格式不正确，我们跳过这个条件而不是抛出异常
            }
        }
        if (appointmentDateEnd != null && !appointmentDateEnd.trim().isEmpty()) {
            try {
                java.util.Date parsedDate = dateFormat.parse(appointmentDateEnd);
                sql.append(" AND entry_datetime::DATE <= ?");
                params.add(new java.sql.Date(parsedDate.getTime()));
            } catch (Exception e) {
                logger.warn("无效的预约结束日期格式: {}", appointmentDateEnd);
                // 如果日期格式不正确，我们跳过这个条件而不是抛出异常
            }
        }
        if (campus != null && !campus.trim().isEmpty()) {
            sql.append(" AND campus = ?");
            params.add(campus);
        }
        if (organization != null && !organization.trim().isEmpty()) {
            sql.append(" AND applicant_organization LIKE ?");
            params.add("%" + organization + "%");
        }
        if (status != null && !status.trim().isEmpty()) {
            sql.append(" AND status = ?");
            params.add(status);
        }

        // 姓名搜索：姓名字段不加密，可以直接搜索
        if (applicantName != null && !applicantName.trim().isEmpty()) {
            sql.append(" AND applicant_name LIKE ?");
            params.add("%" + applicantName + "%");
        }

        // 身份证搜索：由于身份证字段加密，需要特殊处理
        // 这里我们先尝试加密用户输入的身份证号，然后与数据库中的加密值比较
        if (idCard != null && !idCard.trim().isEmpty()) {
            try {
                if (sm4KeyHex != null && !sm4KeyHex.trim().isEmpty()) {
                    // 先尝试加密用户输入的身份证号
                    String encryptedIdCard = CryptoUtils.encryptSM4(idCard, sm4KeyHex);
                    sql.append(" AND applicant_id_card = ?");
                    params.add(encryptedIdCard);
                } else {
                    logger.warn("SM4密钥未配置，无法进行身份证搜索");
                }
            } catch (Exception e) {
                logger.warn("无法加密用户输入的身份证号进行搜索: {}", e.getMessage());
                // 如果加密失败，我们可以选择忽略这个搜索条件，或者记录警告
                // 这里选择忽略，不添加身份证搜索条件
            }
        }
    }

    /**
     * 设置PreparedStatement的参数
     */
    private void setStatementParams(PreparedStatement pstmt, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            pstmt.setObject(i + 1, params.get(i));
        }
    }

    /**
     * 移动端“我的预约”多条件查询
     */
    public List<Appointment> searchAppointments(
            String applicantName,
            String applicantIdCard,
            String applicantPhone,
            Integer applicantUserId,
            Integer departmentId,
            String appointmentType,
            String status,
            java.util.Date startDate,
            java.util.Date endDate
    ) throws SQLException {
        List<Appointment> appointments = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM appointments WHERE 1=1");
        List<Object> params = new ArrayList<>();

        // 只查当前用户
        if (applicantUserId != null) {
            sql.append(" AND applicant_user_id = ?");
            params.add(applicantUserId);
        }
        if (departmentId != null) {
            sql.append(" AND official_visit_department_id = ?");
            params.add(departmentId);
        }
        if (appointmentType != null && !appointmentType.trim().isEmpty()) {
            sql.append(" AND appointment_type = ?");
            params.add(appointmentType);
        }
        if (status != null && !status.trim().isEmpty()) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        if (startDate != null) {
            sql.append(" AND entry_datetime >= ?");
            params.add(new java.sql.Timestamp(startDate.getTime()));
        }
        if (endDate != null) {
            sql.append(" AND entry_datetime <= ?");
            params.add(new java.sql.Timestamp(endDate.getTime()));
        }
        // 姓名模糊查（如未加密）
        if (applicantName != null && !applicantName.trim().isEmpty()) {
            sql.append(" AND applicant_name LIKE ?");
            params.add("%" + applicantName + "%");
        }
        // 手机号加密精确查
        if (applicantPhone != null && !applicantPhone.trim().isEmpty()) {
            try {
                String encryptedPhone = CryptoUtils.encryptSM4(applicantPhone, sm4KeyHex);
                sql.append(" AND applicant_phone = ?");
                params.add(encryptedPhone);
            } catch (Exception e) {
                throw new SQLException("手机号加密失败", e);
            }
        }
        // 身份证加密精确查
        if (applicantIdCard != null && !applicantIdCard.trim().isEmpty()) {
            try {
                String encryptedIdCard = CryptoUtils.encryptSM4(applicantIdCard, sm4KeyHex);
                sql.append(" AND applicant_id_card = ?");
                params.add(encryptedIdCard);
            } catch (Exception e) {
                throw new SQLException("身份证加密失败", e);
            }
        }
        sql.append(" ORDER BY entry_datetime DESC");

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            setStatementParams(pstmt, params);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    appointments.add(mapResultSetToAppointment(rs, true));
                }
            }
        } catch (SQLException e) {
            logger.error("searchAppointments查询失败", e);
            throw e;
        } catch (Exception e) {
            logger.error("searchAppointments解密失败", e);
            throw new SQLException("searchAppointments解密失败", e);
        }
        return appointments;
    }

}
