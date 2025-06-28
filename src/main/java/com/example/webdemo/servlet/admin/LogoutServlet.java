package com.example.webdemo.servlet.admin;

import com.example.webdemo.beans.AuditLog;
import com.example.webdemo.beans.User;
import com.example.webdemo.dao.AuditLogDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;

/**
 * 处理管理员注销的Servlet
 */
public class LogoutServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(LogoutServlet.class);
    private AuditLogDAO auditLogDAO;
    
    @Override
    public void init() throws ServletException {
        super.init();
        auditLogDAO = new AuditLogDAO();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        
        if (session != null) {
            User adminUser = (User) session.getAttribute("adminUser");
            if (adminUser != null) {
                // 记录注销日志
                try {
                    AuditLog log = new AuditLog();
                    log.setUserId(adminUser.getUserId());
                    log.setUsername(adminUser.getUsername());
                    log.setActionType("ADMIN_LOGOUT");
                    log.setDetails("管理员 " + adminUser.getUsername() + " 注销登录");
                    log.setLogTimestamp(new Timestamp(new Date().getTime()));
                    log.setIpAddress(request.getRemoteAddr());
                    auditLogDAO.createLog(log);
                    
                    logger.info("管理员 {} 已注销", adminUser.getUsername());
                } catch (Exception e) {
                    logger.error("记录注销日志时出错", e);
                }
            } else {
                logger.debug("未登录用户尝试注销，session存在但无adminUser");
            }
            
            // 销毁会话
            session.invalidate();
        } else {
            logger.debug("未登录用户尝试注销，session不存在");
        }
        
        // 重定向到登录页
        response.sendRedirect(request.getContextPath() + "/admin/adminLogin.jsp");
    }
}
