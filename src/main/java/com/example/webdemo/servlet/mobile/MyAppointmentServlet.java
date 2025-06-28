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

        // 检查是否有至少一个查询条件
        boolean hasQueryParam = (applicantName != null && !applicantName.trim().isEmpty()) ||
                                (applicantIdCard != null && !applicantIdCard.trim().isEmpty()) ||
                                (applicantPhone != null && !applicantPhone.trim().isEmpty());

        if (hasQueryParam) {
            try {
                // 验证身份证号格式（如果提供）
                if (applicantIdCard != null && !applicantIdCard.trim().isEmpty() && 
                    !applicantIdCard.matches("^\\d{17}[\\dXx]$|^\\d{15}$")) {
                    request.setAttribute("errorMessage", "身份证号格式不正确。");
                } else {
                    // 使用修改后的DAO方法查询预约
                    List<Appointment> appointments = appointmentDAO.searchAppointments(
                        applicantName != null ? applicantName.trim() : null,
                        applicantIdCard != null ? applicantIdCard.trim() : null,
                        applicantPhone != null ? applicantPhone.trim() : null
                    );
                    request.setAttribute("appointments", appointments);
                    
                    // 保存原始查询参数（用于查看通行码后返回查询结果页面）
                    if (applicantIdCard != null && !applicantIdCard.trim().isEmpty()) {
                        request.setAttribute("originalApplicantIdCard", applicantIdCard.trim());
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace(); // 应该使用日志框架记录
                request.setAttribute("errorMessage", "查询预约失败: " + e.getMessage());
            } catch (Exception e) {
                e.printStackTrace(); // 应该使用日志框架记录
                request.setAttribute("errorMessage", "查询处理时发生意外错误: " + e.getMessage());
            }
        }
        
        request.getRequestDispatcher("/mobile/myAppointments.jsp").forward(request, response);
    }
}
