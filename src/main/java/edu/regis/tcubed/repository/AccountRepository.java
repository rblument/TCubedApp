package edu.regis.tcubed.repository;

import edu.regis.tcubed.model.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.sql.*;

/**
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
    
    public Account authenticateUser(String username, String password) throws SQLException {  
        try (Connection conn = connectionManager.getConnection()) {
            String sql = "SELECT * FROM ACCOUNT WHERE UserId = ? AND Password = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                stmt.setString(2, password);
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
}
