<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="com.example.webdemo.util.DataMaskingUtils" %>

<html>
<head>
    <title>Public Appointment Management</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background-color: #f4f4f4; color: #333; }
        h1 { color: #333; }
        table { width: 100%; border-collapse: collapse; margin-top: 20px; box-shadow: 0 0 10px rgba(0,0,0,0.1); background-color: #fff; }
        th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }
        th { background-color: #007bff; color: white; }
        tr:nth-child(even) { background-color: #f9f9f9; }
        tr:hover { background-color: #f1f1f1; }
        a { color: #007bff; text-decoration: none; }
        a:hover { text-decoration: underline; }
        .search-form { margin-bottom: 20px; padding: 15px; background-color: #fff; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1);}
        .search-form input[type="text"], .search-form input[type="date"], .search-form select {
            padding: 8px; margin-right: 10px; border: 1px solid #ddd; border-radius: 4px;
        }
        .search-form input[type="submit"] {
            padding: 8px 15px; background-color: #007bff; color: white; border: none; border-radius: 4px; cursor: pointer;
        }
        .search-form input[type="submit"]:hover { background-color: #0056b3; }
    </style>
</head>
<body>
    <h1>Public Appointment List</h1>

    <%-- Search/Filter Form (Placeholder for future implementation) --%>
    <%-- 
    <div class="search-form">
        <form action="${pageContext.request.contextPath}/admin/publicAppointments?action=search" method="get">
            <input type="text" name="visitorName" placeholder="Visitor Name">
            <input type="text" name="visitorIdCard" placeholder="Visitor ID (partial)">
            <input type="date" name="appointmentDate">
            <select name="status">
                <option value="">All Statuses</option>
                <option value="Approved">Approved</option> 
                <option value="Pending">Pending</option> 
                <option value="Cancelled">Cancelled</option>
            </select>
            <input type="submit" value="Search">
        </form>
    </div>
    --%>

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
                <th>Campus</th>
                <th>Appointment Time</th>
                <th>Visitor Name</th>
                <th>Visitor ID</th>
                <th>Visitor Phone</th>
                <th>Status</th>
                <th>Created At</th>
                <th>Actions</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach var="app" items="${listAppointment}">
                <tr>
                    <td><c:out value="${app.id}"/></td>
                    <td><c:out value="${app.campusArea}"/></td>
                    <td><fmt:formatDate value="${app.appointmentTime}" pattern="yyyy-MM-dd HH:mm"/></td>
                    <td><c:out value="${DataMaskingUtils.maskName(app.visitorName)}"/></td>
                    <td><c:out value="${DataMaskingUtils.maskIdCard(app.visitorIdCard)}"/></td>
                    <td><c:out value="${DataMaskingUtils.maskPhone(app.decryptedVisitorPhone)}"/></td> <%-- Assuming phone is decrypted in servlet or DAO for display --%>
                    <td><c:out value="${app.status}"/></td>
                    <td><fmt:formatDate value="${app.createdAt}" pattern="yyyy-MM-dd HH:mm:ss"/></td>
                    <td>
                        <%-- <a href="${pageContext.request.contextPath}/admin/publicAppointments?action=view&id=${app.id}">View</a> --%>
                        <%-- Add other actions like cancel if applicable for public auto-approved appointments --%>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty listAppointment}">
                <tr>
                    <td colspan="9">No public appointments found.</td>
                </tr>
            </c:if>
        </tbody>
    </table>
    <p><a href="${pageContext.request.contextPath}/admin/dashboard.jsp">Back to Dashboard</a></p>
</body>
</html>
