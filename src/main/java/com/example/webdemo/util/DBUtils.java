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
import java.util.logging.Level; // Import Level
import java.util.logging.Logger; // Import Logger

public class DBUtils {

    private static HikariDataSource dataSource;
    private static final Logger LOGGER = Logger.getLogger(DBUtils.class.getName()); // Create a logger instance

    static {
        Properties props = new Properties();
        try (InputStream input = DBUtils.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (input == null) {
                LOGGER.log(Level.SEVERE, "Sorry, unable to find db.properties. Will use default connection properties.");
            } else {
                props.load(input);
                LOGGER.log(Level.INFO, "db.properties loaded successfully.");
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "IOException while loading db.properties: " + ex.getMessage(), ex);
        }

        HikariConfig config = new HikariConfig();
        String dbUrl = props.getProperty("db.url", "jdbc:postgresql://localhost:5432/campus_access_db");
        String dbUsername = props.getProperty("db.username", "postgres");
        // DO NOT log password in production. For debugging only, and remove immediately.
        // String dbPassword = props.getProperty("db.password", "password"); 

        LOGGER.log(Level.INFO, "Attempting to configure HikariCP with the following properties:");
        LOGGER.log(Level.INFO, "JDBC URL: " + dbUrl);
        LOGGER.log(Level.INFO, "Username: " + dbUsername);
        // LOGGER.log(Level.INFO, "Password: " + dbPassword); // Be careful with logging passwords

        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUsername);
        config.setPassword(props.getProperty("db.password", "password")); // Get password directly here
        config.setDriverClassName(props.getProperty("db.driver", "org.postgresql.Driver"));
        config.addDataSourceProperty("cachePrepStmts", props.getProperty("db.cachePrepStmts", "true"));
        config.addDataSourceProperty("prepStmtCacheSize", props.getProperty("db.prepStmtCacheSize", "250"));
        config.addDataSourceProperty("prepStmtCacheSqlLimit", props.getProperty("db.prepStmtCacheSqlLimit", "2048"));
        config.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.maxPoolSize", "10")));

        try {
            LOGGER.log(Level.INFO, "Initializing HikariDataSource...");
            dataSource = new HikariDataSource(config);
            LOGGER.log(Level.INFO, "HikariDataSource initialized successfully.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize HikariDataSource: " + e.getMessage(), e);
            // This exception will propagate and cause NoClassDefFoundError for DBUtils
            throw e; // Re-throw the exception to ensure the static initializer fails as expected
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            LOGGER.log(Level.SEVERE, "HikariDataSource not initialized. Check db.properties and static block for errors during initialization.");
            throw new SQLException("HikariDataSource not initialized. Check db.properties and static block for errors during initialization.");
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
