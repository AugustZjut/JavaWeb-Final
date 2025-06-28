package com.example.webdemo.servlet.admin;

import com.example.webdemo.beans.Appointment;
import com.example.webdemo.beans.User;
import com.example.webdemo.beans.AuditLog;
import com.example.webdemo.dao.AppointmentDAO;
import com.example.webdemo.dao.AuditLogDAO;
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

// @WebServlet("/admin/publicAppointmentManagement") // Configured in web.xml
public class PublicAppointmentManagementServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(PublicAppointmentManagementServlet.class);
    private AppointmentDAO appointmentDAO;
    private AuditLogDAO auditLogDAO;

    @Override
    public void init() throws ServletException {
        appointmentDAO = new AppointmentDAO(DBUtils.getDataSource());
        auditLogDAO = new AuditLogDAO();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        // 验证用户权限
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
                    listPublicAppointments(request, response, loggedInAdmin);
                    break;
                case "search":
                    searchPublicAppointments(request, response, loggedInAdmin);
                    break;
                case "view":
                    viewAppointmentDetails(request, response, loggedInAdmin);
                    break;
                case "statistics":
                    showStatistics(request, response, loggedInAdmin);
                    break;
                case "export":
                    exportAppointments(request, response, loggedInAdmin);
                    break;
                default:
                    listPublicAppointments(request, response, loggedInAdmin);
                    break;
            }
        } catch (SQLException ex) {
            logger.error("数据库错误", ex);
            request.setAttribute("error", "数据库错误: " + ex.getMessage());
            try {
                listPublicAppointments(request, response, loggedInAdmin);
            } catch (Exception e) {
                logger.error("显示错误页面失败", e);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "系统错误");
            }
        } catch (Exception ex) {
            logger.error("系统错误", ex);
            request.setAttribute("error", "系统错误: " + ex.getMessage());
            try {
                listPublicAppointments(request, response, loggedInAdmin);
            } catch (Exception e) {
                logger.error("显示错误页面失败", e);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "系统错误");
            }
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        // POST请求主要用于统计功能
        doGet(request, response);
    }

    /**
     * 检查用户访问权限
     */
    private User checkAccess(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        HttpSession session = request.getSession(false);
        User loggedInAdmin = (session != null) ? (User) session.getAttribute("adminUser") : null;

        if (loggedInAdmin == null) {
            response.sendRedirect(request.getContextPath() + "/admin/adminLogin.jsp");
            return null;
        }

        // 权限检查：系统管理员、学校管理员，或有公众预约管理权限的部门管理员
        boolean hasAccess = "SYSTEM_ADMIN".equals(loggedInAdmin.getRole()) ||
                           "SCHOOL_ADMIN".equals(loggedInAdmin.getRole()) ||
                           ("DEPARTMENT_ADMIN".equals(loggedInAdmin.getRole()) && 
                            loggedInAdmin.isCanManagePublicAppointments());

        if (!hasAccess) {
            logger.warn("用户 {} 尝试访问公众预约管理，但没有权限", loggedInAdmin.getUsername());
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "您没有权限访问此功能");
            return null;
        }

        return loggedInAdmin;
    }

    /**
     * 列出公众预约
     */
    private void listPublicAppointments(HttpServletRequest request, HttpServletResponse response, 
                                       User loggedInAdmin) throws SQLException, IOException, ServletException, Exception {
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

        // 获取公众预约列表
        logger.info("开始获取公众预约列表，页码: {}, 页面大小: {}", page, pageSize);
        
        try {
            List<Appointment> appointments = appointmentDAO.getPublicAppointments(page, pageSize);
            int totalCount = appointmentDAO.getPublicAppointmentCount();
            logger.info("获取到 {} 条公众预约记录，总数: {}", appointments.size(), totalCount);
            int totalPages = (int) Math.ceil((double) totalCount / pageSize);

            // 数据脱敏
            for (Appointment appointment : appointments) {
                appointment.setApplicantIdCard(DataMaskingUtils.maskIdCard(appointment.getApplicantIdCard()));
                appointment.setApplicantPhone(DataMaskingUtils.maskPhoneNumber(appointment.getApplicantPhone()));
            }

            request.setAttribute("appointments", appointments);
            request.setAttribute("currentPage", page);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("totalCount", totalCount);

            logger.debug("用户 {} 查看公众预约列表，第 {} 页", loggedInAdmin.getUsername(), page);
            request.getRequestDispatcher("/admin/publicAppointmentList.jsp").forward(request, response);
            
        } catch (Exception e) {
            logger.error("获取公众预约列表时发生错误，用户: {}, 页码: {}", loggedInAdmin.getUsername(), page, e);
            request.setAttribute("error", "获取预约列表失败：" + e.getMessage());
            request.setAttribute("appointments", new java.util.ArrayList<>());
            request.setAttribute("currentPage", page);
            request.setAttribute("totalPages", 0);
            request.setAttribute("totalCount", 0);
            request.getRequestDispatcher("/admin/publicAppointmentList.jsp").forward(request, response);
        }
    }

    /**
     * 搜索公众预约
     */
    private void searchPublicAppointments(HttpServletRequest request, HttpServletResponse response, 
                                        User loggedInAdmin) throws SQLException, IOException, ServletException, Exception {
        // 获取搜索参数
        String applicationDateStart = request.getParameter("applicationDateStart");
        String applicationDateEnd = request.getParameter("applicationDateEnd");
        String appointmentDateStart = request.getParameter("appointmentDateStart");
        String appointmentDateEnd = request.getParameter("appointmentDateEnd");
        String campus = request.getParameter("campus");
        String organization = request.getParameter("applicantOrganization");
        String applicantName = request.getParameter("applicantName");
        String idCard = request.getParameter("idCard");
        String status = request.getParameter("status");

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

        // 执行搜索
        List<Appointment> appointments = appointmentDAO.searchPublicAppointments(
            applicationDateStart, applicationDateEnd,
            appointmentDateStart, appointmentDateEnd,
            campus, organization, applicantName, idCard, status,
            page, pageSize
        );

        int totalCount = appointmentDAO.getSearchPublicAppointmentCount(
            applicationDateStart, applicationDateEnd,
            appointmentDateStart, appointmentDateEnd,
            campus, organization, applicantName, idCard, status
        );
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);

        // 数据脱敏
        for (Appointment appointment : appointments) {
            appointment.setApplicantIdCard(DataMaskingUtils.maskIdCard(appointment.getApplicantIdCard()));
            appointment.setApplicantPhone(DataMaskingUtils.maskPhoneNumber(appointment.getApplicantPhone()));
        }

        // 设置搜索参数回显
        request.setAttribute("searchApplicationDateStart", applicationDateStart);
        request.setAttribute("searchApplicationDateEnd", applicationDateEnd);
        request.setAttribute("searchAppointmentDateStart", appointmentDateStart);
        request.setAttribute("searchAppointmentDateEnd", appointmentDateEnd);
        request.setAttribute("searchCampus", campus);
        request.setAttribute("searchOrganization", organization);
        request.setAttribute("searchApplicantName", applicantName);
        request.setAttribute("searchIdCard", idCard);
        request.setAttribute("searchStatus", status);

        request.setAttribute("appointments", appointments);
        request.setAttribute("currentPage", page);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("totalCount", totalCount);
        request.setAttribute("isSearchResult", true);

        logger.debug("用户 {} 搜索公众预约，条件：校区={}, 单位={}, 姓名={}, 结果数量={}", 
            loggedInAdmin.getUsername(), campus, organization, applicantName, appointments.size());

        request.getRequestDispatcher("/admin/publicAppointmentList.jsp").forward(request, response);
    }

    /**
     * 查看预约详情
     */
    private void viewAppointmentDetails(HttpServletRequest request, HttpServletResponse response, 
                                      User loggedInAdmin) throws SQLException, IOException, ServletException, Exception {
        String appointmentIdStr = request.getParameter("id");
        if (appointmentIdStr == null || appointmentIdStr.trim().isEmpty()) {
            request.setAttribute("error", "缺少预约ID参数");
            listPublicAppointments(request, response, loggedInAdmin);
            return;
        }

        try {
            int appointmentId = Integer.parseInt(appointmentIdStr);
            Appointment appointment = appointmentDAO.getAppointmentById(appointmentId);

            if (appointment == null || !"PUBLIC".equals(appointment.getAppointmentType())) {
                request.setAttribute("error", "未找到指定的公众预约记录");
                listPublicAppointments(request, response, loggedInAdmin);
                return;
            }

            // 获取随行人员信息（如果有）
            // List<AccompanyingPerson> accompanyingPersons = appointmentDAO.getAccompanyingPersons(appointmentId);
            // request.setAttribute("accompanyingPersons", accompanyingPersons);

            request.setAttribute("appointment", appointment);
            logger.debug("用户 {} 查看公众预约详情，预约ID={}", loggedInAdmin.getUsername(), appointmentId);
            
            request.getRequestDispatcher("/admin/publicAppointmentDetail.jsp").forward(request, response);
        } catch (NumberFormatException e) {
            request.setAttribute("error", "无效的预约ID格式");
            listPublicAppointments(request, response, loggedInAdmin);
        }
    }

    /**
     * 显示统计数据
     */
    private void showStatistics(HttpServletRequest request, HttpServletResponse response, 
                              User loggedInAdmin) throws SQLException, IOException, ServletException {
        String statisticsType = request.getParameter("type");
        if (statisticsType == null) {
            statisticsType = "monthly";
        }

        String year = request.getParameter("year");
        String month = request.getParameter("month");
        String campus = request.getParameter("campus");

        Map<String, Object> statistics = new HashMap<>();

        switch (statisticsType) {
            case "monthly":
                Map<String, Integer> monthlyData = appointmentDAO.getMonthlyPublicAppointmentStatistics(year, month, campus);
                statistics.put("monthlyData", monthlyData);
                break;
            case "campus":
                Map<String, Integer> campusData = appointmentDAO.getCampusPublicAppointmentStatistics(year, month);
                statistics.put("campusData", campusData);
                break;
            case "status":
                Map<String, Integer> statusData = appointmentDAO.getStatusPublicAppointmentStatistics(year, month, campus);
                statistics.put("statusData", statusData);
                break;
            default:
                Map<String, Integer> defaultData = appointmentDAO.getMonthlyPublicAppointmentStatistics(year, month, campus);
                statistics.put("monthlyData", defaultData);
                break;
        }

        request.setAttribute("statistics", statistics);
        request.setAttribute("statisticsType", statisticsType);
        request.setAttribute("searchYear", year);
        request.setAttribute("searchMonth", month);
        request.setAttribute("searchCampus", campus);

        logger.debug("用户 {} 查看公众预约统计，类型={}, 年份={}, 月份={}, 校区={}", 
            loggedInAdmin.getUsername(), statisticsType, year, month, campus);

        request.getRequestDispatcher("/admin/publicAppointmentStatistics.jsp").forward(request, response);
    }

    /**
     * 导出预约数据
     */
    private void exportAppointments(HttpServletRequest request, HttpServletResponse response, 
                                  User loggedInAdmin) throws SQLException, IOException, Exception {
        // 这里可以实现Excel或CSV导出功能
        // 暂时返回错误消息
        request.setAttribute("message", "导出功能正在开发中");
        try {
            listPublicAppointments(request, response, loggedInAdmin);
        } catch (ServletException e) {
            throw new IOException("导出失败", e);
        } catch (Exception e) {
            throw new IOException("导出失败", e);
        }
    }

    /**
     * 记录审计日志
     */
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
}
