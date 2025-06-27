<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>欢迎访问校园出入预约系统</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4; display: flex; justify-content: center; align-items: center; height: 100vh; text-align: center; }
        .container { background-color: #fff; padding: 30px; border-radius: 8px; box-shadow: 0 0 10px rgba(0,0,0,0.1); }
        h1 { color: #333; }
        ul { list-style-type: none; padding: 0; }
        li { margin: 15px 0; }
        a { text-decoration: none; color: #007bff; font-size: 1.2em; padding: 10px 20px; border: 1px solid #007bff; border-radius: 5px; display: inline-block; transition: background-color 0.3s, color 0.3s; }
        a:hover { background-color: #007bff; color: #fff; }
    </style>
</head>
<body>
    <div class="container">
        <h1>校园出入预约管理系统</h1>
        <ul>
            <li><a href="${pageContext.request.contextPath}/admin/login">管理系统登录</a></li>
            <li><a href="${pageContext.request.contextPath}/mobile/makeAppointment">访客在线预约</a></li>
            <li><a href="${pageContext.request.contextPath}/mobile/myAppointments">我的预约 / 查看通行码</a></li>
        </ul>
    </div>
</body>
</html>
