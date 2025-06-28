<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <title>访客预约</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background-color: #f4f4f4; color: #333; }
        .container { background-color: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0,0,0,0.1); }
        h2 { color: #333; border-bottom: 2px solid #4CAF50; padding-bottom: 10px; }
        label { display: block; margin-top: 10px; font-weight: bold; }
        input[type="text"], input[type="tel"], input[type="datetime-local"], select, textarea {
            width: calc(100% - 22px); padding: 10px; margin-top: 5px; border: 1px solid #ddd; border-radius: 4px; box-sizing: border-box;
        }
        input[type="submit"] {
            background-color: #4CAF50; color: white; padding: 10px 20px; border: none; border-radius: 4px; cursor: pointer; font-size: 16px; margin-top: 20px;
        }
        input[type="submit"]:hover { background-color: #45a049; }
        .error { color: red; font-size: 0.9em; }
        .official-fields { display: none; } /* Initially hidden */
        .accompanying-person { border: 1px solid #eee; padding: 10px; margin-top: 10px; border-radius: 4px; }
        .accompanying-person h4 { margin-top: 0; }
        button { background-color: #007bff; color: white; padding: 8px 15px; border: none; border-radius: 4px; cursor: pointer; margin-top:10px; }
        button:hover { background-color: #0056b3; }
    </style>
    <script>
        function toggleOfficialFields() {
            var appointmentType = document.getElementById("appointmentType").value;
            var officialFields = document.getElementById("officialFields");
            if (appointmentType === "OFFICIAL_VISIT") {
                officialFields.style.display = "block";
            } else {
                officialFields.style.display = "none";
            }
        }

        var nextPersonId = 1; // Used to generate unique IDs for divs and their elements

        function addAccompanyingPerson() {
            var container = document.getElementById("accompanyingPersonsContainer");
            var currentPersonDivs = container.getElementsByClassName("accompanying-person");
            var displayOrder = currentPersonDivs.length + 1;

            var div = document.createElement("div");
            div.className = "accompanying-person";
            div.id = "accompanyingPerson_" + nextPersonId; // Unique ID for the div

            // Note: The remove button now calls removeAccompanyingPerson with the unique nextPersonId
            div.innerHTML = '<h4>随行人员 ' + displayOrder + ' <button type="button" onclick="removeAccompanyingPerson(' + nextPersonId + ')">移除</button></h4>' +
                '<label for="accName_' + nextPersonId + '">姓名:</label>' +
                '<input type="text" id="accName_' + nextPersonId + '" name="accName[]">' +
                '<label for="accIdCard_' + nextPersonId + '">身份证号:</label>' +
                '<input type="text" id="accIdCard_' + nextPersonId + '" name="accIdCard[]" maxlength="18">' +
                '<label for="accPhone_' + nextPersonId + '">联系电话:</label>' +
                '<input type="tel" id="accPhone_' + nextPersonId + '" name="accPhone[]">';
            
            container.appendChild(div);
            nextPersonId++; // Increment for the next person
        }

        function removeAccompanyingPerson(personIdToRemove) {
             var personDiv = document.getElementById("accompanyingPerson_" + personIdToRemove);
             if (personDiv) {
                 personDiv.remove();
                 updateAccompanyingPersonNumbers(); // Update numbers after removal
             }
        }

        function updateAccompanyingPersonNumbers() {
            var container = document.getElementById("accompanyingPersonsContainer");
            var personDivs = container.getElementsByClassName("accompanying-person");
            for (var i = 0; i < personDivs.length; i++) {
                var h4 = personDivs[i].getElementsByTagName("h4")[0];
                if (h4) {
                    // Keep the existing button, only update the text part
                    var buttonHTML = h4.getElementsByTagName("button")[0].outerHTML;
                    h4.innerHTML = "随行人员 " + (i + 1) + " " + buttonHTML;
                }
            }
        }

        // Ensure datetime-local is polyfilled or handled for browsers that don't support it well
        window.onload = function() {
            toggleOfficialFields(); // Initial check
            // For datetime-local, set a default value or min attribute if needed
            var now = new Date();
            now.setMinutes(now.getMinutes() - now.getTimezoneOffset()); // Adjust for local timezone
            var defaultDateTime = now.toISOString().slice(0,16);
            var appointmentTimeInput = document.getElementById("appointmentTime");
            if (appointmentTimeInput) {
                appointmentTimeInput.min = defaultDateTime;
                // appointmentTimeInput.value = defaultDateTime; // Optionally set default to now
            }
        };
    </script>
</head>
<body>
    <div class="container">
        <h2>访客入校预约</h2>

        <c:if test="${not empty errorMessage}">
            <p class="error">${errorMessage}</p>
        </c:if>
        <c:if test="${not empty successMessage}">
            <p style="color: green;">${successMessage}</p>
        </c:if>

        <form action="${pageContext.request.contextPath}/mobile/makeAppointment" method="post">
            <label for="appointmentType">预约类型:</label>
            <select id="appointmentType" name="appointmentType" onchange="toggleOfficialFields()" required>
                <option value="PUBLIC_ACCESS">公开活动/个人参观</option>
                <option value="OFFICIAL_VISIT">公务来访</option>
            </select>

            <label for="campus">到访校区:</label>
            <select id="campus" name="campus" required>
                <option value="朝晖校区">朝晖校区</option>
                <option value="屏峰校区">屏峰校区</option>
                <option value="莫干山校区">莫干山校区</option>
            </select>

            <label for="appointmentTime">预约入校时间:</label>
            <input type="datetime-local" id="appointmentTime" name="appointmentTime" required>

            <label for="applicantName">访客姓名:</label>
            <input type="text" id="applicantName" name="applicantName" required>

            <label for="applicantIdCard">访客身份证号:</label>
            <input type="text" id="applicantIdCard" name="applicantIdCard" required maxlength="18">

            <label for="applicantPhone">访客联系电话:</label>
            <input type="tel" id="applicantPhone" name="applicantPhone" required>
            
            <label for="organization">工作/所属单位 (选填):</label>
            <input type="text" id="organization" name="organization">

            <label for="transportation">交通方式:</label>
            <select id="transportation" name="transportation" required>
                <option value="WALK">步行</option>
                <option value="BICYCLE">自行车</option>
                <option value="ELECTRIC_BICYCLE">电动车</option>
                <option value="MOTORCYCLE">摩托车</option>
                <option value="CAR">自驾车</option>
                <option value="TAXI">出租车/网约车</option>
                <option value="PUBLIC_TRANSPORT">公共交通</option>
                <option value="OTHER">其他</option>
            </select>

            <label for="licensePlate">车牌号 (交通方式为自驾车时填写):</label>
            <input type="text" id="licensePlate" name="licensePlate">

            <div id="officialFields" class="official-fields">
                <label for="visitDepartment">到访部门:</label>
                <input type="text" id="visitDepartment" name="visitDepartment">

                <label for="contactPersonName">校内联系人姓名:</label>
                <input type="text" id="contactPersonName" name="contactPersonName">
                
                <label for="contactPersonPhone">校内联系人电话:</label>
                <input type="tel" id="contactPersonPhone" name="contactPersonPhone">

                <label for="visitReason">事由:</label>
                <textarea id="visitReason" name="visitReason" rows="3"></textarea>
            </div>

            <h3>随行人员 (选填)</h3>
            <div id="accompanyingPersonsContainer">
                <!-- Accompanying persons will be added here by JavaScript -->
            </div>
            <button type="button" onclick="addAccompanyingPerson()">添加随行人员</button>
            <br/>
            <input type="submit" value="提交预约">
        </form>
    </div>
</body>
</html>
