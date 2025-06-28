<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ page import="com.example.webdemo.beans.User" %>
<%@ page import="com.example.webdemo.beans.Appointment" %>
<%@ page import="com.example.webdemo.beans.Department" %>
<%@ page import="java.util.List" %>

<%-- 权限检查 --%>
<%
    User adminUser = (User) session.getAttribute("adminUser");
    if (adminUser == null) {
        response.sendRedirect(request.getContextPath() + "/admin/adminLogin.jsp");
        return;
    }
    boolean hasAccess = "SYSTEM_ADMIN".equals(adminUser.getRole()) ||
                       "SCHOOL_ADMIN".equals(adminUser.getRole()) ||
                       ("DEPARTMENT_ADMIN".equals(adminUser.getRole()) &&
                        adminUser.isCanManageOfficialAppointments());

    if (!hasAccess) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "您没有权限访问此功能");
        return;
    }
    Appointment appointment = (Appointment) request.getAttribute("appointment");
    if (appointment == null) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到指定的预约记录");
        return;
    }
    // 部门管理员只能查看自己部门的记录
    if ("DEPARTMENT_ADMIN".equals(adminUser.getRole()) && !adminUser.getDepartmentId().equals(appointment.getOfficialVisitDepartmentId())) {
         response.sendError(HttpServletResponse.SC_FORBIDDEN, "您没有权限查看此记录");
        return;
    }
%>

