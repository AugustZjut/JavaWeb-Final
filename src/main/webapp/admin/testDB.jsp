<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.example.webdemo.util.DBUtils" %>
<%@ page import="java.sql.*" %>
<%@ page import="com.example.webdemo.beans.User" %>

<%-- 权限检查 --%>
<% 
    User adminUser = (User) session.getAttribute("adminUser");
    if (adminUser == null) {
        response.sendRedirect(request.getContextPath() + "/admin/adminLogin.jsp");
        return;
    }
%>

<html>
<head>
    <title>数据库连接测试</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .test-result { margin: 10px 0; padding: 10px; border-radius: 4px; }
        .success { background-color: #d4edda; color: #155724; border: 1px solid #c3e6cb; }
        .error { background-color: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }
        .info { background-color: #cce7ff; color: #004085; border: 1px solid #b3d9ff; }
        pre { background-color: #f8f9fa; padding: 10px; border-radius: 4px; overflow-x: auto; }
    </style>
</head>
<body>
    <h1>数据库连接和数据查询测试</h1>
    
    <h2>1. 数据库连接测试</h2>
    <%
        Connection conn = null;
        try {
            conn = DBUtils.getConnection();
            if (conn != null && !conn.isClosed()) {
                out.println("<div class='test-result success'>✓ 数据库连接成功</div>");
                
                // 测试基本查询
                String sql = "SELECT COUNT(*) as total FROM appointments WHERE appointment_type = 'PUBLIC'";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    int count = rs.getInt("total");
                    out.println("<div class='test-result info'>📊 公众预约记录总数: " + count + "</div>");
                } else {
                    out.println("<div class='test-result error'>❌ 无法查询公众预约记录数</div>");
                }
                
                rs.close();
                pstmt.close();
                
            } else {
                out.println("<div class='test-result error'>❌ 数据库连接失败</div>");
            }
        } catch (SQLException e) {
            out.println("<div class='test-result error'>❌ 数据库错误: " + e.getMessage() + "</div>");
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    %>
    
    <h2>2. 详细的公众预约数据检查</h2>
    <%
        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT appointment_id, applicant_name, campus, status, application_date " +
                        "FROM appointments WHERE appointment_type = 'PUBLIC' " +
                        "ORDER BY application_date DESC LIMIT 10";
            
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            
            out.println("<div class='test-result info'>📋 最近10条公众预约记录:</div>");
            out.println("<pre>");
            
            boolean hasData = false;
            while (rs.next()) {
                hasData = true;
                out.println("ID: " + rs.getInt("appointment_id") + 
                           ", 申请人: " + rs.getString("applicant_name") + 
                           ", 校区: " + rs.getString("campus") + 
                           ", 状态: " + rs.getString("status") + 
                           ", 申请时间: " + rs.getTimestamp("application_date"));
            }
            
            if (!hasData) {
                out.println("❌ 没有找到任何公众预约记录");
            }
            
            out.println("</pre>");
            
            rs.close();
            pstmt.close();
            
        } catch (SQLException e) {
            out.println("<div class='test-result error'>❌ 查询错误: " + e.getMessage() + "</div>");
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    %>
    
    <h2>3. 检查表结构</h2>
    <%
        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT column_name, data_type FROM information_schema.columns " +
                        "WHERE table_name = 'appointments' ORDER BY ordinal_position";
            
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            
            out.println("<div class='test-result info'>📋 appointments表结构:</div>");
            out.println("<pre>");
            
            while (rs.next()) {
                out.println("列名: " + rs.getString("column_name") + 
                           ", 类型: " + rs.getString("data_type"));
            }
            
            out.println("</pre>");
            
            rs.close();
            pstmt.close();
            
        } catch (SQLException e) {
            out.println("<div class='test-result error'>❌ 查询表结构错误: " + e.getMessage() + "</div>");
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    %>
    
    <h2>4. 测试AppointmentDAO</h2>
    <%
        try {
            com.example.webdemo.dao.AppointmentDAO dao = new com.example.webdemo.dao.AppointmentDAO(DBUtils.getDataSource());
            
            // 测试获取公众预约总数
            int totalCount = dao.getPublicAppointmentCount();
            out.println("<div class='test-result info'>📊 AppointmentDAO.getPublicAppointmentCount(): " + totalCount + "</div>");
            
            // 测试获取公众预约列表
            java.util.List<com.example.webdemo.beans.Appointment> appointments = dao.getPublicAppointments(1, 5);
            out.println("<div class='test-result info'>📋 AppointmentDAO.getPublicAppointments(1, 5): 返回 " + appointments.size() + " 条记录</div>");
            
            if (!appointments.isEmpty()) {
                out.println("<pre>");
                for (com.example.webdemo.beans.Appointment appointment : appointments) {
                    out.println("ID: " + appointment.getAppointmentId() + 
                               ", 申请人: " + appointment.getApplicantName() + 
                               ", 校区: " + appointment.getCampus() + 
                               ", 状态: " + appointment.getStatus());
                }
                out.println("</pre>");
            }
            
        } catch (Exception e) {
            out.println("<div class='test-result error'>❌ AppointmentDAO测试错误: " + e.getMessage() + "</div>");
            e.printStackTrace();
        }
    %>
    
    <div style="margin-top: 20px;">
        <a href="${pageContext.request.contextPath}/admin/dashboard.jsp" style="color: blue;">返回控制台</a>
    </div>
</body>
</html>
