<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="com.example.webdemo.beans.User" %>
<%@ page import="java.util.Calendar" %>

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
    
    // 获取当前年份和月份
    Calendar cal = Calendar.getInstance();
    int currentYear = cal.get(Calendar.YEAR);
    int currentMonth = cal.get(Calendar.MONTH) + 1; // Calendar月份从0开始
%>

<html>
<head>
    <title>公众预约统计分析</title>
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
        h1 { color: #333; }
        
        /* 筛选表单样式 */
        .filter-container {
            background-color: #f8f9fa;
            padding: 20px;
            border-radius: 5px;
            margin-bottom: 30px;
        }
        .filter-row {
            display: flex;
            gap: 20px;
            margin-bottom: 15px;
            align-items: end;
        }
        .filter-field {
            flex: 1;
        }
        .filter-field label {
            display: block;
            margin-bottom: 5px;
            font-weight: bold;
        }
        .filter-field select, .filter-field input {
            width: 100%;
            padding: 8px;
            border: 1px solid #ddd;
            border-radius: 4px;
            box-sizing: border-box;
        }
        
        /* 统计卡片样式 */
        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 20px;
            margin-bottom: 30px;
        }
        .stat-card {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 20px;
            border-radius: 8px;
            text-align: center;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
        }
        .stat-card h3 {
            margin: 0 0 10px 0;
            font-size: 14px;
            opacity: 0.9;
        }
        .stat-card .number {
            font-size: 28px;
            font-weight: bold;
            margin: 10px 0;
        }
        
        /* 表格样式 */
        .stats-table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 20px;
            background-color: white;
        }
        .stats-table th, .stats-table td {
            border: 1px solid #ddd;
            padding: 12px;
            text-align: left;
        }
        .stats-table th {
            background-color: #f2f2f2;
            font-weight: bold;
        }
        .stats-table tr:nth-child(even) {
            background-color: #f9f9f9;
        }
        
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
        .btn-info { background-color: #17a2b8; color: white; }
        .btn:hover { opacity: 0.8; }
        
        /* 选项卡样式 */
        .tab-container {
            margin-bottom: 20px;
        }
        .tab-buttons {
            display: flex;
            border-bottom: 2px solid #dee2e6;
        }
        .tab-button {
            padding: 12px 24px;
            background: none;
            border: none;
            cursor: pointer;
            font-size: 14px;
            border-bottom: 3px solid transparent;
        }
        .tab-button.active {
            color: #007bff;
            border-bottom-color: #007bff;
            font-weight: bold;
        }
        .tab-content {
            padding: 20px 0;
        }
        
        /* 图表容器样式 */
        .chart-container {
            background-color: white;
            padding: 20px;
            border-radius: 5px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            margin-bottom: 20px;
        }
        
        /* 响应式设计 */
        @media (max-width: 768px) {
            .filter-row {
                flex-direction: column;
            }
            .stats-grid {
                grid-template-columns: 1fr;
            }
        }
    </style>
</head>
<body>
    <div class="header">
        <div class="logo">公众预约统计分析</div>
        <div>
            <a href="${pageContext.request.contextPath}/admin/publicAppointmentManagement">返回列表</a>
            <a href="${pageContext.request.contextPath}/admin/dashboard.jsp">返回控制台</a>
        </div>
    </div>

    <div class="container">
        <h1>公众预约统计分析</h1>
        
        <!-- 筛选条件 -->
        <div class="filter-container">
            <form action="${pageContext.request.contextPath}/admin/publicAppointmentManagement" method="get">
                <input type="hidden" name="action" value="statistics">
                <input type="hidden" name="type" value="${requestScope.statisticsType}">
                
                <div class="filter-row">
                    <div class="filter-field">
                        <label for="year">年份：</label>
                        <select id="year" name="year">
                            <option value="">全部年份</option>
                            <% for (int i = currentYear; i >= currentYear - 5; i--) { %>
                                <option value="<%= i %>" ${requestScope.searchYear == '<%= i %>' ? 'selected' : ''}><%= i %>年</option>
                            <% } %>
                        </select>
                    </div>
                    <div class="filter-field">
                        <label for="month">月份：</label>
                        <select id="month" name="month">
                            <option value="">全部月份</option>
                            <% for (int i = 1; i <= 12; i++) { %>
                                <option value="<%= i %>" ${requestScope.searchMonth == '<%= i %>' ? 'selected' : ''}><%= i %>月</option>
                            <% } %>
                        </select>
                    </div>
                    <div class="filter-field">
                        <label for="campus">校区：</label>
                        <input type="text" id="campus" name="campus" placeholder="输入校区名称" 
                               value="<c:out value='${requestScope.searchCampus}'/>">
                    </div>
                    <div class="filter-field">
                        <button type="submit" class="btn btn-primary">更新统计</button>
                    </div>
                </div>
            </form>
        </div>
        
        <!-- 统计类型选项卡 -->
        <div class="tab-container">
            <div class="tab-buttons">
                <button class="tab-button ${requestScope.statisticsType == 'monthly' or empty requestScope.statisticsType ? 'active' : ''}" 
                        onclick="changeStatType('monthly')">月度统计</button>
                <button class="tab-button ${requestScope.statisticsType == 'campus' ? 'active' : ''}" 
                        onclick="changeStatType('campus')">校区统计</button>
                <button class="tab-button ${requestScope.statisticsType == 'status' ? 'active' : ''}" 
                        onclick="changeStatType('status')">状态统计</button>
            </div>
        </div>
        
        <!-- 统计内容 -->
        <div class="tab-content">
            <c:choose>
                <c:when test="${requestScope.statisticsType == 'campus'}">
                    <!-- 校区统计 -->
                    <h3>各校区预约统计</h3>
                    <c:if test="${not empty requestScope.statistics.campusData}">
                        <table class="stats-table">
                            <thead>
                                <tr>
                                    <th>校区</th>
                                    <th>预约总数</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="entry" items="${requestScope.statistics.campusData}">
                                    <tr>
                                        <td>${entry.key}</td>
                                        <td>${entry.value}</td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </c:if>
                    <c:if test="${empty requestScope.statistics.campusData}">
                        <p style="text-align: center; color: #6c757d; padding: 40px;">暂无统计数据</p>
                    </c:if>
                </c:when>
                
                <c:when test="${requestScope.statisticsType == 'status'}">
                    <!-- 状态统计 -->
                    <h3>预约状态分布统计</h3>
                    <c:if test="${not empty requestScope.statistics.statusData}">
                        <div class="stats-grid">
                            <c:forEach var="entry" items="${requestScope.statistics.statusData}">
                                <div class="stat-card">
                                    <h3>
                                        <c:choose>
                                            <c:when test="${entry.key == 'PENDING_APPROVAL'}">待审批</c:when>
                                            <c:when test="${entry.key == 'APPROVED'}">已批准</c:when>
                                            <c:when test="${entry.key == 'REJECTED'}">已拒绝</c:when>
                                            <c:when test="${entry.key == 'CANCELLED'}">已取消</c:when>
                                            <c:when test="${entry.key == 'COMPLETED'}">已完成</c:when>
                                            <c:when test="${entry.key == 'EXPIRED'}">已过期</c:when>
                                            <c:otherwise>${entry.key}</c:otherwise>
                                        </c:choose>
                                    </h3>
                                    <div class="number">${entry.value}</div>
                                    <p>条预约记录</p>
                                </div>
                            </c:forEach>
                        </div>
                    </c:if>
                    <c:if test="${empty requestScope.statistics.statusData}">
                        <p style="text-align: center; color: #6c757d; padding: 40px;">暂无统计数据</p>
                    </c:if>
                </c:when>
                
                <c:otherwise>
                    <!-- 月度统计 -->
                    <h3>月度预约统计</h3>
                    <c:if test="${not empty requestScope.statistics.monthlyData}">
                        <table class="stats-table">
                            <thead>
                                <tr>
                                    <th>日期</th>
                                    <th>预约总数</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="entry" items="${requestScope.statistics.monthlyData}">
                                    <tr>
                                        <td>${entry.key}</td>
                                        <td>${entry.value}</td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </c:if>
                    <c:if test="${empty requestScope.statistics.monthlyData}">
                        <p style="text-align: center; color: #6c757d; padding: 40px;">暂无统计数据</p>
                    </c:if>
                </c:otherwise>
            </c:choose>
        </div>
        
        <!-- 操作按钮 -->
        <div style="text-align: center; margin-top: 30px;">
            <button onclick="window.print()" class="btn btn-info">打印统计报告</button>
            <a href="${pageContext.request.contextPath}/admin/publicAppointmentManagement" class="btn btn-secondary">返回预约列表</a>
        </div>
    </div>

    <script>
        function changeStatType(type) {
            // 构建新的URL
            const params = new URLSearchParams(window.location.search);
            params.set('action', 'statistics');
            params.set('type', type);
            
            // 跳转到新URL
            window.location.href = '${pageContext.request.contextPath}/admin/publicAppointmentManagement?' + params.toString();
        }
        
        // 添加一些交互效果
        document.addEventListener('DOMContentLoaded', function() {
            // 为统计卡片添加悬停效果
            const statCards = document.querySelectorAll('.stat-card');
            statCards.forEach(card => {
                card.addEventListener('mouseenter', function() {
                    this.style.transform = 'translateY(-5px)';
                    this.style.transition = 'transform 0.3s ease';
                });
                card.addEventListener('mouseleave', function() {
                    this.style.transform = 'translateY(0)';
                });
            });
        });
    </script>
</body>
</html>
