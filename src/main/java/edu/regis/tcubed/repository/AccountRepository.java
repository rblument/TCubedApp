package edu.regis.tcubed.repository;

import edu.regis.tcubed.model.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;

/**
 * Repository for managing user account persistence and authentication against the ACCOUNT table.
 * 
 * @author Thomas Wintenburg, Oscar Castillo Saucedo, Dylan Cassagnol
 */
@Repository
public class AccountRepository {
    
    private final DatabaseConnectionManager connectionManager;

    @Autowired
    public AccountRepository(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /**
     * Authenticates a user by checking their UserId and matching either the SHA-256 hashed
     * password or plaintext password (for legacy seed data compatibility).
     *
     * @param username the user ID / email
     * @param password the raw password entered by the user
     * @return the matching {@link Account} if valid, or {@code null} if authentication fails
     * @throws SQLException if a database access error occurs
     */
    public Account authenticateUser(String username, String password) throws SQLException {  
        String hashedPassword = hashPassword(password);
        try (Connection conn = connectionManager.getConnection()) {
            String sql = "SELECT * FROM ACCOUNT WHERE UserId = ? AND (Password = ? OR Password = ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                stmt.setString(2, hashedPassword);
                stmt.setString(3, password);
                ResultSet rs = stmt.executeQuery();

                // If a user is found, map the database row to the new Account object
                if (rs.next()) {
                    return new Account(
                        rs.getString("UserId"),
                        rs.getString("Password"),
                        rs.getString("FirstName"),
                        rs.getString("LastName"),
                        rs.getInt("Question"),
                        rs.getString("Answer"),
                        rs.getBoolean("IsStudent") // Automatically translates the TINYINT
                    );
                }
            }
        }
        // Return null if no matching user was found in the database
        return null; 
    }

    /**
     * Checks whether an account with the given UserId already exists in the database.
     *
     * @param userId the user ID to check
     * @return {@code true} if the user ID is already registered; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean userExists(String userId) throws SQLException {
        try (Connection conn = connectionManager.getConnection()) {
            String sql = "SELECT 1 FROM ACCOUNT WHERE UserId = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, userId);
                ResultSet rs = stmt.executeQuery();
                return rs.next();
            }
        }
    }

    /**
     * Inserts a new user account into the ACCOUNT table with a SHA-256 hashed password.
     *
     * @param account the {@link Account} domain model containing registration details
     * @return {@code true} if the account was successfully inserted
     * @throws SQLException if a database error occurs
     */
    public boolean createAccount(Account account) throws SQLException {
        try (Connection conn = connectionManager.getConnection()) {
            String sql = "INSERT INTO ACCOUNT (UserId, Password, FirstName, LastName, Question, Answer, IsStudent) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, account.getUserId());
                stmt.setString(2, hashPassword(account.getPassword()));
                stmt.setString(3, account.getFirstName());
                stmt.setString(4, account.getLastName());
                stmt.setInt(5, account.getQuestion());
                stmt.setString(6, account.getAnswer());
                stmt.setBoolean(7, account.isStudent());
                return stmt.executeUpdate() > 0;
            }
        }
    }

    /**
     * Encodes a plaintext password using SHA-256 in accordance with database specifications.
     *
     * @param password the raw plaintext password
     * @return the hex-encoded SHA-256 hash string
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedHash.length);
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
