package com.example.webdemo.servlet.admin;

import com.example.webdemo.beans.User;
import com.example.webdemo.beans.Department;
import com.example.webdemo.dao.UserDAO;
import com.example.webdemo.dao.DepartmentDAO;
import com.example.webdemo.dao.AuditLogDAO;
import com.example.webdemo.beans.AuditLog;
import com.example.webdemo.util.CryptoUtils;
import com.example.webdemo.util.DBUtils; // Assuming UserDAO gets datasource from DBUtils or constructor

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList; // Added import
import java.util.Date;
import java.util.List;
import java.util.UUID;

@WebServlet("/admin/userManagement")
public class UserManagementServlet extends HttpServlet {
    private UserDAO userDAO;
    private DepartmentDAO departmentDAO;
    private AuditLogDAO auditLogDAO;
    // private static final String SM4_KEY; // Loaded from properties or constant

    @Override
    public void init() throws ServletException {
        // Initialize DAOs, potentially passing DataSource from DBUtils
        userDAO = new UserDAO(DBUtils.getDataSource()); 
        departmentDAO = new DepartmentDAO(); // Corrected: DepartmentDAO does not take DataSource in constructor
        auditLogDAO = new AuditLogDAO(); // Corrected: AuditLogDAO does not take DataSource in constructor
        // Load SM4_KEY if not already handled by CryptoUtils or UserDAO directly
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User loggedInAdmin = (session != null) ? (User) session.getAttribute("adminUser") : null;

        // Role check based on updated User bean's getRole() which returns String
        if (loggedInAdmin == null || !("SchoolAdmin".equals(loggedInAdmin.getRole()) || "SystemAdmin".equals(loggedInAdmin.getRole()))) {
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
                case "edit":
                    showUserForm(request, response, loggedInAdmin);
                    break;
                case "delete":
                    deleteUser(request, response, loggedInAdmin);
                    break;
                case "list":
                default:
                    listUsers(request, response, loggedInAdmin);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace(); // Log error
            request.setAttribute("errorMessage", "处理请求时发生错误: " + e.getMessage());
            listUsers(request, response, loggedInAdmin);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User loggedInAdmin = (session != null) ? (User) session.getAttribute("adminUser") : null;

        if (loggedInAdmin == null || !("SchoolAdmin".equals(loggedInAdmin.getRole()) || "SystemAdmin".equals(loggedInAdmin.getRole()))) {
            response.sendRedirect(request.getContextPath() + "/admin/adminLogin.jsp");
            return;
        }

        String action = request.getParameter("action");

        try {
            switch (action) {
                case "create":
                    createUser(request, response, loggedInAdmin);
                    break;
                case "update":
                    updateUser(request, response, loggedInAdmin);
                    break;
                default:
                    listUsers(request, response, loggedInAdmin);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace(); // Log error
            request.setAttribute("errorMessage", "处理请求时发生错误: " + e.getMessage());
            // Determine appropriate redirect or forward based on context
            if ("create".equals(action) || "update".equals(action)) {
                 showUserForm(request, response, loggedInAdmin); // Show form again with error
            } else {
                listUsers(request, response, loggedInAdmin);
            }
        }
    }

    private void listUsers(HttpServletRequest request, HttpServletResponse response, User loggedInAdmin) throws ServletException, IOException {
        try {
            List<User> userList;
            if ("SchoolAdmin".equals(loggedInAdmin.getRole())) {
                // SchoolAdmin can only manage DepartmentAdmin users
                userList = userDAO.getUsersByRole("DepartmentAdmin");
            } else if ("SystemAdmin".equals(loggedInAdmin.getRole())) {
                // SystemAdmin can manage all users
                userList = userDAO.listAllUsers();
            } else {
                // Should not happen due to initial role check, but as a fallback:
                userList = new ArrayList<>(); 
                request.setAttribute("errorMessage", "您没有权限查看用户列表。");
            }
            List<Department> departmentList = departmentDAO.getAllDepartments();
            request.setAttribute("userList", userList);
            request.setAttribute("departmentList", departmentList);
            request.getRequestDispatcher("/admin/userList.jsp").forward(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "获取用户列表失败: " + e.getMessage());
            request.getRequestDispatcher("/admin/userList.jsp").forward(request, response);
        }
    }

    private void showUserForm(HttpServletRequest request, HttpServletResponse response, User loggedInAdmin) throws ServletException, IOException {
        String userIdStr = request.getParameter("id");
        User userToEdit = null;
        if (userIdStr != null && !userIdStr.isEmpty()) {
            try {
                userToEdit = userDAO.findById(userIdStr);
                if (userToEdit == null) {
                    request.setAttribute("errorMessage", "未找到指定ID的管理员用户。");
                    listUsers(request, response, loggedInAdmin);
                    return;
                }
                // Role-based access check for editing
                if ("SchoolAdmin".equals(loggedInAdmin.getRole()) && !"DepartmentAdmin".equals(userToEdit.getRole())) {
                    request.setAttribute("errorMessage", "您只能编辑部门管理员用户。");
                    listUsers(request, response, loggedInAdmin);
                    return;
                }
            } catch (Exception e) { 
                request.setAttribute("errorMessage", "获取用户信息失败: " + e.getMessage());
                listUsers(request, response, loggedInAdmin);
                return;
            }
        }
        request.setAttribute("userToEdit", userToEdit);
        try {
            List<Department> departmentList = departmentDAO.getAllDepartments();
            request.setAttribute("departmentList", departmentList);
        } catch (Exception e) {
            request.setAttribute("errorMessage", "获取部门列表失败: " + e.getMessage());
        }
        request.getRequestDispatcher("/admin/userForm.jsp").forward(request, response);
    }

    private void createUser(HttpServletRequest request, HttpServletResponse response, User loggedInAdmin) throws ServletException, IOException {
        User newUser = new User();
        newUser.setUserId(UUID.randomUUID().toString()); 
        newUser.setUsername(request.getParameter("username"));
        String rawPassword = request.getParameter("password");
        String role = request.getParameter("role");

        // Role restriction for SchoolAdmin
        if ("SchoolAdmin".equals(loggedInAdmin.getRole()) && !"DepartmentAdmin".equals(role)) {
            request.setAttribute("errorMessage", "校级管理员只能创建部门管理员账户。");
            request.setAttribute("userToEdit", newUser); // Pass back entered data, including attempted role
            newUser.setRole(role); // Keep the attempted role for the form
            showUserForm(request, response, loggedInAdmin);
            return;
        }
        
        // Password Complexity (example: at least 8 chars, 1 uppercase, 1 lowercase, 1 digit)
        if (rawPassword == null || !rawPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\\\d).{8,}$")) {
            request.setAttribute("errorMessage", "密码必须至少8位长，包含大小写字母和数字。");
            request.setAttribute("userToEdit", newUser); // Pass back entered data
            showUserForm(request, response, loggedInAdmin);
            return;
        }
        // newUser.setPassword(rawPassword); // DAO will hash it using SM3 // Original incorrect line
        String hashedPassword = CryptoUtils.generateSM3Hash(rawPassword);
        newUser.setPasswordHash(hashedPassword); // Corrected: set hashed password

        // newUser.setName(request.getParameter("name")); // Original incorrect line
        newUser.setFullName(request.getParameter("name")); // Corrected: use setFullName
        newUser.setPhone(request.getParameter("phone")); // DAO will encrypt it using SM4
        newUser.setRole(role); // Set the validated or admin-set role
        newUser.setDepartmentId(request.getParameter("departmentId"));
        newUser.setPasswordLastChanged(new Date()); // Set on creation
        newUser.setFailedLoginAttempts(0);
        newUser.setLockoutTime(null);

        try {
            if (userDAO.findByUsername(newUser.getUsername()) != null) {
                request.setAttribute("errorMessage", "用户名 '" + newUser.getUsername() + "' 已存在。");
                request.setAttribute("userToEdit", newUser);
                showUserForm(request, response, loggedInAdmin);
                return;
            }

            boolean success = userDAO.createUser(newUser);
            if (success) {
                logAdminAction(loggedInAdmin, "Create User", "Created new admin user: " + newUser.getUsername(), request.getRemoteAddr());
                request.setAttribute("successMessage", "管理员用户 '" + newUser.getUsername() + "' 创建成功。");
                listUsers(request, response, loggedInAdmin);
            } else {
                request.setAttribute("errorMessage", "创建管理员用户失败。");
                request.setAttribute("userToEdit", newUser);
                showUserForm(request, response, loggedInAdmin);
            }
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "创建用户时发生错误: " + e.getMessage());
            request.setAttribute("userToEdit", newUser);
            showUserForm(request, response, loggedInAdmin);
        }
    }

