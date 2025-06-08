<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<html>
<head>
    <title>${formAction == 'add' ? 'Add New' : 'Edit'} Department</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background-color: #f4f4f4; color: #333; }
        h1 { color: #333; }
        form { background-color: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0,0,0,0.1); width: 50%; margin-top: 20px; }
        label { display: block; margin-bottom: 8px; font-weight: bold; }
        input[type="text"], input[type="password"], select {
            width: calc(100% - 22px); /* Adjust for padding and border */
            padding: 10px;
            margin-bottom: 15px;
            border: 1px solid #ddd;
            border-radius: 4px;
            box-sizing: border-box;
        }
        input[type="submit"], .button {
            background-color: #007bff;
            color: white;
            padding: 10px 15px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            text-decoration: none;
            display: inline-block;
            font-size: 16px;
        }
        input[type="submit"]:hover, .button:hover {
            background-color: #0056b3;
        }
        .button.cancel { background-color: #6c757d; margin-left: 10px; }
        .button.cancel:hover { background-color: #5a6268; }
        .form-group { margin-bottom: 15px; }
    </style>
</head>
<body>
    <h1>${formAction == 'add' ? 'Add New' : 'Edit'} Department</h1>
    <form action="${pageContext.request.contextPath}/admin/departments" method="post">
        <input type="hidden" name="action" value="save">
        <c:if test="${not empty department.departmentId and department.departmentId != '0'}"> <%-- Check for departmentId (String) --%>
            <input type="hidden" name="departmentId" value="<c:out value='${department.departmentId}'/>">
        </c:if>

        <div class="form-group">
            <label for="departmentCode">Department Code:</label>
            <input type="text" id="departmentCode" name="departmentCode" value="<c:out value='${department.departmentCode}'/>" required>
        </div>

        <div class="form-group">
            <label for="departmentType">Department Type:</label>
            <input type="text" id="departmentType" name="departmentType" value="<c:out value='${department.departmentType}'/>" required>
            <%-- Consider using a dropdown if types are predefined --%>
            <%-- 
            <select id="departmentType" name="departmentType" required>
                <option value="" ${empty department.departmentType ? 'selected' : ''} disabled>Select Type</option>
                <option value="Teaching" ${department.departmentType == 'Teaching' ? 'selected' : ''}>Teaching</option>
                <option value="Administrative" ${department.departmentType == 'Administrative' ? 'selected' : ''}>Administrative</option>
                <option value="Research" ${department.departmentType == 'Research' ? 'selected' : ''}>Research</option>
                <option value="Other" ${department.departmentType == 'Other' ? 'selected' : ''}>Other</option>
            </select> 
            --%>
        </div>

        <div class="form-group">
            <label for="departmentName">Department Name:</label>
            <input type="text" id="departmentName" name="departmentName" value="<c:out value='${department.departmentName}'/>" required>
        </div>

        <input type="submit" value="${formAction == 'add' ? 'Create' : 'Save Changes'}">
        <a href="${pageContext.request.contextPath}/admin/departments?action=list" class="button cancel">Cancel</a>
    </form>
</body>
</html>
