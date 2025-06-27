<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.example.webdemo.beans.User" %>
<%@ page import="com.example.webdemo.dao.UserDAO" %>
<%@ page import="com.example.webdemo.dao.DepartmentDAO" %>
<%@ page import="com.example.webdemo.beans.Department" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.stream.Collectors" %>

<%-- Auth Check --%>
<% 
    User loggedInAdmin = (User) session.getAttribute("adminUser");
    if (loggedInAdmin == null || (!"School Admin".equals(loggedInAdmin.getRole()) && !"System Admin".equals(loggedInAdmin.getRole()))) {
        response.sendRedirect(request.getContextPath() + "/admin/login");
        return;
    }
    UserDAO userDAO = new UserDAO();
    DepartmentDAO departmentDAO = new DepartmentDAO();
    List<User> userList = userDAO.listAllUsers();
    List<Department> departmentList = departmentDAO.getAllDepartments();
    Map<Integer, String> departmentMap = departmentList.stream()
            .collect(Collectors.toMap(Department::getDepartmentId, Department::getDepartmentName));
%>

<html>
<head>
    <title>管理员管理</title>
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
        .container { width: 90%; margin: 20px auto; background-color: #fff; padding: 20px; box-shadow: 0 0 10px rgba(0,0,0,0.1); }
        h1 { color: #333; }
        table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 20px;
        }
        th, td {
            border: 1px solid #ddd;
            padding: 8px;
            text-align: left;
        }
        th {
            background-color: #f2f2f2;
        }
        .action-links a {
            margin-right: 10px;
            text-decoration: none;
            color: #007bff;
        }
        .action-links a.delete {
            color: red;
        }
        .add-button {
            display: inline-block;
            padding: 10px 15px;
            background-color: #28a745;
            color: white;
            text-decoration: none;
            border-radius: 5px;
            margin-bottom: 20px;
        }
        .message {
            padding: 10px;
            margin-bottom: 15px;
            border-radius: 4px;
        }
        .success-message { background-color: #d4edda; color: #155724; border: 1px solid #c3e6cb; }
        .error-message { background-color: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }
    </style>
</head>
<body>
    <div class="header">
        <span>管理员: <%= loggedInAdmin.getFullName() %></span>
        <a href="<%= request.getContextPath() %>/admin/dashboard.jsp">控制台</a>
        <a href="<%= request.getContextPath() %>/admin/logout">退出登录</a>
    </div>

    <div class="container">
        <h1>管理员管理</h1>

        <% 
            String successMessage = (String) request.getAttribute("successMessage");
            if (successMessage != null) {
        %>
            <div class="message success-message"><%= successMessage %></div>
        <% 
            }
            String errorMessage = (String) request.getAttribute("errorMessage");
            if (errorMessage != null) {
        %>
            <div class="message error-message"><%= errorMessage %></div>
        <% 
            }
        %>

        <a href="<%= request.getContextPath() %>/admin/userManagement?action=add" class="add-button">添加新管理员</a>

        <table>
            <thead>
                <tr>
                    <th>用户ID</th>
                    <th>用户名</th>
                    <th>姓名</th>
                    <th>部门</th>
                    <th>电话</th>
                    <th>角色</th>
                    <th>状态</th>
                    <th>上次登录</th>
                    <th>操作</th>
                </tr>
            </thead>
            <tbody>
                <% for (User user : userList) { %>
                <tr>
                    <td><%= user.getUserId() %></td>
                    <td><%= user.getUsername() %></td>
                    <td><%= user.getFullName() %></td>
                    <td><%= user.getDepartmentId() == 0 ? "N/A" : departmentMap.getOrDefault(user.getDepartmentId(), "未知部门") %></td>
                    <td><%= user.getPhoneNumber() %></td> <%-- TODO: Decrypt and mask --%>
                    <td><%= user.getRole() %></td>
                    <td><%= user.getAccountStatus() %></td>
                    <td><%= user.getLastLoginTime() != null ? user.getLastLoginTime().toString().substring(0, 19) : "-" %></td>
                    <td class="action-links">
                        <a href="<%= request.getContextPath() %>/admin/userManagement?action=edit&id=<%= user.getUserId() %>">编辑</a>
                        <a href="<%= request.getContextPath() %>/admin/userManagement?action=delete&id=<%= user.getUserId() %>" 
                           class="delete" onclick="return confirm('确定要删除用户 <%= user.getUsername() %> 吗？');">删除</a>
                        <%-- TODO: Add links for lock/unlock, reset password if needed --%>
                    </td>
                </tr>
                <% } %>
                <% if (userList.isEmpty()) { %>
                <tr>
                    <td colspan="9" style="text-align:center;">没有找到管理员用户。</td>
                </tr>
                <% } %>
            </tbody>
        </table>
    </div>
</body>
</html>
