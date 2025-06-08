<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.example.webdemo.beans.User" %>
<%@ page import="com.example.webdemo.dao.UserDAO" %>
<%@ page import="com.example.webdemo.dao.DepartmentDAO" %>
<%@ page import="com.example.webdemo.beans.Department" %>
<%@ page import="java.util.List" %>

<%-- Auth Check --%>
<% 
    User loggedInAdmin = (User) session.getAttribute("adminUser");
    if (loggedInAdmin == null || (!"School Admin".equals(loggedInAdmin.getRole()) && !"System Admin".equals(loggedInAdmin.getRole()))) {
        response.sendRedirect(request.getContextPath() + "/admin/login");
        return;
    }

    UserDAO userDAO = new UserDAO();
    DepartmentDAO departmentDAO = new DepartmentDAO();
    List<Department> departmentList = departmentDAO.getAllDepartments();

    User userToEdit = null;
    String formAction = "create";
    String pageTitle = "添加新管理员";

    String userIdStr = request.getParameter("id");
    if (userIdStr != null && !userIdStr.isEmpty()) {
        try {
            int userId = Integer.parseInt(userIdStr);
            userToEdit = userDAO.findById(userId);
            if (userToEdit != null) {
                formAction = "update";
                pageTitle = "编辑管理员 - " + userToEdit.getUsername();
            } else {
                request.setAttribute("errorMessage", "未找到指定ID的管理员用户。");
                // Forward to list page or show error on this page
            }
        } catch (NumberFormatException e) {
            request.setAttribute("errorMessage", "无效的用户ID格式。");
        }
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
    </style>
</head>
<body>
    <div class="header">
        <span>管理员: <%= loggedInAdmin.getFullName() %></span>
        <a href="<%= request.getContextPath() %>/admin/dashboard.jsp">控制台</a>
        <a href="<%= request.getContextPath() %>/admin/logout">退出登录</a>
    </div>

    <div class="container">
        <h1><%= pageTitle %></h1>

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

            <div>
                <label for="departmentId">所属部门:</label>
                <select id="departmentId" name="departmentId">
                    <option value="0">-- 无部门 --</option> <%-- For System Admin or unassigned --%>
                    <% for (Department dept : departmentList) { %>
                        <option value="<%= dept.getDepartmentId() %>"
                            <%= (userToEdit != null && userToEdit.getDepartmentId() == dept.getDepartmentId()) ? "selected" : "" %>>
                            <%= dept.getDepartmentName() %>
                        </option>
                    <% } %>
                </select>
            </div>

            <div>
                <label for="role">角色:</label>
                <select id="role" name="role" required>
                    <option value="Department Admin" <%= (userToEdit != null && "Department Admin".equals(userToEdit.getRole())) ? "selected" : "" %>>部门管理员</option>
                    <option value="School Admin" <%= (userToEdit != null && "School Admin".equals(userToEdit.getRole())) ? "selected" : "" %>>校级管理员</option>
                    <option value="Audit Admin" <%= (userToEdit != null && "Audit Admin".equals(userToEdit.getRole())) ? "selected" : "" %>>审计管理员</option>
                    <option value="System Admin" <%= (userToEdit != null && "System Admin".equals(userToEdit.getRole())) ? "selected" : "" %>>系统管理员</option>
                </select>
            </div>

            <% if (userToEdit != null) { %>
            <div>
                <label for="accountStatus">账户状态:</label>
                <select id="accountStatus" name="accountStatus" required>
                    <option value="Active" <%= "Active".equals(userToEdit.getAccountStatus()) ? "selected" : "" %>>激活</option>
                    <option value="Inactive" <%= "Inactive".equals(userToEdit.getAccountStatus()) ? "selected" : "" %>>禁用</option>
                    <option value="Locked" <%= "Locked".equals(userToEdit.getAccountStatus()) ? "selected" : "" %>>锁定</option>
                </select>
            </div>
            <% } %>

            <div class="form-actions">
                <input type="submit" value="<%= formAction.equals("create") ? "创建用户" : "更新用户" %>">
                <a href="<%= request.getContextPath() %>/admin/userManagement" class="cancel-button">取消</a>
            </div>
        </form>
    </div>
</body>
</html>
