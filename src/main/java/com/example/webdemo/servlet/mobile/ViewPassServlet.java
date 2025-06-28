package com.example.webdemo.servlet.mobile;

import com.example.webdemo.beans.AccompanyingPerson;
import com.example.webdemo.beans.Appointment;
import com.example.webdemo.dao.AppointmentDAO;
import com.example.webdemo.util.DataMaskingUtils;
import com.example.webdemo.util.QRCodeUtils;
import com.example.webdemo.util.DBUtils;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ViewPassServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private AppointmentDAO appointmentDAO;

    @Override
    public void init() throws ServletException {
        appointmentDAO = new AppointmentDAO(DBUtils.getDataSource());
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String appointmentId = request.getParameter("appointmentId");
        String originalApplicantIdCard = request.getParameter("originalIdCard");

        if (appointmentId == null || appointmentId.trim().isEmpty()) {
            request.setAttribute("errorMessage", "预约ID不能为空");
            request.getRequestDispatcher("/mobile/viewPass.jsp").forward(request, response);
            return;
        }

        try {
            // 将appointmentId从String转换为int
            int id = Integer.parseInt(appointmentId);
            Appointment appointment = appointmentDAO.getAppointmentById(id);

            if (appointment != null) {
                // 检查预约状态和时间，判断通行码是否有效
                boolean isApproved = Appointment.Status.APPROVED.equals(appointment.getStatus());
                
                // 获取当前时间和预约时间，进行比较
                Date currentDate = new Date();
                Timestamp appointmentTime = appointment.getEntryDatetime();
                Date appointmentDate = new Date(appointmentTime.getTime());
                
                // 创建一个日历实例，设置为预约时间
                Calendar appointmentCalendar = Calendar.getInstance();
                appointmentCalendar.setTime(appointmentDate);
                
                // 创建一个日历实例，设置为当前时间
                Calendar currentCalendar = Calendar.getInstance();
                currentCalendar.setTime(currentDate);
                
                // 预约有效期：预约当天的0点到23:59:59
                Calendar startValidTime = Calendar.getInstance();
                startValidTime.setTime(appointmentDate);
                startValidTime.set(Calendar.HOUR_OF_DAY, 0);
                startValidTime.set(Calendar.MINUTE, 0);
                startValidTime.set(Calendar.SECOND, 0);
                
                Calendar endValidTime = Calendar.getInstance();
                endValidTime.setTime(appointmentDate);
                endValidTime.set(Calendar.HOUR_OF_DAY, 23);
                endValidTime.set(Calendar.MINUTE, 59);
                endValidTime.set(Calendar.SECOND, 59);
                
                // 判断当前时间是否在预约有效期内
                boolean isWithinValidTimeRange = currentDate.after(startValidTime.getTime()) && 
                                                 currentDate.before(endValidTime.getTime());
                
                // 最终判断通行码是否有效：状态为已批准且在有效时间范围内
                boolean isValidPass = isApproved && isWithinValidTimeRange;
                
                // 处理并脱敏申请人信息
                String maskedApplicantName = DataMaskingUtils.maskName(appointment.getApplicantName());
                String maskedApplicantIdCard = DataMaskingUtils.maskIdCard(appointment.getApplicantIdCard());
                
                // 获取并处理随行人员信息
                List<Map<String, String>> maskedAccompanyingPersons = new ArrayList<>();
                if (appointment.getAccompanyingPersons() != null) {
                    for (AccompanyingPerson person : appointment.getAccompanyingPersons()) {
                        Map<String, String> maskedPerson = new HashMap<>();
                        maskedPerson.put("maskedName", DataMaskingUtils.maskName(person.getName()));
                        maskedPerson.put("maskedIdCard", DataMaskingUtils.maskIdCard(person.getIdCard()));
                        maskedAccompanyingPersons.add(maskedPerson);
                    }
                }
                
                // 生成二维码内容，包括脱敏姓名、身份证号和生成时间
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date generationTime = new Date();
                String formattedGenerationTime = sdf.format(generationTime);
                
                // 构建二维码内容
                StringBuilder qrCodeContent = new StringBuilder();
                qrCodeContent.append("姓名: ").append(maskedApplicantName).append("\n");
                qrCodeContent.append("身份证: ").append(maskedApplicantIdCard).append("\n");
                qrCodeContent.append("生成时间: ").append(formattedGenerationTime).append("\n");
                qrCodeContent.append("预约校区: ").append(appointment.getCampus()).append("\n");
                qrCodeContent.append("预约日期: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(appointmentTime)).append("\n");
                qrCodeContent.append("状态: ").append(isValidPass ? "有效" : "无效");
                
                String qrCodeBase64 = QRCodeUtils.generateQRCodeBase64(qrCodeContent.toString(), 300, 300);
                
                // 设置请求属性
                request.setAttribute("appointment", appointment);
                request.setAttribute("maskedApplicantName", maskedApplicantName);
                request.setAttribute("maskedApplicantIdCard", maskedApplicantIdCard);
                request.setAttribute("accompanyingPersons", maskedAccompanyingPersons);
                request.setAttribute("qrCodeBase64", qrCodeBase64);
                request.setAttribute("generationTime", generationTime);
                request.setAttribute("isValidPass", isValidPass);
                request.setAttribute("originalApplicantIdCard", originalApplicantIdCard);
                
                // 转发到JSP
                request.getRequestDispatcher("/mobile/viewPass.jsp").forward(request, response);
            } else {
                request.setAttribute("errorMessage", "未找到预约信息");
                request.getRequestDispatcher("/mobile/viewPass.jsp").forward(request, response);
            }
        } catch (NumberFormatException e) {
            request.setAttribute("errorMessage", "无效的预约ID格式");
            request.getRequestDispatcher("/mobile/viewPass.jsp").forward(request, response);
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "数据库错误：" + e.getMessage());
            request.getRequestDispatcher("/mobile/viewPass.jsp").forward(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "系统错误：" + e.getMessage());
            request.getRequestDispatcher("/mobile/viewPass.jsp").forward(request, response);
        }
    }
}
