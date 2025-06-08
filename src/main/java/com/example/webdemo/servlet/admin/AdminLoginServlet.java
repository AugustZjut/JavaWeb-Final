package com.example.webdemo.servlet.admin;

import com.example.webdemo.beans.AuditLog;
import com.example.webdemo.beans.User;
import com.example.webdemo.dao.AuditLogDAO;
import com.example.webdemo.dao.UserDAO;
import com.example.webdemo.util.CryptoUtils;
import com.example.webdemo.util.DBUtils; // Added import for DBUtils

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

        User user = null;

        try {
            // Changed from getUserByLoginName to findByUsername
            user = userDAO.findByUsername(loginName); 

            if (user != null) {
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

                    session.setAttribute("loginError", "账户已锁定，请稍后再试。 当前锁定截止时间: " + new Timestamp(lockoutEndTime));
                    response.sendRedirect("admin/adminLogin.jsp");
                    return;
                } else if (user.getLockoutTime() != null && user.getLockoutTime().getTime() <= System.currentTimeMillis()) {
                    // Lock has expired, unlock the user
                    userDAO.unlockUserAccount(user.getUsername()); // Changed from unlockUser(user.getUserId())
                    user.setLockoutTime(null); // Update local object
                    user.setFailedLoginAttempts(0); // Reset attempts
                    // userDAO.updateUserLockout(user); // unlockUserAccount should handle this
                }

                // Changed from sm3Hash to generateSM3Hash
                String hashedPassword = CryptoUtils.generateSM3Hash(password); 
                if (user.getPasswordHash().equals(hashedPassword)) {
                    // Login successful
                    userDAO.resetFailedLoginAttempts(user.getUsername()); // Changed from getUserId()
                    session.setAttribute("adminUser", user);

                    // Audit log
                    AuditLog log = new AuditLog();
                    log.setUserId(user.getUserId()); 
                    log.setUsername(user.getUsername()); // Changed from getLoginName()
                    log.setActionType("ADMIN_LOGIN_SUCCESS");
                    log.setActionDetails("Admin user " + user.getUsername() + " logged in successfully."); // Changed from getLoginName()
                    log.setActionTime(new Timestamp(new Date().getTime())); // Changed from setTimestamp
                    log.setClientIp(request.getRemoteAddr()); // Changed from setIpAddress
                    auditLogDAO.createLog(log); 

                    response.sendRedirect("admin/dashboard.jsp");
                } else {
                    // Login failed
                    int attempts = userDAO.incrementFailedLoginAttempts(user.getUsername()); // Changed from getUserId()
                    user.setFailedLoginAttempts(attempts); // update local user object with new count
                    // user.setLastFailedLoginTime(new Timestamp(System.currentTimeMillis())); // User bean does not have this field, UserDAO handles attempt times
                    // userDAO.updateUserLockout(user); // incrementFailedLoginAttempts and lockUserAccount handle this

                    if (attempts >= MAX_LOGIN_ATTEMPTS) {
                        // Lock the account by setting lockout_time to current time + duration
                        Timestamp lockoutExpiryTime = new Timestamp(System.currentTimeMillis() + LOCKOUT_DURATION);
                        userDAO.lockUserAccount(user.getUsername(), lockoutExpiryTime); // Changed from lockUser(user.getUserId())
                        user.setLockoutTime(lockoutExpiryTime); // Update local user object
                        session.setAttribute("loginError", "密码错误次数过多，账户已锁定。");
                    } else {
                        session.setAttribute("loginError", "用户名或密码错误。");
                    }

                    // Audit log for failed attempt
                    AuditLog log = new AuditLog();
                    log.setUserId(user.getUserId()); 
                    log.setUsername(user.getUsername()); // Changed from getLoginName()
                    log.setActionType("ADMIN_LOGIN_FAILURE");
                    log.setActionDetails("Admin user " + user.getUsername() + " failed login attempt."); // Changed from getLoginName()
                    log.setActionTime(new Timestamp(new Date().getTime())); // Changed from setTimestamp
                    log.setClientIp(request.getRemoteAddr()); // Changed from setIpAddress
                    auditLogDAO.createLog(log); 

                    response.sendRedirect("admin/adminLogin.jsp");
                }
            } else {
                // User not found
                session.setAttribute("loginError", "用户名或密码错误。");
                response.sendRedirect("admin/adminLogin.jsp");
            }
        } catch (SQLException e) {
            // Log and handle database errors
            e.printStackTrace(); // Proper logging should be used
            session.setAttribute("loginError", "数据库错误，请稍后再试。");
            response.sendRedirect("admin/adminLogin.jsp");
        } catch (Exception e) {
            // Catch other potential exceptions (e.g., from CryptoUtils)
            e.printStackTrace(); // Proper logging
            session.setAttribute("loginError", "发生内部错误，请联系管理员。");
            response.sendRedirect("admin/adminLogin.jsp");
        }
    }
}
