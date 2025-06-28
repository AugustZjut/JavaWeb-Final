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

    /**
     * 根据条件搜索部门
     * @param departmentCode 部门代码（模糊搜索）
     * @param departmentName 部门名称（模糊搜索）
     * @param departmentType 部门类型（精确匹配）
     * @return 符合条件的部门列表
     */
    public List<Department> searchDepartments(String departmentCode, String departmentName, String departmentType) throws SQLException {
        List<Department> departments = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT department_id, department_code, department_name, department_type FROM departments WHERE 1=1");
        List<String> params = new ArrayList<>();

        if (departmentCode != null && !departmentCode.trim().isEmpty()) {
            sql.append(" AND LOWER(department_code) LIKE LOWER(?)");
            params.add("%" + departmentCode.trim() + "%");
        }

        if (departmentName != null && !departmentName.trim().isEmpty()) {
            sql.append(" AND LOWER(department_name) LIKE LOWER(?)");
            params.add("%" + departmentName.trim() + "%");
        }

        if (departmentType != null && !departmentType.trim().isEmpty() && !"ALL".equals(departmentType)) {
            sql.append(" AND department_type = ?");
            params.add(departmentType);
        }

        sql.append(" ORDER BY department_name");

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                pstmt.setString(i + 1, params.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Department dept = new Department();
                    dept.setDepartmentId(rs.getInt("department_id"));
                    dept.setDepartmentCode(rs.getString("department_code"));
                    dept.setDepartmentType(rs.getString("department_type"));
                    dept.setDepartmentName(rs.getString("department_name"));
                    departments.add(dept);
                }
            }
        }
        return departments;
    }

    /**
     * 检查部门代码是否已存在
     * @param departmentCode 要检查的部门代码
     * @param excludeId 排除的部门ID（用于更新时检查）
     * @return true如果代码已存在，false否则
     */
    public boolean isDepartmentCodeExists(String departmentCode, Integer excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM departments WHERE LOWER(department_code) = LOWER(?)";
        if (excludeId != null) {
            sql += " AND department_id != ?";
        }

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, departmentCode);
            if (excludeId != null) {
                pstmt.setInt(2, excludeId);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * 获取部门下的用户数量
     * @param departmentId 部门ID
     * @return 该部门下的用户数量
     */
    public int getUserCountByDepartment(int departmentId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE department_id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, departmentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}
