package com.example.webdemo.servlet.admin;

import com.example.webdemo.beans.AuditLog;
import com.example.webdemo.beans.Department;
import com.example.webdemo.beans.User;
import com.example.webdemo.dao.AuditLogDAO;
import com.example.webdemo.dao.DepartmentDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@WebServlet("/admin/departmentManagement")
public class DepartmentManagementServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(DepartmentManagementServlet.class);
    private DepartmentDAO departmentDAO;
    private AuditLogDAO auditLogDAO;

    @Override
    public void init() throws ServletException {
        departmentDAO = new DepartmentDAO();
        auditLogDAO = new AuditLogDAO();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 验证用户是否登录并具有正确的角色
        HttpSession session = request.getSession(false);
        User loggedInAdmin = (session != null) ? (User) session.getAttribute("adminUser") : null;

        // 角色检查
        if (loggedInAdmin == null || !("SCHOOL_ADMIN".equals(loggedInAdmin.getRole()) || 
                                        "SYSTEM_ADMIN".equals(loggedInAdmin.getRole()))) {
            logger.warn("未授权访问部门管理：{}", loggedInAdmin != null ? loggedInAdmin.getUsername() : "未登录");
            response.sendRedirect(request.getContextPath() + "/admin/adminLogin.jsp");
            return;
        }
        
        String action = request.getParameter("action");
        if (action == null) {
            action = "list"; // Default action
        }

        try {
            switch (action) {
                case "add":
                    showNewForm(request, response);
                    break;
                case "edit":
                    showEditForm(request, response);
                    break;
                case "delete":
                    deleteDepartment(request, response);
                    break;
                case "list":
                default:
                    listDepartments(request, response);
                    break;
            }
        } catch (SQLException ex) {
            request.setAttribute("error", "Database error: " + ex.getMessage());
            listDepartmentsWithError(request, response, "Database error: " + ex.getMessage());
        } catch (Exception ex) {
            request.setAttribute("error", "An unexpected error occurred: " + ex.getMessage());
            listDepartmentsWithError(request, response, "An unexpected error occurred: " + ex.getMessage());
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        if ("save".equals(action)) {
            try {
                saveDepartment(request, response);
            } catch (SQLException ex) {
                 request.setAttribute("error", "Database error during save: " + ex.getMessage());
                 // Forward to form with error and existing data if possible
                 String idParam = request.getParameter("departmentId");
                 Department department = new Department();
                 if (idParam != null && !idParam.isEmpty()) {
                     department.setDepartmentId(Integer.parseInt(idParam)); // 将String转为int
                 }
                 department.setDepartmentCode(request.getParameter("departmentCode"));
                 department.setDepartmentType(request.getParameter("departmentType"));
                 department.setDepartmentName(request.getParameter("departmentName"));
                 request.setAttribute("department", department);
                 request.setAttribute("formAction", (idParam == null || idParam.isEmpty()) ? "add" : "edit");
                 request.getRequestDispatcher("/admin/departmentForm.jsp").forward(request, response);
            } catch (Exception ex) {
                request.setAttribute("error", "An unexpected error occurred during save: " + ex.getMessage());
                doGet(request, response); // Or redirect to list with a general error
            }
        } else {
            doGet(request, response); 
        }
    }

    private void listDepartments(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException, ServletException {
        List<Department> listDepartment = departmentDAO.getAllDepartments();
        request.setAttribute("listDepartment", listDepartment);
        request.getRequestDispatcher("/admin/departmentList.jsp").forward(request, response);
    }
    
    private void listDepartmentsWithError(HttpServletRequest request, HttpServletResponse response, String errorMessage) throws IOException, ServletException {
        try {
            List<Department> listDepartment = departmentDAO.getAllDepartments();
            request.setAttribute("listDepartment", listDepartment);
        } catch (SQLException e) {
            // If fetching departments also fails, set an empty list or handle appropriately
            request.setAttribute("listDepartment", new java.util.ArrayList<Department>());
            if (errorMessage == null || errorMessage.isEmpty()) {
                 request.setAttribute("error", "Could not retrieve department list after an initial error. " + e.getMessage());
            } else {
                 request.setAttribute("error", errorMessage + " Additionally, failed to refresh department list: " + e.getMessage());
            }
        }
        request.getRequestDispatcher("/admin/departmentList.jsp").forward(request, response);
    }


    private void showNewForm(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("department", new Department()); 
        request.setAttribute("formAction", "add");
        request.getRequestDispatcher("/admin/departmentForm.jsp").forward(request, response);
    }

    private void showEditForm(HttpServletRequest request, HttpServletResponse response) throws SQLException, ServletException, IOException {
        String id = request.getParameter("id");
        // 类型转换: 将String转为int
        Department existingDepartment = departmentDAO.getDepartmentById(Integer.parseInt(id));
        if (existingDepartment == null) {
            request.setAttribute("error", "Department not found.");
            listDepartments(request, response);
            return;
        }
        request.setAttribute("department", existingDepartment);
        request.setAttribute("formAction", "edit");
        request.getRequestDispatcher("/admin/departmentForm.jsp").forward(request, response);
    }

    private void saveDepartment(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException, ServletException {
        String departmentId = request.getParameter("departmentId");
        String departmentCode = request.getParameter("departmentCode");
        String departmentType = request.getParameter("departmentType");
        String departmentName = request.getParameter("departmentName");

        Department department = new Department();
        department.setDepartmentCode(departmentCode);
        department.setDepartmentType(departmentType);
        department.setDepartmentName(departmentName);

        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("adminUser") : null; // Corrected session attribute name
        String currentUsername = (currentUser != null) ? currentUser.getUsername() : "System";
        Integer currentUserId = (currentUser != null) ? currentUser.getUserId() : null; // 改为Integer类型

        String actionDetails;
        boolean success;

        if (departmentId == null || departmentId.isEmpty()) {
            // New department
            // 新部门不设置ID，让DAO/数据库自动处理ID生成
            // 不调用setDepartmentId，对于int类型的ID，我们不能设置为null
            success = departmentDAO.addDepartment(department);
            actionDetails = "Created new department: " + department.getDepartmentName() + " (Code: " + department.getDepartmentCode() + ")";
            if (success) {
                request.setAttribute("message", "Department added successfully.");
            } else {
                request.setAttribute("error", "Failed to add department.");
            }
        } else {
            // Update existing department
            // 类型转换: 将String转为Integer
            department.setDepartmentId(Integer.parseInt(departmentId));
            success = departmentDAO.updateDepartment(department);
            actionDetails = "Updated department: " + department.getDepartmentName() + " (ID: " + department.getDepartmentId() + ")";
             if (success) {
                request.setAttribute("message", "Department updated successfully.");
            } else {
                request.setAttribute("error", "Failed to update department.");
            }
        }

        if (success) {
            AuditLog log = new AuditLog(); // Use default constructor
            log.setUserId(currentUserId);
            log.setUsername(currentUsername);
            log.setActionType((departmentId == null || departmentId.isEmpty()) ? "DEPARTMENT_CREATE" : "DEPARTMENT_UPDATE");
            log.setDetails(actionDetails); // 修复方法名: 从setActionDetails改为setDetails
            log.setLogTimestamp(new Timestamp(System.currentTimeMillis())); // 修复方法名: 从setActionTime改为setLogTimestamp
            log.setIpAddress(request.getRemoteAddr()); // 修复方法名: 从setClientIp改为setIpAddress
            auditLogDAO.createLog(log);
        }
        
        // Redirect to list view after save, regardless of success, to show message/error
        listDepartments(request, response); 
    }

    private void deleteDepartment(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException, ServletException {
        String id = request.getParameter("id");
        // 类型转换: 将String转为int
        Department deptToDelete = departmentDAO.getDepartmentById(Integer.parseInt(id));
        String message = "";
        String error = "";

        if (deptToDelete == null) {
            error = "Department not found for deletion.";
        } else {
            try {
                // 类型转换: 将String转为int
                boolean success = departmentDAO.deleteDepartment(Integer.parseInt(id));
                if (success) {
                    message = "Department \"" + deptToDelete.getDepartmentName() + "\" deleted successfully.";
                    
                    HttpSession session = request.getSession(false);
                    User currentUser = (session != null) ? (User) session.getAttribute("adminUser") : null; // Corrected session attribute name
                    String currentUsername = (currentUser != null) ? currentUser.getUsername() : "System";
                    Integer currentUserId = (currentUser != null) ? currentUser.getUserId() : null; // 改为Integer类型

                    AuditLog log = new AuditLog(); // Use default constructor
                    log.setUserId(currentUserId);
                    log.setUsername(currentUsername);
                    log.setActionType("DEPARTMENT_DELETE");
                    log.setDetails("Deleted department: " + deptToDelete.getDepartmentName() + " (ID: " + id + ")"); // 修复方法名
                    log.setLogTimestamp(new Timestamp(System.currentTimeMillis())); // 修复方法名
                    log.setIpAddress(request.getRemoteAddr()); // 修复方法名
                    auditLogDAO.createLog(log);
                } else {
                    error = "Failed to delete department \"" + deptToDelete.getDepartmentName() + "\". It might be in use or a database error occurred.";
                }
            } catch (SQLException ex) {
                 if (ex.getMessage().contains("users are still associated")) {
                    error = "Cannot delete department: " + deptToDelete.getDepartmentName() + " as users are still associated with it.";
                } else {
                    error = "Error deleting department \"" + deptToDelete.getDepartmentName() + "\": " + ex.getMessage();
                }
            }
        }
        request.setAttribute("message", message);
        request.setAttribute("error", error);
        listDepartments(request, response);
    }
}
