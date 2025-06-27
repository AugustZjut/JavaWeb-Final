package com.example.webdemo.beans;

// 部门实体类，对应数据库 departments 表
public class Department {
    // 部门ID，自增主键
    private int departmentId;
    // 部门编号，唯一
    private String departmentCode;
    // 部门名称
    private String departmentName;
    // 部门类型（ADMINISTRATIVE、DIRECTLY_AFFILIATED、COLLEGE）
    private String departmentType;

    public Department() {
    }

    public Department(int departmentId, String departmentCode, String departmentName, String departmentType) {
        this.departmentId = departmentId;
        this.departmentCode = departmentCode;
        this.departmentName = departmentName;
        this.departmentType = departmentType;
    }

    public int getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(int departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getDepartmentType() {
        return departmentType;
    }

    public void setDepartmentType(String departmentType) {
        this.departmentType = departmentType;
    }

    @Override
    public String toString() {
        return "Department{" +
                "departmentId=" + departmentId +
                ", departmentCode='" + departmentCode + '\'' +
                ", departmentName='" + departmentName + '\'' +
                ", departmentType='" + departmentType + '\'' +
                '}';
    }
}
