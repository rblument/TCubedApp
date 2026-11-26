package edu.regis.tcubed.repository;

import edu.regis.tcubed.model.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests verifying persistence and query logic in {@link AccountRepository}
 * using mocked JDBC connections.
 *
 * @author Oscar Castillo Saucedo
 */
class AccountRepositoryTest {

    private DatabaseConnectionManager manager;
    private Connection connection;
    private PreparedStatement statement;
    private ResultSet results;
    private AccountRepository repository;

    @BeforeEach
    void setUp() throws SQLException {
        manager = mock(DatabaseConnectionManager.class);
        connection = mock(Connection.class);
        statement = mock(PreparedStatement.class);
        results = mock(ResultSet.class);

        when(manager.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(results);

        repository = new AccountRepository(manager);
    }

    @Test
    @DisplayName("findByUserId returns populated Account with null password when record exists")
    void findByUserIdReturnsPopulatedAccount() throws SQLException {
        when(results.next()).thenReturn(true);
        when(results.getString("UserId")).thenReturn("test@regis.edu");
        when(results.getString("FirstName")).thenReturn("Testy");
        when(results.getString("LastName")).thenReturn("McTest");
        when(results.getInt("Question")).thenReturn(2);
        when(results.getString("Answer")).thenReturn("Denver");
        when(results.getBoolean("IsStudent")).thenReturn(true);

        Account account = repository.findByUserId("test@regis.edu");

        assertNotNull(account);
        assertEquals("test@regis.edu", account.getUserId());
        assertNull(account.getPassword(), "Password must be null to avoid leaking hashes to views");
        assertEquals("Testy", account.getFirstName());
        assertEquals("McTest", account.getLastName());
        assertEquals(2, account.getQuestion());
        assertEquals("Denver", account.getAnswer());
        assertTrue(account.isStudent());
        assertEquals("TM", account.getInitials());

        verify(statement).setString(1, "test@regis.edu");
    }

    @Test
    @DisplayName("findByUserId returns null when no matching record is found")
    void findByUserIdReturnsNullWhenNotFound() throws SQLException {
        when(results.next()).thenReturn(false);

        Account account = repository.findByUserId("nonexistent@regis.edu");

        assertNull(account);
        verify(statement).setString(1, "nonexistent@regis.edu");
    }

    @Test
    @DisplayName("updateProfile sets correct parameters and returns true on successful update")
    void updateProfileSuccess() throws SQLException {
        when(statement.executeUpdate()).thenReturn(1);

        boolean updated = repository.updateProfile("test@regis.edu", "Oscar", "Castillo", 1, "Buddy", false);

        assertTrue(updated);
        verify(statement).setString(1, "Oscar");
        verify(statement).setString(2, "Castillo");
        verify(statement).setInt(3, 1);
        verify(statement).setString(4, "Buddy");
        verify(statement).setBoolean(5, false);
        verify(statement).setString(6, "test@regis.edu");
    }

    @Test
    @DisplayName("updateProfile returns false when no rows were affected")
    void updateProfileNoRowsAffected() throws SQLException {
        when(statement.executeUpdate()).thenReturn(0);

        boolean updated = repository.updateProfile("missing@regis.edu", "Oscar", "Castillo", 1, "Buddy", false);

        assertFalse(updated);
    }

    @Test
    @DisplayName("updatePassword hashes raw password using SHA-256 and updates database")
    void updatePasswordSuccess() throws SQLException {
        when(statement.executeUpdate()).thenReturn(1);

        String rawPassword = "NewSecretPassword123!";
        String expectedHash = AccountRepository.hashPassword(rawPassword);

        boolean updated = repository.updatePassword("test@regis.edu", rawPassword);

        assertTrue(updated);
        verify(statement).setString(1, expectedHash);
        verify(statement).setString(2, "test@regis.edu");
    }
}
