package com.example.webdemo.dao;

import com.example.webdemo.beans.User;
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
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

public class UserDAO {

    private DataSource dataSource;
    private static String sm4KeyHex; // For phone number encryption

    static {
        try (InputStream input = DBUtils.class.getClassLoader().getResourceAsStream("db.properties")) {
            Properties prop = new Properties();
            if (input == null) {
                System.out.println("Sorry, unable to find db.properties for UserDAO");
            } else {
                prop.load(input);
                sm4KeyHex = prop.getProperty("sm4.keyHex");
                if (sm4KeyHex == null || sm4KeyHex.isEmpty() || sm4KeyHex.equals("YOUR_128_BIT_SM4_KEY_HEX_HERE_32_CHARS")) {
                    System.err.println("WARNING: SM4 key is not configured properly in db.properties for UserDAO. Phone encryption/decryption will fail.");
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace(); 
        }
    }

    public UserDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM Users WHERE username = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs, true); 
                }
            }
        } catch (SQLException e) {
            e.printStackTrace(); 
            throw e;
        } catch (Exception e) { // Crypto exceptions
            e.printStackTrace();
            throw new SQLException("Decryption/mapping failed while finding user by username.", e);
        }
        return null;
    }

    public User findById(String userId) throws SQLException {
        String sql = "SELECT * FROM Users WHERE user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId); // userId is String
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs, true); 
                }
            }
        } catch (SQLException e) {
            e.printStackTrace(); 
            throw e;
        } catch (Exception e) { // Crypto exceptions
            e.printStackTrace();
            throw new SQLException("Decryption/mapping failed while finding user by ID.", e);
        }
        return null;
    }

    public List<User> listAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM Users ORDER BY username";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs, true)); 
            }
        } catch (SQLException e) {
            e.printStackTrace(); 
            throw e;
        } catch (Exception e) { // Crypto exceptions
            e.printStackTrace();
            throw new SQLException("Decryption/mapping failed while listing all users.", e);
        }
        return users;
    }

    public List<User> getUsersByRole(String role) throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM Users WHERE role = ? ORDER BY username";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, role);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToUser(rs, true)); // Decrypt PII for display/editing
                }
            }
        } catch (SQLException e) {
            e.printStackTrace(); 
            throw e;
        } catch (Exception e) { // Crypto exceptions
            e.printStackTrace();
            throw new SQLException("Decryption/mapping failed while listing users by role.", e);
        }
        return users;
    }

    public boolean createUser(User user) throws SQLException {
        // Ensure passwordLastChanged is set, typically in the User bean constructor or service layer
        if (user.getPasswordLastChanged() == null) {
            user.setPasswordLastChanged(new Date());
        }
        String sql = "INSERT INTO Users (user_id, username, password_hash, name, department_id, phone, role, password_last_changed, failed_login_attempts, lockout_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getUserId()); // Changed from int to String
            pstmt.setString(2, user.getUsername());
            // Assuming user.getPasswordHash() already contains the pre-hashed password if creating from a bean that way
            // Or, if user.getPassword() was intended to be a raw password for new users:
            // pstmt.setString(3, CryptoUtils.generateSM3Hash(user.getPassword())); // Hash the raw password
            // For now, let's assume passwordHash is set correctly in the bean before calling createUser
            pstmt.setString(3, user.getPasswordHash()); // Use getPasswordHash()
            pstmt.setString(4, user.getFullName()); // Changed from getName()
            pstmt.setString(5, user.getDepartmentId());
            if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                pstmt.setString(6, CryptoUtils.encryptSM4(user.getPhone(), sm4KeyHex));
            } else {
                pstmt.setNull(6, Types.VARCHAR);
            }
            pstmt.setString(7, user.getRole());
            pstmt.setTimestamp(8, new Timestamp(user.getPasswordLastChanged().getTime()));
            pstmt.setInt(9, 0); // Initial failed attempts
            pstmt.setNull(10, Types.TIMESTAMP); // Initial lockout time
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) { // Crypto exceptions
            e.printStackTrace();
            throw new SQLException("Hashing/Encryption failed during user creation.", e);
        }
    }

    public boolean updateUser(User user) throws SQLException {
        // Note: Password update should be a separate method for security reasons.
        // This method does not update the password_hash, password_last_changed, failed_login_attempts, or lockout_time.
        String sql = "UPDATE Users SET name = ?, department_id = ?, phone = ?, role = ? WHERE user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getFullName()); // Changed from getName()
            pstmt.setString(2, user.getDepartmentId());
            if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                pstmt.setString(3, CryptoUtils.encryptSM4(user.getPhone(), sm4KeyHex));
            } else {
                pstmt.setNull(3, Types.VARCHAR);
            }
            pstmt.setString(4, user.getRole());
            pstmt.setString(5, user.getUserId()); // userId is String
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) { // Crypto exceptions
            e.printStackTrace();
            throw new SQLException("Encryption failed during user update.", e);
        }
    }

    public boolean deleteUser(String userId) throws SQLException {
        String sql = "DELETE FROM Users WHERE user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public boolean updatePassword(String userId, String newPassword) throws SQLException {
        String sql = "UPDATE Users SET password_hash = ?, password_last_changed = ?, failed_login_attempts = 0, lockout_time = NULL WHERE user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, CryptoUtils.generateSM3Hash(newPassword));
            pstmt.setTimestamp(2, new Timestamp(new Date().getTime())); // Set password_last_changed to now
            pstmt.setString(3, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) { // CryptoUtils.generateSM3Hash can throw
            e.printStackTrace();
            throw new SQLException("Password hashing failed during password update.", e);
        }
    }

    public int incrementFailedLoginAttempts(String username) throws SQLException {
        String sqlSelect = "SELECT failed_login_attempts FROM Users WHERE username = ?";
        String sqlUpdate = "UPDATE Users SET failed_login_attempts = ? WHERE username = ?";
        int attempts = 0;
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmtSelect = conn.prepareStatement(sqlSelect)) {
                pstmtSelect.setString(1, username);
                ResultSet rs = pstmtSelect.executeQuery();
                if (rs.next()) {
                    attempts = rs.getInt("failed_login_attempts");
                }
            }
            attempts++;
            try (PreparedStatement pstmtUpdate = conn.prepareStatement(sqlUpdate)) {
                pstmtUpdate.setInt(1, attempts);
                pstmtUpdate.setString(2, username);
                pstmtUpdate.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            // Consider rollback
            e.printStackTrace();
            throw e;
        } finally {
            // Ensure auto-commit is reset if necessary, though HikariCP usually handles this.
        }
        return attempts;
    }

    public void resetFailedLoginAttempts(String username) throws SQLException {
        String sql = "UPDATE Users SET failed_login_attempts = 0 WHERE username = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void lockUserAccount(String username, Timestamp lockoutTime) throws SQLException {
        String sql = "UPDATE Users SET lockout_time = ? WHERE username = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, lockoutTime);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }
    
    public void unlockUserAccount(String username) throws SQLException {
        String sql = "UPDATE Users SET failed_login_attempts = 0, lockout_time = NULL WHERE username = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    private User mapResultSetToUser(ResultSet rs, boolean decryptPII) throws SQLException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException, IOException {
        User user = new User();
        user.setUserId(rs.getString("user_id")); // Changed from getInt to getString
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash")); // Changed from setPassword
        user.setFullName(rs.getString("name")); // Changed from setName, and column name is 'name'
        user.setDepartmentId(rs.getString("department_id"));
        
        String encryptedPhone = rs.getString("phone");
        if (decryptPII && encryptedPhone != null && !encryptedPhone.isEmpty() && sm4KeyHex != null && !sm4KeyHex.startsWith("YOUR")) {
            try {
                user.setPhone(CryptoUtils.decryptSM4(encryptedPhone, sm4KeyHex));
            } catch (Exception e) {
                System.err.println("Failed to decrypt phone for user " + user.getUsername() + ": " + e.getMessage());
                user.setPhone("Decryption Error"); // Or handle as appropriate
            }
        } else {
            user.setPhone(encryptedPhone); // Store raw encrypted or null, or if key is missing
        }

        user.setRole(rs.getString("role"));
        user.setPasswordLastChanged(rs.getTimestamp("password_last_changed"));
        user.setFailedLoginAttempts(rs.getInt("failed_login_attempts"));
        user.setLockoutTime(rs.getTimestamp("lockout_time"));
        return user;
    }
}
