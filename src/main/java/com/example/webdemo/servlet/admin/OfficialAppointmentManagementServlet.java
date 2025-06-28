package com.example.webdemo.servlet.admin;

import com.example.webdemo.beans.Appointment;
import com.example.webdemo.beans.User;
import com.example.webdemo.beans.AuditLog;
import com.example.webdemo.beans.Department;
import com.example.webdemo.dao.AppointmentDAO;
import com.example.webdemo.dao.AuditLogDAO;
import com.example.webdemo.dao.DepartmentDAO;
import com.example.webdemo.util.DataMaskingUtils;
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
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

// @WebServlet("/admin/officialAppointmentManagement") // Configured in web.xml
public class OfficialAppointmentManagementServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(OfficialAppointmentManagementServlet.class);
    private AppointmentDAO appointmentDAO;
    private AuditLogDAO auditLogDAO;
    private DepartmentDAO departmentDAO;

    @Override
    public void init() throws ServletException {
        appointmentDAO = new AppointmentDAO(DBUtils.getDataSource());
        auditLogDAO = new AuditLogDAO();
        departmentDAO = new DepartmentDAO();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        User loggedInAdmin = checkAccess(request, response);
        if (loggedInAdmin == null) {
            return;
        }

        String action = request.getParameter("action");
        if (action == null) {
            action = "list";
        }

        try {
            switch (action) {
                case "list":
                    listOfficialAppointments(request, response, loggedInAdmin);
                    break;
                case "search":
                    searchOfficialAppointments(request, response, loggedInAdmin);
                    break;
                case "detail": // Corrected from "view"
                    viewAppointmentDetails(request, response, loggedInAdmin);
                    break;
                case "statistics":
                    showStatistics(request, response, loggedInAdmin);
                    break;
                case "approve": // Quick approve from list
                    updateStatusFromList(request, response, loggedInAdmin, "APPROVED");
                    break;
                case "reject": // Quick reject from list
                    updateStatusFromList(request, response, loggedInAdmin, "REJECTED");
                    break;
                default:
                    listOfficialAppointments(request, response, loggedInAdmin);
                    break;
            }
        } catch (SQLException ex) {
            logger.error("数据库错误", ex);
            request.setAttribute("error", "数据库错误: " + ex.getMessage());
            try {
                listOfficialAppointments(request, response, loggedInAdmin);
            } catch (Exception e) {
                logger.error("显示错误页面失败", e);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "系统错误");
            }
        } catch (Exception ex) {
            logger.error("系统错误", ex);
            request.setAttribute("error", "系统错误: " + ex.getMessage());
            try {
                listOfficialAppointments(request, response, loggedInAdmin);
            } catch (Exception e) {
                logger.error("显示错误页面失败", e);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "系统错误");
            }
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        User loggedInAdmin = checkAccess(request, response);
        if (loggedInAdmin == null) {
            return;
        }
        
        String action = request.getParameter("action");
        String appointmentIdStr = request.getParameter("appointmentId");
        logger.info("处理POST请求，action={}, appointmentId={}", action, appointmentIdStr);
        
        if (action == null || action.trim().isEmpty()) {
            logger.error("POST请求缺少action参数");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少操作参数");
            return;
        }

        try {
            switch (action) {
                case "approve":
                    logger.info("准备执行批准操作");
                    updateAppointmentStatus(request, response, loggedInAdmin, "APPROVED");
                    break;
                case "reject":
                    logger.info("准备执行驳回操作");
                    updateAppointmentStatus(request, response, loggedInAdmin, "REJECTED");
                    break;
                case "statistics":
                     logger.info("准备显示统计信息");
                     showStatistics(request, response, loggedInAdmin);
                     break;
                default:
                    logger.warn("未知的action值: {}", action);
                    // Forward to detail view for other POST actions if any, or show error
                    if (appointmentIdStr != null && !appointmentIdStr.trim().isEmpty()) {
                        logger.info("尝试显示预约详情");
                        viewAppointmentDetails(request, response, loggedInAdmin);
                    } else {
                        logger.error("无法处理请求：缺少appointmentId参数");
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少预约ID参数");
                    }
                    break;
            }
        } catch (SQLException ex) {
            logger.error("数据库操作失败", ex);
            // 将错误消息存入session
            request.getSession().setAttribute("error", "数据库操作失败: " + ex.getMessage());
            
            // 重定向到列表页面
            response.sendRedirect(request.getContextPath() + "/admin/officialAppointmentManagement");
        } catch (Exception ex) {
            logger.error("系统错误", ex);
            // 将错误消息存入session
            request.getSession().setAttribute("error", "系统错误: " + ex.getMessage());
            
            // 重定向到列表页面
            response.sendRedirect(request.getContextPath() + "/admin/officialAppointmentManagement");
        }
    }

    private User checkAccess(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        HttpSession session = request.getSession(false);
        User loggedInAdmin = (session != null) ? (User) session.getAttribute("adminUser") : null;

        if (loggedInAdmin == null) {
            response.sendRedirect(request.getContextPath() + "/admin/adminLogin.jsp");
            return null;
        }

        // 权限检查：系统管理员、学校管理员，或有公务预约管理权限的部门管理员
        boolean hasAccess = "SYSTEM_ADMIN".equals(loggedInAdmin.getRole()) ||
                           "SCHOOL_ADMIN".equals(loggedInAdmin.getRole()) ||
                           ("DEPARTMENT_ADMIN".equals(loggedInAdmin.getRole()) && 
                            loggedInAdmin.isCanManageOfficialAppointments());

        if (!hasAccess) {
            logger.warn("用户 {} 尝试访问公务预约管理，但没有权限", loggedInAdmin.getUsername());
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "您没有权限访问此功能");
            return null;
        }

        return loggedInAdmin;
    }

    private void listOfficialAppointments(HttpServletRequest request, HttpServletResponse response, 
                                       User loggedInAdmin) throws Exception {
        int page = 1;
        int pageSize = 20;
        
        String pageParam = request.getParameter("page");
        if (pageParam != null && !pageParam.isEmpty()) {
            try {
                page = Integer.parseInt(pageParam);
            } catch (NumberFormatException e) {
                page = 1;
            }
        }

        Integer departmentIdForFilter = getDepartmentIdForFilter(loggedInAdmin);

        List<Appointment> appointments = appointmentDAO.getOfficialAppointments(page, pageSize, departmentIdForFilter);
        int totalCount = appointmentDAO.getOfficialAppointmentCount(departmentIdForFilter);
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);

        maskPiiData(appointments);

        request.setAttribute("appointments", appointments);
        request.setAttribute("currentPage", page);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("totalCount", totalCount);

        // 获取部门列表用于搜索表单
        fetchAllDepartments(request);

        logger.debug("用户 {} 查看公务预约列表，第 {} 页", loggedInAdmin.getUsername(), page);
        request.getRequestDispatcher("/admin/officialAppointmentList.jsp").forward(request, response);
    }

    private void searchOfficialAppointments(HttpServletRequest request, HttpServletResponse response, 
                                        User loggedInAdmin) throws Exception {
        String applicationDateStart = request.getParameter("applicationDateStart");
        String applicationDateEnd = request.getParameter("applicationDateEnd");
        String appointmentDateStart = request.getParameter("appointmentDateStart");
        String appointmentDateEnd = request.getParameter("appointmentDateEnd");
        String campus = request.getParameter("campus");
        String applicantOrganization = request.getParameter("applicantOrganization");
        String applicantName = request.getParameter("applicantName");
        String idCard = request.getParameter("idCard");
        String status = request.getParameter("status");
        String visitDepartmentIdStr = request.getParameter("visitDepartmentId");
        String visitContactPerson = request.getParameter("visitContactPerson");

        int page = 1;
        int pageSize = 20;
        String pageParam = request.getParameter("page");
        if (pageParam != null && !pageParam.isEmpty()) {
            page = Integer.parseInt(pageParam);
        }

        Integer departmentIdForFilter = getDepartmentIdForFilter(loggedInAdmin);
        Integer visitDepartmentId = null;
        if (visitDepartmentIdStr != null && !visitDepartmentIdStr.isEmpty()) {
            try {
                visitDepartmentId = Integer.parseInt(visitDepartmentIdStr);
            } catch (NumberFormatException e) {
                logger.warn("Invalid visitDepartmentId format: {}", visitDepartmentIdStr);
            }
        }

        List<Appointment> appointments = appointmentDAO.searchOfficialAppointments(
            applicationDateStart, applicationDateEnd,
            appointmentDateStart, appointmentDateEnd,
            campus, applicantOrganization, applicantName, idCard, 
            visitDepartmentId, visitContactPerson, status,
            departmentIdForFilter, // Apply department filter for department admins
            page, pageSize
        );

        int totalCount = appointmentDAO.getSearchOfficialAppointmentCount(
            applicationDateStart, applicationDateEnd,
            appointmentDateStart, appointmentDateEnd,
            campus, applicantOrganization, applicantName, idCard, 
            visitDepartmentId, visitContactPerson, status,
            departmentIdForFilter
        );
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);

        maskPiiData(appointments);

        // Set attributes for search criteria repopulation
        request.setAttribute("searchApplicationDateStart", applicationDateStart);
        request.setAttribute("searchApplicationDateEnd", applicationDateEnd);
        request.setAttribute("searchAppointmentDateStart", appointmentDateStart);
        request.setAttribute("searchAppointmentDateEnd", appointmentDateEnd);
        request.setAttribute("searchCampus", campus);
        request.setAttribute("searchApplicantOrganization", applicantOrganization);
        request.setAttribute("searchApplicantName", applicantName);
        request.setAttribute("searchIdCard", idCard);
        request.setAttribute("searchStatus", status);
        request.setAttribute("searchVisitDepartmentId", visitDepartmentIdStr);
        request.setAttribute("searchVisitContactPerson", visitContactPerson);


        request.setAttribute("appointments", appointments);
        request.setAttribute("currentPage", page);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("totalCount", totalCount);
        request.setAttribute("isSearchResult", true);

        fetchAllDepartments(request); // Also fetch departments for the search form
        request.getRequestDispatcher("/admin/officialAppointmentList.jsp").forward(request, response);
    }

    private void viewAppointmentDetails(HttpServletRequest request, HttpServletResponse response, 
                                      User loggedInAdmin) throws Exception {
        String appointmentIdStr = request.getParameter("id");
        // 同时支持id和appointmentId两个参数名
        if (appointmentIdStr == null || appointmentIdStr.trim().isEmpty()) {
            appointmentIdStr = request.getParameter("appointmentId");
            if (appointmentIdStr == null || appointmentIdStr.trim().isEmpty()) {
                request.setAttribute("error", "缺少预约ID参数");
                listOfficialAppointments(request, response, loggedInAdmin);
                return;
            }
        }
        
        try {
            int appointmentId = Integer.parseInt(appointmentIdStr);
            Appointment appointment = appointmentDAO.getAppointmentById(appointmentId);

            if (appointment == null || !"OFFICIAL".equals(appointment.getAppointmentType())) {
                request.setAttribute("error", "未找到指定的公务预约记录");
                listOfficialAppointments(request, response, loggedInAdmin);
                return;
            }

        // Security check: Department admin can only view their department's appointments
        Integer departmentIdForFilter = getDepartmentIdForFilter(loggedInAdmin);
        if (departmentIdForFilter != null && !departmentIdForFilter.equals(appointment.getOfficialVisitDepartmentId())) {
             logger.warn("部门管理员 {} 尝试查看不属于本部门的公务预约 {}", loggedInAdmin.getUsername(), appointmentId);
             response.sendError(HttpServletResponse.SC_FORBIDDEN, "您只能查看本部门的预约记录");
             return;
        }

        request.setAttribute("appointment", appointment);
        // Also fetch departments for the detail page dropdown/display
        fetchAllDepartments(request);
        request.getRequestDispatcher("/admin/officialAppointmentDetail.jsp").forward(request, response);
        } catch (NumberFormatException e) {
            request.setAttribute("error", "无效的预约ID格式");
            listOfficialAppointments(request, response, loggedInAdmin);
        }
    }

    private void updateStatusFromList(HttpServletRequest request, HttpServletResponse response, User loggedInAdmin, String newStatus) throws Exception {
        int appointmentId;
        try {
            appointmentId = Integer.parseInt(request.getParameter("id"));
        } catch (NumberFormatException e) {
            logger.warn("Invalid appointment ID for status update from list page: {}", request.getParameter("id"));
            request.getSession().setAttribute("error", "无效的预约ID。");
            response.sendRedirect(request.getContextPath() + "/admin/officialAppointmentManagement?action=list");
            return;
        }

        // Security check before any action
        Integer departmentIdForFilter = getDepartmentIdForFilter(loggedInAdmin);
        if (departmentIdForFilter != null) {
            Appointment appointmentForCheck = appointmentDAO.getAppointmentById(appointmentId);
            if (appointmentForCheck == null || !departmentIdForFilter.equals(appointmentForCheck.getOfficialVisitDepartmentId())) {
                logger.warn("部门管理员 {} 尝试审批不属于本部门的公务预约 {}", loggedInAdmin.getUsername(), appointmentId);
                request.getSession().setAttribute("error", "您只能审批本部门的预约记录。");
                response.sendRedirect(request.getContextPath() + "/admin/officialAppointmentManagement?action=list");
                return;
            }
        }

        // Use the DAO method that doesn't require a rejection reason
        boolean success = appointmentDAO.updateAppointmentStatus(appointmentId, newStatus, loggedInAdmin.getUserId());

        if (success) {
            logAdminAction(loggedInAdmin, "APPOINTMENT_" + newStatus, "(快速审批) 公务预约ID: " + appointmentId, request.getRemoteAddr());
            request.getSession().setAttribute("message", "预约状态更新成功。");
        } else {
            request.getSession().setAttribute("error", "更新预约状态失败。");
        }
        // Redirect back to the list page to show the message
        response.sendRedirect(request.getContextPath() + "/admin/officialAppointmentManagement?action=list");
    }

    private void updateAppointmentStatus(HttpServletRequest request, HttpServletResponse response, User loggedInAdmin, String newStatus) throws Exception {
        String appointmentIdStr = request.getParameter("appointmentId");
        logger.info("更新预约状态，appointmentId={}, newStatus={}", appointmentIdStr, newStatus);
        
        if (appointmentIdStr == null || appointmentIdStr.trim().isEmpty()) {
            logger.error("更新预约状态失败：缺少appointmentId参数");
            request.setAttribute("error", "缺少预约ID参数");
            listOfficialAppointments(request, response, loggedInAdmin);
            return;
        }
        
        int appointmentId = Integer.parseInt(appointmentIdStr);
        String rejectionReason = request.getParameter("rejectionReason");

        Appointment appointment = appointmentDAO.getAppointmentById(appointmentId);
        if (appointment == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "预约记录未找到");
            return;
        }

        // Security check
        Integer departmentIdForFilter = getDepartmentIdForFilter(loggedInAdmin);
        if (departmentIdForFilter != null && !departmentIdForFilter.equals(appointment.getOfficialVisitDepartmentId())) {
             logger.warn("部门管理员 {} 尝试审批不属于本部门的公务预约 {}", loggedInAdmin.getUsername(), appointmentId);
             response.sendError(HttpServletResponse.SC_FORBIDDEN, "您只能审批本部门的预约记录");
             return;
        }

        logger.info("开始更新预约状态，appointmentId={}, newStatus={}, rejectionReason={}", appointmentId, newStatus, rejectionReason);
        
        try {
            boolean success;
            if ("REJECTED".equals(newStatus) && rejectionReason != null && !rejectionReason.trim().isEmpty()) {
                logger.info("执行带驳回理由的状态更新");
                success = appointmentDAO.updateAppointmentStatus(appointmentId, newStatus, loggedInAdmin.getUserId(), rejectionReason);
            } else {
                logger.info("执行不带驳回理由的状态更新");
                success = appointmentDAO.updateAppointmentStatus(appointmentId, newStatus, loggedInAdmin.getUserId());
            }
            
            logger.info("状态更新结果: {}", success ? "成功" : "失败");

            if (success) {
                logAdminAction(loggedInAdmin, "APPOINTMENT_" + newStatus, "公务预约ID: " + appointmentId + (rejectionReason != null ? " 原因: " + rejectionReason : ""), request.getRemoteAddr());
                logger.info("预约状态更新成功，重定向到列表页面");
                
                // 将成功消息放入session，以便在重定向后能够显示
                request.getSession().setAttribute("message", "预约状态更新为：" + (newStatus.equals("APPROVED") ? "已通过" : "已驳回"));
                
                try {
                    // 直接重定向到列表页面，避免详情页的复杂性
                    String redirectUrl = request.getContextPath() + "/admin/officialAppointmentManagement";
                    logger.info("准备重定向到: {}", redirectUrl);
                    response.sendRedirect(redirectUrl);
                    logger.info("重定向已执行");
                } catch (Exception e) {
                    logger.error("重定向过程中出错", e);
                    throw e;
                }
            } else {
                logger.error("更新预约状态失败");
                request.setAttribute("error", "更新预约状态失败");
                
                // 确保在转发之前设置了appointment和departments属性
                request.setAttribute("appointment", appointment);
                fetchAllDepartments(request);
                
                request.getRequestDispatcher("/admin/officialAppointmentDetail.jsp").forward(request, response);
            }
        } catch (Exception e) {
            logger.error("在处理状态更新过程中发生异常", e);
            throw e;
        }
    }

    private void showStatistics(HttpServletRequest request, HttpServletResponse response, 
                              User loggedInAdmin) throws SQLException, IOException, ServletException {
        String statisticsType = request.getParameter("type");
        if (statisticsType == null) {
            statisticsType = "monthly";
        }

        String year = request.getParameter("year");
        String month = request.getParameter("month");
        String campus = request.getParameter("campus");
        String visitDepartmentId = request.getParameter("visitDepartmentId");

        Map<String, Object> statistics = new HashMap<>();
        Integer departmentIdForFilter = getDepartmentIdForFilter(loggedInAdmin);
        
        // If a department admin is searching, they can only search within their own department
        if (departmentIdForFilter != null) {
            visitDepartmentId = String.valueOf(departmentIdForFilter);
        }

        switch (statisticsType) {
            case "monthly":
                statistics = appointmentDAO.getMonthlyOfficialAppointmentStatistics(year, month, campus, visitDepartmentId);
                break;
            case "campus":
                Map<String, Integer> campusStats = appointmentDAO.getCampusOfficialAppointmentStatistics(year, month, visitDepartmentId);
                statistics.put("campusCounts", campusStats);
                break;
            case "department":
                Map<String, Integer> deptStats = appointmentDAO.getDepartmentOfficialAppointmentStatistics(year, month, campus);
                statistics.put("departmentCounts", deptStats);
                break;
            default:
                statistics = appointmentDAO.getMonthlyOfficialAppointmentStatistics(year, month, campus, visitDepartmentId);
                break;
        }

        request.setAttribute("statistics", statistics);
        request.setAttribute("statisticsType", statisticsType);
        // ... set other search params for repopulation ...

        fetchAllDepartments(request); // Fetch departments for the filter dropdown
        request.getRequestDispatcher("/admin/officialAppointmentStatistics.jsp").forward(request, response);
    }

    private Integer getDepartmentIdForFilter(User loggedInAdmin) {
        if ("DEPARTMENT_ADMIN".equals(loggedInAdmin.getRole())) {
            return loggedInAdmin.getDepartmentId();
        }
        return null;
    }

    private void maskPiiData(List<Appointment> appointments) {
        for (Appointment appointment : appointments) {
            appointment.setApplicantIdCard(DataMaskingUtils.maskIdCard(appointment.getApplicantIdCard()));
            appointment.setApplicantPhone(DataMaskingUtils.maskPhoneNumber(appointment.getApplicantPhone()));
            appointment.setApplicantName(DataMaskingUtils.maskName(appointment.getApplicantName()));
        }
    }

    private void logAdminAction(User admin, String actionType, String details, String clientIp) {
        try {
            AuditLog log = new AuditLog();
            log.setUserId(admin.getUserId());
            log.setUsername(admin.getUsername());
            log.setActionType(actionType);
            log.setDetails(details);
            log.setLogTimestamp(new Timestamp(System.currentTimeMillis()));
            log.setIpAddress(clientIp);
            auditLogDAO.createLog(log);
        } catch (Exception e) {
            logger.error("记录审计日志失败", e);
        }
    }

    private void fetchAllDepartments(HttpServletRequest request) throws SQLException {
        List<Department> departments = departmentDAO.getAllDepartments();
        request.setAttribute("departments", departments);
    }
}
