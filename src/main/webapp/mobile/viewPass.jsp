<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<html>
<head>
    <title>访客通行码</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        body { font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4; display: flex; justify-content: center; align-items: center; min-height: 100vh; }
        .pass-container {
            background-color: #fff;
            padding: 30px;
            border-radius: 10px;
            box-shadow: 0 5px 15px rgba(0,0,0,0.2);
            text-align: center;
            width: 350px; /* Adjust as needed */
            position: relative;
            overflow: hidden;
        }
        .pass-container.valid {
            border-top: 10px solid #4CAF50; /* Green for valid */
        }
        .pass-container.invalid {
            border-top: 10px solid #F44336; /* Red for invalid */
        }
        .status-banner {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            padding: 8px 0;
            color: white;
            font-weight: bold;
            font-size: 16px;
        }
        .status-banner.valid {
            background-color: #4CAF50;
        }
        .status-banner.invalid {
            background-color: #F44336;
        }
        .qr-code {
            margin: 60px 0 30px; /* Add extra top margin to account for status banner */
        }
        .qr-code img {
            max-width: 80%;
            height: auto;
            margin: 0 auto;
            border: 1px solid #eee;
            padding: 10px;
            background-color: white;
        }
        h2 {
            color: #333;
            margin-bottom: 10px;
        }
        p { color: #555; line-height: 1.6; margin: 8px 0; }
        .info-label { font-weight: bold; }
        .accompanying-persons {
            margin-top: 20px;
            padding: 15px;
            background-color: #f9f9f9;
            border-radius: 5px;
        }
        .footer-links { margin-top: 30px; }
        .footer-links a { color: #007bff; text-decoration: none; margin: 0 10px; }
        .footer-links a:hover { text-decoration: underline; }
        .error { color: red; font-weight: bold; }
        .return-link {
            display: inline-block;
            margin-top: 15px;
            padding: 8px 15px;
            background-color: #007bff;
            color: white;
            text-decoration: none;
            border-radius: 4px;
        }
        .return-link:hover {
            background-color: #0056b3;
        }
    </style>
</head>
<body>
    <c:choose>
        <c:when test="${not empty appointment}">
            <div class="pass-container ${isValidPass ? 'valid' : 'invalid'}">
                <div class="status-banner ${isValidPass ? 'valid' : 'invalid'}">
                    ${isValidPass ? '有效通行码' : '无效通行码'}
                </div>
                
                <h2>访客通行码</h2>
                
                <c:if test="${not isValidPass}">
                    <p class="error">
                        <c:choose>
                            <c:when test="${appointment.status eq 'PENDING_APPROVAL'}">预约正在审批中，尚未批准</c:when>
                            <c:when test="${appointment.status eq 'REJECTED'}">预约已被拒绝</c:when>
                            <c:when test="${appointment.status eq 'CANCELLED'}">预约已被取消</c:when>
                            <c:when test="${appointment.status eq 'APPROVED'}">预约日期不在当前日期，通行码无效</c:when>
                            <c:otherwise>通行码无效</c:otherwise>
                        </c:choose>
                    </p>
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
                        <c:when test="${appointment.campus eq 'MAIN_CAMPUS' || appointment.campus eq '朝晖校区'}">朝晖校区</c:when>
                        <c:when test="${appointment.campus eq 'NORTH_CAMPUS' || appointment.campus eq '屏峰校区'}">屏峰校区</c:when>
                        <c:when test="${appointment.campus eq 'SOUTH_CAMPUS' || appointment.campus eq '莫干山校区'}">莫干山校区</c:when>
                        <c:otherwise><c:out value="${appointment.campus}"/></c:otherwise>
                    </c:choose>
                </p>
                <p><span class="info-label">预约时间:</span> <fmt:formatDate value="${appointment.entryDatetime}" pattern="yyyy-MM-dd HH:mm"/></p>
                <p><span class="info-label">通行码生成时间:</span> <fmt:formatDate value="${generationTime}" pattern="yyyy-MM-dd HH:mm:ss"/></p>
                
                <c:if test="${not empty accompanyingPersons && accompanyingPersons.size() > 0}">
                    <div class="accompanying-persons">
                        <h4>随行人员:</h4>
                        <c:forEach var="person" items="${accompanyingPersons}" varStatus="status">
                            <p style="font-size:0.9em;">
                                <strong>随行人员 ${status.index+1}:</strong><br>
                                姓名: <c:out value="${person.maskedName}"/><br>
                                身份证: <c:out value="${person.maskedIdCard}"/>
                            </p>
                        </c:forEach>
                    </div>
                </c:if>
                
                <a href="${pageContext.request.contextPath}/mobile/myAppointments?applicantIdCard=${originalApplicantIdCard}" class="return-link">返回我的预约</a>
            </div>
        </c:when>
        <c:otherwise>
            <div class="pass-container invalid">
                <div class="status-banner invalid">无法加载通行码</div>
                <h2>通行码无效</h2>
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
                <a href="${pageContext.request.contextPath}/mobile/myAppointments" class="return-link">返回我的预约</a>
            </div>
        </c:otherwise>
    </c:choose>
</body>
</html>
