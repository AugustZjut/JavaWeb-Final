<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.example.webdemo.beans.User" %>
<%@ page import="com.example.webdemo.dao.UserDAO" %>
<%@ page import="com.example.webdemo.dao.DepartmentDAO" %>
<%@ page import="com.example.webdemo.beans.Department" %>
<%@ page import="java.util.List" %>

<%-- Auth Check --%>
<% 
    User adminUser = (User) session.getAttribute("adminUser");
    if (adminUser == null || (!"SCHOOL_ADMIN".equals(adminUser.getRole()) && !"SYSTEM_ADMIN".equals(adminUser.getRole()))) {
        response.sendRedirect(request.getContextPath() + "/admin/adminLogin.jsp");
        return;
    }

    // 使用从请求属性中获取的数据，而不是直接创建DAO
    List<Department> departmentList = (List<Department>) request.getAttribute("departmentList");
    User userToEdit = (User) request.getAttribute("userToEdit");
    boolean isMyAccount = Boolean.TRUE.equals(request.getAttribute("isMyAccount"));
    
    // 检查是否是强制修改密码模式
    // 只有在明确指定forcePasswordChange参数且是"我的账户"页面时才启用强制修改密码模式
    boolean forcePasswordChange = isMyAccount && 
                                  ("true".equals(request.getParameter("forcePasswordChange")) || 
                                   Boolean.TRUE.equals(session.getAttribute("forcePasswordChange")));

    // 表单行为由Servlet处理并设置属性
    String formAction;
    String pageTitle;
    if (forcePasswordChange) {
        formAction = "updateMyAccount";
        pageTitle = "强制修改密码";
    } else if (isMyAccount) {
        formAction = "updateMyAccount";
        pageTitle = "我的账户";
    } else {
        formAction = (userToEdit != null) ? "update" : "create";
        pageTitle = (userToEdit != null) ? "编辑管理员 - " + userToEdit.getUsername() : "添加新管理员";
    }
%>

