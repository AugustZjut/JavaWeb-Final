package com.example.webdemo.dao;

import com.example.webdemo.beans.Department;
import com.example.webdemo.util.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DepartmentDAO {

    public boolean addDepartment(Department department) throws SQLException {
        String sql = "INSERT INTO Departments (department_id, department_code, department_type, department_name) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            department.setDepartmentId(UUID.randomUUID().toString());
            pstmt.setString(1, department.getDepartmentId());
            pstmt.setString(2, department.getDepartmentCode());
            pstmt.setString(3, department.getDepartmentType());
            pstmt.setString(4, department.getDepartmentName());
            return pstmt.executeUpdate() > 0;
        }
    }

    public List<Department> getAllDepartments() throws SQLException {
        List<Department> departments = new ArrayList<>();
        String sql = "SELECT department_id, department_code, department_type, department_name FROM Departments ORDER BY department_name";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Department dept = new Department();
                dept.setDepartmentId(rs.getString("department_id"));
                dept.setDepartmentCode(rs.getString("department_code"));
                dept.setDepartmentType(rs.getString("department_type"));
                dept.setDepartmentName(rs.getString("department_name"));
                departments.add(dept);
            }
        }
        return departments;
    }

    public Department getDepartmentById(String departmentId) throws SQLException {
        String sql = "SELECT department_id, department_code, department_type, department_name FROM Departments WHERE department_id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, departmentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Department dept = new Department();
                    dept.setDepartmentId(rs.getString("department_id"));
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
        String sql = "UPDATE Departments SET department_code = ?, department_type = ?, department_name = ? WHERE department_id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, department.getDepartmentCode());
            pstmt.setString(2, department.getDepartmentType());
            pstmt.setString(3, department.getDepartmentName());
            pstmt.setString(4, department.getDepartmentId());
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean deleteDepartment(String departmentId) throws SQLException {
        // Add check for existing users in this department before deleting
        String checkUserSql = "SELECT COUNT(*) FROM Users WHERE department_id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkUserSql)) {
            checkStmt.setString(1, departmentId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    // Cannot delete department if users are associated with it
                    // Or, alternatively, handle this by setting department_id to null for those users,
                    // or preventing deletion through UI and showing a message.
                    // For now, we'll prevent deletion.
                    throw new SQLException("Cannot delete department: users are still associated with this department.");
                }
            }
        }

        String sql = "DELETE FROM Departments WHERE department_id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, departmentId);
            return pstmt.executeUpdate() > 0;
        }
    }
}
