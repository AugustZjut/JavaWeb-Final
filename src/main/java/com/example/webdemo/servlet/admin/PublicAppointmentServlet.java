package com.example.webdemo.servlet.admin;

import com.example.webdemo.beans.Appointment;
import com.example.webdemo.dao.AppointmentDAO;
import com.example.webdemo.util.DBUtils;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class PublicAppointmentServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private AppointmentDAO appointmentDAO;

    @Override
    public void init() throws ServletException {
        appointmentDAO = new AppointmentDAO(DBUtils.getDataSource()); // Pass DataSource
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        if (action == null) {
            action = "list"; // Default action
        }

        try {
            switch (action) {
                case "list":
                default:
                    listPublicAppointments(request, response);
                    break;
                // Add cases for search, view details, stats later
            }
        } catch (SQLException ex) {
            throw new ServletException("Database error in PublicAppointmentServlet", ex);
        }
    }

    private void listPublicAppointments(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException, ServletException {
        // Assuming 'Public' is the type string stored in the database for public appointments
        List<Appointment> listApp = appointmentDAO.getAppointmentsByType("Public"); // Updated method call
        request.setAttribute("listAppointment", listApp);
        request.getRequestDispatcher("/admin/publicAppointmentList.jsp").forward(request, response);
    }

    // doPost might be needed for actions like bulk delete or status change if added later
    // protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    //    doGet(request, response); 
    // }
}
