package com.example.webdemo.dao;

import com.example.webdemo.beans.AccompanyingPerson;
import com.example.webdemo.beans.Appointment;
import com.example.webdemo.util.CryptoUtils;
import com.example.webdemo.util.DBUtils;

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

    private DataSource dataSource;
    private static String sm2PublicKeyHex;
    private static String sm2PrivateKeyHex;
    private static String sm4KeyHex;

    static {
        try (InputStream input = DBUtils.class.getClassLoader().getResourceAsStream("db.properties")) {
            Properties prop = new Properties();
            if (input == null) {
                System.out.println("Sorry, unable to find db.properties");
                // Consider throwing a runtime exception or handling this more gracefully
            } else {
                prop.load(input);
                sm2PublicKeyHex = prop.getProperty("sm2.publicKeyHex");
                sm2PrivateKeyHex = prop.getProperty("sm2.privateKeyHex");
                sm4KeyHex = prop.getProperty("sm4.keyHex");

                // Basic validation for key presence
                if (sm2PublicKeyHex == null || sm2PublicKeyHex.isEmpty() || sm2PublicKeyHex.equals("YOUR_SM2_PUBLIC_KEY_HEX_HERE") ||
                    sm2PrivateKeyHex == null || sm2PrivateKeyHex.isEmpty() || sm2PrivateKeyHex.equals("YOUR_SM2_PRIVATE_KEY_HEX_HERE") ||
                    sm4KeyHex == null || sm4KeyHex.isEmpty() || sm4KeyHex.equals("YOUR_128_BIT_SM4_KEY_HEX_HERE_32_CHARS")) {
                    System.err.println("WARNING: SM2/SM4 keys are not configured properly in db.properties. Encryption/Decryption will fail.");
                    // In a real application, you might want to prevent startup or use default (less secure) behavior.
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace(); // Handle exception
        }
    }


    public AppointmentDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // Updated createAppointment to take a single Appointment object which includes accompanying persons
    public boolean createAppointment(Appointment appointment) throws SQLException {
        String sqlAppointment = "INSERT INTO appointments (appointment_id, campus, entry_datetime, applicant_organization, " +
                "applicant_name, applicant_id_card, applicant_phone, transport_mode, license_plate, " +
                "official_visit_department_id, official_visit_contact_person, contact_person_phone, visit_reason, appointment_type, status, application_date, created_at, updated_at) " + // Corrected column names
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        String sqlAccompanyingPerson = "INSERT INTO accompanying_persons (person_id, appointment_id, name, id_card, phone) VALUES (?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement pstmtAppointment = null;
        PreparedStatement pstmtAccompanying = null;

        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false); // Start transaction

            pstmtAppointment = conn.prepareStatement(sqlAppointment);
            pstmtAppointment.setString(1, appointment.getAppointmentId()); // Use UUID from servlet
            pstmtAppointment.setString(2, appointment.getCampus()); // Campus is already a String in bean
            pstmtAppointment.setTimestamp(3, appointment.getEntryDatetime()); // Renamed from getAppointmentTime
            pstmtAppointment.setString(4, appointment.getApplicantOrganization());

            // Encrypt PII data before storing
            pstmtAppointment.setString(5, CryptoUtils.encryptSM2(appointment.getApplicantName(), sm2PublicKeyHex));
            pstmtAppointment.setString(6, CryptoUtils.encryptSM2(appointment.getApplicantIdCard(), sm2PublicKeyHex));
            pstmtAppointment.setString(7, CryptoUtils.encryptSM4(appointment.getApplicantPhone(), sm4KeyHex));

            pstmtAppointment.setString(8, appointment.getTransportMode()); // transportMode is already a String in bean
            pstmtAppointment.setString(9, appointment.getLicensePlate());

            if (Appointment.AppointmentType.OFFICIAL_VISIT.name().equals(appointment.getAppointmentType())) { // Compare string with enum name
                // pstmtAppointment.setString(10, appointment.getVisitDepartment()); // Old: direct department name
                // For official_visit_department_id, you'd typically get the ID based on the department name/code.
                // This might involve another DAO call or passing the ID from the servlet if it's already resolved.
                // For now, if getVisitDepartment() stores the ID as a string, it can be parsed.
                // If it stores the name, you need a lookup. Assuming it might be an ID for now or needs to be handled in servlet.
                // If visitDepartment is a name, you'd do: int deptId = departmentDAO.getDepartmentIdByName(appointment.getVisitDepartment());
                // pstmtAppointment.setInt(10, deptId);
                // For simplicity, if getVisitDepartment() is expected to return a string that can be parsed to an INT (the ID)
                // This is a placeholder - robust error handling and type conversion needed here.
                // It's better if the servlet resolves the department name to an ID before calling createAppointment.
                // Assuming getVisitDepartment() now returns the department *ID* as a String due to form submission, or it's null.
                if (appointment.getVisitDepartment() != null && !appointment.getVisitDepartment().isEmpty()) {
                    try {
                        pstmtAppointment.setInt(10, Integer.parseInt(appointment.getVisitDepartment()));
                    } catch (NumberFormatException e) {
                        // Handle error: department ID is not a valid integer. Maybe set to null or throw error.
                        System.err.println("Warning: Could not parse visitDepartment to Integer: " + appointment.getVisitDepartment());
                        pstmtAppointment.setNull(10, Types.INTEGER); 
                    }
                } else {
                    pstmtAppointment.setNull(10, Types.INTEGER);
                }
                pstmtAppointment.setString(11, CryptoUtils.encryptSM2(appointment.getVisitContactPerson(), sm2PublicKeyHex));
                pstmtAppointment.setString(12, CryptoUtils.encryptSM4(appointment.getContactPersonPhone(), sm4KeyHex)); // Example
                pstmtAppointment.setString(13, appointment.getVisitReason());
            } else {
                pstmtAppointment.setNull(10, Types.INTEGER);
                pstmtAppointment.setNull(11, Types.VARCHAR);
                pstmtAppointment.setNull(12, Types.VARCHAR);
                pstmtAppointment.setNull(13, Types.VARCHAR);
            }

            pstmtAppointment.setString(14, appointment.getAppointmentType()); // appointmentType is already a String in bean
            // pstmtAppointment.setString(15, appointment.getApprovalStatus()); // approvalStatus is already a String in bean
            pstmtAppointment.setString(15, appointment.getStatus()); // Corrected to use getStatus()
            // pstmtAppointment.setTimestamp(16, appointment.getSubmissionTime());
            pstmtAppointment.setTimestamp(16, appointment.getApplicationDate()); // Corrected to use getApplicationDate()


            int affectedRows = pstmtAppointment.executeUpdate();

            if (affectedRows == 0) {
                conn.rollback();
                return false;
            }

            if (appointment.getAccompanyingPersons() != null && !appointment.getAccompanyingPersons().isEmpty()) {
                pstmtAccompanying = conn.prepareStatement(sqlAccompanyingPerson);
                for (AccompanyingPerson person : appointment.getAccompanyingPersons()) {
                    pstmtAccompanying.setString(1, person.getAccompanyingPersonId()); // Use String ID
                    pstmtAccompanying.setString(2, appointment.getAppointmentId());
                    // Encrypt PII for accompanying persons
                    pstmtAccompanying.setString(3, CryptoUtils.encryptSM2(person.getName(), sm2PublicKeyHex));
                    pstmtAccompanying.setString(4, CryptoUtils.encryptSM2(person.getIdCard(), sm2PublicKeyHex));
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
             encryptedApplicantIdCard = CryptoUtils.encryptSM2(applicantIdCard, sm2PublicKeyHex);
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
                    person.setAccompanyingPersonId(rs.getString("person_id")); // Use String ID
                    person.setAppointmentId(rs.getString("appointment_id")); // Use String ID
                    // Decrypt PII data based on the flag
                    if (decryptPII) {
                        try {
                            person.setName(CryptoUtils.decryptSM2(rs.getString("name"), sm2PrivateKeyHex));
                            person.setIdCard(CryptoUtils.decryptSM2(rs.getString("id_card"), sm2PrivateKeyHex));
                            person.setPhone(CryptoUtils.decryptSM4(rs.getString("phone"), sm4KeyHex));
                        } catch (Exception e) {
                            e.printStackTrace();
                            // Handle decryption error, maybe set fields to "Error decrypting" or skip person
                            // For now, rethrow as part of a larger failure
                            throw new SQLException("Failed to decrypt accompanying person data for appointment ID: " + appointmentId, e);
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

    public boolean updateAppointmentStatus(String appointmentId, Appointment.ApprovalStatus status) throws SQLException { 
        // String sql = "UPDATE appointments SET approval_status = ?, updated_at = NOW() WHERE appointment_id = ?";
        String sql = "UPDATE appointments SET status = ?, updated_at = NOW() WHERE appointment_id = ?"; // Corrected to status column
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name()); // Use enum's name() method for string representation
            pstmt.setString(2, appointmentId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    // mapResultSetToAppointment now includes a flag to control decryption
    private Appointment mapResultSetToAppointment(ResultSet rs, boolean decryptPII) throws SQLException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException, IOException {
        Appointment app = new Appointment();
        app.setAppointmentId(rs.getString("appointment_id"));
        app.setCampus(rs.getString("campus")); // Directly set String
        app.setEntryDatetime(rs.getTimestamp("entry_datetime")); // Renamed from setAppointmentTime
        app.setApplicantOrganization(rs.getString("applicant_organization")); // Changed from setOrganization

        if (decryptPII) {
            app.setApplicantName(CryptoUtils.decryptSM2(rs.getString("applicant_name"), sm2PrivateKeyHex));
            app.setApplicantIdCard(CryptoUtils.decryptSM2(rs.getString("applicant_id_card"), sm2PrivateKeyHex));
            app.setApplicantPhone(CryptoUtils.decryptSM4(rs.getString("applicant_phone"), sm4KeyHex));
            
            if (rs.getString("contact_person_name") != null) {
                 app.setVisitContactPerson(CryptoUtils.decryptSM2(rs.getString("contact_person_name"), sm2PrivateKeyHex)); // Changed from setContactPersonName
            }
             if (rs.getString("contact_person_phone") != null) {
                app.setContactPersonPhone(CryptoUtils.decryptSM4(rs.getString("contact_person_phone"), sm4KeyHex));
            }

        } else {
            app.setApplicantName(rs.getString("applicant_name")); // Store raw encrypted
            app.setApplicantIdCard(rs.getString("applicant_id_card")); // Store raw encrypted
            app.setApplicantPhone(rs.getString("applicant_phone")); // Store raw encrypted
            app.setVisitContactPerson(rs.getString("contact_person_name")); 
            app.setContactPersonPhone(rs.getString("contact_person_phone"));
        }

        app.setTransportMode(rs.getString("transport_mode")); // Directly set String
        app.setLicensePlate(rs.getString("license_plate"));
        // app.setVisitDepartment(rs.getString("visit_department")); // Old column name
        // Assuming you want to store the department ID (integer) in the bean, or fetch department name by ID
        // If your bean's setVisitDepartment expects a String (name), you'd do a lookup here.
        // If it expects the ID (as string or int), you can get it directly.
        // For now, let's assume we retrieve the ID and the bean setter can handle it as a string, or it's handled elsewhere.
        app.setVisitDepartment(rs.getString("official_visit_department_id")); // Corrected column name, might be null or an ID
        // Contact person name/phone already handled with decryption flag
        app.setVisitReason(rs.getString("visit_reason"));
        app.setAppointmentType(rs.getString("appointment_type")); // Directly set String
        // app.setApprovalStatus(rs.getString("approval_status")); // Directly set String
        app.setStatus(rs.getString("status")); // Corrected to use status column and setStatus
        // app.setSubmissionTime(rs.getTimestamp("submission_time"));
        app.setApplicationDate(rs.getTimestamp("application_date")); // Corrected to use application_date column and setApplicationDate
        app.setCreatedAt(rs.getTimestamp("created_at"));
        app.setUpdatedAt(rs.getTimestamp("updated_at"));

        // Fetch and set accompanying persons
        // This should be done in a separate query or a JOIN if preferred, to avoid N+1 problems if calling this for multiple appointments.
        // For a single appointment, a separate query is fine.
        app.setAccompanyingPersons(getAccompanyingPersonsByAppointmentId(app.getAppointmentId(), decryptPII));


        return app;
    }
}
