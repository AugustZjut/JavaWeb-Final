<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<html>
<head>
    <title>部门管理</title>
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
        <div class="logo">部门管理</div>
        <div>
            <a href="${pageContext.request.contextPath}/admin/dashboard.jsp">返回控制台</a>
        </div>
    </div>

    <div class="container">
        <h1>部门列表</h1>
        
        <a href="${pageContext.request.contextPath}/admin/departments?action=add" class="add-button">添加新部门</a>
        
        <!-- 搜索表单 -->
        <div class="search-container" style="margin-bottom: 20px; padding: 15px; background-color: #f8f9fa; border-radius: 5px;">
            <form action="${pageContext.request.contextPath}/admin/departments" method="get">
                <input type="hidden" name="action" value="search">
                <div style="display: flex; gap: 15px; align-items: end;">
                    <div style="flex: 1;">
                        <label for="searchCode">部门代码:</label>
                        <input type="text" id="searchCode" name="departmentCode" value="<c:out value='${requestScope.searchDepartmentCode}'/>" placeholder="输入部门代码">
                    </div>
                    <div style="flex: 1;">
                        <label for="searchName">部门名称:</label>
                        <input type="text" id="searchName" name="departmentName" value="<c:out value='${requestScope.searchDepartmentName}'/>" placeholder="输入部门名称">
                    </div>
                    <div style="flex: 1;">
                        <label for="searchType">部门类型:</label>
                        <select id="searchType" name="departmentType">
                            <option value="">全部类型</option>
                            <option value="ADMINISTRATIVE" ${requestScope.searchDepartmentType == 'ADMINISTRATIVE' ? 'selected' : ''}>行政部门</option>
                            <option value="DIRECTLY_AFFILIATED" ${requestScope.searchDepartmentType == 'DIRECTLY_AFFILIATED' ? 'selected' : ''}>直属部门</option>
                            <option value="COLLEGE" ${requestScope.searchDepartmentType == 'COLLEGE' ? 'selected' : ''}>学院</option>
                        </select>
                    </div>
                    <div>
                        <button type="submit" style="padding: 10px 20px; background-color: #007bff; color: white; border: none; border-radius: 4px; cursor: pointer;">搜索</button>
                        <a href="${pageContext.request.contextPath}/admin/departments" style="padding: 10px 15px; background-color: #6c757d; color: white; text-decoration: none; border-radius: 4px; margin-left: 5px;">重置</a>
                    </div>
                </div>
            </form>
        </div>
        
        <c:if test="${not empty requestScope.message}">
            <div class="message success-message">${requestScope.message}</div>
        </c:if>
        <c:if test="${not empty requestScope.error}">
            <div class="message error-message">${requestScope.error}</div>
        </c:if>
        
        <table>
            <thead>
                <tr>
                    <th>ID</th>
                    <th>代码</th>
                    <th>类型</th>
                    <th>名称</th>
                    <th>操作</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="dept" items="${listDepartment}">
                    <tr>
                        <td><c:out value="${dept.departmentId}"/></td>
                        <td><c:out value="${dept.departmentCode}"/></td>
                        <td>
                            <c:choose>
                                <c:when test="${dept.departmentType == 'ADMINISTRATIVE'}">行政部门</c:when>
                                <c:when test="${dept.departmentType == 'DIRECTLY_AFFILIATED'}">直属部门</c:when>
                                <c:when test="${dept.departmentType == 'COLLEGE'}">学院</c:when>
                                <c:otherwise><c:out value="${dept.departmentType}"/></c:otherwise>
                            </c:choose>
                        </td>
                        <td><c:out value="${dept.departmentName}"/></td>
                        <td class="action-links">
                            <a href="${pageContext.request.contextPath}/admin/departments?action=edit&id=${dept.departmentId}">编辑</a>
                            <a href="${pageContext.request.contextPath}/admin/departments?action=delete&id=${dept.departmentId}" class="delete" onclick="return confirm('您确定要删除这个部门吗？')">删除</a>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty listDepartment}">
                    <tr>
                        <td colspan="5" style="text-align: center;">未找到部门记录</td>
                    </tr>
                </c:if>
            </tbody>
        </table>
    </div>
</body>
</html>
