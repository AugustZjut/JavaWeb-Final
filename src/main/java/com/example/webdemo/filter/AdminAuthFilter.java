package com.example.webdemo.filter;

import com.example.webdemo.beans.User;

import jakarta.servlet.*; // Replaced javax.servlet with jakarta.servlet
import jakarta.servlet.http.HttpServletRequest; // Replaced javax.servlet with jakarta.servlet
import jakarta.servlet.http.HttpServletResponse; // Replaced javax.servlet with jakarta.servlet
import jakarta.servlet.http.HttpSession; // Replaced javax.servlet with jakarta.servlet
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminAuthFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(AdminAuthFilter.class);
    private Map<String, List<String>> rolePermissions; // Role -> List of allowed URL patterns

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        rolePermissions = new HashMap<>();
        // 定义每个角色的权限 - 改为数据库中的角色名称格式（全大写+下划线）
        // SCHOOL_ADMIN 可以访问所有 admin/* 路径
        rolePermissions.put("SCHOOL_ADMIN", Arrays.asList("/admin/.*")); // Regex for all admin paths

        // DEPARTMENT_ADMIN 可以访问用户管理（只读）、部门（只读）以及公务/公开预约管理
        rolePermissions.put("DEPARTMENT_ADMIN", Arrays.asList(
                "/admin/userList.jsp", "/admin/userManagement.*", 
                "/admin/departmentList.jsp", "/admin/departmentManagement.*", 
                "/admin/publicAppointmentList.jsp", "/admin/publicAppointmentManagement.*",
                "/admin/officialAppointmentList.jsp", "/admin/officialAppointmentManagement.*",
                "/admin/dashboard.jsp"
        ));

        // AUDIT_ADMIN 可以访问审计日志和控制台
        rolePermissions.put("AUDIT_ADMIN", Arrays.asList(
                "/admin/auditLogList.jsp", "/admin/auditLog.*",
                "/admin/dashboard.jsp"
        ));
        
        // SYSTEM_ADMIN 可以访问所有内容（与 SCHOOL_ADMIN 相同，可以更细化）
        rolePermissions.put("SYSTEM_ADMIN", Arrays.asList("/admin/.*"));
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);

        String contextPath = httpRequest.getContextPath();
        String requestURI = httpRequest.getRequestURI();
        String pathWithinApp = requestURI.substring(contextPath.length());
        String queryString = httpRequest.getQueryString();
        String fullPath = pathWithinApp + (queryString == null ? "" : "?" + queryString);

        logger.debug("处理请求: {}, 应用内路径: {}", requestURI, pathWithinApp);

        // 允许访问登录页面和静态资源
        if (pathWithinApp.endsWith("/admin/adminLogin.jsp") || pathWithinApp.endsWith("/adminLoginServlet") || 
            pathWithinApp.startsWith("/css/") || 
            pathWithinApp.startsWith("/js/")) {
            logger.debug("允许访问登录页面或静态资源: {}", pathWithinApp);
            chain.doFilter(request, response);
            return;
        }

        User adminUser = (session != null) ? (User) session.getAttribute("adminUser") : null;
        
        if (adminUser == null) {
            logger.warn("未登录用户尝试访问受保护资源: {}", pathWithinApp);
            httpResponse.sendRedirect(contextPath + "/admin/adminLogin.jsp");
            return;
        }

        String userRole = adminUser.getRole();
        logger.debug("用户: {}, 角色: {}, 尝试访问: {}", adminUser.getUsername(), userRole, pathWithinApp);
        
        if (userRole == null) {
            logger.error("用户角色为 null: {}", adminUser.getUsername());
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "访问被拒绝：用户角色未定义。");
            return;
        }
        
        List<String> allowedPaths = rolePermissions.get(userRole);

        if (allowedPaths != null) {
            for (String pattern : allowedPaths) {
                logger.debug("检查路径 {} 是否匹配模式 {}", pathWithinApp, pattern);
                if (pathWithinApp.matches(pattern)) {
                    logger.debug("路径匹配成功，允许访问");
                    chain.doFilter(request, response);
                    return;
                }
            }
        }
        
        // 如果没有匹配的模式，拒绝访问
        logger.warn("访问被拒绝：角色 {} 无权访问路径 {}", userRole, pathWithinApp);
        httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "访问被拒绝：您没有权限访问此资源。");
    }

    @Override
    public void destroy() {
        // 清理代码，如果有的话
    }
}
