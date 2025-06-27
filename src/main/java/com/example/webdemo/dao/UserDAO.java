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
        String sql = "SELECT * FROM users WHERE username = ?";
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

    public User findById(int userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId); // userId is int
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
        String sql = "SELECT * FROM users ORDER BY username";
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
        String sql = "SELECT * FROM users WHERE role = ? ORDER BY username";
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
            user.setPasswordLastChanged(new Timestamp(System.currentTimeMillis()));
        }
        String sql = "INSERT INTO users (username, password_hash, full_name, department_id, phone_number, role, password_last_changed, failed_login_attempts, lockout_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getFullName());
            if (user.getDepartmentId() != null) {
                pstmt.setInt(4, user.getDepartmentId());
            } else {
                pstmt.setNull(4, Types.INTEGER);
            }
            if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
                pstmt.setString(5, CryptoUtils.encryptSM4(user.getPhoneNumber(), sm4KeyHex));
            } else {
                pstmt.setNull(5, Types.VARCHAR);
            }
            pstmt.setString(6, user.getRole());
            pstmt.setTimestamp(7, user.getPasswordLastChanged());
            pstmt.setInt(8, 0); // Initial failed attempts
            pstmt.setNull(9, Types.TIMESTAMP); // Initial lockout time
            
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
        String sql = "UPDATE users SET full_name = ?, department_id = ?, phone_number = ?, role = ? WHERE user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getFullName());
            if (user.getDepartmentId() != null) {
                pstmt.setInt(2, user.getDepartmentId());
            } else {
                pstmt.setNull(2, Types.INTEGER);
            }
            if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
                pstmt.setString(3, CryptoUtils.encryptSM4(user.getPhoneNumber(), sm4KeyHex));
            } else {
                pstmt.setNull(3, Types.VARCHAR);
            }
            pstmt.setString(4, user.getRole());
            pstmt.setInt(5, user.getUserId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) { // Crypto exceptions
            e.printStackTrace();
            throw new SQLException("Encryption failed during user update.", e);
        }
    }

    public boolean deleteUser(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public boolean updatePassword(int userId, String newPassword) throws SQLException {
        String sql = "UPDATE users SET password_hash = ?, password_change_required = FALSE, password_last_changed = ?, failed_login_attempts = 0, lockout_time = NULL WHERE user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, CryptoUtils.generateSM3Hash(newPassword));
            pstmt.setTimestamp(2, new Timestamp(new Date().getTime())); // Set password_last_changed to now
            pstmt.setInt(3, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) { // CryptoUtils.generateSM3Hash can throw
            e.printStackTrace();
            throw new SQLException("Password hashing failed during password update.", e);
        }
    }

    /**
     * 更新用户密码 - 注意：此方法应只在用户重置密码或管理员修改密码时使用
     * @param user 包含更新后密码哈希和passwordLastChanged的用户对象
     * @return 是否更新成功
     */
    public boolean updateUserPassword(User user) throws SQLException {
        String sql = "UPDATE users SET password_hash = ?, password_last_changed = ?, failed_login_attempts = 0, lockout_time = NULL WHERE user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getPasswordHash());
            pstmt.setTimestamp(2, user.getPasswordLastChanged());
            pstmt.setInt(3, user.getUserId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * 更新用户的锁定状态
     * @param userId 用户ID
     * @param isLocked 是否锁定账户
     * @param lockDurationMinutes 如果锁定，锁定时长(分钟)，null表示解锁
     * @return 是否更新成功
     */
    public boolean updateLockStatus(int userId, boolean isLocked, Integer lockDurationMinutes) throws SQLException {
        String sql = "UPDATE users SET lockout_time = ?, failed_login_attempts = ? WHERE user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            if (isLocked && lockDurationMinutes != null) {
                // 设置锁定时间为当前时间加上指定分钟数
                long lockoutTimeMillis = System.currentTimeMillis() + (lockDurationMinutes * 60 * 1000L);
                pstmt.setTimestamp(1, new Timestamp(lockoutTimeMillis));
                pstmt.setInt(2, 5); // 设置为临界登录失败次数
            } else {
                // 解锁账户
                pstmt.setNull(1, Types.TIMESTAMP);
                pstmt.setInt(2, 0); // 重置登录失败次数
            }
            
            pstmt.setInt(3, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * 重置用户的登录失败次数
     * @param userId 用户ID
     * @return 是否重置成功
     */
    public boolean resetFailedLoginAttempts(int userId) throws SQLException {
        String sql = "UPDATE users SET failed_login_attempts = 0 WHERE user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public int incrementFailedLoginAttempts(String username) throws SQLException {
        String sqlSelect = "SELECT failed_login_attempts FROM users WHERE username = ?";
        String sqlUpdate = "UPDATE users SET failed_login_attempts = ? WHERE username = ?";
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
        String sql = "UPDATE users SET failed_login_attempts = 0 WHERE username = ?";
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
        String sql = "UPDATE users SET lockout_time = ? WHERE username = ?";
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
        String sql = "UPDATE users SET failed_login_attempts = 0, lockout_time = NULL WHERE username = ?";
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
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFullName(rs.getString("full_name"));
        int deptId = rs.getInt("department_id");
        user.setDepartmentId(rs.wasNull() ? null : deptId);
        String encryptedPhone = rs.getString("phone_number");
        if (decryptPII && encryptedPhone != null && !encryptedPhone.isEmpty() && sm4KeyHex != null && !sm4KeyHex.startsWith("YOUR")) {
            try {
                user.setPhoneNumber(CryptoUtils.decryptSM4(encryptedPhone, sm4KeyHex));
            } catch (Exception e) {
                System.err.println("Failed to decrypt phone for user " + user.getUsername() + ": " + e.getMessage());
                user.setPhoneNumber("Decryption Error"); // Or handle as appropriate
            }
        } else {
            user.setPhoneNumber(encryptedPhone); // Store raw encrypted or null, or if key is missing
        }

        user.setRole(rs.getString("role"));
        user.setPasswordLastChanged(rs.getTimestamp("password_last_changed"));
        user.setFailedLoginAttempts(rs.getInt("failed_login_attempts"));
        user.setLockoutTime(rs.getTimestamp("lockout_time"));
        user.setPasswordChangeRequired(rs.getBoolean("password_change_required"));
        return user;
    }
}
