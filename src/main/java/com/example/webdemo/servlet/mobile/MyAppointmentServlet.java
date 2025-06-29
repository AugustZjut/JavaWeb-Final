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
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        String applicantName = request.getParameter("applicantName");
        String applicantIdCard = request.getParameter("applicantIdCard");
        String applicantPhone = request.getParameter("applicantPhone");
        String departmentIdStr = request.getParameter("departmentId");
        String appointmentType = request.getParameter("appointmentType");
        String status = request.getParameter("status");
        String startDateStr = request.getParameter("startDate");
        String endDateStr = request.getParameter("endDate");
        Integer departmentId = null;
        if (departmentIdStr != null && !departmentIdStr.trim().isEmpty()) {
            try { departmentId = Integer.parseInt(departmentIdStr); } catch (Exception ignored) {}
        }
        java.util.Date startDate = null, endDate = null;
        try {
            if (startDateStr != null && !startDateStr.trim().isEmpty()) {
                startDate = java.sql.Date.valueOf(startDateStr);
            }
            if (endDateStr != null && !endDateStr.trim().isEmpty()) {
                endDate = java.sql.Date.valueOf(endDateStr);
            }
        } catch (Exception ignored) {}

        // 必须三项主身份参数全部填写且精确匹配才允许查询
        boolean hasAllIdentity = (applicantName != null && !applicantName.trim().isEmpty()) &&
                                 (applicantIdCard != null && !applicantIdCard.trim().isEmpty()) &&
                                 (applicantPhone != null && !applicantPhone.trim().isEmpty());

        if (!hasAllIdentity) {
            request.setAttribute("errorMessage", "为保护隐私，必须同时输入姓名、身份证号和手机号，且全部精确匹配，才能查询预约记录。");
        } else if (applicantIdCard != null && !applicantIdCard.trim().isEmpty() &&
                   !applicantIdCard.matches("^\\d{17}[\\dXx]$|^\\d{15}$")) {
            request.setAttribute("errorMessage", "身份证号格式不正确。");
        } else {
            try {
                List<Appointment> appointments = appointmentDAO.searchAppointments(
                    applicantName.trim(),
                    applicantIdCard.trim(),
                    applicantPhone.trim(),
                    null, // applicantUserId
                    departmentId,
                    appointmentType != null && !appointmentType.trim().isEmpty() ? appointmentType.trim() : null,
                    status != null && !status.trim().isEmpty() ? status.trim() : null,
                    startDate,
                    endDate
                );
                request.setAttribute("appointments", appointments);
                // 保存原始查询参数（用于查看通行码后返回查询结果页面）
                request.setAttribute("originalApplicantIdCard", applicantIdCard.trim());
            } catch (SQLException e) {
                e.printStackTrace();
                request.setAttribute("errorMessage", "查询预约失败: " + e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                request.setAttribute("errorMessage", "查询处理时发生意外错误: " + e.getMessage());
            }
        }
        request.getRequestDispatcher("/mobile/myAppointments.jsp").forward(request, response);
    }
}
