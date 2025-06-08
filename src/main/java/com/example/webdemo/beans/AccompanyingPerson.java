package com.example.webdemo.beans;

public class AccompanyingPerson {
    private String accompanyingPersonId; // Changed from int to String
    private String appointmentId;      // Changed from int to String
    private String name;
    private String idCard;
    private String phone;

    public AccompanyingPerson() {
    }

    public AccompanyingPerson(String accompanyingPersonId, String appointmentId, String name, String idCard, String phone) { // Changed id types to String
        this.accompanyingPersonId = accompanyingPersonId;
        this.appointmentId = appointmentId;
        this.name = name;
        this.idCard = idCard;
        this.phone = phone;
    }

    // Getters and Setters
    public String getAccompanyingPersonId() { // Changed return type to String
        return accompanyingPersonId;
    }

    public void setAccompanyingPersonId(String accompanyingPersonId) { // Changed parameter type to String
        this.accompanyingPersonId = accompanyingPersonId;
    }

    public String getAppointmentId() { // Changed return type to String
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) { // Changed parameter type to String
        this.appointmentId = appointmentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public String toString() {
        return "AccompanyingPerson{" +
                "accompanyingPersonId='" + accompanyingPersonId + '\'' + // Adjusted for String type
                ", appointmentId='" + appointmentId + '\'' + // Adjusted for String type
                ", name='" + (name != null ? name.replace("'", "\\'") : null) + '\'' +
                ", idCard='" + (idCard != null ? idCard.replace("'", "\\'") : null) + '\'' +
                ", phone='" + (phone != null ? phone.replace("'", "\\'") : null) + '\'' +
                '}';
    }
}
