package com.example.webdemo.servlet.mobile;

import com.example.webdemo.beans.AccompanyingPerson;
import com.example.webdemo.beans.Appointment;
import com.example.webdemo.dao.AppointmentDAO;
import com.example.webdemo.dao.UserDAO; // For potential PII encryption of user data if needed in future
import com.example.webdemo.util.CryptoUtils;
import com.example.webdemo.util.DBUtils;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// @WebServlet("/mobile/makeAppointment") // Removed as per web.xml configuration
public class MobileAppointmentServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private AppointmentDAO appointmentDAO;
    // private UserDAO userDAO; // If direct user PII needs handling here

    @Override
    public void init() throws ServletException {
        try {
            appointmentDAO = new AppointmentDAO(DBUtils.getDataSource());
            // userDAO = new UserDAO(DBUtils.getDataSource());
        } catch (Exception e) {
            throw new ServletException("Failed to initialize DAO for MobileAppointmentServlet", e);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Display the appointment form
        request.getRequestDispatcher("/mobile/makeAppointment.jsp").forward(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        String appointmentTypeStr = request.getParameter("appointmentType");
        String campusStr = request.getParameter("campus");
        String appointmentTimeStr = request.getParameter("appointmentTime");
        String applicantName = request.getParameter("applicantName");
        String applicantIdCard = request.getParameter("applicantIdCard");
        String applicantPhone = request.getParameter("applicantPhone");
        String organization = request.getParameter("organization");
        String transportationStr = request.getParameter("transportation");
        String licensePlate = request.getParameter("licensePlate");

        // Official visit fields
        String visitDepartment = request.getParameter("visitDepartment");
        String contactPersonName = request.getParameter("contactPersonName");
        String contactPersonPhone = request.getParameter("contactPersonPhone");
        String visitReason = request.getParameter("visitReason");

        // Basic validation
        if (applicantName == null || applicantName.trim().isEmpty() ||
            applicantIdCard == null || applicantIdCard.trim().isEmpty() || // Original check uses trim for emptiness
            applicantPhone == null || applicantPhone.trim().isEmpty() ||
            appointmentTimeStr == null || appointmentTimeStr.trim().isEmpty()) {
            request.setAttribute("errorMessage", "必填字段不能为空 (姓名, 身份证, 电话, 预约时间).");
            System.out.println("Validation Error: Missing required fields. ApplicantID was: " + applicantIdCard);
            request.getRequestDispatcher("/mobile/makeAppointment.jsp").forward(request, response);
            return;
        }
        
        // Log the raw and trimmed ID card value
        System.out.println("Raw applicantIdCard from request: [" + applicantIdCard + "]");
        String trimmedApplicantIdCard = applicantIdCard.trim();
        System.out.println("Trimmed applicantIdCard: [" + trimmedApplicantIdCard + "]");

        // Validate ID card format (basic) - Allow 'x' or 'X', 18 digits only
        if (!trimmedApplicantIdCard.matches("^\\d{17}[\\dXx]$")){ // Corrected regex: \\d for digit
            request.setAttribute("errorMessage", "访客身份证号格式不正确 (应为18位).");
            System.out.println("Validation Error: ID card format incorrect. Value was: [" + trimmedApplicantIdCard + "]");
            request.getRequestDispatcher("/mobile/makeAppointment.jsp").forward(request, response);
            return;
        }

        Appointment.AppointmentType appointmentTypeEnum = Appointment.AppointmentType.valueOf(appointmentTypeStr);
        Appointment.Campus campusEnum = Appointment.Campus.valueOf(campusStr);
        // Appointment.Transportation transportation = Appointment.Transportation.valueOf(transportationStr); // Corrected: transportMode is a String
        Timestamp appointmentTime;
        try {
            appointmentTime = Timestamp.valueOf(LocalDateTime.parse(appointmentTimeStr));
        } catch (DateTimeParseException | IllegalArgumentException e) {
            request.setAttribute("errorMessage", "预约时间格式无效.");
            request.getRequestDispatcher("/mobile/makeAppointment.jsp").forward(request, response);
            return;
        }

        Appointment appointment = new Appointment();
        appointment.setAppointmentId(UUID.randomUUID().toString());
        appointment.setApplicantName(applicantName.trim()); // Trim before setting
        appointment.setApplicantIdCard(trimmedApplicantIdCard); // Use trimmed and validated ID
        appointment.setApplicantPhone(applicantPhone.trim()); // Trim before setting
        // appointment.setOrganization(organization); // Corrected: use setApplicantOrganization
        appointment.setApplicantOrganization(organization != null ? organization.trim() : null); // Trim before setting
        // appointment.setAppointmentType(appointmentType); // Corrected: use enum.name()
        appointment.setAppointmentType(appointmentTypeEnum.name());
        // appointment.setCampus(campus); // Corrected: use enum.name()
        appointment.setCampus(campusEnum.name());
        // appointment.setAppointmentTime(appointmentTime);
        appointment.setEntryDatetime(appointmentTime); // 使用更正的setter方法
        // appointment.setTransportation(transportation); // Corrected: use setTransportMode with String
        appointment.setTransportMode(transportationStr);
        appointment.setLicensePlate(licensePlate); // Potentially encrypt if sensitive
        // appointment.setSubmissionTime(new Timestamp(System.currentTimeMillis()));
        appointment.setApplicationDate(new Timestamp(System.currentTimeMillis())); // Corrected to setApplicationDate

        // if (appointmentType == Appointment.AppointmentType.OFFICIAL_VISIT) { // Original incorrect line
        if (appointmentTypeEnum == Appointment.AppointmentType.OFFICIAL_VISIT) { // Corrected: use appointmentTypeEnum
            if (visitDepartment == null || visitDepartment.trim().isEmpty() ||
                contactPersonName == null || contactPersonName.trim().isEmpty() ||
                contactPersonPhone == null || contactPersonPhone.trim().isEmpty() ||
                visitReason == null || visitReason.trim().isEmpty()) {
                request.setAttribute("errorMessage", "公务来访必填字段不能为空 (到访部门, 联系人, 事由).");
                request.getRequestDispatcher("/mobile/makeAppointment.jsp").forward(request, response);
                return;
            }
            appointment.setVisitDepartment(visitDepartment.trim()); // Trim
            // appointment.setContactPersonName(contactPersonName); // Potentially encrypt // Corrected: use setVisitContactPerson
            appointment.setVisitContactPerson(contactPersonName.trim()); // Trim
            appointment.setContactPersonPhone(contactPersonPhone.trim()); // Trim
            appointment.setVisitReason(visitReason.trim()); // Trim
            // appointment.setApprovalStatus(Appointment.ApprovalStatus.PENDING.name());
            appointment.setStatus(Appointment.ApprovalStatus.PENDING.name()); // Corrected to setStatus
        } else {
            // appointment.setApprovalStatus(Appointment.ApprovalStatus.AUTO_APPROVED.name()); // Auto-approve public access
            appointment.setStatus(Appointment.ApprovalStatus.AUTO_APPROVED.name()); // Corrected to setStatus
        }

        List<AccompanyingPerson> accompanyingPersons = new ArrayList<>();
        String[] accNames = request.getParameterValues("accName[]");
        String[] accIdCards = request.getParameterValues("accIdCard[]");
        String[] accPhones = request.getParameterValues("accPhone[]");

        if (accNames != null) {
            for (int i = 0; i < accNames.length; i++) {
                // Trim all parts of accompanying person's data at the beginning of the check
                String currentAccName = (accNames[i] != null) ? accNames[i].trim() : "";
                String currentAccIdCard = (accIdCards[i] != null) ? accIdCards[i].trim() : "";
                String currentAccPhone = (accPhones[i] != null) ? accPhones[i].trim() : "";

                if (!currentAccName.isEmpty() && !currentAccIdCard.isEmpty() && !currentAccPhone.isEmpty()) {
                    
                    // Validate accompanying ID card format (basic) - CORRECTED REGEX and use trimmed, allow 'x' or 'X', 18 digits only
                    if (!currentAccIdCard.matches("^\\d{17}[\\dXx]$")){ // Corrected regex: \\d for digit
                        request.setAttribute("errorMessage", "随行人员 " + (i+1) + " 身份证号格式不正确 (应为18位).");
                        request.getRequestDispatcher("/mobile/makeAppointment.jsp").forward(request, response);
                        return;
                    }

                    AccompanyingPerson person = new AccompanyingPerson();
                    // person.setPersonId(UUID.randomUUID().toString()); // Corrected: use setAccompanyingPersonId
                    person.setAccompanyingPersonId(UUID.randomUUID().toString());
                    person.setAppointmentId(appointment.getAppointmentId());
                    person.setName(currentAccName); // Use trimmed
                    person.setIdCard(currentAccIdCard); // Use trimmed
                    person.setPhone(currentAccPhone); // Use trimmed
                    accompanyingPersons.add(person);
                } else if (!currentAccName.isEmpty() || !currentAccIdCard.isEmpty() || !currentAccPhone.isEmpty()){
                    // If any field for an accompanying person is filled (after trimming), all main ones should be.
                    request.setAttribute("errorMessage", "随行人员 " + (i+1) + " 信息不完整 (姓名, 身份证, 电话都必填).");
                    request.getRequestDispatcher("/mobile/makeAppointment.jsp").forward(request, response);
                    return;
                }
            }
        }
        appointment.setAccompanyingPersons(accompanyingPersons);

        try (Connection conn = DBUtils.getDataSource().getConnection()) {
            boolean success = appointmentDAO.createAppointment(appointment); // DAO handles encryption
            if (success) {
                request.setAttribute("successMessage", "预约已提交! 公开访问预约已自动批准，公务来访请等待审批。");
                // Clear form or redirect to a success page/my appointments
                 response.sendRedirect(request.getContextPath() + "/mobile/makeAppointment?success=true"); // Simple redirect with query param
            } else {
                request.setAttribute("errorMessage", "预约提交失败，请稍后重试。");
                request.getRequestDispatcher("/mobile/makeAppointment.jsp").forward(request, response);
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Log this properly
            request.setAttribute("errorMessage", "数据库错误，预约失败: " + e.getMessage());
            request.getRequestDispatcher("/mobile/makeAppointment.jsp").forward(request, response);
        } catch (Exception e) {
            e.printStackTrace(); // Log this properly
            request.setAttribute("errorMessage", "处理预约时发生意外错误: " + e.getMessage());
            request.getRequestDispatcher("/mobile/makeAppointment.jsp").forward(request, response);
        }
    }
}
