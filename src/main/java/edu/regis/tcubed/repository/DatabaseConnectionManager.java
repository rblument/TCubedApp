package edu.regis.tcubed.repository;

import org.springframework.stereotype.Component;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Centralized helper component for managing JDBC database connections.
 * Validates environment/VM properties before opening a connection.
 * 
 * @author Thomas Wintenburg
 */
@Component
public class DatabaseConnectionManager {

    private final String dbUrl = System.getProperty("DB_URL") != null
            ? System.getProperty("DB_URL") : System.getenv("DB_URL");
    private final String dbUser = System.getProperty("DB_USER") != null
            ? System.getProperty("DB_USER") : System.getenv("DB_USER");
    private final String dbPassword = System.getProperty("DB_PASSWORD") != null
            ? System.getProperty("DB_PASSWORD") : System.getenv("DB_PASSWORD");

    /**
     * Validates that DB_URL, DB_USER, and DB_PASSWORD are set and returns
     * an active SSL-enabled JDBC Connection.
     *
     * @return a configured {@link Connection} to the MySQL database
     * @throws SQLException if any required property is missing or the connection fails
     */
    public Connection getConnection() throws SQLException {
        if (dbUrl == null || dbUrl.isBlank()) {
            throw new SQLException(
                "DB_URL is not set. Add -DDB_URL in NetBeans VM Options or set the DB_URL environment variable."
            );
        }
        if (dbUser == null || dbUser.isBlank()) {
            throw new SQLException(
                "DB_USER is not set. Add -DDB_USER in NetBeans VM Options or set the DB_USER environment variable."
            );
        }
        if (dbPassword == null) {
            throw new SQLException(
                "DB_PASSWORD is not set. Add -DDB_PASSWORD in NetBeans VM Options or set the DB_PASSWORD environment variable."
            );
        }

        String secureUrl = dbUrl + (dbUrl.contains("?") ? "&" : "?") + "sslmode=REQUIRED";
        return DriverManager.getConnection(secureUrl, dbUser, dbPassword);
    }
}