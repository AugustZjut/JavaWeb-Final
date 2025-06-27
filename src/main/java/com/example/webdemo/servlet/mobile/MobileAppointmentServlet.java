package com.example.webdemo.servlet.mobile;

import com.example.webdemo.beans.AccompanyingPerson;
import com.example.webdemo.beans.Appointment;
import com.example.webdemo.dao.AppointmentDAO;
import com.example.webdemo.dao.UserDAO; 
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

// @WebServlet("/mobile/makeAppointment") // Removed as per web.xml configuration
public class MobileAppointmentServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private AppointmentDAO appointmentDAO;

    @Override
    public void init() throws ServletException {
        try {
            appointmentDAO = new AppointmentDAO(DBUtils.getDataSource());
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
        String visitDepartmentStr = request.getParameter("visitDepartment");
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

        // 不使用枚举，直接使用字符串常量
        String appointmentType = Appointment.AppointmentType.PUBLIC; // 默认值
        if ("OFFICIAL".equalsIgnoreCase(appointmentTypeStr)) {
            appointmentType = Appointment.AppointmentType.OFFICIAL;
        }
        
        String campus = Appointment.Campus.MAIN; // 默认值
        if ("SOUTH".equalsIgnoreCase(campusStr)) {
            campus = Appointment.Campus.SOUTH;
        } else if ("EAST".equalsIgnoreCase(campusStr)) {
            campus = Appointment.Campus.EAST;
        }

        Timestamp appointmentTime;
        try {
            appointmentTime = Timestamp.valueOf(LocalDateTime.parse(appointmentTimeStr));
        } catch (DateTimeParseException | IllegalArgumentException e) {
            request.setAttribute("errorMessage", "预约时间格式无效.");
            request.getRequestDispatcher("/mobile/makeAppointment.jsp").forward(request, response);
            return;
        }

        Appointment appointment = new Appointment();
        // 不设置ID，让数据库自动生成
        appointment.setApplicantName(applicantName.trim()); 
        appointment.setApplicantIdCard(trimmedApplicantIdCard); 
        appointment.setApplicantPhone(applicantPhone.trim()); 
        appointment.setApplicantOrganization(organization != null ? organization.trim() : null);
        appointment.setAppointmentType(appointmentType);
        appointment.setCampus(campus);
        appointment.setEntryDatetime(appointmentTime); 
        appointment.setTransportMode(transportationStr);
        appointment.setLicensePlate(licensePlate); 
        appointment.setApplicationDate(new Timestamp(System.currentTimeMillis())); 

        // 如果是公务预约
        if (Appointment.AppointmentType.OFFICIAL.equals(appointmentType)) {
            if (visitDepartmentStr == null || visitDepartmentStr.trim().isEmpty() ||
                contactPersonName == null || contactPersonName.trim().isEmpty() ||
                visitReason == null || visitReason.trim().isEmpty()) {
                request.setAttribute("errorMessage", "公务来访必填字段不能为空 (到访部门, 联系人, 事由).");
                request.getRequestDispatcher("/mobile/makeAppointment.jsp").forward(request, response);
                return;
            }
            
            // 类型转换: String转为Integer (如果visitDepartmentStr是部门ID)
            Integer officialVisitDepartmentId = null;
            try {
                officialVisitDepartmentId = Integer.parseInt(visitDepartmentStr);
            } catch (NumberFormatException e) {
                request.setAttribute("errorMessage", "部门ID格式无效.");
                request.getRequestDispatcher("/mobile/makeAppointment.jsp").forward(request, response);
                return;
            }
            
            appointment.setOfficialVisitDepartmentId(officialVisitDepartmentId);
            appointment.setOfficialVisitContactPerson(contactPersonName.trim());
            appointment.setVisitReason(visitReason.trim());
            appointment.setStatus(Appointment.Status.PENDING_APPROVAL);
        } else {
            // 社会公众预约自动批准
            appointment.setStatus(Appointment.Status.APPROVED);
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
                    // 不设置ID，由数据库自动生成
                    // 注意：这里需要修正appointmentId的处理，因为我们还没有保存appointment，所以没有ID
                    // 我们会在添加完appointment后再设置此值
                    person.setName(currentAccName); 
                    person.setIdCard(currentAccIdCard); 
                    person.setPhone(currentAccPhone); 
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
