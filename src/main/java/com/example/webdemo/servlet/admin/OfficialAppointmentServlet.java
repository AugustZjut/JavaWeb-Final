package com.example.webdemo.servlet.admin;

import com.example.webdemo.beans.Appointment;
import com.example.webdemo.dao.AppointmentDAO;
import com.example.webdemo.dao.AuditLogDAO;
import com.example.webdemo.beans.User;
import com.example.webdemo.beans.AuditLog; // Added import
import com.example.webdemo.util.DBUtils;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

public class OfficialAppointmentServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private AppointmentDAO appointmentDAO;
    private AuditLogDAO auditLogDAO;

    @Override
    public void init() throws ServletException {
        appointmentDAO = new AppointmentDAO(DBUtils.getDataSource()); // Provide DataSource
        auditLogDAO = new AuditLogDAO();
        // Properties loading removed, DAO handles its keys internally
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
        // Assuming 'Official' is the type string stored in the database
        // The DAO will handle decryption using its internally configured keys.
        List<Appointment> listApp = appointmentDAO.getAppointmentsByType(Appointment.AppointmentType.OFFICIAL_VISIT.name());
        request.setAttribute("listAppointment", listApp);
        request.getRequestDispatcher("/admin/officialAppointmentList.jsp").forward(request, response);
    }

    private void showApproveForm(HttpServletRequest request, HttpServletResponse response) throws SQLException, ServletException, IOException {
        String appointmentId = request.getParameter("id");
        if (appointmentId == null || appointmentId.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Appointment ID is required for approval view.");
            return;
        }
        // Corrected: getAppointmentById in DAO only takes ID, uses internal keys for decryption
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

        boolean success = appointmentDAO.updateAppointmentStatus(appointmentId, Appointment.ApprovalStatus.APPROVED); // Use Enum
        if (success) {
            AuditLog log = new AuditLog();
            log.setUserId(currentUser.getUserId()); // Corrected: getUserId()
            log.setUsername(currentUser.getUsername());
            log.setActionType("OFFICIAL_APPOINTMENT_APPROVE");
            log.setActionDetails("Approved official appointment ID: " + appointmentId + " by user " + currentUser.getUsername());
            log.setActionTime(new java.sql.Timestamp(new Date().getTime()));
            log.setClientIp(request.getRemoteAddr());
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

        boolean success = appointmentDAO.updateAppointmentStatus(appointmentId, Appointment.ApprovalStatus.REJECTED); // Use Enum
        if (success) {
            AuditLog log = new AuditLog();
            log.setUserId(currentUser.getUserId()); // Corrected: getUserId()
            log.setUsername(currentUser.getUsername());
            log.setActionType("OFFICIAL_APPOINTMENT_REJECT");
            log.setActionDetails("Rejected official appointment ID: " + appointmentId + " by user " + currentUser.getUsername());
            log.setActionTime(new java.sql.Timestamp(new Date().getTime()));
            log.setClientIp(request.getRemoteAddr());
            auditLogDAO.createLog(log); // Corrected: use createLog with AuditLog object
            response.sendRedirect(request.getContextPath() + "/admin/officialAppointments?action=list&message=AppointmentRejected");
        } else {
            response.sendRedirect(request.getContextPath() + "/admin/officialAppointments?action=list&error=RejectionFailed");
        }
    }
}
