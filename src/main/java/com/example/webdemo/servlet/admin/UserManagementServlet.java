package com.example.webdemo.servlet.admin;

import com.example.webdemo.beans.User;
import com.example.webdemo.beans.Department;
import com.example.webdemo.dao.UserDAO;
import com.example.webdemo.dao.DepartmentDAO;
import com.example.webdemo.dao.AuditLogDAO;
import com.example.webdemo.beans.AuditLog;
import com.example.webdemo.util.CryptoUtils;
import com.example.webdemo.util.DBUtils;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@WebServlet("/admin/userManagement")
public class UserManagementServlet extends HttpServlet {
    private UserDAO userDAO;
    private DepartmentDAO departmentDAO;
    private AuditLogDAO auditLogDAO;

    @Override
    public void init() throws ServletException {
        userDAO = new UserDAO(DBUtils.getDataSource()); 
        departmentDAO = new DepartmentDAO();
        auditLogDAO = new AuditLogDAO();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User loggedInAdmin = (session != null) ? (User) session.getAttribute("adminUser") : null;

        // Role check based on updated User bean's getRole() which returns String
        if (loggedInAdmin == null || !("SCHOOL_ADMIN".equals(loggedInAdmin.getRole()) || "SYSTEM_ADMIN".equals(loggedInAdmin.getRole()))) {
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

        if (loggedInAdmin == null || !("SCHOOL_ADMIN".equals(loggedInAdmin.getRole()) || "SYSTEM_ADMIN".equals(loggedInAdmin.getRole()))) {
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
            if ("SCHOOL_ADMIN".equals(loggedInAdmin.getRole())) {
                // SchoolAdmin can only manage DepartmentAdmin users
                userList = userDAO.getUsersByRole("DEPARTMENT_ADMIN");
            } else if ("SYSTEM_ADMIN".equals(loggedInAdmin.getRole())) {
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
                // 修复类型转换: String转为int
                userToEdit = userDAO.findById(Integer.parseInt(userIdStr));
                if (userToEdit == null) {
                    request.setAttribute("errorMessage", "未找到指定ID的管理员用户。");
                    listUsers(request, response, loggedInAdmin);
                    return;
                }
                // Role-based access check for editing
                if ("SCHOOL_ADMIN".equals(loggedInAdmin.getRole()) && !"DEPARTMENT_ADMIN".equals(userToEdit.getRole())) {
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
        // 移除设置userId，数据库会自动生成
        newUser.setUsername(request.getParameter("username"));
        String rawPassword = request.getParameter("password");
        String role = request.getParameter("role");

        // Role restriction for SchoolAdmin
        if ("SCHOOL_ADMIN".equals(loggedInAdmin.getRole()) && !"DEPARTMENT_ADMIN".equals(role)) {
            request.setAttribute("errorMessage", "校级管理员只能创建部门管理员账户。");
            request.setAttribute("userToEdit", newUser); // Pass back entered data, including attempted role
            newUser.setRole(role); // Keep the attempted role for the form
            showUserForm(request, response, loggedInAdmin);
            return;
        }
        
        // Password Complexity (example: at least 8 chars, 1 uppercase, 1 lowercase, 1 digit)
        if (rawPassword == null || !rawPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")) {
            request.setAttribute("errorMessage", "密码必须至少8位长，包含大小写字母和数字。");
            request.setAttribute("userToEdit", newUser); // Pass back entered data
            showUserForm(request, response, loggedInAdmin);
            return;
        }
        // 生成密码哈希
        String hashedPassword;
        try {
            hashedPassword = CryptoUtils.generateSM3Hash(rawPassword);
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "关键安全组件初始化失败，无法创建用户。请联系系统管理员。");
            request.setAttribute("userToEdit", newUser);
            showUserForm(request, response, loggedInAdmin);
            return;
        }
        newUser.setPasswordHash(hashedPassword);

        // 设置用户信息
        newUser.setFullName(request.getParameter("fullName")); // 使用fullName字段
        newUser.setPhoneNumber(request.getParameter("phoneNumber")); // 使用phoneNumber字段
        newUser.setRole(role); // 设置已验证的角色
        // 处理部门ID
        String deptIdStr = request.getParameter("departmentId");
        if (deptIdStr != null && !deptIdStr.isEmpty()) {
            newUser.setDepartmentId(Integer.parseInt(deptIdStr));
        }
        // 修复类型转换: Date转为Timestamp
        newUser.setPasswordLastChanged(new Timestamp(System.currentTimeMillis()));
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
        String userIdStr = request.getParameter("userId");
        String newRole = request.getParameter("role");
        try {
            // 修复类型转换: String转为int
            User userToUpdate = userDAO.findById(Integer.parseInt(userIdStr));

            if (userToUpdate == null) {
                request.setAttribute("errorMessage", "尝试更新的用户不存在。");
                listUsers(request, response, loggedInAdmin);
                return;
            }

            // Role-based access check for updating
            if ("SCHOOL_ADMIN".equals(loggedInAdmin.getRole())) {
                if (!"DEPARTMENT_ADMIN".equals(userToUpdate.getRole())) {
                    request.setAttribute("errorMessage", "您只能修改部门管理员用户。");
                    listUsers(request, response, loggedInAdmin);
                    return;
                }
                if (!"DEPARTMENT_ADMIN".equals(newRole)) {
                    request.setAttribute("errorMessage", "校级管理员不能将部门管理员更改为其他角色。");
                    request.setAttribute("userToEdit", userToUpdate); // Pass back current data
                    showUserForm(request, response, loggedInAdmin);
                    return;
                }
            }

            // 更新用户信息
            userToUpdate.setFullName(request.getParameter("fullName")); 
            userToUpdate.setPhoneNumber(request.getParameter("phoneNumber")); 
            userToUpdate.setRole(newRole);
            // 处理部门ID
            String deptIdStr = request.getParameter("departmentId");
            if (deptIdStr != null && !deptIdStr.isEmpty()) {
                userToUpdate.setDepartmentId(Integer.parseInt(deptIdStr));
            }

            // 处理锁定状态更新
            String lockoutStatus = request.getParameter("lockoutStatus");
            boolean shouldLock = "locked".equals(lockoutStatus);
            
            // 处理重置登录失败次数
            String resetFailedAttempts = request.getParameter("resetFailedAttempts");
            boolean shouldResetAttempts = "true".equals(resetFailedAttempts);
            
            boolean success = userDAO.updateUser(userToUpdate);
            
            // 更新锁定状态
            if (shouldLock) {
                // 锁定账户 30 分钟
                success = success && userDAO.updateLockStatus(userToUpdate.getUserId(), true, 30);
            } else {
                // 解锁账户
                success = success && userDAO.updateLockStatus(userToUpdate.getUserId(), false, null);
            }
            
            // 重置登录失败次数
            if (shouldResetAttempts) {
                success = success && userDAO.resetFailedLoginAttempts(userToUpdate.getUserId());
            }

            // 处理密码更新，如果提供了新密码
            String newPassword = request.getParameter("password");
            if (newPassword != null && !newPassword.isEmpty()) {
                // 密码复杂度检查
                if (!newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")) {
                    request.setAttribute("errorMessage", "新密码必须至少8位长，包含大小写字母和数字。");
                    request.setAttribute("userToEdit", userToUpdate);
                    showUserForm(request, response, loggedInAdmin);
                    return;
                }
                
                // 处理密码更新
                try {
                    String hashedPassword = CryptoUtils.generateSM3Hash(newPassword);
                    userToUpdate.setPasswordHash(hashedPassword);
                    userToUpdate.setPasswordLastChanged(new Timestamp(System.currentTimeMillis()));
                    success = success && userDAO.updateUserPassword(userToUpdate);
                } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
                    e.printStackTrace();
                    request.setAttribute("errorMessage", "密码加密失败，无法更新用户密码。");
                    request.setAttribute("userToEdit", userToUpdate);
                    showUserForm(request, response, loggedInAdmin);
                    return;
                }
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
            try { userForForm = userDAO.findById(Integer.parseInt(userIdStr)); } catch (Exception ignored) {}
            request.setAttribute("userToEdit", userForForm != null ? userForForm : new User()); 
            showUserForm(request, response, loggedInAdmin);
        }
    }

    private void deleteUser(HttpServletRequest request, HttpServletResponse response, User loggedInAdmin) throws ServletException, IOException {
        String userId = request.getParameter("id");
        try {
            // 修复类型转换: String转为int
            User userToDelete = userDAO.findById(Integer.parseInt(userId));

            if (userToDelete == null) {
                request.setAttribute("errorMessage", "尝试删除的用户不存在。");
                listUsers(request, response, loggedInAdmin);
                return;
            }

            // Role-based access check for deleting
            if ("SCHOOL_ADMIN".equals(loggedInAdmin.getRole()) && !"DEPARTMENT_ADMIN".equals(userToDelete.getRole())) {
                request.setAttribute("errorMessage", "您只能删除部门管理员用户。");
                listUsers(request, response, loggedInAdmin);
                return;
            }
            
            // 修复类型转换: 比较整数ID
            if (loggedInAdmin.getUserId() == Integer.parseInt(userId)) {
                request.setAttribute("errorMessage", "不能删除当前登录的管理员账户。");
                listUsers(request, response, loggedInAdmin);
                return;
            }

            // 修复类型转换: String转为int
            boolean success = userDAO.deleteUser(Integer.parseInt(userId));
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
        log.setUserId(admin.getUserId()); // 现在是int类型
        log.setUsername(admin.getUsername());
        log.setActionType(actionType);
        // 修复方法名: setActionDetails -> setDetails
        log.setDetails(details);
        // 修复方法名: setActionTime -> setLogTimestamp
        log.setLogTimestamp(new Timestamp(new Date().getTime()));
        // 修复方法名: setClientIp -> setIpAddress
        log.setIpAddress(clientIp);
        try {
            auditLogDAO.createLog(log);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
