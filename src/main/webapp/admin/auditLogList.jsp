<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.example.webdemo.beans.User" %>
<%@ page import="com.example.webdemo.beans.AuditLog" %>
<%@ page import="java.util.List" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%-- Check if user is logged in --%>
<% 
    User adminUser = (User) session.getAttribute("adminUser");
    if (adminUser == null) {
        response.sendRedirect(request.getContextPath() + "/admin/adminLogin.jsp");
        return;
    }
    
    // 检查权限
    String userRole = adminUser.getRole();
    if (!("AUDIT_ADMIN".equals(userRole) || "SYSTEM_ADMIN".equals(userRole))) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "访问被拒绝：您没有权限访问此资源。");
        return;
    }
    
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
%>
<html>
<head>
    <title>审计日志管理</title>
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
            width: 90%;
            margin: 20px auto;
            background-color: #fff;
            padding: 20px;
            box-shadow: 0 0 10px rgba(0,0,0,0.1);
        }
        h1, h2 {
            color: #333;
        }
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
            color: #333;
        }
        tr:nth-child(even) {
            background-color: #f9f9f9;
        }
        .filter-form {
            margin-bottom: 20px;
            padding: 15px;
            background-color: #f2f2f2;
            border-radius: 5px;
        }
        .filter-form input, .filter-form select {
            padding: 8px;
            margin-right: 10px;
            border: 1px solid #ddd;
            border-radius: 4px;
        }
        .filter-form button {
            padding: 8px 15px;
            background-color: #007bff;
            color: white;
            border: none;
            border-radius: 4px;
            cursor: pointer;
        }
        .navigation {
            margin: 20px 0;
        }
        .navigation a {
            margin-right: 10px;
            text-decoration: none;
            color: #007bff;
        }
        .pagination {
            margin-top: 20px;
            text-align: center;
        }
        .pagination a {
            display: inline-block;
            padding: 8px 16px;
            text-decoration: none;
            background-color: #f1f1f1;
            color: black;
            margin: 0 4px;
        }
        .pagination a.active {
            background-color: #007bff;
            color: white;
        }
        .pagination a:hover:not(.active) {
            background-color: #ddd;
        }
    </style>
</head>
<body>
    <div class="header">
        <span>欢迎, <%= adminUser.getFullName() %> (<%= adminUser.getRole() %>)</span>
        <a href="<%= request.getContextPath() %>/admin/dashboard.jsp">控制台</a>
        <a href="<%= request.getContextPath() %>/admin/logout">退出登录</a>
    </div>

    <div class="container">
        <div class="navigation">
            <a href="<%= request.getContextPath() %>/admin/dashboard.jsp">← 返回控制台</a>
        </div>
        
        <h1>审计日志管理</h1>
        
        <div class="filter-form">
            <form action="<%= request.getContextPath() %>/admin/auditLog" method="get">
                <label for="startDate">开始日期：</label>
                <input type="date" id="startDate" name="startDate" value="${param.startDate}">
                
                <label for="endDate">结束日期：</label>
                <input type="date" id="endDate" name="endDate" value="${param.endDate}">
                
                <label for="username">用户名：</label>
                <input type="text" id="username" name="username" value="${param.username}">
                
                <label for="actionType">操作类型：</label>
                <select id="actionType" name="actionType">
                    <option value="">全部</option>
                    <option value="USER_LOGIN" ${param.actionType == 'USER_LOGIN' ? 'selected' : ''}>用户登录</option>
                    <option value="ADMIN_LOGIN_SUCCESS" ${param.actionType == 'ADMIN_LOGIN_SUCCESS' ? 'selected' : ''}>管理员登录</option>
                    <option value="ADMIN_LOGOUT" ${param.actionType == 'ADMIN_LOGOUT' ? 'selected' : ''}>管理员注销</option>
                    <option value="CREATE_USER" ${param.actionType == 'CREATE_USER' ? 'selected' : ''}>创建用户</option>
                    <option value="UPDATE_USER" ${param.actionType == 'UPDATE_USER' ? 'selected' : ''}>更新用户</option>
                    <option value="DELETE_USER" ${param.actionType == 'DELETE_USER' ? 'selected' : ''}>删除用户</option>
                    <option value="APPROVE_APPOINTMENT" ${param.actionType == 'APPROVE_APPOINTMENT' ? 'selected' : ''}>批准预约</option>
                    <option value="REJECT_APPOINTMENT" ${param.actionType == 'REJECT_APPOINTMENT' ? 'selected' : ''}>拒绝预约</option>
                </select>
                
                <button type="submit">筛选</button>
                <button type="button" onclick="location.href='<%= request.getContextPath() %>/admin/auditLog'">重置</button>
            </form>
        </div>

        <table>
            <thead>
                <tr>
                    <th>ID</th>
                    <th>用户ID</th>
                    <th>用户名</th>
                    <th>操作类型</th>
                    <th>目标实体</th>
                    <th>目标ID</th>
                    <th>详情</th>
                    <th>IP地址</th>
                    <th>时间</th>
                </tr>
            </thead>
            <tbody>
                <% 
                    List<AuditLog> logs = (List<AuditLog>) request.getAttribute("auditLogs");
                    if (logs != null && !logs.isEmpty()) {
                        for (AuditLog log : logs) {
                %>
                <tr>
                    <td><%= log.getLogId() %></td>
                    <td><%= log.getUserId() %></td>
                    <td><%= log.getUsername() %></td>
                    <td><%= log.getActionType() %></td>
                    <td><%= log.getTargetEntity() != null ? log.getTargetEntity() : "" %></td>
                    <td><%= log.getTargetEntityId() != null ? log.getTargetEntityId() : "" %></td>
                    <td><%= log.getDetails() %></td>
                    <td><%= log.getIpAddress() %></td>
                    <td><%= log.getLogTimestamp() != null ? sdf.format(log.getLogTimestamp()) : "" %></td>
                </tr>
                <% 
                        }
                    } else {
                %>
                <tr>
                    <td colspan="9" style="text-align: center;">没有找到审计日志记录</td>
                </tr>
                <% } %>
            </tbody>
        </table>
        
        <% 
            Integer currentPage = (Integer) request.getAttribute("currentPage");
            Integer totalPages = (Integer) request.getAttribute("totalPages");
            
            if (currentPage != null && totalPages != null && totalPages > 1) {
        %>
        <div class="pagination">
            <% if (currentPage > 1) { %>
                <a href="<%= request.getContextPath() %>/admin/auditLog?page=<%= currentPage - 1 %>&startDate=${param.startDate}&endDate=${param.endDate}&username=${param.username}&actionType=${param.actionType}">&laquo; 上一页</a>
            <% } %>
            
            <% for (int i = 1; i <= totalPages; i++) { %>
                <a href="<%= request.getContextPath() %>/admin/auditLog?page=<%= i %>&startDate=${param.startDate}&endDate=${param.endDate}&username=${param.username}&actionType=${param.actionType}" <%= i == currentPage ? "class='active'" : "" %>><%= i %></a>
            <% } %>
            
            <% if (currentPage < totalPages) { %>
                <a href="<%= request.getContextPath() %>/admin/auditLog?page=<%= currentPage + 1 %>&startDate=${param.startDate}&endDate=${param.endDate}&username=${param.username}&actionType=${param.actionType}">下一页 &raquo;</a>
            <% } %>
        </div>
        <% } %>
    </div>
</body>
</html>