<html>
<head>
    <title>公务预约详情</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4; }
        .container { width: 80%; margin: 20px auto; background-color: #fff; padding: 30px; box-shadow: 0 0 15px rgba(0,0,0,0.1); border-radius: 8px; }
        h1 { color: #333; border-bottom: 2px solid #007bff; padding-bottom: 10px; margin-bottom: 20px; }
        .detail-grid { display: grid; grid-template-columns: 1fr 2fr; gap: 15px 20px; margin-bottom: 25px; }
        .detail-grid strong { font-weight: bold; color: #555; text-align: right; }
        .detail-grid span { background-color: #f8f9fa; padding: 8px; border-radius: 4px; border: 1px solid #eee; }
        .status { padding: 5px 10px; border-radius: 5px; font-size: 14px; font-weight: bold; color: white; text-transform: capitalize; }
        .status-pending_approval, .status-pending { background-color: #ffc107; }
        .status-approved { background-color: #28a745; }
        .status-rejected { background-color: #dc3545; }
        .status-cancelled { background-color: #6c757d; }
        .status-completed { background-color: #17a2b8; }
        .status-expired { background-color: #6c757d; }
        .section-title { font-size: 1.2em; font-weight: bold; color: #007bff; margin-top: 30px; margin-bottom: 15px; border-bottom: 1px solid #ddd; padding-bottom: 5px; }
        table { width: 100%; border-collapse: collapse; margin-top: 10px; }
        th, td { border: 1px solid #ddd; padding: 10px; text-align: left; font-size: 14px; }
        th { background-color: #f2f2f2; font-weight: bold; }
        .action-buttons { margin-top: 30px; text-align: center; }
        .action-buttons a, .action-buttons button { padding: 12px 25px; border-radius: 5px; text-decoration: none; font-size: 16px; margin: 0 10px; border: none; cursor: pointer; }
        .btn-approve { background-color: #28a745; color: white; }
        .btn-reject { background-color: #dc3545; color: white; }
        .btn-back { background-color: #6c757d; color: white; }
        .message { padding: 15px; margin-bottom: 20px; border-radius: 5px; font-size: 16px; }
        .success-message { background-color: #d4edda; color: #155724; border: 1px solid #c3e6cb; }
        .error-message { background-color: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }
        .form-group { margin-bottom: 15px; }
        .form-group label { display: block; margin-bottom: 5px; font-weight: bold; color: #555; }
        .form-group textarea { width: 100%; padding: 10px; border: 1px solid #ccc; border-radius: 4px; box-sizing: border-box; }
    </style>
</head>
<body>

<div class="container">
    <c:if test="${not empty sessionScope.message}">
        <div class="message success-message">${sessionScope.message}</div>
        <c:remove var="message" scope="session" />
    </c:if>

    <h1>公务预约详情</h1>

    <div class="detail-grid">
        <strong>预约ID:</strong> <span>${appointment.appointmentId}</span>
        <strong>申请日期:</strong> <span><fmt:formatDate value="${appointment.applicationDate}" pattern="yyyy-MM-dd HH:mm:ss"/></span>
        <strong>预约入校时间:</strong> <span><fmt:formatDate value="${appointment.entryDatetime}" pattern="yyyy-MM-dd HH:mm:ss"/></span>
        <strong>校区:</strong> <span>${appointment.campus}</span>
        <strong>状态:</strong> 
        <span>
            <span class="status status-${fn:toLowerCase(appointment.status)}">
                <c:choose>
                    <c:when test="${appointment.status == 'PENDING_APPROVAL'}">待审核</c:when>
                    <c:when test="${appointment.status == 'APPROVED'}">已通过</c:when>
                    <c:when test="${appointment.status == 'REJECTED'}">已驳回</c:when>
                    <c:when test="${appointment.status == 'CANCELLED'}">已取消</c:when>
                    <c:when test="${appointment.status == 'COMPLETED'}">已完成</c:when>
                    <c:when test="${appointment.status == 'EXPIRED'}">已过期</c:when>
                    <c:otherwise>${appointment.status}</c:otherwise>
                </c:choose>
            </span>
        </span>
    </div>

    <div class="section-title">申请人信息</div>
    <div class="detail-grid">
        <strong>姓名:</strong> <span>${appointment.applicantName}</span>
        <strong>身份证号:</strong> <span>${appointment.applicantIdCard}</span>
        <strong>手机号:</strong> <span>${appointment.applicantPhone}</span>
        <strong>工作单位:</strong> <span>${appointment.applicantOrganization}</span>
    </div>

    <div class="section-title">访问信息</div>
    <div class="detail-grid">
        <strong>访问部门:</strong> 
        <span>
             <c:forEach var="dept" items="${departments}">
                <c:if test="${dept.departmentId == appointment.officialVisitDepartmentId}">${dept.departmentName}</c:if>
            </c:forEach>
        </span>
        <strong>接待人:</strong> <span>${appointment.officialVisitContactPerson}</span>
        <strong>事由:</strong> <span>${appointment.visitReason}</span>
        <strong>交通方式:</strong> <span>${appointment.transportMode}</span>
        <strong>车牌号:</strong> <span>${empty appointment.licensePlate ? '无' : appointment.licensePlate}</span>
    </div>

    <c:if test="${not empty appointment.accompanyingPersons}">
        <div class="section-title">随行人员</div>
        <table>
            <thead>
                <tr>
                    <th>姓名</th>
                    <th>身份证号</th>
                    <th>手机号</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="person" items="${appointment.accompanyingPersons}">
                    <tr>
                        <td>${person.name}</td>
                        <td>${person.idCard}</td>
                        <td>${person.phone}</td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:if>

    <c:if test="${appointment.status == 'PENDING_APPROVAL'}">
        <div class="section-title">审核操作</div>
        <form id="approvalForm" action="${pageContext.request.contextPath}/admin/officialAppointmentManagement" method="post">
            <input type="hidden" name="appointmentId" value="${appointment.appointmentId}">
            <input type="hidden" id="actionInput" name="action" value="">

            <div class="form-group">
                <label for="rejectionReason">驳回理由 (如果驳回，请填写):</label>
                <textarea id="rejectionReason" name="rejectionReason" rows="3" class="form-control"></textarea>
            </div>

            <div class="action-buttons">
                <button type="button" class="btn-approve" onclick="submitApproval('approve')">通过</button>
                <button type="button" class="btn-reject" onclick="submitApproval('reject')">驳回</button>
                <a href="javascript:history.back()" class="btn-back">返回</a>
            </div>
        </form>

        <script>
            function submitApproval(action) {
                if (action === 'reject' && document.getElementById('rejectionReason').value.trim() === '') {
                    alert('请填写驳回理由。');
                    return;
                }
                if (confirm('确认' + (action === 'approve' ? '通过' : '驳回') + '此预约吗？')) {
                    document.getElementById('actionInput').value = action;
                    document.getElementById('approvalForm').submit();
                }
            }
        </script>
    </c:if>

    <c:if test="${appointment.status != 'PENDING_APPROVAL'}">
        <div class="action-buttons">
            <a href="javascript:history.back()" class="btn-back">返回</a>
        </div>
    </c:if>

</div>

</body>
</html>
