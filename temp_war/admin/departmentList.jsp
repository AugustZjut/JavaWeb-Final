<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<html>
<head>
    <title>Department Management</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background-color: #f4f4f4; color: #333; }
        h1 { color: #333; }
        table { width: 80%; border-collapse: collapse; margin-top: 20px; box-shadow: 0 0 10px rgba(0,0,0,0.1); background-color: #fff; }
        th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }
        th { background-color: #007bff; color: white; }
        tr:nth-child(even) { background-color: #f9f9f9; }
        tr:hover { background-color: #f1f1f1; }
        a { color: #007bff; text-decoration: none; margin-right: 10px; }
        a:hover { text-decoration: underline; }
        .add-button { display: inline-block; padding: 10px 15px; background-color: #28a745; color: white; text-decoration: none; border-radius: 5px; margin-bottom: 20px; }
        .add-button:hover { background-color: #218838; }
        .action-links a { margin-right: 8px;}
    </style>
</head>
<body>
    <h1>Department List</h1>
    <a href="${pageContext.request.contextPath}/admin/departments?action=add" class="add-button">Add New Department</a>
    <c:if test="${not empty requestScope.message}">
        <p style="color: green;">${requestScope.message}</p>
    </c:if>
    <c:if test="${not empty requestScope.error}">
        <p style="color: red;">${requestScope.error}</p>
    </c:if>
    <table>
        <thead>
            <tr>
                <th>ID</th>
                <th>Code</th>
                <th>Type</th>
                <th>Name</th>
                <th>Actions</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach var="dept" items="${listDepartment}">
                <tr>
                    <td><c:out value="${dept.departmentId}"/></td>
                    <td><c:out value="${dept.departmentCode}"/></td>
                    <td><c:out value="${dept.departmentType}"/></td>
                    <td><c:out value="${dept.departmentName}"/></td>
                    <td class="action-links">
                        <a href="${pageContext.request.contextPath}/admin/departments?action=edit&id=${dept.departmentId}">Edit</a>
                        <a href="${pageContext.request.contextPath}/admin/departments?action=delete&id=${dept.departmentId}" onclick="return confirm('Are you sure you want to delete this department?')">Delete</a>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty listDepartment}">
                <tr>
                    <td colspan="5">No departments found.</td>
                </tr>
            </c:if>
        </tbody>
    </table>
    <p><a href="${pageContext.request.contextPath}/admin/dashboard.jsp">Back to Dashboard</a></p>
</body>
</html>
