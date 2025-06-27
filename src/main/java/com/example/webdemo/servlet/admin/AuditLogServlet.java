package com.example.webdemo.servlet.admin;

import com.example.webdemo.beans.AuditLog;
import com.example.webdemo.beans.User;
import com.example.webdemo.dao.AuditLogDAO;
import com.example.webdemo.util.DBUtils;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 处理审计日志查询和展示的Servlet
 */
@WebServlet("/admin/auditLog")
public class AuditLogServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(AuditLogServlet.class);
    private AuditLogDAO auditLogDAO;
    private static final int PAGE_SIZE = 15; // 每页显示的记录数

    @Override
    public void init() throws ServletException {
        super.init();
        auditLogDAO = new AuditLogDAO(DBUtils.getDataSource());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User adminUser = (session != null) ? (User) session.getAttribute("adminUser") : null;
        
        if (adminUser == null) {
            response.sendRedirect(request.getContextPath() + "/admin/adminLogin.jsp");
            return;
        }
        
        // 检查权限
        String userRole = adminUser.getRole();
        if (!("AUDIT_ADMIN".equals(userRole) || "SYSTEM_ADMIN".equals(userRole))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "访问被拒绝：您没有权限访问此资源。");
            return;
        }

        try {
            // 分页参数
            int page = 1;
            try {
                String pageStr = request.getParameter("page");
                if (pageStr != null && !pageStr.isEmpty()) {
                    page = Integer.parseInt(pageStr);
                    if (page < 1) page = 1;
                }
            } catch (NumberFormatException e) {
                logger.warn("无效的页码参数", e);
                page = 1;
            }

            // 筛选参数
            String username = request.getParameter("username");
            String actionType = request.getParameter("actionType");
            
            // 日期过滤
            Timestamp startDate = null;
            Timestamp endDate = null;
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            
            try {
                String startDateStr = request.getParameter("startDate");
                if (startDateStr != null && !startDateStr.isEmpty()) {
                    Date date = dateFormat.parse(startDateStr);
                    startDate = new Timestamp(date.getTime());
                }
            } catch (Exception e) {
                logger.warn("无效的开始日期格式", e);
            }
            
            try {
                String endDateStr = request.getParameter("endDate");
                if (endDateStr != null && !endDateStr.isEmpty()) {
                    Date date = dateFormat.parse(endDateStr);
                    // 设置为当天的结束时间 23:59:59.999
                    date.setHours(23);
                    date.setMinutes(59);
                    date.setSeconds(59);
                    endDate = new Timestamp(date.getTime() + 999);
                }
            } catch (Exception e) {
                logger.warn("无效的结束日期格式", e);
            }

            // 获取总记录数
            int totalRecords = auditLogDAO.countFilteredLogs(username, actionType, startDate, endDate);
            int totalPages = (int) Math.ceil((double) totalRecords / PAGE_SIZE);
            
            // 确保页码在有效范围内
            if (page > totalPages && totalPages > 0) {
                page = totalPages;
            }
            
            // 计算偏移量
            int offset = (page - 1) * PAGE_SIZE;
            
            // 查询日志记录
            List<AuditLog> logs = auditLogDAO.getFilteredLogs(username, actionType, startDate, endDate, offset, PAGE_SIZE);
            
            // 设置请求属性
            request.setAttribute("auditLogs", logs);
            request.setAttribute("currentPage", page);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("totalRecords", totalRecords);
            
            // 记录操作日志
            AuditLog log = new AuditLog();
            log.setUserId(adminUser.getUserId());
            log.setUsername(adminUser.getUsername());
            log.setActionType("VIEW_AUDIT_LOGS");
            log.setDetails("查看审计日志，筛选条件：" + 
                    (username != null ? "用户名=" + username + ", " : "") +
                    (actionType != null ? "操作类型=" + actionType + ", " : "") +
                    (startDate != null ? "开始日期=" + startDate + ", " : "") +
                    (endDate != null ? "结束日期=" + endDate : ""));
            log.setLogTimestamp(new Timestamp(System.currentTimeMillis()));
            log.setIpAddress(request.getRemoteAddr());
            auditLogDAO.createLog(log);

            // 转发到 JSP 页面
            request.getRequestDispatcher("/admin/auditLogList.jsp").forward(request, response);
            
        } catch (Exception e) {
            logger.error("查询审计日志时发生错误", e);
            request.setAttribute("error", "查询审计日志时发生错误：" + e.getMessage());
            request.getRequestDispatcher("/admin/auditLogList.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        // 可以用于处理导出日志等功能，这里暂时转发到 doGet
        doGet(request, response);
    }
}