    private void updateUser(HttpServletRequest request, HttpServletResponse response, User loggedInAdmin) throws ServletException, IOException {
        String userId = request.getParameter("userId");
        String newRole = request.getParameter("role");
        try {
            User userToUpdate = userDAO.findById(userId);

            if (userToUpdate == null) {
                request.setAttribute("errorMessage", "尝试更新的用户不存在。");
                listUsers(request, response, loggedInAdmin);
                return;
            }

            // Role-based access check for updating
            if ("SchoolAdmin".equals(loggedInAdmin.getRole())) {
                if (!"DepartmentAdmin".equals(userToUpdate.getRole())) {
                    request.setAttribute("errorMessage", "您只能修改部门管理员用户。");
                    listUsers(request, response, loggedInAdmin);
                    return;
                }
                if (!"DepartmentAdmin".equals(newRole)) {
                    request.setAttribute("errorMessage", "校级管理员不能将部门管理员更改为其他角色。");
                    request.setAttribute("userToEdit", userToUpdate); // Pass back current data
                    showUserForm(request, response, loggedInAdmin);
                    return;
                }
            }

            userToUpdate.setFullName(request.getParameter("name")); 
            userToUpdate.setPhone(request.getParameter("phone")); 
            userToUpdate.setRole(newRole);
            userToUpdate.setDepartmentId(request.getParameter("departmentId"));
            // Note: accountStatus, failedLoginAttempts, lockoutTime are managed by login process or specific admin actions

            boolean success = userDAO.updateUser(userToUpdate);

            String newPassword = request.getParameter("password");
            if (newPassword != null && !newPassword.isEmpty()) {
                if (!newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\\\d).{8,}$")) {
                    request.setAttribute("errorMessage", "新密码必须至少8位长，包含大小写字母和数字。");
                    request.setAttribute("userToEdit", userToUpdate);
                    showUserForm(request, response, loggedInAdmin);
                    return;
                }
                success = success && userDAO.updatePassword(userId, newPassword);
            }

            if (success) {
                logAdminAction(loggedInAdmin, "Update User", "Updated admin user: " + userToUpdate.getUsername(), request.getRemoteAddr());
                request.setAttribute("successMessage", "管理员用户 '" + userToUpdate.getUsername() + "' 更新成功。");
                listUsers(request, response, loggedInAdmin);
            } else {
                request.setAttribute("errorMessage", "更新管理员用户失败。");
                request.setAttribute("userToEdit", userToUpdate);
                showUserForm(request, response, loggedInAdmin);
            }
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "更新用户时发生错误: " + e.getMessage());
            // Attempt to refetch user data for the form if possible
            User userForForm = null; 
            try { userForForm = userDAO.findById(userId); } catch (Exception ignored) {}
            request.setAttribute("userToEdit", userForForm != null ? userForForm : new User()); 
            showUserForm(request, response, loggedInAdmin);
        }
    }

    private void deleteUser(HttpServletRequest request, HttpServletResponse response, User loggedInAdmin) throws ServletException, IOException {
        String userId = request.getParameter("id");
        try {
            User userToDelete = userDAO.findById(userId);

            if (userToDelete == null) {
                request.setAttribute("errorMessage", "尝试删除的用户不存在。");
                listUsers(request, response, loggedInAdmin);
                return;
            }

            // Role-based access check for deleting
            if ("SchoolAdmin".equals(loggedInAdmin.getRole()) && !"DepartmentAdmin".equals(userToDelete.getRole())) {
                request.setAttribute("errorMessage", "您只能删除部门管理员用户。");
                listUsers(request, response, loggedInAdmin);
                return;
            }
            
            if (loggedInAdmin.getUserId().equals(userId)) {
                request.setAttribute("errorMessage", "不能删除当前登录的管理员账户。");
                listUsers(request, response, loggedInAdmin);
                return;
            }

            boolean success = userDAO.deleteUser(userId);
            if (success) {
                logAdminAction(loggedInAdmin, "Delete User", "Deleted admin user: " + userToDelete.getUsername() + " (ID: " + userId + ")", request.getRemoteAddr());
                request.setAttribute("successMessage", "管理员用户 '" + userToDelete.getUsername() + "' 删除成功。");
            } else {
                request.setAttribute("errorMessage", "删除管理员用户失败。");
            }
        } catch (Exception e) {
            request.setAttribute("errorMessage", "删除用户时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
        listUsers(request, response, loggedInAdmin);
    }

    private void logAdminAction(User admin, String actionType, String details, String clientIp) {
        AuditLog log = new AuditLog();
        log.setUserId(admin.getUserId()); // Assumes admin.getUserId() returns String (UUID)
        log.setUsername(admin.getUsername());
        log.setActionType(actionType);
        log.setActionDetails(details);
        log.setActionTime(new Timestamp(new Date().getTime()));
        log.setClientIp(clientIp);
        try {
            // auditLogDAO.addLog(log); // Assuming addLog is the method in AuditLogDAO // Original incorrect line
            auditLogDAO.createLog(log); // Corrected: use createLog
        } catch (Exception e) {
            e.printStackTrace(); // Log failure to add audit log
        }
    }
}
