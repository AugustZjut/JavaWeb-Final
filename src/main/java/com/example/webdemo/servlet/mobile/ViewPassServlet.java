package com.example.webdemo.servlet.mobile;

import com.example.webdemo.beans.Appointment;
import com.example.webdemo.dao.AppointmentDAO;
import com.example.webdemo.util.DataMaskingUtils;
import com.example.webdemo.util.QRCodeUtils;
import com.example.webdemo.util.DBUtils;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ViewPassServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private AppointmentDAO appointmentDAO;

    @Override
    public void init() throws ServletException {
        appointmentDAO = new AppointmentDAO(DBUtils.getDataSource());
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String appointmentId = request.getParameter("id");

        if (appointmentId == null || appointmentId.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Appointment ID is required.");
            return;
        }

        try {
            // appointmentId保持为String类型
            Appointment appointment = appointmentDAO.getAppointmentById(appointmentId);

            if (appointment != null) {
                // Mask PII
                String maskedName = DataMaskingUtils.maskName(appointment.getApplicantName());
                String maskedIdCard = DataMaskingUtils.maskIdCard(appointment.getApplicantIdCard());

                // Prepare QR code content
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String generationTime = sdf.format(new Date());
                String qrCodeContent = "Name: " + maskedName + "\n" +
                                       "ID: " + maskedIdCard + "\n" +
                                       "Time: " + generationTime;

                String qrCodeBase64 = QRCodeUtils.generateQRCodeBase64(qrCodeContent, 300, 300);

                request.setAttribute("appointment", appointment);
                request.setAttribute("maskedName", maskedName);
                request.setAttribute("maskedIdCard", maskedIdCard);
                request.setAttribute("qrCodeBase64", qrCodeBase64);
                request.setAttribute("generationTime", generationTime);

                // 使用Status直接比较
                boolean isValid = Appointment.Status.APPROVED.equals(appointment.getStatus()) ||
                                 (Appointment.AppointmentType.PUBLIC.equals(appointment.getAppointmentType()) && 
                                  !Appointment.Status.CANCELLED.equals(appointment.getStatus()) &&
                                  !Appointment.Status.REJECTED.equals(appointment.getStatus()) && 
                                  Appointment.Status.APPROVED.equals(appointment.getStatus()));
                
                request.setAttribute("isValidPass", isValid);

                request.getRequestDispatcher("/mobile/viewPass.jsp").forward(request, response);
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Appointment not found.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new ServletException("Database error while fetching appointment.", e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException("Error generating QR code or processing pass.", e);
        }
    }
}
