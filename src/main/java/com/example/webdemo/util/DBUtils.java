package com.example.webdemo.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DBUtils {

    private static HikariDataSource dataSource;

    static {
        Properties props = new Properties();
        try (InputStream input = DBUtils.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (input == null) {
                System.err.println("Sorry, unable to find db.properties");
                // Consider throwing a runtime exception or exiting if db.properties is critical
            } else {
                props.load(input);
            }
        } catch (IOException ex) {
            // Log error
            System.err.println("IOException while loading db.properties: " + ex.getMessage());
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(props.getProperty("db.url", "jdbc:postgresql://localhost:5432/campus_access_db")); // Default if not in props
        config.setUsername(props.getProperty("db.username", "postgres"));
        config.setPassword(props.getProperty("db.password", "password"));
        config.setDriverClassName(props.getProperty("db.driver", "org.postgresql.Driver"));
        config.addDataSourceProperty("cachePrepStmts", props.getProperty("db.cachePrepStmts", "true"));
        config.addDataSourceProperty("prepStmtCacheSize", props.getProperty("db.prepStmtCacheSize", "250"));
        config.addDataSourceProperty("prepStmtCacheSqlLimit", props.getProperty("db.prepStmtCacheSqlLimit", "2048"));
        config.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.maxPoolSize", "10")));

        dataSource = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("HikariDataSource not initialized. Check db.properties and static block.");
        }
        return dataSource.getConnection();
    }

    // Added getter for the DataSource
    public static HikariDataSource getDataSource() {
        return dataSource;
    }

    public static void close(Connection conn, Statement stmt, ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing ResultSet: " + e.getMessage());
        }
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing Statement: " + e.getMessage());
        }
        try {
            if (conn != null) {
                conn.close(); // Returns the connection to the pool
            }
        } catch (SQLException e) {
            System.err.println("Error closing Connection: " + e.getMessage());
        }
    }

    public static void close(Connection conn, Statement stmt) {
        close(conn, stmt, null);
    }

    // Optional: A method to gracefully shutdown the datasource when the application undeploys
    public static void shutdownDataSource() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
