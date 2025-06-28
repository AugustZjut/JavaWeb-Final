package com.example.webdemo.beans;

import java.sql.Timestamp;
import java.util.List;

// 预约记录实体类，对应数据库 appointments 表
public class Appointment {
    // 预约类型枚举
    public static class AppointmentType {
        public static final String PUBLIC = "PUBLIC";  // 社会公众预约
        public static final String OFFICIAL = "OFFICIAL";  // 公务预约
    }
    
    // 预约状态枚举 
    public static class Status {
        public static final String PENDING_APPROVAL = "PENDING_APPROVAL"; // 待审批
        public static final String APPROVED = "APPROVED";  // 已批准
        public static final String REJECTED = "REJECTED";  // 已拒绝
        public static final String CANCELLED = "CANCELLED";  // 已取消
        public static final String COMPLETED = "COMPLETED";  // 已完成
        public static final String EXPIRED = "EXPIRED";  // 已过期
    }

    // 校区枚举
    public static class Campus {
        public static final String MAIN = "朝晖校区"; 
        public static final String SOUTH = "屏峰校区"; 
        public static final String EAST = "莫干山校区"; 
    }
    
    // 预约ID，自增主键
    private int appointmentId;
    // 预约类型（PUBLIC、OFFICIAL）
    private String appointmentType;
    // 校区
    private String campus;
    // 入校时间
    private Timestamp entryDatetime;
    // 申请单位
    private String applicantOrganization;
    // 申请人姓名（加密）
    private String applicantName;
    // 申请人身份证号（加密）
    private String applicantIdCard;
    // 申请人手机号（加密）
    private String applicantPhone;
    // 交通方式
    private String transportMode;
    // 车牌号
    private String licensePlate;
    // 被访部门ID
    private Integer officialVisitDepartmentId;
    // 被访联系人
    private String officialVisitContactPerson;
    // 访问事由
    private String visitReason;
    // 状态
    private String status;
    // 二维码数据
    private String qrCodeData;
    // 二维码生成时间
    private Timestamp qrCodeGeneratedAt;
    // 申请时间
    private Timestamp applicationDate;
    // 审批人ID
    private Integer approvedByUserId;
    // 审批时间
    private Timestamp approvalDatetime;
    // 创建时间
    private Timestamp createdAt;
    // 更新时间
    private Timestamp updatedAt;
    // 随行人员列表
    private List<AccompanyingPerson> accompanyingPersons;

    public Appointment() {
    }

    // 省略构造方法，可按需添加

    public int getAppointmentId() { return appointmentId; }
    public void setAppointmentId(int appointmentId) { this.appointmentId = appointmentId; }

    public String getAppointmentType() { return appointmentType; }
    public void setAppointmentType(String appointmentType) { this.appointmentType = appointmentType; }

    public String getCampus() { return campus; }
    public void setCampus(String campus) { this.campus = campus; }

    public Timestamp getEntryDatetime() { return entryDatetime; }
    public void setEntryDatetime(Timestamp entryDatetime) { this.entryDatetime = entryDatetime; }

    public String getApplicantOrganization() { return applicantOrganization; }
    public void setApplicantOrganization(String applicantOrganization) { this.applicantOrganization = applicantOrganization; }

    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }

    public String getApplicantIdCard() { return applicantIdCard; }
    public void setApplicantIdCard(String applicantIdCard) { this.applicantIdCard = applicantIdCard; }

    public String getApplicantPhone() { return applicantPhone; }
    public void setApplicantPhone(String applicantPhone) { this.applicantPhone = applicantPhone; }

    public String getTransportMode() { return transportMode; }
    public void setTransportMode(String transportMode) { this.transportMode = transportMode; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public Integer getOfficialVisitDepartmentId() { return officialVisitDepartmentId; }
    public void setOfficialVisitDepartmentId(Integer officialVisitDepartmentId) { this.officialVisitDepartmentId = officialVisitDepartmentId; }

    public String getOfficialVisitContactPerson() { return officialVisitContactPerson; }
    public void setOfficialVisitContactPerson(String officialVisitContactPerson) { this.officialVisitContactPerson = officialVisitContactPerson; }

    public String getVisitReason() { return visitReason; }
    public void setVisitReason(String visitReason) { this.visitReason = visitReason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getQrCodeData() { return qrCodeData; }
    public void setQrCodeData(String qrCodeData) { this.qrCodeData = qrCodeData; }

    public Timestamp getQrCodeGeneratedAt() { return qrCodeGeneratedAt; }
    public void setQrCodeGeneratedAt(Timestamp qrCodeGeneratedAt) { this.qrCodeGeneratedAt = qrCodeGeneratedAt; }

    public Timestamp getApplicationDate() { return applicationDate; }
    public void setApplicationDate(Timestamp applicationDate) { this.applicationDate = applicationDate; }

    public Integer getApprovedByUserId() { return approvedByUserId; }
    public void setApprovedByUserId(Integer approvedByUserId) { this.approvedByUserId = approvedByUserId; }

    public Timestamp getApprovalDatetime() { return approvalDatetime; }
    public void setApprovalDatetime(Timestamp approvalDatetime) { this.approvalDatetime = approvalDatetime; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public List<AccompanyingPerson> getAccompanyingPersons() { return accompanyingPersons; }
    public void setAccompanyingPersons(List<AccompanyingPerson> accompanyingPersons) { this.accompanyingPersons = accompanyingPersons; }

    // 以下是为了兼容旧代码中使用的appointmentTime属性添加的方法
    public Timestamp getAppointmentTime() {
        return entryDatetime;
    }

    public void setAppointmentTime(Timestamp appointmentTime) {
        this.entryDatetime = appointmentTime;
    }

    @Override
    public String toString() {
        return "Appointment{" +
                "appointmentId=" + appointmentId +
                ", appointmentType='" + appointmentType + '\'' +
                ", campus='" + campus + '\'' +
                ", entryDatetime=" + entryDatetime +
                ", applicantOrganization='" + applicantOrganization + '\'' +
                ", applicantName='" + applicantName + '\'' +
                ", applicantIdCard='" + applicantIdCard + '\'' +
                ", applicantPhone='" + applicantPhone + '\'' +
                ", transportMode='" + transportMode + '\'' +
                ", licensePlate='" + licensePlate + '\'' +
                ", officialVisitDepartmentId=" + officialVisitDepartmentId +
                ", officialVisitContactPerson='" + officialVisitContactPerson + '\'' +
                ", visitReason='" + visitReason + '\'' +
                ", status='" + status + '\'' +
                ", qrCodeData='" + qrCodeData + '\'' +
                ", qrCodeGeneratedAt=" + qrCodeGeneratedAt +
                ", applicationDate=" + applicationDate +
                ", approvedByUserId=" + approvedByUserId +
                ", approvalDatetime=" + approvalDatetime +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", accompanyingPersons=" + (accompanyingPersons != null ? accompanyingPersons.toString() : null) +
                '}';
    }
}
