package com.example.webdemo.beans;

public class Department {
    private String departmentId; // Changed from int to String
    private String departmentCode;
    private String departmentType; // E.g., "School", "Department"
    private String departmentName;

    public Department() {
    }

    public Department(String departmentId, String departmentCode, String departmentType, String departmentName) { // Changed from int to String
        this.departmentId = departmentId;
        this.departmentCode = departmentCode;
        this.departmentType = departmentType;
        this.departmentName = departmentName;
    }

    // Getters and Setters
    public String getDepartmentId() { // Changed from int to String
        return departmentId;
    }

    public void setDepartmentId(String departmentId) { // Changed from int to String
        this.departmentId = departmentId;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }

    public String getDepartmentType() {
        return departmentType;
    }

    public void setDepartmentType(String departmentType) {
        this.departmentType = departmentType;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    @Override
    public String toString() {
        return "Department{" +
                "departmentId='" + departmentId + '\'' + // Corrected for String
                ", departmentCode='" + departmentCode + '\'' +
                ", departmentType='" + departmentType + '\'' +
                ", departmentName='" + departmentName + '\'' +
                '}';
    }
}
