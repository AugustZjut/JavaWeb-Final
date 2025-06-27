package com.example.webdemo.dao;

import com.example.webdemo.beans.Department;
import com.example.webdemo.util.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DepartmentDAO {

    public boolean addDepartment(Department department) throws SQLException {
        String sql = "INSERT INTO departments (department_code, department_name, department_type) VALUES (?, ?, ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, department.getDepartmentCode());
            pstmt.setString(2, department.getDepartmentName());
            pstmt.setString(3, department.getDepartmentType());
            return pstmt.executeUpdate() > 0;
        }
    }

    public List<Department> getAllDepartments() throws SQLException {
        List<Department> departments = new ArrayList<>();
        String sql = "SELECT department_id, department_code, department_name, department_type FROM departments ORDER BY department_name";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Department dept = new Department();
                dept.setDepartmentId(rs.getInt("department_id"));
                dept.setDepartmentCode(rs.getString("department_code"));
                dept.setDepartmentType(rs.getString("department_type"));
                dept.setDepartmentName(rs.getString("department_name"));
                departments.add(dept);
            }
        }
        return departments;
    }

    public Department getDepartmentById(int departmentId) throws SQLException {
        String sql = "SELECT department_id, department_code, department_name, department_type FROM departments WHERE department_id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, departmentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Department dept = new Department();
                    dept.setDepartmentId(rs.getInt("department_id"));
                    dept.setDepartmentCode(rs.getString("department_code"));
                    dept.setDepartmentType(rs.getString("department_type"));
                    dept.setDepartmentName(rs.getString("department_name"));
                    return dept;
                }
            }
        }
        return null;
    }

    public boolean updateDepartment(Department department) throws SQLException {
        String sql = "UPDATE departments SET department_code = ?, department_name = ?, department_type = ? WHERE department_id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, department.getDepartmentCode());
            pstmt.setString(2, department.getDepartmentName());
            pstmt.setString(3, department.getDepartmentType());
            pstmt.setInt(4, department.getDepartmentId());
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean deleteDepartment(int departmentId) throws SQLException {
        String checkUserSql = "SELECT COUNT(*) FROM users WHERE department_id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkUserSql)) {
            checkStmt.setInt(1, departmentId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new SQLException("Cannot delete department: users are still associated with this department.");
                }
            }
        }
        String sql = "DELETE FROM departments WHERE department_id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, departmentId);
            return pstmt.executeUpdate() > 0;
        }
    }
}