<html>
<head>
    <title><%= pageTitle %></title>
    <style>
        body { font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4; }
        .header {
            background-color: #333;
            color: white;
            padding: 15px 20px;
            text-align: right;
        }
        .header a { color: white; text-decoration: none; margin-left: 15px; }
        .header span { float: left; }
        .container { width: 50%; margin: 20px auto; background-color: #fff; padding: 20px; box-shadow: 0 0 10px rgba(0,0,0,0.1); border-radius: 8px; }
        h1 { color: #333; text-align: center; }
        label { display: block; margin-top: 10px; margin-bottom: 5px; color: #555; }
        input[type="text"], input[type="password"], select {
            width: 100%;
            padding: 10px;
            margin-bottom: 15px;
            border: 1px solid #ddd;
            border-radius: 4px;
            box-sizing: border-box;
        }
        input[type="submit"], .cancel-button {
            padding: 10px 15px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-size: 16px;
            text-decoration: none;
        }
        input[type="submit"] { background-color: #007bff; color: white; margin-right: 10px; }
        input[type="submit"]:hover { background-color: #0056b3; }
        .cancel-button { background-color: #6c757d; color: white; display: inline-block; }
        .cancel-button:hover { background-color: #5a6268; }
        .form-actions { margin-top: 20px; text-align: right; }
        .error-message { color: red; margin-bottom: 10px; }
        .warning-message { 
            background-color: #fff3cd; 
            color: #856404; 
            border: 1px solid #ffeaa7; 
            padding: 15px; 
            border-radius: 5px; 
            margin-bottom: 20px; 
        }
        .warning-message p { 
            margin: 0; 
        }
    </style>
</head>
<body>
    <div class="header">
        <span>管理员: <%= adminUser.getFullName() %></span>
        <a href="<%= request.getContextPath() %>/admin/dashboard.jsp">控制台</a>
        <a href="<%= request.getContextPath() %>/admin/logout">退出登录</a>
    </div>

    <div class="container">
        <h1><%= pageTitle %></h1>

        <% if (forcePasswordChange) { %>
            <div class="warning-message">
                <p><strong>注意：</strong>为了您的账户安全，您需要修改密码后才能继续使用系统。</p>
            </div>
        <% } %>

        <% 
            String errorMessage = (String) request.getAttribute("errorMessage");
            if (errorMessage != null) {
        %>
            <p class="error-message"><%= errorMessage %></p>
        <% 
            }
        %>

        <form action="<%= request.getContextPath() %>/admin/userManagement" method="post">
            <input type="hidden" name="action" value="<%= formAction %>">
            <% if (userToEdit != null) { %>
                <input type="hidden" name="userId" value="<%= userToEdit.getUserId() %>">
            <% } %>
            <% if (forcePasswordChange) { %>
                <input type="hidden" name="forcePasswordChange" value="true">
            <% } %>

            <% if (forcePasswordChange) { %>
                <!-- 强制修改密码模式：只显示基本信息和密码字段 -->
                <div>
                    <label>用户名:</label>
                    <input type="text" value="<%= adminUser.getUsername() %>" readonly>
                </div>

                <div>
                    <label>姓名:</label>
                    <input type="text" value="<%= adminUser.getFullName() %>" readonly>
                </div>

                <div>
                    <label for="password">新密码 <span style="color: red;">*</span>:</label>
                    <input type="password" id="password" name="password" required>
                    <small style="color: #666;">密码至少8位，包含字母和数字</small>
                </div>

                <div>
                    <label for="confirmPassword">确认新密码 <span style="color: red;">*</span>:</label>
                    <input type="password" id="confirmPassword" name="confirmPassword" required>
                </div>

            <% } else { %>
                <!-- 正常模式：显示所有字段 -->
                <div>
                    <label for="username">用户名:</label>
                    <input type="text" id="username" name="username" value="<%= userToEdit != null ? userToEdit.getUsername() : "" %>" <%= userToEdit != null ? "readonly" : "required" %>>
                </div>

                <div>
                    <label for="fullName">姓名:</label>
                    <input type="text" id="fullName" name="fullName" value="<%= userToEdit != null ? userToEdit.getFullName() : "" %>" required>
                </div>

                <% if (formAction.equals("create")) { %>
                <div>
                    <label for="password">密码:</label>
                    <input type="password" id="password" name="password" required>
                </div>
                <% } else { %>
                <div>
                    <label for="password">新密码 (留空则不修改):</label>
                    <input type="password" id="password" name="password">
                </div>
                <% } %>

                <div>
                    <label for="phoneNumber">电话号码:</label>
                    <input type="text" id="phoneNumber" name="phoneNumber" value="<%= userToEdit != null ? userToEdit.getPhoneNumber() : "" %>" required>
                </div>
            <% } %>

            <% if (!forcePasswordChange) { %>
                <!-- 正常模式下显示部门和角色字段 -->
                <div>
                    <label for="departmentId">所属部门:</label>
                    <% if (isMyAccount) { %>
                        <input type="text" value="<%= userToEdit != null && userToEdit.getDepartmentId() != null && userToEdit.getDepartmentId() > 0 ? 
                            (departmentList.stream().filter(d -> d.getDepartmentId() == userToEdit.getDepartmentId()).findFirst().map(d -> d.getDepartmentName()).orElse("未知部门")) : "无部门" %>" readonly>
                        <input type="hidden" name="departmentId" value="<%= userToEdit != null && userToEdit.getDepartmentId() != null ? userToEdit.getDepartmentId() : 0 %>">
                    <% } else { %>
                    <select id="departmentId" name="departmentId">
                        <option value="0">-- 无部门 --</option> <%-- For System Admin or unassigned --%>
                        <% for (Department dept : departmentList) { %>
                            <option value="<%= dept.getDepartmentId() %>"
                                <%= (userToEdit != null && userToEdit.getDepartmentId() != null && userToEdit.getDepartmentId() == dept.getDepartmentId()) ? "selected" : "" %>>
                                <%= dept.getDepartmentName() %>
                            </option>
                        <% } %>
                    </select>
                    <% } %>
                </div>

                <div>
                    <label for="role">角色:</label>
                    <% if (isMyAccount) { %>
                        <input type="text" value="<%= 
                            userToEdit != null ? 
                            ("SYSTEM_ADMIN".equals(userToEdit.getRole()) ? "系统管理员" :
                             "SCHOOL_ADMIN".equals(userToEdit.getRole()) ? "校级管理员" :
                             "AUDIT_ADMIN".equals(userToEdit.getRole()) ? "审计管理员" :
                             "DEPARTMENT_ADMIN".equals(userToEdit.getRole()) ? "部门管理员" : userToEdit.getRole()) : "" %>" readonly>
                        <input type="hidden" name="role" value="<%= userToEdit != null ? userToEdit.getRole() : "" %>">
                    <% } else { %>
                    <select id="role" name="role" required>
                        <option value="DEPARTMENT_ADMIN" <%= (userToEdit != null && "DEPARTMENT_ADMIN".equals(userToEdit.getRole())) ? "selected" : "" %>>部门管理员</option>
                        <% if ("SYSTEM_ADMIN".equals(adminUser.getRole())) { %>
                        <option value="SCHOOL_ADMIN" <%= (userToEdit != null && "SCHOOL_ADMIN".equals(userToEdit.getRole())) ? "selected" : "" %>>校级管理员</option>
                        <option value="AUDIT_ADMIN" <%= (userToEdit != null && "AUDIT_ADMIN".equals(userToEdit.getRole())) ? "selected" : "" %>>审计管理员</option>
                        <option value="SYSTEM_ADMIN" <%= (userToEdit != null && "SYSTEM_ADMIN".equals(userToEdit.getRole())) ? "selected" : "" %>>系统管理员</option>
                        <% } %>
                    </select>
                    <% } %>
                </div>
            <% } %>

            <% if (!isMyAccount && !forcePasswordChange) { %>
            <div id="publicAppointmentPermissionField">
                <label for="canManagePublicAppointments">公众预约管理权限:</label>
                <select id="canManagePublicAppointments" name="canManagePublicAppointments" required>
                    <option value="false" <%= (userToEdit == null || !userToEdit.isCanManagePublicAppointments()) ? "selected" : "" %>>无权限</option>
                    <option value="true" <%= (userToEdit != null && userToEdit.isCanManagePublicAppointments()) ? "selected" : "" %>>有权限</option>
                </select>
                <div id="permissionHint" style="font-size: 0.9em; color: #666; margin-top: 5px;">
                    授权后该用户可以管理社会公众的通行预约申请，包括查询、统计、详情查看等功能。
                </div>
            </div>
            <% } %>

            <% if (userToEdit != null && !isMyAccount && !forcePasswordChange) { %>
            <div>
                <label for="lockoutTime">锁定状态:</label>
                <select id="lockoutStatus" name="lockoutStatus" required>
                    <option value="active" <%= userToEdit.getLockoutTime() == null || userToEdit.getLockoutTime().getTime() < System.currentTimeMillis() ? "selected" : "" %>>激活</option>
                    <option value="locked" <%= userToEdit.getLockoutTime() != null && userToEdit.getLockoutTime().getTime() > System.currentTimeMillis() ? "selected" : "" %>>锁定</option>
                </select>
            </div>
            <div>
                <label for="resetFailedAttempts">重置登录失败次数:</label>
                <input type="checkbox" id="resetFailedAttempts" name="resetFailedAttempts" value="true">
                <span style="font-size: 0.9em; color: #666;">（当前失败次数：<%= userToEdit.getFailedLoginAttempts() %>）</span>
            </div>
            <% } %>

            <div class="form-actions">
                <input type="submit" value="<%= 
                    forcePasswordChange ? "修改密码" :
                    (isMyAccount ? "保存修改" : 
                    (formAction.equals("create") ? "创建用户" : "更新用户")) %>">
                <% if (!forcePasswordChange) { %>
                <a href="<%= isMyAccount ? request.getContextPath() + "/admin/dashboard.jsp" : request.getContextPath() + "/admin/userManagement" %>" class="cancel-button">
                    <%= isMyAccount ? "返回控制台" : "取消" %>
                </a>
                <% } %>
            </div>
        </form>
    </div>

    <% if (forcePasswordChange) { %>
    <script>
        // 强制修改密码模式下的表单验证
        document.querySelector('form').addEventListener('submit', function(e) {
            var password = document.getElementById('password').value;
            var confirmPassword = document.getElementById('confirmPassword').value;
            
            if (password !== confirmPassword) {
                e.preventDefault();
                alert('两次输入的密码不一致，请重新输入。');
                return false;
            }
            
            if (password.length < 8) {
                e.preventDefault();
                alert('密码长度至少为8位。');
                return false;
            }
            
            if (!/(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/.test(password)) {
                e.preventDefault();
                alert('密码必须包含大小写字母和数字。');
                return false;
            }
            
            return true;
        });
        
        // 角色变化处理逻辑
        document.getElementById('role').addEventListener('change', function() {
            const role = this.value;
            const permissionField = document.getElementById('publicAppointmentPermissionField');
            const permissionSelect = document.getElementById('canManagePublicAppointments');
            const permissionHint = document.getElementById('permissionHint');
            
            if (role === 'SYSTEM_ADMIN' || role === 'SCHOOL_ADMIN') {
                // 系统管理员和学校管理员自动拥有权限，不可修改
                permissionSelect.value = 'true';
                permissionSelect.disabled = true;
                permissionHint.innerHTML = '<span style="color: #28a745; font-weight: bold;">✓ 该角色自动拥有公众预约管理权限</span>';
            } else if (role === 'DEPARTMENT_ADMIN') {
                // 部门管理员可以手动设置权限
                permissionSelect.disabled = false;
                permissionHint.innerHTML = '授权后该用户可以管理社会公众的通行预约申请，包括查询、统计、详情查看等功能。';
            } else {
                // 审计管理员或其他角色不允许有此权限
                permissionSelect.value = 'false';
                permissionSelect.disabled = true;
                permissionHint.innerHTML = '<span style="color: #dc3545; font-weight: bold;">✗ 该角色不允许拥有公众预约管理权限</span>';
            }
        });
        
        // 页面加载时触发一次角色变化处理
        document.getElementById('role').dispatchEvent(new Event('change'));
    </script>
    <% } %>
</body>
</html>
