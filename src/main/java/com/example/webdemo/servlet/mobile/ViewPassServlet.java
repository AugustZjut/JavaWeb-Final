package com.example.webdemo.servlet.mobile;

import com.example.webdemo.beans.Appointment;
import com.example.webdemo.dao.AppointmentDAO;
import com.example.webdemo.util.DataMaskingUtils;
import com.example.webdemo.util.QRCodeUtils;
import com.example.webdemo.util.DBUtils; // For potential key loading

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

@WebServlet("/mobile/viewPass")
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
            Appointment appointment = appointmentDAO.getAppointmentById(appointmentId);

            if (appointment != null) {
                // Mask PII
                String maskedName = DataMaskingUtils.maskName(appointment.getApplicantName());
                String maskedIdCard = DataMaskingUtils.maskIdCard(appointment.getApplicantIdCard());
                // Phone is already encrypted, if needed for display, decrypt and mask
                // String decryptedPhone = CryptoUtils.decryptSM4(appointment.getVisitorPhone(), sm4Key);
                // String maskedPhone = DataMaskingUtils.maskPhone(decryptedPhone);

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
                // request.setAttribute("maskedPhone", maskedPhone); // If displaying phone
                request.setAttribute("qrCodeBase64", qrCodeBase64);
                request.setAttribute("generationTime", generationTime);

                // Determine pass validity (example logic, adjust as needed)
                boolean isValid = Appointment.ApprovalStatus.APPROVED.name().equalsIgnoreCase(appointment.getApprovalStatus()) ||
                                  (Appointment.AppointmentType.PUBLIC.name().equalsIgnoreCase(appointment.getAppointmentType()) && 
                                   !Appointment.ApprovalStatus.CANCELLED.name().equalsIgnoreCase(appointment.getApprovalStatus()) &&
                                   !Appointment.ApprovalStatus.REJECTED.name().equalsIgnoreCase(appointment.getApprovalStatus()) && // Also check for REJECTED for public
                                   Appointment.ApprovalStatus.AUTO_APPROVED.name().equalsIgnoreCase(appointment.getApprovalStatus()) // Explicitly check for AUTO_APPROVED
                                   );
                // Potentially add time-based validity check here
                // e.g., if appointment.getAppointmentTime() is in the past or too far in the future

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
