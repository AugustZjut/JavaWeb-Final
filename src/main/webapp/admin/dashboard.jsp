<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.example.webdemo.beans.User" %>
<%-- Check if user is logged in --%>
<% 
    User adminUser = (User) session.getAttribute("adminUser");
    if (adminUser == null) {
        response.sendRedirect(request.getContextPath() + "/admin/login");
        return;
    }
%>
<html>
<head>
    <title>管理员控制台</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4; }
        .header {
            background-color: #333;
            color: white;
            padding: 15px 20px;
            text-align: right;
        }
        .header a {
            color: white;
            text-decoration: none;
            margin-left: 15px;
        }
        .header span { float: left; }
        .container {
            width: 80%;
            margin: 20px auto;
            background-color: #fff;
            padding: 20px;
            box-shadow: 0 0 10px rgba(0,0,0,0.1);
        }
        h1, h2 {
            color: #333;
        }
        nav ul {
            list-style-type: none;
            padding: 0;
        }
        nav ul li {
            display: inline;
            margin-right: 20px;
        }
        nav ul li a {
            text-decoration: none;
            color: #007bff;
        }
        nav ul li a:hover {
            text-decoration: underline;
        }
    </style>
</head>
<body>
    <div class="header">
        <span>欢迎, <%= adminUser.getFullName() %> (<%= adminUser.getRole() %>)</span>
        <a href="<%= request.getContextPath() %>/admin/logout">退出登录</a>
    </div>

    <div class="container">
        <h1>管理员控制台</h1>
        <nav>
            <ul>
                <% if ("School Admin".equals(adminUser.getRole()) || "System Admin".equals(adminUser.getRole())) { %>
                    <li><a href="<%= request.getContextPath() %>/admin/userManagement">管理员管理</a></li>
                    <li><a href="<%= request.getContextPath() %>/admin/departmentManagement">部门管理</a></li>
                <% } %>
                <li><a href="<%= request.getContextPath() %>/admin/publicAppointmentManagement">公开预约管理</a></li>
                <li><a href="<%= request.getContextPath() %>/admin/officialAppointmentManagement">公务预约管理</a></li>
                <% if ("Audit Admin".equals(adminUser.getRole()) || "System Admin".equals(adminUser.getRole())) { %>
                    <li><a href="<%= request.getContextPath() %>/admin/auditLog">审计日志</a></li>
                <% } %>
            </ul>
        </nav>
        
        <hr>
        <h2>快速操作</h2>
        <p>请从上方导航栏选择一个管理模块。</p>
        
        <%-- TODO: Add more dashboard elements as needed --%>

    </div>

</body>
</html>
