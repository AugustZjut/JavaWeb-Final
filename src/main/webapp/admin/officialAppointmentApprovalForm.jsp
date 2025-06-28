<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="com.example.webdemo.util.DataMaskingUtils" %>

<html>
<head>
    <title>审核公务预约</title>
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
        .form-container {
            max-width: 800px;
            margin: 0 auto;
        }
        .detail-item { margin-bottom: 15px; }
        .detail-item label { font-weight: bold; display: inline-block; width: 180px; }
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
        textarea { width: 100%; padding: 10px; margin-top:5px; border: 1px solid #ddd; border-radius: 4px; box-sizing: border-box; min-height: 80px;}
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
        <div class="logo">审核公务预约</div>
        <div>
            <a href="${pageContext.request.contextPath}/admin/officialAppointmentManagement">返回公务预约列表</a>
            <a href="${pageContext.request.contextPath}/admin/dashboard.jsp">返回控制台</a>
        </div>
    </div>

    <div class="container">
        <div class="form-container">
            <h1>审核公务预约 (ID: <c:out value="${appointment.id}"/>)</h1>

            <div class="detail-item">
                <label>校区:</label>
                <span><c:out value="${appointment.campusArea}"/></span>
            </div>
            <div class="detail-item">
                <label>预约时间:</label>
                <span><fmt:formatDate value="${appointment.appointmentTime}" pattern="yyyy-MM-dd HH:mm"/></span>
            </div>
            <div class="detail-item">
                <label>访客姓名:</label>
                <span><c:out value="${DataMaskingUtils.maskName(appointment.visitorName)}"/></span>
            </div>
            <div class="detail-item">
                <label>访客身份证:</label>
                <span><c:out value="${DataMaskingUtils.maskIdCard(appointment.visitorIdCard)}"/></span>
            </div>
            <div class="detail-item">
                <label>访客电话:</label>
                <span><c:out value="${DataMaskingUtils.maskPhone(appointment.decryptedVisitorPhone)}"/></span>
            </div>
            <div class="detail-item">
                <label>工作单位:</label>
                <span><c:out value="${appointment.visitorOrganization}"/></span>
            </div>
            <div class="detail-item">
                <label>交通方式:</label>
                <span><c:out value="${appointment.transportType}"/></span>
            </div>
            <c:if test="${not empty appointment.licensePlate}">
                <div class="detail-item">
                    <label>车牌号:</label>
                    <span><c:out value="${appointment.licensePlate}"/></span>
                </div>
            </c:if>

            <hr style="margin: 20px 0;">

            <div class="detail-item">
                <label>访问部门:</label>
                <span><c:out value="${appointment.visitDepartment}"/></span>
            </div>
            <div class="detail-item">
                <label>联系人:</label>
                <span><c:out value="${appointment.visitContactPerson}"/></span>
            </div>
            <div class="detail-item">
                <label>访问事由:</label>
                <div style="white-space: pre-wrap; margin-left:185px;"><c:out value="${appointment.visitReason}"/></div>
            </div>
            
            <c:if test="${not empty appointment.accompanyingPersons}">
                <hr style="margin: 20px 0;">
                <h3>随行人员:</h3>
                <c:forEach var="person" items="${appointment.accompanyingPersons}" varStatus="status">
                    <div class="detail-item">
                        <label>人员${status.count} 姓名:</label>
                        <span><c:out value="${DataMaskingUtils.maskName(person.name)}"/></span>
                    </div>
                    <div class="detail-item">
                        <label>人员${status.count} 身份证:</label>
                        <span><c:out value="${DataMaskingUtils.maskIdCard(person.idCard)}"/></span>
                    </div>
                     <div class="detail-item">
                        <label>人员${status.count} 电话:</label>
                        <span><c:out value="${DataMaskingUtils.maskPhone(person.decryptedPhone)}"/></span>
                    </div>
                    <br/>
                </c:forEach>
            </c:if>

            <hr style="margin: 20px 0;">
            <div class="detail-item">
                <label>当前状态:</label>
                <span style="font-weight:bold;"><c:out value="${appointment.status}"/></span>
            </div>
             <div class="detail-item">
                <label>提交时间:</label>
                <span><fmt:formatDate value="${appointment.createdAt}" pattern="yyyy-MM-dd HH:mm:ss"/></span>
            </div>

            <c:if test="${appointment.status == 'Pending' || appointment.status == 'Pending Approval'}">
                <div class="actions">
                    <form action="${pageContext.request.contextPath}/admin/officialAppointmentManagement" method="post" style="display:inline;">
                        <input type="hidden" name="action" value="approve">
                        <input type="hidden" name="appointmentId" value="${appointment.id}">
                        <button type="submit" class="button approve-button">批准</button>
                    </form>
                    <form action="${pageContext.request.contextPath}/admin/officialAppointmentManagement" method="post" style="display:inline;">
                        <input type="hidden" name="action" value="reject">
                        <input type="hidden" name="appointmentId" value="${appointment.id}">
                        <button type="submit" class="button reject-button">拒绝</button>
                    </form>
                </div>
            </c:if>

            <div style="margin-top: 20px;">
                <a href="${pageContext.request.contextPath}/admin/officialAppointmentManagement?action=list" class="button cancel-button">返回列表</a>
            </div>
        </div>
    </div>
</body>
</html>
