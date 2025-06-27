package com.example.webdemo.servlet.admin;

import com.example.webdemo.beans.AuditLog;
import com.example.webdemo.beans.User;
import com.example.webdemo.dao.AuditLogDAO;
import com.example.webdemo.dao.UserDAO;
import com.example.webdemo.util.CryptoUtils;
import com.example.webdemo.util.DBUtils; // Added import for DBUtils
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

@WebServlet("/adminLoginServlet")
public class AdminLoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(AdminLoginServlet.class);
    private UserDAO userDAO;
    private AuditLogDAO auditLogDAO;
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION = 15 * 60 * 1000; // 15 minutes

    @Override
    public void init() throws ServletException {
        super.init();
        // UserDAO now requires a DataSource in its constructor
        userDAO = new UserDAO(DBUtils.getDataSource()); 
        auditLogDAO = new AuditLogDAO();
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String loginName = request.getParameter("loginName");
        String password = request.getParameter("password");
        HttpSession session = request.getSession();

        // 记录登录尝试到日志（不打印敏感信息）
        logger.debug("Login attempt received for username: {}", loginName);
        
        logger.info("Attempting login for user: {}", loginName);

        User user = null;

        try {
            // Changed from getUserByLoginName to findByUsername
            user = userDAO.findByUsername(loginName); 
            
            if (user != null) {
                
                logger.debug("User found: {}", user.getUsername());
                // Changed from isLocked() to a combination of getLockoutTime() and System.currentTimeMillis()
                if (user.getLockoutTime() != null && user.getLockoutTime().getTime() > System.currentTimeMillis()) {
                    long lockoutEndTime = user.getLockoutTime().getTime(); // This is already the end time
                    // The original logic was: user.getLastFailedLoginTime().getTime() + LOCKOUT_DURATION;
                    // Assuming lockoutTime in DB directly stores when the lock expires.
                    // If lockoutTime stores when the lock *started*, then the original logic is needed.
                    // For now, assuming lockoutTime is the expiry time.
                    // If it's the start time, it should be: 
                    // long lockoutEndTime = user.getLockoutTime().getTime() + LOCKOUT_DURATION; 
                    // And the field should probably be named lastLockoutAttemptTime or similar.
                    // Let's stick to user.getLockoutTime() as the expiry for now.

                    logger.warn("User account is locked: {}. Lockout expires at: {}", loginName, new Timestamp(lockoutEndTime));
                    session.setAttribute("loginError", "账户已锁定，请稍后再试。 当前锁定截止时间: " + new Timestamp(lockoutEndTime));
                    response.sendRedirect("admin/adminLogin.jsp");
                    return;
                } else if (user.getLockoutTime() != null && user.getLockoutTime().getTime() <= System.currentTimeMillis()) {
                    // Lock has expired, unlock the user
                    logger.info("User lock has expired, unlocking account for: {}", loginName);
                    userDAO.unlockUserAccount(user.getUsername()); // Changed from unlockUser(user.getUserId())
                    user.setLockoutTime(null); // Update local object
                    user.setFailedLoginAttempts(0); // Reset attempts
                    // userDAO.updateUserLockout(user); // unlockUserAccount should handle this
                }

                // Changed from sm3Hash to generateSM3Hash
                String hashedPassword = CryptoUtils.generateSM3Hash(password); 
                logger.debug("Password hash generated for input. Comparing with stored hash.");
                
                // 使用日志记录密码验证尝试（不打印实际哈希值）
                logger.debug("Comparing password hash for user: {}", user.getUsername());

                if (user.getPasswordHash().equals(hashedPassword)) {
                    // Login successful
                    logger.info("Password match for user: {}. Login successful.", loginName);
                    userDAO.resetFailedLoginAttempts(user.getUsername()); // Changed from getUserId()
                    session.setAttribute("adminUser", user);

                    // Audit log
                    AuditLog log = new AuditLog();
                    log.setUserId(user.getUserId()); 
                    log.setUsername(user.getUsername()); // Changed from getLoginName()
                    log.setActionType("ADMIN_LOGIN_SUCCESS");
                    // 修复方法名: setActionDetails -> setDetails
                    log.setDetails("Admin user " + user.getUsername() + " logged in successfully.");
                    // 修复方法名: setActionTime -> setLogTimestamp
                    log.setLogTimestamp(new Timestamp(new Date().getTime()));
                    // 修复方法名: setClientIp -> setIpAddress
                    log.setIpAddress(request.getRemoteAddr());
                    auditLogDAO.createLog(log); 
                    logger.debug("Audit log created for successful login of user: {}", loginName);

                    // 修改为使用上下文路径的绝对路径
                    response.sendRedirect(request.getContextPath() + "/admin/dashboard.jsp");
                } else {
                    // Login failed
                    logger.warn("Password mismatch for user: {}", loginName);
                    int attempts = userDAO.incrementFailedLoginAttempts(user.getUsername()); // Changed from getUserId()
                    logger.debug("Failed login attempts for user {}: {}", loginName, attempts);
                    user.setFailedLoginAttempts(attempts); // update local user object with new count

                    if (attempts >= MAX_LOGIN_ATTEMPTS) {
                        logger.warn("User account for {} has been locked due to too many failed login attempts.", loginName);
                        long lockoutExpiryTime = System.currentTimeMillis() + LOCKOUT_DURATION;
                        userDAO.lockUserAccount(user.getUsername(), new Timestamp(lockoutExpiryTime));
                        session.setAttribute("loginError", "密码错误次数过多，账户已锁定。");
                    } else {
                        session.setAttribute("loginError", "用户名或密码错误。");
                    }
                    response.sendRedirect("admin/adminLogin.jsp");
                }
            } else {
                logger.warn("Login failed. User not found: {}", loginName);
                session.setAttribute("loginError", "用户名或密码错误。");
                response.sendRedirect("admin/adminLogin.jsp");
            }
        } catch (SQLException e) {
            logger.error("Database error during login for user: " + loginName, e);
            e.printStackTrace(); // 添加堆栈跟踪以便在控制台上查看
            session.setAttribute("loginError", "数据库错误，请联系管理员。");
            response.sendRedirect("admin/adminLogin.jsp");
        } catch (Exception e) {
            logger.error("An unexpected error occurred during login for user: " + loginName, e);
            e.printStackTrace(); // 添加堆栈跟踪以便在控制台上查看
            session.setAttribute("loginError", "系统发生未知错误，请联系管理员。" + e.getMessage());
            response.sendRedirect("admin/adminLogin.jsp");
        }
    }
}
