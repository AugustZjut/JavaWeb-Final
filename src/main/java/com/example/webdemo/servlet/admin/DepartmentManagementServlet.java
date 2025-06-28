package com.example.webdemo.servlet.admin;

import com.example.webdemo.beans.AuditLog;
import com.example.webdemo.beans.Department;
import com.example.webdemo.beans.User;
import com.example.webdemo.dao.AuditLogDAO;
import com.example.webdemo.dao.DepartmentDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

// @WebServlet("/admin/departments") // Configured in web.xml
public class DepartmentManagementServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(DepartmentManagementServlet.class);
    private DepartmentDAO departmentDAO;
    private AuditLogDAO auditLogDAO;

    @Override
    public void init() throws ServletException {
        departmentDAO = new DepartmentDAO();
        auditLogDAO = new AuditLogDAO();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 验证用户是否登录并具有正确的角色
        HttpSession session = request.getSession(false);
        User loggedInAdmin = (session != null) ? (User) session.getAttribute("adminUser") : null;

        // 角色检查：只有系统管理员和学校管理员可以访问部门管理
        if (loggedInAdmin == null || !("SCHOOL_ADMIN".equals(loggedInAdmin.getRole()) || 
                                        "SYSTEM_ADMIN".equals(loggedInAdmin.getRole()))) {
            logger.warn("未授权访问部门管理：{}", loggedInAdmin != null ? loggedInAdmin.getUsername() : "未登录");
            response.sendRedirect(request.getContextPath() + "/admin/adminLogin.jsp");
            return;
        }
        
        String action = request.getParameter("action");
        if (action == null) {
            action = "list"; // Default action
        }

        try {
            switch (action) {
                case "add":
                    showNewForm(request, response, loggedInAdmin);
                    break;
                case "edit":
                    showEditForm(request, response, loggedInAdmin);
                    break;
                case "delete":
                    deleteDepartment(request, response, loggedInAdmin);
                    break;
                case "search":
                    searchDepartments(request, response, loggedInAdmin);
                    break;
                case "list":
                default:
                    listDepartments(request, response, loggedInAdmin);
                    break;
            }
        } catch (SQLException ex) {
            request.setAttribute("error", "数据库错误: " + ex.getMessage());
            listDepartmentsWithError(request, response, "数据库错误: " + ex.getMessage());
        } catch (Exception ex) {
            request.setAttribute("error", "发生意外错误: " + ex.getMessage());
            listDepartmentsWithError(request, response, "发生意外错误: " + ex.getMessage());
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 验证用户是否登录并具有正确的角色
        HttpSession session = request.getSession(false);
        User loggedInAdmin = (session != null) ? (User) session.getAttribute("adminUser") : null;

        if (loggedInAdmin == null || !("SCHOOL_ADMIN".equals(loggedInAdmin.getRole()) || 
                                        "SYSTEM_ADMIN".equals(loggedInAdmin.getRole()))) {
            response.sendRedirect(request.getContextPath() + "/admin/adminLogin.jsp");
            return;
        }

        String action = request.getParameter("action");
        if ("save".equals(action)) {
            try {
                saveDepartment(request, response, loggedInAdmin);
            } catch (SQLException ex) {
                 request.setAttribute("error", "保存时数据库错误: " + ex.getMessage());
                 // Forward to form with error and existing data if possible
                 String idParam = request.getParameter("departmentId");
                 Department department = new Department();
                 if (idParam != null && !idParam.isEmpty()) {
                     department.setDepartmentId(Integer.parseInt(idParam)); // 将String转为int
                 }
                 department.setDepartmentCode(request.getParameter("departmentCode"));
                 department.setDepartmentType(request.getParameter("departmentType"));
                 department.setDepartmentName(request.getParameter("departmentName"));
                 request.setAttribute("department", department);
                 request.setAttribute("formAction", (idParam == null || idParam.isEmpty()) ? "add" : "edit");
                 request.getRequestDispatcher("/admin/departmentForm.jsp").forward(request, response);
            } catch (Exception ex) {
                request.setAttribute("error", "保存时发生意外错误: " + ex.getMessage());
                doGet(request, response); // Or redirect to list with a general error
            }
        } else {
            doGet(request, response); 
        }
    }

    private void listDepartments(HttpServletRequest request, HttpServletResponse response, User loggedInAdmin) throws SQLException, IOException, ServletException {
        List<Department> listDepartment = departmentDAO.getAllDepartments();
        request.setAttribute("listDepartment", listDepartment);
        logger.debug("用户 {} 查看部门列表，共 {} 个部门", loggedInAdmin.getUsername(), listDepartment.size());
        request.getRequestDispatcher("/admin/departmentList.jsp").forward(request, response);
    }
    
    private void listDepartmentsWithError(HttpServletRequest request, HttpServletResponse response, String errorMessage) throws IOException, ServletException {
        try {
            List<Department> listDepartment = departmentDAO.getAllDepartments();
            request.setAttribute("listDepartment", listDepartment);
        } catch (SQLException e) {
            // If fetching departments also fails, set an empty list or handle appropriately
            request.setAttribute("listDepartment", new java.util.ArrayList<Department>());
            if (errorMessage == null || errorMessage.isEmpty()) {
                 request.setAttribute("error", "初始错误后无法检索部门列表: " + e.getMessage());
            } else {
                 request.setAttribute("error", errorMessage + " 另外，刷新部门列表失败: " + e.getMessage());
            }
        }
        request.getRequestDispatcher("/admin/departmentList.jsp").forward(request, response);
    }


    private void showNewForm(HttpServletRequest request, HttpServletResponse response, User loggedInAdmin) throws ServletException, IOException {
        request.setAttribute("department", new Department()); 
        request.setAttribute("formAction", "add");
        logger.debug("用户 {} 访问新增部门表单", loggedInAdmin.getUsername());
        request.getRequestDispatcher("/admin/departmentForm.jsp").forward(request, response);
    }

    private void showEditForm(HttpServletRequest request, HttpServletResponse response, User loggedInAdmin) throws SQLException, ServletException, IOException {
        String id = request.getParameter("id");
        if (id == null || id.trim().isEmpty()) {
            request.setAttribute("error", "缺少部门ID参数。");
            listDepartments(request, response, loggedInAdmin);
            return;
        }
        
        try {
            // 类型转换: 将String转为int
            Department existingDepartment = departmentDAO.getDepartmentById(Integer.parseInt(id));
            if (existingDepartment == null) {
                request.setAttribute("error", "未找到部门。");
                listDepartments(request, response, loggedInAdmin);
                return;
            }
            request.setAttribute("department", existingDepartment);
            request.setAttribute("formAction", "edit");
            logger.debug("用户 {} 编辑部门：{}", loggedInAdmin.getUsername(), existingDepartment.getDepartmentName());
            request.getRequestDispatcher("/admin/departmentForm.jsp").forward(request, response);
        } catch (NumberFormatException e) {
            request.setAttribute("error", "无效的部门ID格式。");
            listDepartments(request, response, loggedInAdmin);
        }
    }

    private void saveDepartment(HttpServletRequest request, HttpServletResponse response, User loggedInAdmin) throws SQLException, IOException, ServletException {
        String departmentId = request.getParameter("departmentId");
        String departmentCode = request.getParameter("departmentCode");
        String departmentType = request.getParameter("departmentType");
        String departmentName = request.getParameter("departmentName");

        // 输入验证
        if (departmentCode == null || departmentCode.trim().isEmpty() ||
            departmentName == null || departmentName.trim().isEmpty() ||
            departmentType == null || departmentType.trim().isEmpty()) {
            request.setAttribute("error", "所有字段都必须填写。");
            Department department = new Department();
            department.setDepartmentCode(departmentCode);
            department.setDepartmentType(departmentType);
            department.setDepartmentName(departmentName);
            if (departmentId != null && !departmentId.isEmpty()) {
                department.setDepartmentId(Integer.parseInt(departmentId));
            }
            request.setAttribute("department", department);
            request.setAttribute("formAction", (departmentId == null || departmentId.isEmpty()) ? "add" : "edit");
            request.getRequestDispatcher("/admin/departmentForm.jsp").forward(request, response);
            return;
        }

        // 检查部门代码是否已存在
        Integer excludeId = (departmentId != null && !departmentId.isEmpty()) ? Integer.parseInt(departmentId) : null;
        if (departmentDAO.isDepartmentCodeExists(departmentCode.trim(), excludeId)) {
            request.setAttribute("error", "部门代码 '" + departmentCode + "' 已存在，请使用其他代码。");
            Department department = new Department();
            department.setDepartmentCode(departmentCode);
            department.setDepartmentType(departmentType);
            department.setDepartmentName(departmentName);
            if (excludeId != null) {
                department.setDepartmentId(excludeId);
            }
            request.setAttribute("department", department);
            request.setAttribute("formAction", (departmentId == null || departmentId.isEmpty()) ? "add" : "edit");
            request.getRequestDispatcher("/admin/departmentForm.jsp").forward(request, response);
            return;
        }

        Department department = new Department();
        department.setDepartmentCode(departmentCode.trim());
        department.setDepartmentType(departmentType);
        department.setDepartmentName(departmentName.trim());

        String actionDetails;
        boolean success;

        if (departmentId == null || departmentId.isEmpty()) {
            // 添加新部门
            success = departmentDAO.addDepartment(department);
            actionDetails = "创建新部门: " + department.getDepartmentName() + " (代码: " + department.getDepartmentCode() + ")";
            if (success) {
                request.setAttribute("message", "部门添加成功。");
                logger.info("用户 {} 添加了新部门：{}", loggedInAdmin.getUsername(), department.getDepartmentName());
            } else {
                request.setAttribute("error", "添加部门失败。");
            }
        } else {
            // 更新现有部门
            department.setDepartmentId(Integer.parseInt(departmentId));
            success = departmentDAO.updateDepartment(department);
            actionDetails = "更新部门: " + department.getDepartmentName() + " (ID: " + department.getDepartmentId() + ")";
             if (success) {
                request.setAttribute("message", "部门更新成功。");
                logger.info("用户 {} 更新了部门：{}", loggedInAdmin.getUsername(), department.getDepartmentName());
            } else {
                request.setAttribute("error", "更新部门失败。");
            }
        }

        if (success) {
            AuditLog log = new AuditLog(); // Use default constructor
            log.setUserId(loggedInAdmin.getUserId());
            log.setUsername(loggedInAdmin.getUsername());
            log.setActionType((departmentId == null || departmentId.isEmpty()) ? "DEPARTMENT_CREATE" : "DEPARTMENT_UPDATE");
            log.setDetails(actionDetails); // 修复方法名: 从setActionDetails改为setDetails
            log.setLogTimestamp(new Timestamp(System.currentTimeMillis())); // 修复方法名: 从setActionTime改为setLogTimestamp
            log.setIpAddress(request.getRemoteAddr()); // 修复方法名: 从setClientIp改为setIpAddress
            auditLogDAO.createLog(log);
        }
        
        // Redirect to list view after save, regardless of success, to show message/error
        listDepartments(request, response, loggedInAdmin); 
    }

    private void deleteDepartment(HttpServletRequest request, HttpServletResponse response, User loggedInAdmin) throws SQLException, IOException, ServletException {
        String id = request.getParameter("id");
        // 类型转换: 将String转为int
        Department deptToDelete = departmentDAO.getDepartmentById(Integer.parseInt(id));
        String message = "";
        String error = "";

        if (deptToDelete == null) {
            error = "未找到要删除的部门。";
        } else {
            try {
                // 类型转换: 将String转为int
                boolean success = departmentDAO.deleteDepartment(Integer.parseInt(id));
                if (success) {
                    message = "部门 \"" + deptToDelete.getDepartmentName() + "\" 删除成功。";
                    
                    AuditLog log = new AuditLog(); // Use default constructor
                    log.setUserId(loggedInAdmin.getUserId());
                    log.setUsername(loggedInAdmin.getUsername());
                    log.setActionType("DEPARTMENT_DELETE");
                    log.setDetails("删除部门: " + deptToDelete.getDepartmentName() + " (ID: " + id + ")"); // 修复方法名
                    log.setLogTimestamp(new Timestamp(System.currentTimeMillis())); // 修复方法名
                    log.setIpAddress(request.getRemoteAddr()); // 修复方法名
                    auditLogDAO.createLog(log);
                } else {
                    error = "删除部门 \"" + deptToDelete.getDepartmentName() + "\" 失败。可能该部门正在使用中或发生数据库错误。";
                }
            } catch (SQLException ex) {
                 if (ex.getMessage().contains("users are still associated")) {
                    error = "无法删除部门：" + deptToDelete.getDepartmentName() + "，仍有用户关联到该部门。";
                } else {
                    error = "删除部门 \"" + deptToDelete.getDepartmentName() + "\" 时发生错误：" + ex.getMessage();
                }
            }
        }
        request.setAttribute("message", message);
        request.setAttribute("error", error);
        listDepartments(request, response, loggedInAdmin);
    }

    private void searchDepartments(HttpServletRequest request, HttpServletResponse response, User loggedInAdmin) throws SQLException, IOException, ServletException {
        String departmentCode = request.getParameter("departmentCode");
        String departmentName = request.getParameter("departmentName");
        String departmentType = request.getParameter("departmentType");
        
        List<Department> listDepartment = departmentDAO.searchDepartments(departmentCode, departmentName, departmentType);
        
        // 设置查询参数回显
        request.setAttribute("searchDepartmentCode", departmentCode);
        request.setAttribute("searchDepartmentName", departmentName);
        request.setAttribute("searchDepartmentType", departmentType);
        request.setAttribute("listDepartment", listDepartment);
        request.setAttribute("isSearchResult", true);
        
        logger.debug("用户 {} 搜索部门：代码={}, 名称={}, 类型={}, 结果数量={}", 
            loggedInAdmin.getUsername(), departmentCode, departmentName, departmentType, listDepartment.size());
        
        request.getRequestDispatcher("/admin/departmentList.jsp").forward(request, response);
    }
}
