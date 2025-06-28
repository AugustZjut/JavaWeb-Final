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
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .header .welcome { 
            font-size: 16px; 
        }
        .header .user-menu { 
            position: relative; 
            display: inline-block; 
        }
        .header .user-menu .dropdown-btn {
            background: none;
            border: none;
            color: white;
            cursor: pointer;
            padding: 10px 15px;
            font-size: 14px;
            display: flex;
            align-items: center;
            border-radius: 3px;
        }
        .header .user-menu .dropdown-btn:hover {
            background-color: #555;
        }
        .header .user-menu .dropdown-btn::after {
            content: " ▼";
            margin-left: 5px;
            font-size: 10px;
        }
        .header .user-menu .dropdown-content {
            display: none;
            position: absolute;
            right: 0;
            background-color: #f9f9f9;
            min-width: 160px;
            box-shadow: 0px 8px 16px 0px rgba(0,0,0,0.2);
            z-index: 1;
            border-radius: 5px;
        }
        .header .user-menu .dropdown-content a {
            color: black;
            padding: 12px 16px;
            text-decoration: none;
            display: block;
        }
        .header .user-menu .dropdown-content a:hover {
            background-color: #f1f1f1;
        }
        .header .user-menu:hover .dropdown-content {
            display: block;
        }
        .header a {
            color: white;
            text-decoration: none;
            margin-left: 15px;
        }
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
        <div class="welcome">欢迎, <%= adminUser.getFullName() %> (<%= adminUser.getRole() %>)</div>
        <div class="user-menu">
            <button class="dropdown-btn">我的账户</button>
            <div class="dropdown-content">
                <a href="<%= request.getContextPath() %>/admin/userManagement?action=myAccount">账户信息</a>
                <a href="<%= request.getContextPath() %>/admin/logout">退出登录</a>
            </div>
        </div>
    </div>

    <div class="container">
        <h1>管理员控制台</h1>
        <nav>
            <ul>
                <% if ("SCHOOL_ADMIN".equals(adminUser.getRole()) || "SYSTEM_ADMIN".equals(adminUser.getRole())) { %>
                    <li><a href="<%= request.getContextPath() %>/admin/userManagement">管理员管理</a></li>
                    <li><a href="<%= request.getContextPath() %>/admin/departments">部门管理</a></li>
                <% } %>
                
                <%-- 公众预约管理：系统管理员、学校管理员，或有权限的部门管理员 --%>
                <% 
                    boolean canManagePublic = "SYSTEM_ADMIN".equals(adminUser.getRole()) ||
                                            "SCHOOL_ADMIN".equals(adminUser.getRole()) ||
                                            ("DEPARTMENT_ADMIN".equals(adminUser.getRole()) && 
                                             adminUser.isCanManagePublicAppointments());
                %>
                <% if (canManagePublic) { %>
                    <li><a href="<%= request.getContextPath() %>/admin/publicAppointmentManagement">公众预约管理</a></li>
                <% } %>
                
                <li><a href="<%= request.getContextPath() %>/admin/officialAppointmentManagement">公务预约管理</a></li>
                <% if ("AUDIT_ADMIN".equals(adminUser.getRole()) || "SYSTEM_ADMIN".equals(adminUser.getRole())) { %>
                    <li><a href="<%= request.getContextPath() %>/admin/auditLog">审计日志</a></li>
                <% } %>
            </ul>
        </nav>
        
        <hr>
        <h2>快速操作</h2>
        <p>请从上方导航栏选择一个管理模块。</p>
        
        <% if ("SYSTEM_ADMIN".equals(adminUser.getRole())) { %>
        <div style="margin-top: 20px; padding: 10px; background-color: #f8f9fa; border-radius: 4px;">
            <h3>系统调试</h3>
            <a href="${pageContext.request.contextPath}/admin/testDB.jsp" style="color: #007bff;">🔧 数据库连接测试</a>
        </div>
        <% } %>
        
        <%-- TODO: Add more dashboard elements as needed --%>

    </div>

</body>
</html>
