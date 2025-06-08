package com.example.webdemo.servlet.mobile;

import com.example.webdemo.beans.Appointment;
import com.example.webdemo.dao.AppointmentDAO;
import com.example.webdemo.util.DBUtils;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/mobile/myAppointments")
public class MyAppointmentServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private AppointmentDAO appointmentDAO;

    @Override
    public void init() throws ServletException {
        try {
            appointmentDAO = new AppointmentDAO(DBUtils.getDataSource());
        } catch (Exception e) {
            throw new ServletException("Failed to initialize DAO for MyAppointmentServlet", e);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String applicantIdCard = request.getParameter("applicantIdCard");
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        if (applicantIdCard != null && !applicantIdCard.trim().isEmpty()) {
            // Basic validation for ID card format
            if (!applicantIdCard.matches("^\\d{17}[\\dX]$|^\\d{15}$")){
                request.setAttribute("errorMessage", "身份证号格式不正确。");
            } else {
                try {
                    List<Appointment> appointments = appointmentDAO.getAppointmentsByApplicantIdCard(applicantIdCard);
                    request.setAttribute("appointments", appointments);
                } catch (SQLException e) {
                    e.printStackTrace(); // Log this properly
                    request.setAttribute("errorMessage", "查询预约失败: " + e.getMessage());
                } catch (Exception e) {
                    e.printStackTrace(); // Log crypto or other exceptions
                    request.setAttribute("errorMessage", "查询处理时发生意外错误: " + e.getMessage());
                }
            }
        }
        request.getRequestDispatcher("/mobile/myAppointments.jsp").forward(request, response);
    }
}
