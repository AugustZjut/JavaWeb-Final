<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="com.example.webdemo.beans.User" %>

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
                        adminUser.isCanManagePublicAppointments());
    
    if (!hasAccess) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "您没有权限访问此功能");
        return;
    }
%>

<html>
<head>
    <title>公众预约详情</title>
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
        .container { 
            width: 90%; 
            max-width: 900px;
            margin: 20px auto; 
            background-color: #fff; 
            padding: 30px; 
            box-shadow: 0 0 10px rgba(0,0,0,0.1); 
            border-radius: 8px;
        }
        h1 { color: #333; margin-bottom: 30px; }
        
        .detail-section {
            margin-bottom: 30px;
            padding: 20px;
            border: 1px solid #e9ecef;
            border-radius: 5px;
            background-color: #f8f9fa;
        }
        .detail-section h3 {
            margin-top: 0;
            margin-bottom: 20px;
            color: #495057;
            border-bottom: 2px solid #007bff;
            padding-bottom: 10px;
        }
        .detail-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 20px;
        }
        .detail-item {
            display: flex;
            flex-direction: column;
        }
        .detail-item label {
            font-weight: bold;
            color: #495057;
            margin-bottom: 5px;
        }
        .detail-item .value {
            color: #212529;
            padding: 8px 12px;
            background-color: white;
            border: 1px solid #ced4da;
            border-radius: 4px;
            min-height: 20px;
        }
        .detail-item.full-width {
            grid-column: 1 / -1;
        }
        
        /* 状态样式 */
        .status {
            padding: 6px 12px;
            border-radius: 4px;
            font-size: 14px;
            font-weight: bold;
            display: inline-block;
        }
        .status-pending { background-color: #fff3cd; color: #856404; }
        .status-approved { background-color: #d4edda; color: #155724; }
        .status-rejected { background-color: #f8d7da; color: #721c24; }
        .status-cancelled { background-color: #f1f1f1; color: #6c757d; }
        .status-completed { background-color: #cff4fc; color: #055160; }
        .status-expired { background-color: #e2e3e5; color: #383d41; }
        
        /* 按钮样式 */
        .btn {
            padding: 10px 20px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            text-decoration: none;
            display: inline-block;
            font-size: 14px;
            margin: 5px;
        }
        .btn-primary { background-color: #007bff; color: white; }
        .btn-secondary { background-color: #6c757d; color: white; }
        .btn:hover { opacity: 0.8; }
        
        .action-buttons {
            text-align: center;
            margin-top: 30px;
            padding-top: 20px;
            border-top: 1px solid #dee2e6;
        }
        
        /* 消息样式 */
        .message {
            padding: 15px;
            margin-bottom: 20px;
            border-radius: 4px;
        }
        .error-message { background-color: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }
        
        /* QR码样式 */
        .qr-code {
            text-align: center;
            padding: 20px;
        }
        .qr-code img {
            border: 1px solid #ddd;
            padding: 10px;
            background-color: white;
        }
        
        @media (max-width: 768px) {
            .detail-grid {
                grid-template-columns: 1fr;
            }
            .container {
                width: 95%;
                padding: 20px;
            }
        }
    </style>
</head>
<body>
    <div class="header">
        <div class="logo">公众预约详情</div>
        <div>
            <a href="${pageContext.request.contextPath}/admin/publicAppointmentManagement">返回列表</a>
            <a href="${pageContext.request.contextPath}/admin/dashboard.jsp">返回控制台</a>
        </div>
    </div>

    <div class="container">
        <c:if test="${not empty requestScope.error}">
            <div class="message error-message">${requestScope.error}</div>
        </c:if>
        
        <c:if test="${not empty requestScope.appointment}">
            <h1>预约详情 - ID: ${requestScope.appointment.appointmentId}</h1>
            
            <!-- 基本信息 -->
            <div class="detail-section">
                <h3>基本信息</h3>
                <div class="detail-grid">
                    <div class="detail-item">
                        <label>预约ID：</label>
                        <div class="value">${requestScope.appointment.appointmentId}</div>
                    </div>
                    <div class="detail-item">
                        <label>预约类型：</label>
                        <div class="value">公众预约</div>
                    </div>
                    <div class="detail-item">
                        <label>申请日期：</label>
                        <div class="value">
                            <fmt:formatDate value="${requestScope.appointment.applicationDate}" pattern="yyyy年MM月dd日 HH:mm:ss"/>
                        </div>
                    </div>
                    <div class="detail-item">
                        <label>当前状态：</label>
                        <div class="value">
                            <span class="status status-${requestScope.appointment.status.toLowerCase().replace('_', '-')}">
                                <c:choose>
                                    <c:when test="${requestScope.appointment.status == 'PENDING_APPROVAL'}">待审批</c:when>
                                    <c:when test="${requestScope.appointment.status == 'APPROVED'}">已批准</c:when>
                                    <c:when test="${requestScope.appointment.status == 'REJECTED'}">已拒绝</c:when>
                                    <c:when test="${requestScope.appointment.status == 'CANCELLED'}">已取消</c:when>
                                    <c:when test="${requestScope.appointment.status == 'COMPLETED'}">已完成</c:when>
                                    <c:when test="${requestScope.appointment.status == 'EXPIRED'}">已过期</c:when>
                                    <c:otherwise>${requestScope.appointment.status}</c:otherwise>
                                </c:choose>
                            </span>
                        </div>
                    </div>
                </div>
            </div>
            
            <!-- 预约信息 -->
            <div class="detail-section">
                <h3>预约信息</h3>
                <div class="detail-grid">
                    <div class="detail-item">
                        <label>预约校区：</label>
                        <div class="value">${requestScope.appointment.campus}</div>
                    </div>
                    <div class="detail-item">
                        <label>预约进校时间：</label>
                        <div class="value">
                            <fmt:formatDate value="${requestScope.appointment.entryDatetime}" pattern="yyyy年MM月dd日 HH:mm"/>
                        </div>
                    </div>
                    <div class="detail-item">
                        <label>交通方式：</label>
                        <div class="value">
                            <c:choose>
                                <c:when test="${not empty requestScope.appointment.transportMode}">
                                    ${requestScope.appointment.transportMode}
                                </c:when>
                                <c:otherwise>未填写</c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                    <div class="detail-item">
                        <label>车牌号码：</label>
                        <div class="value">
                            <c:choose>
                                <c:when test="${not empty requestScope.appointment.licensePlate}">
                                    ${requestScope.appointment.licensePlate}
                                </c:when>
                                <c:otherwise>无</c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                    <div class="detail-item full-width">
                        <label>来访事由：</label>
                        <div class="value">
                            <c:choose>
                                <c:when test="${not empty requestScope.appointment.visitReason}">
                                    ${requestScope.appointment.visitReason}
                                </c:when>
                                <c:otherwise>未填写</c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </div>
            </div>
            
            <!-- 申请人信息 -->
            <div class="detail-section">
                <h3>申请人信息</h3>
                <div class="detail-grid">
                    <div class="detail-item">
                        <label>姓名：</label>
                        <div class="value">${requestScope.appointment.applicantName}</div>
                    </div>
                    <div class="detail-item">
                        <label>所在单位：</label>
                        <div class="value">
                            <c:choose>
                                <c:when test="${not empty requestScope.appointment.applicantOrganization}">
                                    ${requestScope.appointment.applicantOrganization}
                                </c:when>
                                <c:otherwise>未填写</c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                    <div class="detail-item">
                        <label>身份证号：</label>
                        <div class="value">${requestScope.appointment.applicantIdCard}</div>
                    </div>
                    <div class="detail-item">
                        <label>联系电话：</label>
                        <div class="value">${requestScope.appointment.applicantPhone}</div>
                    </div>
                </div>
            </div>
            
            <!-- 随行人员信息 -->
            <c:if test="${not empty requestScope.accompanyingPersons}">
                <div class="detail-section">
                    <h3>随行人员信息</h3>
                    <table style="width: 100%; border-collapse: collapse;">
                        <thead>
                            <tr style="background-color: #f1f3f4;">
                                <th style="padding: 10px; border: 1px solid #ddd;">姓名</th>
                                <th style="padding: 10px; border: 1px solid #ddd;">身份证号</th>
                                <th style="padding: 10px; border: 1px solid #ddd;">联系电话</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="person" items="${requestScope.accompanyingPersons}">
                                <tr>
                                    <td style="padding: 10px; border: 1px solid #ddd;">${person.name}</td>
                                    <td style="padding: 10px; border: 1px solid #ddd;">${person.idCard}</td>
                                    <td style="padding: 10px; border: 1px solid #ddd;">${person.phone}</td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:if>
            
            <!-- 审批信息 -->
            <c:if test="${requestScope.appointment.status == 'APPROVED' or requestScope.appointment.status == 'REJECTED'}">
                <div class="detail-section">
                    <h3>审批信息</h3>
                    <div class="detail-grid">
                        <div class="detail-item">
                            <label>审批时间：</label>
                            <div class="value">
                                <c:choose>
                                    <c:when test="${not empty requestScope.appointment.approvalDatetime}">
                                        <fmt:formatDate value="${requestScope.appointment.approvalDatetime}" pattern="yyyy年MM月dd日 HH:mm:ss"/>
                                    </c:when>
                                    <c:otherwise>-</c:otherwise>
                                </c:choose>
                            </div>
                        </div>
                        <div class="detail-item">
                            <label>审批人ID：</label>
                            <div class="value">
                                <c:choose>
                                    <c:when test="${not empty requestScope.appointment.approvedByUserId}">
                                        ${requestScope.appointment.approvedByUserId}
                                    </c:when>
                                    <c:otherwise>-</c:otherwise>
                                </c:choose>
                            </div>
                        </div>
                    </div>
                </div>
            </c:if>
            
            <!-- 通行码信息 -->
            <c:if test="${requestScope.appointment.status == 'APPROVED' and not empty requestScope.appointment.qrCodeData}">
                <div class="detail-section">
                    <h3>通行码信息</h3>
                    <div class="detail-grid">
                        <div class="detail-item">
                            <label>生成时间：</label>
                            <div class="value">
                                <fmt:formatDate value="${requestScope.appointment.qrCodeGeneratedAt}" pattern="yyyy年MM月dd日 HH:mm:ss"/>
                            </div>
                        </div>
                        <div class="detail-item full-width qr-code">
                            <label>通行码：</label>
                            <div style="margin-top: 10px;">
                                <!-- 这里可以显示QR码图片，如果有的话 -->
                                <div style="padding: 20px; background-color: #f8f9fa; border: 1px solid #dee2e6; border-radius: 4px;">
                                    <p>QR码数据：${requestScope.appointment.qrCodeData}</p>
                                    <small style="color: #6c757d;">请使用此信息在校门口验证通行</small>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </c:if>
            
            <!-- 操作按钮 -->
            <div class="action-buttons">
                <a href="${pageContext.request.contextPath}/admin/publicAppointmentManagement" class="btn btn-secondary">返回列表</a>
                <c:if test="${requestScope.appointment.status == 'APPROVED'}">
                    <button onclick="window.print()" class="btn btn-primary">打印详情</button>
                </c:if>
            </div>
        </c:if>
        
        <c:if test="${empty requestScope.appointment}">
            <div class="message error-message">
                未找到指定的预约记录，请检查预约ID是否正确。
            </div>
            <div class="action-buttons">
                <a href="${pageContext.request.contextPath}/admin/publicAppointmentManagement" class="btn btn-secondary">返回列表</a>
            </div>
        </c:if>
    </div>
</body>
</html>
