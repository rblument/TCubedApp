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
     * password.
     *
     * @param username the user ID / email
     * @param password the raw password entered by the user
     * @return the matching {@link Account} if valid, or {@code null} if authentication fails
     * @throws SQLException if a database access error occurs
     */
    public Account authenticateUser(String username, String password) throws SQLException {  
        String hashedPassword = hashPassword(password);
        
        try (Connection conn = connectionManager.getConnection()) {
            String sql = "SELECT * FROM ACCOUNT WHERE UserId = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();

                // If a user is found, map the database row to the new Account object
                if (rs.next()) {
                    String dbHash = rs.getString("Password");
                    if (hashedPassword.equals(dbHash)) {
                        return new Account(
                            rs.getString("UserId"),
                            dbHash,
                            rs.getString("FirstName"),
                            rs.getString("LastName"),
                            rs.getInt("Question"),
                            rs.getString("Answer"),
                            rs.getBoolean("IsStudent")
                        );
                    }
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
     * Retrieves an account by its unique user ID without returning the sensitive password hash.
     *
     * @param userId the user ID / email of the account to look up
     * @return the populated {@link Account} with a {@code null} password, or {@code null} if not found
     * @throws SQLException if a database access error occurs
     */
    public Account findByUserId(String userId) throws SQLException {
        try (Connection conn = connectionManager.getConnection()) {
            String sql = "SELECT UserId, FirstName, LastName, Question, Answer, IsStudent FROM ACCOUNT WHERE UserId = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new Account(
                            rs.getString("UserId"),
                            null,
                            rs.getString("FirstName"),
                            rs.getString("LastName"),
                            rs.getInt("Question"),
                            rs.getString("Answer"),
                            rs.getBoolean("IsStudent")
                        );
                    }
                }
            }
        }
        return null;
    }

    /**
     * Updates profile and security question details for an existing user account.
     *
     * @param userId    the unique user ID / email identifying the account
     * @param firstName the updated first name
     * @param lastName  the updated last name
     * @param question  the index of the selected security question
     * @param answer    the updated answer to the security question
     * @param isStudent flag indicating whether the user is a student
     * @return {@code true} if the profile was successfully updated; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean updateProfile(String userId, String firstName, String lastName, int question, String answer, boolean isStudent) throws SQLException {
        try (Connection conn = connectionManager.getConnection()) {
            String sql = "UPDATE ACCOUNT SET FirstName = ?, LastName = ?, Question = ?, Answer = ?, IsStudent = ? WHERE UserId = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, firstName);
                stmt.setString(2, lastName);
                stmt.setInt(3, question);
                stmt.setString(4, answer);
                stmt.setBoolean(5, isStudent);
                stmt.setString(6, userId);
                return stmt.executeUpdate() > 0;
            }
        }
    }

    /**
     * Hashes the plaintext password and updates the user's password record in the database.
     *
     * @param userId         the unique user ID / email identifying the account
     * @param newRawPassword the new plaintext password to hash and persist
     * @return {@code true} if the password was successfully updated; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean updatePassword(String userId, String newRawPassword) throws SQLException {
        String hashedPassword = hashPassword(newRawPassword);
        try (Connection conn = connectionManager.getConnection()) {
            String sql = "UPDATE ACCOUNT SET Password = ? WHERE UserId = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, hashedPassword);
                stmt.setString(2, userId);
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
