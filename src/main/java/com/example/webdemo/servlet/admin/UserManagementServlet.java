package com.example.webdemo.servlet.admin;

import com.example.webdemo.beans.User;
import com.example.webdemo.beans.Department;
import com.example.webdemo.dao.UserDAO;
import com.example.webdemo.dao.DepartmentDAO;
import com.example.webdemo.dao.AuditLogDAO;
import com.example.webdemo.beans.AuditLog;
import com.example.webdemo.util.CryptoUtils;
import com.example.webdemo.util.DataMaskingUtils;
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
                case "search":
                    searchUsers(request, response, loggedInAdmin);
                    break;
                case "myAccount":
                    showMyAccount(request, response, loggedInAdmin);
                    break;
                case "list":
                default:
                    listUsers(request, response, loggedInAdmin);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace(); // Log error
            request.setAttribute("errorMessage", "处理请求时发生错误: " + e.getMessage());
            // 根据操作类型决定错误处理方式
            if ("myAccount".equals(action)) {
                // 如果是我的账户操作失败，显示账户页面而不是用户列表
                request.setAttribute("userToEdit", loggedInAdmin);
                request.setAttribute("isMyAccount", true);
                try {
                    List<Department> departmentList = departmentDAO.getAllDepartments();
                    request.setAttribute("departmentList", departmentList);
                } catch (Exception ignored) {}
                request.getRequestDispatcher("/admin/userForm.jsp").forward(request, response);
            } else {
                listUsers(request, response, loggedInAdmin);
            }
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
                case "updateMyAccount":
                    updateMyAccount(request, response, loggedInAdmin);
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
            
            // 排除当前登录的管理员账号
            userList.removeIf(user -> user.getUserId() == loggedInAdmin.getUserId());
            
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
        // 确保在编辑其他用户时清除可能残留的强制修改密码标志
        request.getSession().removeAttribute("forcePasswordChange");
        
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
        // 处理公众预约管理权限
        String canManagePublicAppointments = request.getParameter("canManagePublicAppointments");
        
        // 系统管理员和学校管理员自动拥有公众预约管理权限
        // 部门管理员根据表单参数设置，审计管理员不允许有此权限
        if ("SYSTEM_ADMIN".equals(role) || "SCHOOL_ADMIN".equals(role)) {
            newUser.setCanManagePublicAppointments(true);
        } else if ("DEPARTMENT_ADMIN".equals(role)) {
            newUser.setCanManagePublicAppointments("true".equals(canManagePublicAppointments));
        } else {
            // 审计管理员或其他角色不允许有此权限
            newUser.setCanManagePublicAppointments(false);
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
            // 权限处理：公众+公务
            if ("SYSTEM_ADMIN".equals(newRole) || "SCHOOL_ADMIN".equals(newRole)) {
                userToUpdate.setCanManagePublicAppointments(true);
                userToUpdate.setCanManageOfficialAppointments(true);
            } else if ("DEPARTMENT_ADMIN".equals(newRole)) {
                boolean canPublic = "true".equals(request.getParameter("canManagePublicAppointments"));
                boolean canOfficial = "true".equals(request.getParameter("canManageOfficialAppointments"));
                userToUpdate.setCanManagePublicAppointments(canPublic);
                userToUpdate.setCanManageOfficialAppointments(canOfficial);
            } else {
                userToUpdate.setCanManagePublicAppointments(false);
                userToUpdate.setCanManageOfficialAppointments(false);
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

    private void searchUsers(HttpServletRequest request, HttpServletResponse response, User loggedInAdmin) throws ServletException, IOException {
        try {
            // 获取查询参数
            String username = request.getParameter("username");
            String fullName = request.getParameter("fullName");
            String nameSearchType = request.getParameter("nameSearchType"); // "exact" or "fuzzy"
            boolean exactNameMatch = "exact".equals(nameSearchType);
            
            String departmentIdStr = request.getParameter("departmentId");
            Integer departmentId = null;
            if (departmentIdStr != null && !departmentIdStr.trim().isEmpty() && !"ALL".equals(departmentIdStr)) {
                try {
                    departmentId = Integer.parseInt(departmentIdStr);
                } catch (NumberFormatException e) {
                    // 忽略无效的部门ID
                }
            }
            
            String role = request.getParameter("role");
            String accountStatus = request.getParameter("accountStatus");
            String passwordStatus = request.getParameter("passwordStatus");

            // 根据登录管理员的角色限制查询范围
            List<User> userList;
            if ("SCHOOL_ADMIN".equals(loggedInAdmin.getRole())) {
                // 学校管理员只能查看部门管理员
                if (role == null || role.trim().isEmpty() || "ALL".equals(role)) {
                    role = "DEPARTMENT_ADMIN"; // 强制设置为部门管理员
                } else if (!"DEPARTMENT_ADMIN".equals(role)) {
                    // 如果指定了其他角色，返回空结果
                    userList = new ArrayList<>();
                    request.setAttribute("userList", userList);
                    request.setAttribute("errorMessage", "您只能查看部门管理员用户。");
                    loadFormData(request);
                    request.getRequestDispatcher("/admin/userList.jsp").forward(request, response);
                    return;
                }
            }

            userList = userDAO.searchUsers(username, fullName, exactNameMatch, departmentId, role, accountStatus, passwordStatus);
            
            // 排除当前登录的管理员账号
            userList.removeIf(user -> user.getUserId() == loggedInAdmin.getUserId());
            
            // 设置查询参数回显
            request.setAttribute("searchUsername", username);
            request.setAttribute("searchFullName", fullName);
            request.setAttribute("searchNameType", nameSearchType);
            request.setAttribute("searchDepartmentId", departmentIdStr);
            request.setAttribute("searchRole", role);
            request.setAttribute("searchAccountStatus", accountStatus);
            request.setAttribute("searchPasswordStatus", passwordStatus);
            
            request.setAttribute("userList", userList);
            loadFormData(request);
            request.setAttribute("isSearchResult", true);
            request.getRequestDispatcher("/admin/userList.jsp").forward(request, response);
            
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "查询用户失败: " + e.getMessage());
            listUsers(request, response, loggedInAdmin);
        }
    }

    private void loadFormData(HttpServletRequest request) throws Exception {
        List<Department> departmentList = departmentDAO.getAllDepartments();
        request.setAttribute("departmentList", departmentList);
    }

    private void showMyAccount(HttpServletRequest request, HttpServletResponse response, User loggedInAdmin) throws ServletException, IOException {
        // 获取当前登录用户的最新信息
        try {
            User currentUser = userDAO.findById(loggedInAdmin.getUserId());
            if (currentUser == null) {
                request.setAttribute("errorMessage", "无法获取当前用户信息。");
                // 使用当前登录的用户信息作为备用
                currentUser = loggedInAdmin;
            }
            
            // 如果用户已经不需要强制修改密码，清除session中的标志
            if (!currentUser.isPasswordChangeRequired()) {
                request.getSession().removeAttribute("forcePasswordChange");
            }
            
            request.setAttribute("userToEdit", currentUser);
            request.setAttribute("isMyAccount", true); // 标识这是"我的账户"页面
            
            List<Department> departmentList = departmentDAO.getAllDepartments();
            request.setAttribute("departmentList", departmentList);
            
            request.getRequestDispatcher("/admin/userForm.jsp").forward(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "获取用户信息失败: " + e.getMessage());
            // 使用当前登录的用户信息作为备用
            request.setAttribute("userToEdit", loggedInAdmin);
            request.setAttribute("isMyAccount", true);
            try {
                List<Department> departmentList = departmentDAO.getAllDepartments();
                request.setAttribute("departmentList", departmentList);
            } catch (Exception ignored) {
                // 如果连部门列表都获取不到，至少显示用户表单
            }
            request.getRequestDispatcher("/admin/userForm.jsp").forward(request, response);
        }
    }

    private void updateMyAccount(HttpServletRequest request, HttpServletResponse response, User loggedInAdmin) throws ServletException, IOException {
        String userIdStr = request.getParameter("userId");
        boolean forcePasswordChange = "true".equals(request.getParameter("forcePasswordChange")) || 
                                      Boolean.TRUE.equals(request.getSession().getAttribute("forcePasswordChange"));
        
        try {
            // 确保只能修改自己的账户
            if (!userIdStr.equals(String.valueOf(loggedInAdmin.getUserId()))) {
                request.setAttribute("errorMessage", "您只能修改自己的账户信息。");
                showMyAccount(request, response, loggedInAdmin);
                return;
            }
            
            User userToUpdate = userDAO.findById(Integer.parseInt(userIdStr));
            if (userToUpdate == null) {
                request.setAttribute("errorMessage", "用户不存在。");
                showMyAccount(request, response, loggedInAdmin);
                return;
            }

            // 在强制修改密码模式下，只更新密码；否则更新基本信息
            if (!forcePasswordChange) {
                userToUpdate.setFullName(request.getParameter("fullName")); 
                userToUpdate.setPhoneNumber(request.getParameter("phoneNumber")); 
                // 注意：在"我的账户"中不允许修改角色和部门
            }
            
            boolean success = true;
            if (!forcePasswordChange) {
                success = userDAO.updateUser(userToUpdate);
            }

            // 处理密码更新
            String newPassword = request.getParameter("password");
            if (newPassword != null && !newPassword.isEmpty()) {
                // 在强制修改密码模式下，密码是必须的
                if (forcePasswordChange && newPassword.trim().isEmpty()) {
                    request.setAttribute("errorMessage", "在强制修改密码模式下，必须提供新密码。");
                    request.setAttribute("userToEdit", userToUpdate);
                    request.setAttribute("isMyAccount", true);
                    showUserForm(request, response, loggedInAdmin);
                    return;
                }
                
                // 密码复杂度检查
                if (!newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")) {
                    request.setAttribute("errorMessage", "新密码必须至少8位长，包含大小写字母和数字。");
                    request.setAttribute("userToEdit", userToUpdate);
                    request.setAttribute("isMyAccount", true);
                    showUserForm(request, response, loggedInAdmin);
                    return;
                }
                
                // 处理密码更新
                try {
                    String hashedPassword = CryptoUtils.generateSM3Hash(newPassword);
                    userToUpdate.setPasswordHash(hashedPassword);
                    userToUpdate.setPasswordLastChanged(new Timestamp(System.currentTimeMillis()));
                    // 清除密码修改要求标志
                    userToUpdate.setPasswordChangeRequired(false);
                    success = success && userDAO.updateUserPassword(userToUpdate);
                } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
                    e.printStackTrace();
                    request.setAttribute("errorMessage", "密码加密失败，无法更新密码。");
                    request.setAttribute("userToEdit", userToUpdate);
                    request.setAttribute("isMyAccount", true);
                    showUserForm(request, response, loggedInAdmin);
                    return;
                }
            } else if (forcePasswordChange) {
                // 强制修改密码模式下，必须提供密码
                request.setAttribute("errorMessage", "必须设置新密码。");
                request.setAttribute("userToEdit", userToUpdate);
                request.setAttribute("isMyAccount", true);
                showUserForm(request, response, loggedInAdmin);
                return;
            }

            if (success) {
                // 更新session中的用户信息
                User updatedUser = userDAO.findById(loggedInAdmin.getUserId());
                if (updatedUser != null) {
                    request.getSession().setAttribute("adminUser", updatedUser);
                    // 清除强制修改密码标志
                    request.getSession().removeAttribute("forcePasswordChange");
                }
                
                logAdminAction(loggedInAdmin, "Update My Account", 
                    forcePasswordChange ? "Completed forced password change" : "Updated own account information", 
                    request.getRemoteAddr());
                
                if (forcePasswordChange) {
                    request.setAttribute("successMessage", "密码修改成功！您现在可以正常使用系统了。");
                    // 重定向到控制台
                    response.sendRedirect(request.getContextPath() + "/admin/dashboard.jsp");
                } else {
                    request.setAttribute("successMessage", "账户信息更新成功。");
                    showMyAccount(request, response, updatedUser != null ? updatedUser : loggedInAdmin);
                }
            } else {
                request.setAttribute("errorMessage", "更新账户信息失败。");
                request.setAttribute("userToEdit", userToUpdate);
                request.setAttribute("isMyAccount", true);
                showUserForm(request, response, loggedInAdmin);
            }
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "更新账户信息时发生错误: " + e.getMessage());
            showMyAccount(request, response, loggedInAdmin);
        }
    }
}
