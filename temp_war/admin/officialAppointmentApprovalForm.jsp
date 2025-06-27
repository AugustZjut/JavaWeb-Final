<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="com.example.webdemo.util.DataMaskingUtils" %>

<html>
<head>
    <title>Review Official Appointment</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background-color: #f4f4f4; color: #333; }
        h1 { color: #333; }
        .container { background-color: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0,0,0,0.1); width: 70%; margin-top: 20px; }
        .detail-item { margin-bottom: 15px; }
        .detail-item label { font-weight: bold; display: inline-block; width: 180px; }
        .detail-item span { }
        .actions { margin-top: 20px; }
        .button {
            padding: 10px 15px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            text-decoration: none;
            display: inline-block;
            font-size: 16px;
            margin-right: 10px;
        }
        .approve-button { background-color: #28a745; color: white; }
        .approve-button:hover { background-color: #218838; }
        .reject-button { background-color: #dc3545; color: white; }
        .reject-button:hover { background-color: #c82333; }
        .cancel-button { background-color: #6c757d; color: white; }
        .cancel-button:hover { background-color: #5a6268; }
        textarea { width: calc(100% - 22px); padding: 10px; margin-top:5px; border: 1px solid #ddd; border-radius: 4px; box-sizing: border-box; min-height: 80px;}
    </style>
</head>
<body>
    <div class="container">
        <h1>Review Official Appointment (ID: <c:out value="${appointment.id}"/>)</h1>

        <div class="detail-item">
            <label>Campus Area:</label>
            <span><c:out value="${appointment.campusArea}"/></span>
        </div>
        <div class="detail-item">
            <label>Appointment Time:</label>
            <span><fmt:formatDate value="${appointment.appointmentTime}" pattern="yyyy-MM-dd HH:mm"/></span>
        </div>
        <div class="detail-item">
            <label>Visitor Name:</label>
            <span><c:out value="${DataMaskingUtils.maskName(appointment.visitorName)}"/></span>
        </div>
        <div class="detail-item">
            <label>Visitor ID Card:</label>
            <span><c:out value="${DataMaskingUtils.maskIdCard(appointment.visitorIdCard)}"/></span>
        </div>
        <div class="detail-item">
            <label>Visitor Phone:</label>
            <span><c:out value="${DataMaskingUtils.maskPhone(appointment.decryptedVisitorPhone)}"/></span>
        </div>
        <div class="detail-item">
            <label>Organization:</label>
            <span><c:out value="${appointment.visitorOrganization}"/></span>
        </div>
        <div class="detail-item">
            <label>Transport:</label>
            <span><c:out value="${appointment.transportType}"/></span>
        </div>
        <c:if test="${not empty appointment.licensePlate}">
            <div class="detail-item">
                <label>License Plate:</label>
                <span><c:out value="${appointment.licensePlate}"/></span>
            </div>
        </c:if>

        <hr style="margin: 20px 0;">

        <div class="detail-item">
            <label>Visiting Department:</label>
            <span><c:out value="${appointment.visitDepartment}"/></span>
        </div>
        <div class="detail-item">
            <label>Contact Person:</label>
            <span><c:out value="${appointment.visitContactPerson}"/></span>
        </div>
        <div class="detail-item">
            <label>Reason for Visit:</label>
            <div style="white-space: pre-wrap; margin-left:185px;"><c:out value="${appointment.visitReason}"/></div>
        </div>
        
        <c:if test="${not empty appointment.accompanyingPersons}">
            <hr style="margin: 20px 0;">
            <h3>Accompanying Persons:</h3>
            <c:forEach var="person" items="${appointment.accompanyingPersons}" varStatus="status">
                <div class="detail-item">
                    <label>Person ${status.count} Name:</label>
                    <span><c:out value="${DataMaskingUtils.maskName(person.name)}"/></span>
                </div>
                <div class="detail-item">
                    <label>Person ${status.count} ID Card:</label>
                    <span><c:out value="${DataMaskingUtils.maskIdCard(person.idCard)}"/></span>
                </div>
                 <div class="detail-item">
                    <label>Person ${status.count} Phone:</label>
                    <span><c:out value="${DataMaskingUtils.maskPhone(person.decryptedPhone)}"/></span>
                </div>
                <br/>
            </c:forEach>
        </c:if>

        <hr style="margin: 20px 0;">
        <div class="detail-item">
            <label>Current Status:</label>
            <span style="font-weight:bold;"><c:out value="${appointment.status}"/></span>
        </div>
         <div class="detail-item">
            <label>Submitted At:</label>
            <span><fmt:formatDate value="${appointment.createdAt}" pattern="yyyy-MM-dd HH:mm:ss"/></span>
        </div>

        <c:if test="${appointment.status == 'Pending' || appointment.status == 'Pending Approval'}">
            <div class="actions">
                <form action="${pageContext.request.contextPath}/admin/officialAppointments" method="post" style="display:inline;">
                    <input type="hidden" name="action" value="approve">
                    <input type="hidden" name="appointmentId" value="${appointment.id}">
                    <%-- <label for="approvalNotes">Approval Notes (Optional):</label><br>
                    <textarea id="approvalNotes" name="approvalNotes"></textarea><br> --%>
                    <button type="submit" class="button approve-button">Approve</button>
                </form>
                <form action="${pageContext.request.contextPath}/admin/officialAppointments" method="post" style="display:inline;">
                    <input type="hidden" name="action" value="reject">
                    <input type="hidden" name="appointmentId" value="${appointment.id}">
                    <%-- <label for="rejectionReason">Rejection Reason (Required if rejecting):</label><br>
                    <textarea id="rejectionReason" name="rejectionReason" required></textarea><br> --%>
                    <button type="submit" class="button reject-button">Reject</button>
                </form>
            </div>
        </c:if>

        <div style="margin-top: 20px;">
            <a href="${pageContext.request.contextPath}/admin/officialAppointments?action=list" class="button cancel-button">Back to List</a>
        </div>
    </div>
</body>
</html>
