<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<html>
<head>
    <title>我的预约</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background-color: #f4f4f4; color: #333; }
        .container { background-color: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0,0,0,0.1); }
        h2 { color: #333; border-bottom: 2px solid #4CAF50; padding-bottom: 10px; }
        label { display: block; margin-top: 10px; font-weight: bold; }
        input[type="text"], input[type="submit"] {
            padding: 10px; margin-top: 5px; border: 1px solid #ddd; border-radius: 4px; box-sizing: border-box;
        }
        input[type="submit"] {
            background-color: #4CAF50; color: white; cursor: pointer; font-size: 16px; margin-top: 20px;
        }
        input[type="submit"]:hover { background-color: #45a049; }
        .error { color: red; font-size: 0.9em; }
        table { width: 100%; border-collapse: collapse; margin-top: 20px; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f0f0f0; }
        .pass-link { color: #007bff; text-decoration: none; }
        .pass-link:hover { text-decoration: underline; }
    </style>
</head>
<body>
    <div class="container">
        <h2>查询我的预约</h2>
        <form action="${pageContext.request.contextPath}/mobile/myAppointments" method="GET">
            <label for="applicantIdCard">请输入您的身份证号查询:</label>
            <input type="text" id="applicantIdCard" name="applicantIdCard" value="<c:out value='${param.applicantIdCard}'/>" required maxlength="18">
            <input type="submit" value="查询">
        </form>

        <c:if test="${not empty errorMessage}">
            <p class="error">${errorMessage}</p>
        </c:if>

        <c:if test="${not empty appointments}">
            <h3>预约记录</h3>
            <table>
                <thead>
                    <tr>
                        <th>预约ID</th>
                        <th>校区</th>
                        <th>预约时间</th>
                        <th>类型</th>
                        <th>状态</th>
                        <th>提交时间</th>
                        <th>操作</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="app" items="${appointments}">
                        <tr>
                            <td><c:out value="${app.appointmentId}"/></td>
                            <td>
                                <c:choose>
                                    <c:when test="${app.campus eq 'MAIN_CAMPUS'}">主校区</c:when>
                                    <c:when test="${app.campus eq 'NORTH_CAMPUS'}">北校区</c:when>
                                    <c:when test="${app.campus eq 'SOUTH_CAMPUS'}">南校区</c:when>
                                    <c:when test="${app.campus eq 'EAST_CAMPUS'}">东校区</c:when>
                                    <c:otherwise><c:out value="${app.campus}"/></c:otherwise>
                                </c:choose>
                            </td>
                            <td><fmt:formatDate value="${app.appointmentTime}" pattern="yyyy-MM-dd HH:mm"/></td>
                            <td>
                                <c:choose>
                                    <c:when test="${app.appointmentType eq 'PUBLIC_ACCESS'}">公开活动/个人参观</c:when>
                                    <c:when test="${app.appointmentType eq 'OFFICIAL_VISIT'}">公务来访</c:when>
                                    <c:otherwise><c:out value="${app.appointmentType}"/></c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                <c:choose>
                                    <c:when test="${app.status eq 'PENDING_APPROVAL'}">待审批</c:when>
                                    <c:when test="${app.status eq 'APPROVED'}">已批准</c:when>
                                    <c:when test="${app.status eq 'REJECTED'}">已拒绝</c:when>
                                    <c:when test="${app.status eq 'CANCELLED'}">已取消</c:when>
                                    <c:when test="${app.status eq 'EXPIRED'}">已过期</c:when>
                                     <c:when test="${app.status eq 'USED'}">已使用</c:when>
                                    <c:otherwise><c:out value="${app.status}"/></c:otherwise>
                                </c:choose>
                            </td>
                            <td><fmt:formatDate value="${app.submissionTime}" pattern="yyyy-MM-dd HH:mm:ss"/></td>
                            <td>
                                <c:if test="${app.status eq 'APPROVED'}">
                                    <a href="${pageContext.request.contextPath}/mobile/viewPass?appointmentId=${app.appointmentId}" class="pass-link">查看通行码</a>
                                </c:if>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </c:if>
        <c:if test="${empty appointments and not empty param.applicantIdCard and empty errorMessage}">
            <p>未找到与该身份证号相关的预约记录。</p>
        </c:if>
        
        <p style="margin-top: 20px;"><a href="${pageContext.request.contextPath}/mobile/makeAppointment.jsp">返回预约页面</a></p>
    </div>
</body>
</html>
