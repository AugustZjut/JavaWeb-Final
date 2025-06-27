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
import java.util.List;
import java.util.Properties;
import java.util.UUID;

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

                // 验证 SM4 密钥是否正确配置
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
            pstmtAppointment.setString(4, CryptoUtils.encryptSM4(appointment.getApplicantName(), sm4KeyHex));
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
        // This query needs to fetch all and then filter by decrypted ID card,
        // or if the DB supports it, use a function for decryption (less likely with SM2).
        // For now, let's assume we might need to iterate or use a different strategy if this is too slow.
        // A direct WHERE on encrypted data only works if the encrypted value is identical every time (no salt/IV changes).
        // SM2 is asymmetric, so this is tricky. A search index on a hash of the ID might be better.
        // For simplicity here, we will fetch and decrypt. This is NOT efficient for large datasets.
        
        String encryptedApplicantIdCard;
        try {
            // We need to encrypt the search term to match what's in DB.
            // However, direct comparison of SM2 encrypted ciphertexts is generally not reliable due to the nature of asymmetric crypto.
            // A better approach for searching encrypted data is to use a blind index (e.g., store a hash of the ID card).
            // For this example, we'll proceed with the less optimal approach of decrypting all relevant records.
            // This implies we cannot directly use `WHERE applicant_id_card = ?` with an encrypted value effectively for SM2.
            // We will retrieve records and decrypt in application layer. This is INEFFICIENT.
            // A proper solution would involve re-thinking the search strategy for encrypted data.
            // One common pattern is to store a separate, searchable hash (e.g., HMAC-SHA256) of the ID card.
            // For now, we will retrieve all appointments and filter in memory (VERY BAD FOR PERFORMANCE).
            // A more realistic approach for a small number of user's appointments:
            // If a user is logged in and we have their *unencrypted* ID, we can encrypt it and search.
            // But for a public search by ID, this is problematic.
            // The requirement is "My Appointments": Query history. This implies the user is known.
            // Let's assume for "My Appointments", the user provides their ID card, and we encrypt it for the query.
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

    public Appointment getAppointmentById(String appointmentId) throws SQLException {
        String sql = "SELECT * FROM appointments WHERE appointment_id = ?";
        Appointment appointment = null;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, appointmentId);
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

    public List<AccompanyingPerson> getAccompanyingPersonsByAppointmentId(String appointmentId, boolean decryptPII) throws SQLException { // Added decryptPII parameter
        List<AccompanyingPerson> persons = new ArrayList<>();
        String sql = "SELECT * FROM accompanying_persons WHERE appointment_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, appointmentId);
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

    public boolean updateAppointmentStatus(String appointmentId, String status) throws SQLException {
        String sql = "UPDATE appointments SET status = ?, updated_at = NOW() WHERE appointment_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setString(2, appointmentId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
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
        app.setAccompanyingPersons(getAccompanyingPersonsByAppointmentId(String.valueOf(app.getAppointmentId()), decryptPII));
        return app;
    }
}
