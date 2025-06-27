package com.example.webdemo.servlet.admin;

import com.example.webdemo.beans.Appointment;
import com.example.webdemo.dao.AppointmentDAO;
import com.example.webdemo.dao.AuditLogDAO;
import com.example.webdemo.beans.User;
import com.example.webdemo.beans.AuditLog; // Added import
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
import java.util.Date;
import java.util.List;

@WebServlet("/admin/officialAppointmentManagement")
public class OfficialAppointmentServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(OfficialAppointmentServlet.class);
    private AppointmentDAO appointmentDAO;
    private AuditLogDAO auditLogDAO;

    @Override
    public void init() throws ServletException {
        appointmentDAO = new AppointmentDAO(DBUtils.getDataSource()); // Provide DataSource
        auditLogDAO = new AuditLogDAO();
        // Properties loading removed, DAO handles its keys internally
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 验证用户是否登录
        HttpSession session = request.getSession(false);
        User adminUser = (session != null) ? (User) session.getAttribute("adminUser") : null;
        
        if (adminUser == null) {
            logger.warn("未登录用户尝试访问公务预约管理");
            response.sendRedirect(request.getContextPath() + "/admin/adminLogin.jsp");
            return;
        }
        
        String action = request.getParameter("action");
        if (action == null) {
            action = "list"; // Default action
        }

        try {
            switch (action) {
                case "list":
                    listOfficialAppointments(request, response);
                    break;
                case "approveView":
                    showApproveForm(request, response);
                    break;
                // Add cases for search, stats later
                default:
                    listOfficialAppointments(request, response);
                    break;
            }
        } catch (SQLException ex) {
            throw new ServletException("Database error in OfficialAppointmentServlet", ex);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("user") : null;

        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/admin/adminLogin.jsp?error=Session expired. Please login again.");
            return;
        }

        try {
            if ("approve".equals(action)) {
                approveAppointment(request, response, currentUser);
            } else if ("reject".equals(action)) {
                rejectAppointment(request, response, currentUser);
            } else {
                doGet(request, response);
            }
        } catch (SQLException ex) {
            throw new ServletException("Database error during POST action in OfficialAppointmentServlet", ex);
        }
    }

    private void listOfficialAppointments(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException, ServletException {
        // 修正: 使用新添加的AppointmentType静态常量
        List<Appointment> listApp = appointmentDAO.getAppointmentsByType(Appointment.AppointmentType.OFFICIAL);
        request.setAttribute("listAppointment", listApp);
        request.getRequestDispatcher("/admin/officialAppointmentList.jsp").forward(request, response);
    }

    private void showApproveForm(HttpServletRequest request, HttpServletResponse response) throws SQLException, ServletException, IOException {
        String appointmentId = request.getParameter("id");
        if (appointmentId == null || appointmentId.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Appointment ID is required for approval view.");
            return;
        }
        // appointmentId已经是String类型
        Appointment appointment = appointmentDAO.getAppointmentById(appointmentId); 
        if (appointment == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Appointment not found.");
            return;
        }
        request.setAttribute("appointment", appointment);
        request.getRequestDispatcher("/admin/officialAppointmentApprovalForm.jsp").forward(request, response);
    }

    private void approveAppointment(HttpServletRequest request, HttpServletResponse response, User currentUser) throws SQLException, IOException {
        String appointmentId = request.getParameter("appointmentId");
        // Potentially add approval comments/notes from form
        // String approvalNotes = request.getParameter("approvalNotes"); 

        if (appointmentId == null || appointmentId.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/admin/officialAppointments?action=list&error=InvalidAppointmentId");
            return;
        }

        // appointmentId已经是String类型，Status需要传入String值
        boolean success = appointmentDAO.updateAppointmentStatus(appointmentId, Appointment.Status.APPROVED); 
        if (success) {
            AuditLog log = new AuditLog();
            log.setUserId(currentUser.getUserId()); // Corrected: getUserId()
            log.setUsername(currentUser.getUsername());
            log.setActionType("OFFICIAL_APPOINTMENT_APPROVE");
            // 修复方法名: setActionDetails -> setDetails
            log.setDetails("Approved official appointment ID: " + appointmentId + " by user " + currentUser.getUsername());
            // 修复方法名: setActionTime -> setLogTimestamp
            log.setLogTimestamp(new java.sql.Timestamp(new Date().getTime()));
            // 修复方法名: setClientIp -> setIpAddress
            log.setIpAddress(request.getRemoteAddr());
            auditLogDAO.createLog(log); // Corrected: use createLog with AuditLog object
            response.sendRedirect(request.getContextPath() + "/admin/officialAppointments?action=list&message=AppointmentApproved");
        } else {
            response.sendRedirect(request.getContextPath() + "/admin/officialAppointments?action=list&error=ApprovalFailed");
        }
    }

    private void rejectAppointment(HttpServletRequest request, HttpServletResponse response, User currentUser) throws SQLException, IOException {
        String appointmentId = request.getParameter("appointmentId");
        // String rejectionReason = request.getParameter("rejectionReason"); // Important to capture this

        if (appointmentId == null || appointmentId.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/admin/officialAppointments?action=list&error=InvalidAppointmentId");
            return;
        }

        // appointmentId已经是String类型，Status需要传入String值
        boolean success = appointmentDAO.updateAppointmentStatus(appointmentId, Appointment.Status.REJECTED);
        if (success) {
            AuditLog log = new AuditLog();
            log.setUserId(currentUser.getUserId()); // Corrected: getUserId()
            log.setUsername(currentUser.getUsername());
            log.setActionType("OFFICIAL_APPOINTMENT_REJECT");
            // 修复方法名: setActionDetails -> setDetails
            log.setDetails("Rejected official appointment ID: " + appointmentId + " by user " + currentUser.getUsername());
            // 修复方法名: setActionTime -> setLogTimestamp 
            log.setLogTimestamp(new java.sql.Timestamp(new Date().getTime()));
            // 修复方法名: setClientIp -> setIpAddress
            log.setIpAddress(request.getRemoteAddr());
            auditLogDAO.createLog(log); // Corrected: use createLog with AuditLog object
            response.sendRedirect(request.getContextPath() + "/admin/officialAppointments?action=list&message=AppointmentRejected");
        } else {
            response.sendRedirect(request.getContextPath() + "/admin/officialAppointments?action=list&error=RejectionFailed");
        }
    }
}
