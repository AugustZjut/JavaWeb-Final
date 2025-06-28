<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<html>
<head>
    <title>${formAction == 'add' ? '添加新' : '编辑'}部门</title>
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
            max-width: 600px;
            margin: 0 auto;
        }
        label { display: block; margin-bottom: 8px; font-weight: bold; }
        input[type="text"], input[type="password"], select {
            width: 100%;
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
        <div class="logo">${formAction == 'add' ? '添加新' : '编辑'}部门</div>
        <div>
            <a href="${pageContext.request.contextPath}/admin/departments">返回部门列表</a>
            <a href="${pageContext.request.contextPath}/admin/dashboard.jsp">返回控制台</a>
        </div>
    </div>

    <div class="container">
        <div class="form-container">
            
            <form action="${pageContext.request.contextPath}/admin/departments" method="post">
                <input type="hidden" name="action" value="save">
                <c:if test="${not empty department.departmentId and department.departmentId != '0'}">
                    <input type="hidden" name="departmentId" value="<c:out value='${department.departmentId}'/>">
                </c:if>

                <div class="form-group">
                    <label for="departmentCode">部门代码:</label>
                    <input type="text" id="departmentCode" name="departmentCode" value="<c:out value='${department.departmentCode}'/>" required>
                </div>

                <div class="form-group">
                    <label for="departmentType">部门类型:</label>
                    <select id="departmentType" name="departmentType" required>
                        <option value="" ${empty department.departmentType ? 'selected' : ''} disabled>选择类型</option>
                        <option value="ADMINISTRATIVE" ${department.departmentType == 'ADMINISTRATIVE' ? 'selected' : ''}>行政部门</option>
                        <option value="DIRECTLY_AFFILIATED" ${department.departmentType == 'DIRECTLY_AFFILIATED' ? 'selected' : ''}>直属部门</option>
                        <option value="COLLEGE" ${department.departmentType == 'COLLEGE' ? 'selected' : ''}>学院</option>
                    </select>
                </div>

                <div class="form-group">
                    <label for="departmentName">部门名称:</label>
                    <input type="text" id="departmentName" name="departmentName" value="<c:out value='${department.departmentName}'/>" required>
                </div>

                <input type="submit" value="${formAction == 'add' ? '创建' : '保存更改'}">
                <a href="${pageContext.request.contextPath}/admin/departments?action=list" class="button cancel">取消</a>
            </form>
        </div>
    </div>
</body>
</html>
