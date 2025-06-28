<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.example.webdemo.beans.User" %>
<%@ page import="com.example.webdemo.dao.UserDAO" %>
<%@ page import="com.example.webdemo.dao.DepartmentDAO" %>
<%@ page import="com.example.webdemo.beans.Department" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.stream.Collectors" %>
<%@ page import="com.example.webdemo.util.DataMaskingUtils" %>

<%-- Auth Check --%>
<% 
    User adminUser = (User) session.getAttribute("adminUser");
    if (adminUser == null || (!"SCHOOL_ADMIN".equals(adminUser.getRole()) && !"SYSTEM_ADMIN".equals(adminUser.getRole()))) {
        response.sendRedirect(request.getContextPath() + "/admin/adminLogin.jsp");
        return;
    }
    
    // 使用请求属性中的数据，而不是直接创建DAO
    List<User> userList = (List<User>) request.getAttribute("userList");
    List<Department> departmentList = (List<Department>) request.getAttribute("departmentList");
    
    // 如果数据为空（即不是从Servlet转发来的），则重定向到Servlet
    if (userList == null) {
        response.sendRedirect(request.getContextPath() + "/admin/userManagement");
        return;
    }
    
    // 创建部门ID到部门名称的映射
    Map<Integer, String> departmentMap = null;
    if (departmentList != null) {
        departmentMap = departmentList.stream()
                .collect(Collectors.toMap(Department::getDepartmentId, Department::getDepartmentName));
    }
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
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .header .logo { 
            font-size: 18px; 
            font-weight: bold; 
        }
        .header a { 
            color: white; 
            text-decoration: none; 
            margin-left: 15px; 
        }
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
        .search-form {
            background-color: #f8f9fa;
            padding: 20px;
            border-radius: 5px;
            margin-bottom: 20px;
            border: 1px solid #dee2e6;
        }
        .search-form table {
            width: 100%;
            border: none;
            margin-top: 0;
        }
        .search-form td {
            border: none;
            padding: 5px 10px;
            vertical-align: middle;
        }
        .search-form input, .search-form select {
            width: 100%;
            padding: 5px;
            border: 1px solid #ccc;
            border-radius: 3px;
        }
        .search-form .search-buttons {
            text-align: right;
            padding-top: 10px;
        }
        .search-form .search-buttons button, .search-form .search-buttons a {
            padding: 8px 15px;
            margin-left: 10px;
            border: none;
            border-radius: 3px;
            text-decoration: none;
            cursor: pointer;
        }
        .search-btn {
            background-color: #007bff;
            color: white;
        }
        .reset-btn {
            background-color: #6c757d;
            color: white;
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
        <div class="logo">管理员管理</div>
        <div>
            <a href="<%= request.getContextPath() %>/admin/dashboard.jsp">返回控制台</a>
        </div>
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

        <!-- 查询表单 -->
        <div class="search-form">
            <h3>查询条件</h3>
            <form method="get" action="<%= request.getContextPath() %>/admin/userManagement">
                <input type="hidden" name="action" value="search">
                <table>
                    <tr>
                        <td style="width: 120px;"><strong>用户名:</strong></td>
                        <td style="width: 200px;">
                            <input type="text" name="username" value="<%= request.getAttribute("searchUsername") != null ? request.getAttribute("searchUsername") : "" %>" placeholder="精确匹配用户名">
                        </td>
                        <td style="width: 120px;"><strong>姓名:</strong></td>
                        <td style="width: 200px;">
                            <input type="text" name="fullName" value="<%= request.getAttribute("searchFullName") != null ? request.getAttribute("searchFullName") : "" %>" placeholder="输入姓名">
                        </td>
                        <td style="width: 120px;"><strong>姓名匹配:</strong></td>
                        <td>
                            <select name="nameSearchType">
                                <option value="fuzzy" <%= "fuzzy".equals(request.getAttribute("searchNameType")) ? "selected" : "" %>>模糊匹配</option>
                                <option value="exact" <%= "exact".equals(request.getAttribute("searchNameType")) ? "selected" : "" %>>精确匹配</option>
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <td><strong>部门:</strong></td>
                        <td>
                            <select name="departmentId">
                                <option value="ALL">所有部门</option>
                                <% 
                                String searchDeptId = (String) request.getAttribute("searchDepartmentId");
                                if (departmentList != null) {
                                    for (Department dept : departmentList) { 
                                %>
                                <option value="<%= dept.getDepartmentId() %>" <%= String.valueOf(dept.getDepartmentId()).equals(searchDeptId) ? "selected" : "" %>><%= dept.getDepartmentName() %></option>
                                <% 
                                    }
                                }
                                %>
                            </select>
                        </td>
                        <td><strong>角色:</strong></td>
                        <td>
                            <select name="role">
                                <option value="ALL">所有角色</option>
                                <% 
                                String searchRole = (String) request.getAttribute("searchRole");
                                if ("SYSTEM_ADMIN".equals(adminUser.getRole())) { 
                                %>
                                <option value="SYSTEM_ADMIN" <%= "SYSTEM_ADMIN".equals(searchRole) ? "selected" : "" %>>系统管理员</option>
                                <option value="SCHOOL_ADMIN" <%= "SCHOOL_ADMIN".equals(searchRole) ? "selected" : "" %>>学校管理员</option>
                                <option value="AUDIT_ADMIN" <%= "AUDIT_ADMIN".equals(searchRole) ? "selected" : "" %>>审计管理员</option>
                                <% } %>
                                <option value="DEPARTMENT_ADMIN" <%= "DEPARTMENT_ADMIN".equals(searchRole) ? "selected" : "" %>>部门管理员</option>
                            </select>
                        </td>
                        <td><strong>账号状态:</strong></td>
                        <td>
                            <select name="accountStatus">
                                <option value="ALL">所有状态</option>
                                <option value="NORMAL" <%= "NORMAL".equals(request.getAttribute("searchAccountStatus")) ? "selected" : "" %>>正常</option>
                                <option value="LOCKED" <%= "LOCKED".equals(request.getAttribute("searchAccountStatus")) ? "selected" : "" %>>已锁定</option>
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <td><strong>密码状态:</strong></td>
                        <td>
                            <select name="passwordStatus">
                                <option value="ALL">所有状态</option>
                                <option value="NORMAL" <%= "NORMAL".equals(request.getAttribute("searchPasswordStatus")) ? "selected" : "" %>>正常</option>
                                <option value="CHANGE_REQUIRED" <%= "CHANGE_REQUIRED".equals(request.getAttribute("searchPasswordStatus")) ? "selected" : "" %>>需修改密码</option>
                            </select>
                        </td>
                        <td colspan="4"></td>
                    </tr>
                </table>
                <div class="search-buttons">
                    <button type="submit" class="search-btn">查询</button>
                    <a href="<%= request.getContextPath() %>/admin/userManagement" class="reset-btn">重置</a>
                </div>
            </form>
        </div>

        <% if (request.getAttribute("isSearchResult") != null) { %>
        <div style="margin-bottom: 10px; color: #666;">
            <strong>查询结果:</strong> 共找到 <%= userList.size() %> 个用户
        </div>
        <% } %>

        <table>
            <thead>
                <tr>
                    <th>用户ID</th>
                    <th>用户名</th>
                    <th>姓名</th>
                    <th>部门</th>
                    <th>电话</th>
                    <th>角色</th>
                    <th>公众预约管理</th>
                    <th>账号状态</th>
                    <th>密码状态</th>
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
                    <td><%= user.getDepartmentId() == null || user.getDepartmentId() == 0 ? "N/A" : departmentMap.getOrDefault(user.getDepartmentId(), "未知部门") %></td>
                    <td><%= user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty() ? DataMaskingUtils.maskPhoneNumber(user.getPhoneNumber()) : "-" %></td>
                    <td><%= user.getRole() %></td>
                    <td><%= user.isCanManagePublicAppointments() ? "<span style='color: green;'>有权限</span>" : "<span style='color: gray;'>无权限</span>" %></td>
                    <td><%= user.getLockoutTime() != null && user.getLockoutTime().getTime() > System.currentTimeMillis() ? "已锁定" : "正常" %></td>
                    <td><%= user.isPasswordChangeRequired() ? "需修改密码" : "正常" %></td>
                    <td><%= user.getUpdatedAt() != null ? user.getUpdatedAt().toString().substring(0, 19) : "-" %></td>
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
                    <td colspan="10" style="text-align:center;">没有找到管理员用户。</td>
                </tr>
                <% } %>
            </tbody>
        </table>
    </div>
</body>
</html>
