package com.example.webdemo.servlet.admin;

import com.example.webdemo.beans.Appointment;
import com.example.webdemo.beans.AuditLog;
import com.example.webdemo.beans.User;
import com.example.webdemo.dao.AppointmentDAO;
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
import java.sql.SQLException;
import java.util.List;

@WebServlet("/admin/publicAppointmentManagement")
public class PublicAppointmentServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(PublicAppointmentServlet.class);
    private AppointmentDAO appointmentDAO;
    private AuditLogDAO auditLogDAO;

    @Override
    public void init() throws ServletException {
        appointmentDAO = new AppointmentDAO(DBUtils.getDataSource()); // Pass DataSource
        auditLogDAO = new AuditLogDAO(DBUtils.getDataSource());
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 验证用户是否登录
        HttpSession session = request.getSession(false);
        User adminUser = (session != null) ? (User) session.getAttribute("adminUser") : null;
        
        if (adminUser == null) {
            logger.warn("未登录用户尝试访问公开预约管理");
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
                default:
                    listPublicAppointments(request, response);
                    break;
                // Add cases for search, view details, stats later
            }
        } catch (SQLException ex) {
            throw new ServletException("Database error in PublicAppointmentServlet", ex);
        }
    }

    private void listPublicAppointments(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException, ServletException {
        // 使用Appointment类中定义的静态常量
        List<Appointment> listApp = appointmentDAO.getAppointmentsByType(Appointment.AppointmentType.PUBLIC);
        request.setAttribute("listAppointment", listApp);
        
        logger.debug("加载了 {} 条公开预约记录", listApp.size());
        request.getRequestDispatcher("/admin/publicAppointmentList.jsp").forward(request, response);
    }

    // doPost might be needed for actions like bulk delete or status change if added later
    // protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    //    doGet(request, response); 
    // }
}
