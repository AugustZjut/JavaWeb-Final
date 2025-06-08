<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<html>
<head>
    <title>访客通行码</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4; display: flex; justify-content: center; align-items: center; min-height: 100vh; }
        .pass-container {
            background-color: #fff;
            padding: 30px;
            border-radius: 10px;
            box-shadow: 0 5px 15px rgba(0,0,0,0.2);
            text-align: center;
            width: 350px; /* Adjust as needed */
        }
        .pass-container.valid {
            border-top: 10px solid purple; /* Purple for valid */
        }
        .pass-container.invalid {
            border-top: 10px solid gray; /* Gray for invalid */
        }
        .qr-code img {
            max-width: 80%;
            height: auto;
            margin: 20px 0;
            border: 1px solid #eee;
        }
        h2 {
            color: #333;
            margin-bottom: 10px;
        }
        p { color: #555; line-height: 1.6; margin: 8px 0; }
        .info-label { font-weight: bold; }
        .footer-links { margin-top: 30px; }
        .footer-links a { color: #007bff; text-decoration: none; margin: 0 10px; }
        .footer-links a:hover { text-decoration: underline; }
        .error { color: red; font-weight: bold; }
    </style>
</head>
<body>
    <c:choose>
        <c:when test="${not empty appointment}">
            <div class="pass-container ${appointment.status eq 'APPROVED' ? 'valid' : 'invalid'}">
                <h2>访客通行码</h2>
                <c:if test="${appointment.status ne 'APPROVED'}">
                    <p class="error">注意：此通行码当前无效 (状态: 
                        <c:choose>
                            <c:when test="${appointment.status eq 'PENDING_APPROVAL'}">待审批</c:when>
                            <c:when test="${appointment.status eq 'REJECTED'}">已拒绝</c:when>
                            <c:when test="${appointment.status eq 'CANCELLED'}">已取消</c:when>
                            <c:when test="${appointment.status eq 'EXPIRED'}">已过期</c:when>
                            <c:when test="${appointment.status eq 'USED'}">已使用</c:when>
                            <c:otherwise><c:out value="${appointment.status}"/></c:otherwise>
                        </c:choose>
                    )</p>
                </c:if>

                <div class="qr-code">
                    <c:if test="${not empty qrCodeBase64}">
                        <img src="data:image/png;base64,${qrCodeBase64}" alt="通行二维码">
                    </c:if>
                    <c:if test="${empty qrCodeBase64 and not empty qrError}">
                        <p class="error">二维码生成失败: <c:out value="${qrError}"/></p>
                    </c:if>
                </div>

                <p><span class="info-label">访客姓名:</span> <c:out value="${maskedApplicantName}"/></p>
                <p><span class="info-label">身份证号:</span> <c:out value="${maskedApplicantIdCard}"/></p>
                <p><span class="info-label">预约校区:</span> 
                     <c:choose>
                        <c:when test="${appointment.campus eq 'MAIN_CAMPUS'}">主校区</c:when>
                        <c:when test="${appointment.campus eq 'NORTH_CAMPUS'}">北校区</c:when>
                        <c:when test="${appointment.campus eq 'SOUTH_CAMPUS'}">南校区</c:when>
                        <c:when test="${appointment.campus eq 'EAST_CAMPUS'}">东校区</c:when>
                        <c:otherwise><c:out value="${appointment.campus}"/></c:otherwise>
                    </c:choose>
                </p>
                <p><span class="info-label">预约时间:</span> <fmt:formatDate value="${appointment.appointmentTime}" pattern="yyyy-MM-dd HH:mm"/></p>
                <p><span class="info-label">通行码生成时间:</span> <fmt:formatDate value="${generationTime}" pattern="yyyy-MM-dd HH:mm:ss"/></p>
                
                <c:if test="${not empty accompanyingPersons}">
                    <h4>随行人员:</h4>
                    <c:forEach var="person" items="${accompanyingPersons}">
                        <p style="font-size:0.9em;">
                            姓名: <c:out value="${person.maskedName}"/>, 
                            身份证: <c:out value="${person.maskedIdCard}"/>
                        </p>
                    </c:forEach>
                </c:if>

            </div>
        </c:when>
        <c:otherwise>
            <div class="pass-container invalid">
                 <h2>无法加载通行码</h2>
                <p class="error">
                    <c:choose>
                        <c:when test="${not empty errorMessage}">
                            <c:out value="${errorMessage}"/>
                        </c:when>
                        <c:otherwise>
                            未能找到指定的预约信息，或者预约ID无效。
                        </c:otherwise>
                    </c:choose>
                </p>
            </div>
        </c:otherwise>
    </c:choose>
    <div style="position: fixed; bottom: 20px; text-align: center; width:100%;">
         <a href="${pageContext.request.contextPath}/mobile/myAppointments?applicantIdCard=${originalApplicantIdCard}" style="color: #007bff; text-decoration: none;">返回我的预约</a>
    </div>
</body>
</html>
