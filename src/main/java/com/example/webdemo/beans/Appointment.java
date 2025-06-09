package com.example.webdemo.beans;

import java.sql.Timestamp;
import java.util.List; // Added for accompanyingPersons

public class Appointment {
    private String appointmentId; // Changed to String for UUID
    private String campus;
    private Timestamp entryDatetime; // Renamed from appointmentTime
    private String applicantOrganization; // Renamed from organization
    private String applicantName;
    private String applicantIdCard;
    private String applicantPhone;
    private String transportMode; // Renamed from transportation
    private String licensePlate; // Optional
    private String visitDepartment;
    private String visitContactPerson; // Renamed from contactPersonName
    private String contactPersonPhone; // Added
    private String visitReason;
    private String appointmentType; // "PUBLIC" or "OFFICIAL_VISIT"
    private String status; // Renamed from approvalStatus to match DB
    private Timestamp applicationDate; // Renamed from submissionTime to match DB
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private List<AccompanyingPerson> accompanyingPersons; // Added

    // Enum for Campus
    public enum Campus {
        ZHAOHUI_CAMPUS, PINGFENG_CAMPUS, MOGANSHAN_CAMPUS // Updated to actual campus values
    }

    // Enum for AppointmentType
    public enum AppointmentType {
        PUBLIC_ACCESS, OFFICIAL_VISIT // Modified from PUBLIC to PUBLIC_ACCESS
    }

    // Enum for ApprovalStatus
    public enum ApprovalStatus {
        PENDING, APPROVED, REJECTED, CANCELLED, AUTO_APPROVED
    }

    // Enum for Transportation (if it's an enum, otherwise keep as String)
    // Assuming transportMode is a String as per previous usage.
    // If it were an enum:
    // public enum Transportation {
    //     WALK, BICYCLE, CAR, PUBLIC_TRANSPORT
    // }


    public Appointment() {
    }

    // Getters and Setters
    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getCampus() {
        return campus;
    }

    public void setCampus(String campus) {
        this.campus = campus;
    }

    public Timestamp getEntryDatetime() { // Renamed from getAppointmentTime
        return entryDatetime;
    }

    public void setEntryDatetime(Timestamp entryDatetime) { // Renamed from setAppointmentTime
        this.entryDatetime = entryDatetime;
    }

    public String getApplicantOrganization() {
        return applicantOrganization;
    }

    public void setApplicantOrganization(String applicantOrganization) {
        this.applicantOrganization = applicantOrganization;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public String getApplicantIdCard() {
        return applicantIdCard;
    }

    public void setApplicantIdCard(String applicantIdCard) {
        this.applicantIdCard = applicantIdCard;
    }

    public String getApplicantPhone() {
        return applicantPhone;
    }

    public void setApplicantPhone(String applicantPhone) {
        this.applicantPhone = applicantPhone;
    }

    public String getTransportMode() {
        return transportMode;
    }

    public void setTransportMode(String transportMode) {
        this.transportMode = transportMode;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getVisitDepartment() {
        return visitDepartment;
    }

    public void setVisitDepartment(String visitDepartment) {
        this.visitDepartment = visitDepartment;
    }

    public String getVisitContactPerson() {
        return visitContactPerson;
    }

    public void setVisitContactPerson(String visitContactPerson) {
        this.visitContactPerson = visitContactPerson;
    }
    
    public String getContactPersonPhone() {
        return contactPersonPhone;
    }

    public void setContactPersonPhone(String contactPersonPhone) {
        this.contactPersonPhone = contactPersonPhone;
    }

    public String getVisitReason() {
        return visitReason;
    }

    public void setVisitReason(String visitReason) {
        this.visitReason = visitReason;
    }

    public String getAppointmentType() {
        return appointmentType;
    }

    public void setAppointmentType(String appointmentType) {
        this.appointmentType = appointmentType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getApplicationDate() {
        return applicationDate;
    }

    public void setApplicationDate(Timestamp applicationDate) {
        this.applicationDate = applicationDate;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<AccompanyingPerson> getAccompanyingPersons() {
        return accompanyingPersons;
    }

    public void setAccompanyingPersons(List<AccompanyingPerson> accompanyingPersons) {
        this.accompanyingPersons = accompanyingPersons;
    }

    // Convenience getter for DAO layer that might expect organization
    public String getOrganization() {
        return applicantOrganization;
    }
    
    // Convenience getter for DAO layer that might expect transportation as an enum string
    // This assumes transportMode is the string representation.
    public String getTransportation() { 
        return transportMode;
    }

    // Convenience getter for DAO layer
    public String getContactPersonName() {
        return visitContactPerson;
    }


    @Override
    public String toString() {
        return "Appointment{" +
                "appointmentId='" + (appointmentId != null ? appointmentId.replace("'", "\\'") : null) + '\'' +
                ", campus='" + (campus != null ? campus.replace("'", "\\'") : null) + '\'' +
                ", entryDatetime=" + entryDatetime +
                ", applicantOrganization='" + (applicantOrganization != null ? applicantOrganization.replace("'", "\\'") : null) + '\'' +
                ", applicantName='" + (applicantName != null ? applicantName.replace("'", "\\'") : null) + '\'' +
                ", applicantIdCard='" + (applicantIdCard != null ? applicantIdCard.replace("'", "\\'") : null) + '\'' +
                ", applicantPhone='" + (applicantPhone != null ? applicantPhone.replace("'", "\\'") : null) + '\'' +
                ", transportMode='" + (transportMode != null ? transportMode.replace("'", "\\'") : null) + '\'' +
                ", licensePlate='" + (licensePlate != null ? licensePlate.replace("'", "\\'") : null) + '\'' +
                ", visitDepartment='" + (visitDepartment != null ? visitDepartment.replace("'", "\\'") : null) + '\'' +
                ", visitContactPerson='" + (visitContactPerson != null ? visitContactPerson.replace("'", "\\'") : null) + '\'' +
                ", contactPersonPhone='" + (contactPersonPhone != null ? contactPersonPhone.replace("'", "\\'") : null) + '\'' +
                ", visitReason='" + (visitReason != null ? visitReason.replace("'", "\\'") : null) + '\'' +
                ", appointmentType='" + (appointmentType != null ? appointmentType.replace("'", "\\'") : null) + '\'' +
                ", status='" + (status != null ? status.replace("'", "\\'") : null) + '\'' +
                ", applicationDate=" + applicationDate +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", accompanyingPersons=" + (accompanyingPersons != null ? accompanyingPersons.toString().replace("'", "\\'") : null) +
                '}';
    }
}
