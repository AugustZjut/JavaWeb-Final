<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="com.example.webdemo.beans.User" %>
<%@ page import="java.util.Map" %>

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
%>

<html>
<head>
    <title>公务预约统计</title>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        body { font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4; }
        .container { width: 90%; margin: 20px auto; background-color: #fff; padding: 30px; box-shadow: 0 0 15px rgba(0,0,0,0.1); border-radius: 8px; }
        h1 { color: #333; border-bottom: 2px solid #007bff; padding-bottom: 10px; margin-bottom: 30px; text-align: center; }
        .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 30px; margin-bottom: 40px; }
        .stat-card { background-color: #f8f9fa; border: 1px solid #dee2e6; border-radius: 8px; padding: 20px; text-align: center; }
        .stat-card h3 { margin-top: 0; color: #007bff; }
        .stat-card .count { font-size: 2.5em; font-weight: bold; color: #333; }
        .charts-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 40px; }
        .chart-container { padding: 20px; border: 1px solid #ddd; border-radius: 8px; }
        .btn-back { display: inline-block; margin-top: 30px; padding: 12px 25px; background-color: #6c757d; color: white; border-radius: 5px; text-decoration: none; font-size: 16px; }
        .btn-back:hover { background-color: #5a6268; }
    </style>
</head>
<body>

<div class="container">
    <h1>公务预约统计</h1>

    <div class="stats-grid">
        <div class="stat-card">
            <h3>总预约数</h3>
            <p class="count">${statistics.totalCount}</p>
        </div>
        <div class="stat-card">
            <h3>待审核</h3>
            <p class="count">${statistics.statusCounts['PENDING_APPROVAL'] != null ? statistics.statusCounts['PENDING_APPROVAL'] : 0}</p>
        </div>
        <div class="stat-card">
            <h3>已通过</h3>
            <p class="count">${statistics.statusCounts['APPROVED'] != null ? statistics.statusCounts['APPROVED'] : 0}</p>
        </div>
        <div class="stat-card">
            <h3>已驳回</h3>
            <p class="count">${statistics.statusCounts['REJECTED'] != null ? statistics.statusCounts['REJECTED'] : 0}</p>
        </div>
    </div>

    <div class="charts-grid">
        <div class="chart-container">
            <h3>按状态分布</h3>
            <canvas id="statusChart"></canvas>
        </div>
        <div class="chart-container">
            <h3>各部门预约数量</h3>
            <canvas id="departmentChart"></canvas>
        </div>
    </div>
    
    <div style="text-align: center;">
         <a href="${pageContext.request.contextPath}/admin/officialAppointmentManagement?action=list" class="btn-back">返回列表</a>
    </div>

</div>

<script>
    // 按状态分布 - 饼图
    const statusCtx = document.getElementById('statusChart').getContext('2d');
    new Chart(statusCtx, {
        type: 'pie',
        data: {
            labels: [<c:forEach var="entry" items="${statistics.statusCounts}" varStatus="loop">'${entry.key}'<c:if test="${!loop.last}">,</c:if></c:forEach>],
            datasets: [{
                label: '预约数量',
                data: [<c:forEach var="entry" items="${statistics.statusCounts}" varStatus="loop">${entry.value}<c:if test="${!loop.last}">,</c:if></c:forEach>],
                backgroundColor: [
                    'rgba(255, 193, 7, 0.7)',
                    'rgba(40, 167, 69, 0.7)',
                    'rgba(220, 53, 69, 0.7)',
                    'rgba(108, 117, 125, 0.7)',
                    'rgba(23, 162, 184, 0.7)',
                    'rgba(226, 227, 229, 0.7)'
                ],
                borderColor: '#fff',
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            plugins: {
                legend: {
                    position: 'top',
                }
            }
        }
    });

    // 各部门预约数量 - 柱状图
    const departmentCtx = document.getElementById('departmentChart').getContext('2d');
    new Chart(departmentCtx, {
        type: 'bar',
        data: {
            labels: [<c:forEach var="entry" items="${statistics.departmentCounts}" varStatus="loop">'${entry.key}'<c:if test="${!loop.last}">,</c:if></c:forEach>],
            datasets: [{
                label: '预约数量',
                data: [<c:forEach var="entry" items="${statistics.departmentCounts}" varStatus="loop">${entry.value}<c:if test="${!loop.last}">,</c:if></c:forEach>],
                backgroundColor: 'rgba(0, 123, 255, 0.7)',
                borderColor: 'rgba(0, 123, 255, 1)',
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            plugins: {
                legend: {
                    display: false
                }
            },
            scales: {
                y: {
                    beginAtZero: true
                }
            }
        }
    });
</script>

</body>
</html>
