<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="com.example.webdemo.util.DataMaskingUtils" %>

<html>
<head>
    <title>Official Appointment Management</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background-color: #f4f4f4; color: #333; }
        h1 { color: #333; }
        table { width: 100%; border-collapse: collapse; margin-top: 20px; box-shadow: 0 0 10px rgba(0,0,0,0.1); background-color: #fff; }
        th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }
        th { background-color: #007bff; color: white; }
        tr:nth-child(even) { background-color: #f9f9f9; }
        tr:hover { background-color: #f1f1f1; }
        a { color: #007bff; text-decoration: none; margin-right: 10px;}
        a:hover { text-decoration: underline; }
        .search-form { margin-bottom: 20px; padding: 15px; background-color: #fff; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1);}
        .search-form input[type="text"], .search-form input[type="date"], .search-form select {
            padding: 8px; margin-right: 10px; border: 1px solid #ddd; border-radius: 4px;
        }
        .search-form input[type="submit"] {
            padding: 8px 15px; background-color: #007bff; color: white; border: none; border-radius: 4px; cursor: pointer;
        }
        .search-form input[type="submit"]:hover { background-color: #0056b3; }
        .status-pending { color: orange; font-weight: bold; }
        .status-approved { color: green; font-weight: bold; }
        .status-rejected { color: red; font-weight: bold; }
        .status-cancelled { color: grey; font-weight: bold; }
    </style>
</head>
<body>
    <h1>Official Appointment List</h1>

    <%-- Search/Filter Form (Placeholder) --%>

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
                <th>Visiting Department</th>
                <th>Contact Person</th>
                <th>Reason</th>
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
                    <td><c:out value="${DataMaskingUtils.maskPhone(app.decryptedVisitorPhone)}"/></td>
                    <td><c:out value="${app.visitDepartment}"/></td>
                    <td><c:out value="${app.visitContactPerson}"/></td>
                    <td><c:out value="${app.visitReason}"/></td>
                    <td>
                        <span class="status-${fn:toLowerCase(app.status)}"><c:out value="${app.status}"/></span>
                    </td>
                    <td><fmt:formatDate value="${app.createdAt}" pattern="yyyy-MM-dd HH:mm:ss"/></td>
                    <td>
                        <c:if test="${app.status == 'Pending' || app.status == 'Pending Approval'}"> 
                            <a href="${pageContext.request.contextPath}/admin/officialAppointments?action=approveView&id=${app.id}">Review/Approve</a>
                        </c:if>
                        <%-- <a href="${pageContext.request.contextPath}/admin/officialAppointments?action=viewDetails&id=${app.id}">Details</a> --%>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty listAppointment}">
                <tr>
                    <td colspan="12">No official appointments found.</td>
                </tr>
            </c:if>
        </tbody>
    </table>
    <p><a href="${pageContext.request.contextPath}/admin/dashboard.jsp">Back to Dashboard</a></p>
</body>
</html>
