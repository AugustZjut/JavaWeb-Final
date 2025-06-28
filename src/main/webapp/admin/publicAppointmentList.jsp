<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ page import="com.example.webdemo.util.DataMaskingUtils" %>
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
    <title>公众预约管理</title>
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
            width: 95%; 
            margin: 20px auto; 
            background-color: #fff; 
            padding: 20px; 
            box-shadow: 0 0 10px rgba(0,0,0,0.1); 
        }
        h1 { color: #333; margin-bottom: 20px; }
        
        /* 错误消息样式 */
        .error-message {
            background-color: #f8d7da;
            color: #721c24;
            border: 1px solid #f5c6cb;
            padding: 15px;
            border-radius: 4px;
            margin-bottom: 20px;
        }
        
        /* 表格样式 */
        table { 
            width: 100%; 
            border-collapse: collapse; 
            margin-top: 20px; 
            box-shadow: 0 0 10px rgba(0,0,0,0.1); 
            background-color: #fff; 
        }
        th, td { 
            border: 1px solid #ddd; 
            padding: 12px; 
            text-align: left; 
        }
        th { 
            background-color: #007bff; 
            color: white; 
            font-weight: bold;
        }
        tr:nth-child(even) { 
            background-color: #f9f9f9; 
        }
        tr:hover { 
            background-color: #f1f1f1; 
        }
        
        /* 链接样式 */
        a { 
            color: #007bff; 
            text-decoration: none; 
        }
        a:hover { 
            text-decoration: underline; 
        }
        
        /* 状态样式 */
        .status {
            padding: 4px 8px;
            border-radius: 4px;
            font-size: 12px;
            font-weight: bold;
            display: inline-block;
        }
        .status-pending { background-color: #fff3cd; color: #856404; }
        .status-approved { background-color: #d4edda; color: #155724; }
        .status-rejected { background-color: #f8d7da; color: #721c24; }
        .status-cancelled { background-color: #f1f1f1; color: #6c757d; }
        .status-completed { background-color: #cff4fc; color: #055160; }
        .status-expired { background-color: #e2e3e5; color: #383d41; }
        
        /* 空数据提示 */
        .no-data {
            text-align: center;
            color: #6c757d;
            font-style: italic;
            padding: 40px;
        }
        
        .search-form {
            margin-bottom: 20px;
            padding: 20px;
            background-color: #f8f9fa;
            border: 1px solid #dee2e6;
            border-radius: 5px;
            display: flex;
            flex-wrap: wrap;
            gap: 15px;
            align-items: center;
        }
        .search-form .form-group {
            display: flex;
            flex-direction: column;
        }
        .search-form label {
            margin-bottom: 5px;
            font-weight: bold;
            font-size: 14px;
        }
        .search-form input[type="text"], .search-form input[type="date"], .search-form select {
            padding: 8px;
            border: 1px solid #ddd;
            border-radius: 4px;
            font-size: 14px;
        }
        .search-form .btn-group {
            align-self: flex-end;
        }
        .search-form input[type="submit"], .search-form a {
            padding: 8px 15px;
            background-color: #007bff;
            color: white;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            text-decoration: none;
            font-size: 14px;
            display: inline-block;
            text-align: center;
        }
        .search-form input[type="submit"]:hover { 
            background-color: #0056b3; 
        }
        .search-form a.clear-btn {
            background-color: #6c757d;
        }
        .search-form a.clear-btn:hover {
            background-color: #5a6268;
        }
    </style>
</head>
<body>
    <div class="header">
        <div class="logo">公众预约管理</div>
        <div>
            <a href="${pageContext.request.contextPath}/admin/publicAppointmentManagement?action=statistics">统计分析</a>
            <a href="${pageContext.request.contextPath}/admin/dashboard.jsp">返回控制台</a>
        </div>
    </div>

    <div class="container">
        <h1>公众预约列表</h1>
        
        <!-- 显示错误消息 -->
        <c:if test="${not empty requestScope.error}">
            <div class="error-message">${requestScope.error}</div>
        </c:if>

        <%-- 搜索表单 --%>
        <form action="${pageContext.request.contextPath}/admin/publicAppointmentManagement" method="get" class="search-form">
            <input type="hidden" name="action" value="search">
            <div class="form-group">
                <label for="applicationDateStart">申请日期 (起)</label>
                <input type="date" id="applicationDateStart" name="applicationDateStart" value="${param.applicationDateStart}">
            </div>
            <div class="form-group">
                <label for="applicationDateEnd">申请日期 (止)</label>
                <input type="date" id="applicationDateEnd" name="applicationDateEnd" value="${param.applicationDateEnd}">
            </div>
            <div class="form-group">
                <label for="appointmentDateStart">预约日期 (起)</label>
                <input type="date" id="appointmentDateStart" name="appointmentDateStart" value="${param.appointmentDateStart}">
            </div>
            <div class="form-group">
                <label for="appointmentDateEnd">预约日期 (止)</label>
                <input type="date" id="appointmentDateEnd" name="appointmentDateEnd" value="${param.appointmentDateEnd}">
            </div>
            <div class="form-group">
                <label for="campus">校区</label>
                <select id="campus" name="campus">
                    <option value="">全部</option>
                    <option value="校本部" ${param.campus == '校本部' ? 'selected' : ''}>校本部</option>
                    <option value="东校区" ${param.campus == '东校区' ? 'selected' : ''}>东校区</option>
                    <option value="南校区" ${param.campus == '南校区' ? 'selected' : ''}>南校区</option>
                </select>
            </div>
            <div class="form-group">
                <label for="applicantOrganization">所在单位</label>
                <input type="text" id="applicantOrganization" name="applicantOrganization" value="${param.applicantOrganization}" placeholder="输入申请人所在单位">
            </div>
            <div class="form-group">
                <label for="applicantName">姓名</label>
                <input type="text" id="applicantName" name="applicantName" value="${param.applicantName}" placeholder="输入申请人姓名">
            </div>
            <div class="form-group">
                <label for="idCard">身份证号</label>
                <input type="text" id="idCard" name="idCard" value="${param.idCard}" placeholder="输入申请人身份证号">
            </div>
            <div class="form-group">
                <label for="status">审核状态</label>
                <select id="status" name="status">
                    <option value="">全部</option>
                    <option value="PENDING_APPROVAL" ${param.status == 'PENDING_APPROVAL' ? 'selected' : ''}>待审核</option>
                    <option value="APPROVED" ${param.status == 'APPROVED' ? 'selected' : ''}>已通过</option>
                    <option value="REJECTED" ${param.status == 'REJECTED' ? 'selected' : ''}>已驳回</option>
                    <option value="CANCELLED" ${param.status == 'CANCELLED' ? 'selected' : ''}>已取消</option>
                    <option value="COMPLETED" ${param.status == 'COMPLETED' ? 'selected' : ''}>已完成</option>
                    <option value="EXPIRED" ${param.status == 'EXPIRED' ? 'selected' : ''}>已过期</option>
                </select>
            </div>
            <div class="btn-group">
                <input type="submit" value="查询">
                <a href="${pageContext.request.contextPath}/admin/publicAppointmentManagement?action=list" class="clear-btn">重置</a>
                <a href="${pageContext.request.contextPath}/admin/publicAppointmentManagement?action=statistics" class="clear-btn" style="background-color: #28a745;">统计</a>
            </div>
        </form>

    <c:if test="${not empty requestScope.message}">
        <p style="color: green;">${requestScope.message}</p>
    </c:if>
    <c:if test="${not empty requestScope.error}">
        <p style="color: red;">${requestScope.error}</p>
    </c:if>

        <table>
            <thead>
                <tr>
                    <th>预约ID</th>
                    <th>校区</th>
                    <th>入校时间</th>
                    <th>所在单位</th>
                    <th>申请人姓名</th>
                    <th>身份证号</th>
                    <th>联系电话</th>
                    <th>状态</th>
                    <th>申请时间</th>
                    <th>操作</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="app" items="${appointments}">
                    <tr>
                        <td><c:out value="${app.appointmentId}"/></td>
                        <td><c:out value="${app.campus}"/></td>
                        <td><fmt:formatDate value="${app.entryDatetime}" pattern="yyyy年MM月dd日 HH:mm"/></td>
                        <td><c:out value="${app.applicantOrganization}"/></td>
                        <td><c:out value="${app.applicantName}"/></td>
                        <td><c:out value="${app.applicantIdCard}"/></td>
                        <td><c:out value="${app.applicantPhone}"/></td>
                        <td>
                            <span class="status status-${fn:toLowerCase(fn:replace(app.status, '_', '-'))}">
                                <c:choose>
                                    <c:when test="${app.status == 'PENDING_APPROVAL'}">待审批</c:when>
                                    <c:when test="${app.status == 'APPROVED'}">已批准</c:when>
                                    <c:when test="${app.status == 'REJECTED'}">已拒绝</c:when>
                                    <c:when test="${app.status == 'CANCELLED'}">已取消</c:when>
                                    <c:when test="${app.status == 'COMPLETED'}">已完成</c:when>
                                    <c:when test="${app.status == 'EXPIRED'}">已过期</c:when>
                                    <c:otherwise><c:out value="${app.status}"/></c:otherwise>
                                </c:choose>
                            </span>
                        </td>
                        <td><fmt:formatDate value="${app.applicationDate}" pattern="yyyy年MM月dd日 HH:mm"/></td>
                        <td>
                            <a href="${pageContext.request.contextPath}/admin/publicAppointmentManagement?action=view&id=${app.appointmentId}">查看详情</a>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty appointments}">
                    <tr>
                        <td colspan="10" class="no-data">暂无公众预约记录</td>
                    </tr>
                </c:if>
            </tbody>
        </table>
    </div>
</body>
</html>
