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

public class AdminAuthFilter implements Filter {

    private Map<String, List<String>> rolePermissions; // Role -> List of allowed URL patterns

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        rolePermissions = new HashMap<>();
        // Define permissions for each role
        // SchoolAdmin can access all admin/* paths
        rolePermissions.put("SchoolAdmin", Arrays.asList("/admin/.*")); // Regex for all admin paths

        // DepartmentAdmin can access user management (view only), department (view only), and official/public appointments
        rolePermissions.put("DepartmentAdmin", Arrays.asList(
                "/admin/userList.jsp", "/admin/UserManagementServlet\\?action=list", // Corrected: Escaped '?' for regex
                "/admin/departmentList.jsp", "/admin/DepartmentManagementServlet\\?action=list", // Corrected: Escaped '?' for regex
                "/admin/publicAppointmentList.jsp", "/admin/PublicAppointmentServlet.*",
                "/admin/officialAppointmentList.jsp", "/admin/OfficialAppointmentServlet.*",
                "/admin/dashboard.jsp"
        ));

        // AuditAdmin can access audit logs and dashboard
        rolePermissions.put("AuditAdmin", Arrays.asList(
                "/admin/auditLogList.jsp", "/admin/AuditLogServlet.*",
                "/admin/dashboard.jsp"
        ));
        
        // SystemAdmin can access everything (same as SchoolAdmin for simplicity here, can be more granular)
        rolePermissions.put("SystemAdmin", Arrays.asList("/admin/.*"));
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);

        String requestURI = httpRequest.getRequestURI();
        String queryString = httpRequest.getQueryString();
        String fullPath = requestURI + (queryString == null ? "" : "?" + queryString);


        // Allow access to login page and static resources
        if (requestURI.endsWith("adminLogin.jsp") || requestURI.endsWith("AdminLoginServlet") || 
            requestURI.startsWith(httpRequest.getContextPath() + "/css/") || 
            requestURI.startsWith(httpRequest.getContextPath() + "/js/")) {
            chain.doFilter(request, response);
            return;
        }

        User adminUser = (session != null) ? (User) session.getAttribute("adminUser") : null;

        if (adminUser == null) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/admin/adminLogin.jsp");
            return;
        }

        String userRole = adminUser.getRole();
        if (userRole == null) {
            System.err.println("User role is null for user: " + adminUser.getUsername());
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied: User role not defined.");
            return;
        }
        
        List<String> allowedPaths = rolePermissions.get(userRole);

        if (allowedPaths != null) {
            for (String pattern : allowedPaths) {
                if (fullPath.matches(httpRequest.getContextPath() + pattern)) {
                    chain.doFilter(request, response);
                    return;
                }
            }
        }
        
        // If no pattern matched, deny access
        System.out.println("Access Denied for role " + userRole + " to path " + fullPath);
        httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied: You do not have permission to access this resource.");
    }

    @Override
    public void destroy() {
        // Cleanup code, if any
    }
}
