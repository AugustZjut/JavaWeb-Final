package com.example.webdemo.beans;

import java.sql.Timestamp;

// 随行人员实体类，对应数据库 accompanying_persons 表
public class AccompanyingPerson {
    // 随行人员ID，自增主键
    private int accompanyingPersonId;
    // 预约ID，外键
    private int appointmentId;
    // 姓名
    private String name;
    // 身份证号（加密）
    private String idCard;
    // 手机号（加密）
    private String phone;
    // 创建时间
    private Timestamp createdAt;

    public AccompanyingPerson() {
    }

    public int getAccompanyingPersonId() { return accompanyingPersonId; }
    public void setAccompanyingPersonId(int accompanyingPersonId) { this.accompanyingPersonId = accompanyingPersonId; }

    public int getAppointmentId() { return appointmentId; }
    public void setAppointmentId(int appointmentId) { this.appointmentId = appointmentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "AccompanyingPerson{" +
                "accompanyingPersonId=" + accompanyingPersonId +
                ", appointmentId=" + appointmentId +
                ", name='" + name + '\'' +
                ", idCard='" + idCard + '\'' +
                ", phone='" + phone + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
