package edu.regis.tcubed.repository;

import edu.regis.tcubed.model.Account;
import edu.regis.tcubed.model.Project;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests project lookup and membership filter ability with mocked JDBC objects.
 * You dont need a live MySQL database connection to test
 *
 * @author Dylan Cassagnol
 */
class ProjectRepositoryTest {

    private DatabaseConnectionManager manager;
    private Connection connection;
    private PreparedStatement statement;
    private ResultSet results;
    private ProjectRepository repository;

    @BeforeEach
    void setUp() throws SQLException {
        manager = mock(DatabaseConnectionManager.class);
        connection = mock(Connection.class);
        statement = mock(PreparedStatement.class);
        results = mock(ResultSet.class);

        when(manager.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(results);

        repository = new ProjectRepository(manager);
    }

    @Test
    void returnsEmptyListWhenNoRowsMatch() throws SQLException {
        when(results.next()).thenReturn(false);

        List<Project> projects = repository.getProjectsByUserId("sample-user");

        assertTrue(projects.isEmpty());
        verify(statement).setString(1, "sample-user");
        verify(statement).executeQuery();
    }

    @Test
    void returnsProjectsThatMatchMembershipQuery() throws SQLException {
        when(results.next()).thenReturn(true, true, false);
        when(results.getInt("ProjectId")).thenReturn(3, 7);
        when(results.getString("Name")).thenReturn("Alpha", "Beta");
        when(results.getString("Description")).thenReturn("First", "Second");

        List<Project> projects = repository.getProjectsByUserId("sample-user");

        assertEquals(2, projects.size());
        assertEquals(3, projects.get(0).getProjectId());
        assertEquals("Alpha", projects.get(0).getName());
        assertEquals("First", projects.get(0).getDescription());
        assertEquals(7, projects.get(1).getProjectId());
        assertEquals("Beta", projects.get(1).getName());
        assertEquals("Second", projects.get(1).getDescription());
        verify(statement).setString(1, "sample-user");
    }

    @Test
    void blankUserIdDoesNotQueryDatabase() throws SQLException {
        assertTrue(repository.getProjectsByUserId("  ").isEmpty());
        verifyNoInteractions(manager);
    }

    @Test
    void findsProjectUsingBothProjectIdAndUserId() throws SQLException {
        when(results.next()).thenReturn(true);
        when(results.getInt("ProjectId")).thenReturn(7);
        when(results.getString("Name")).thenReturn("Capstone");
        when(results.getString("Description")).thenReturn("Team work");

        Project project = repository.findProject(7, "sample-user");

        assertNotNull(project);
        assertEquals(7, project.getProjectId());
        assertEquals("Capstone", project.getName());
        assertEquals("Team work", project.getDescription());
        verify(statement).setInt(1, 7);
        verify(statement).setString(2, "sample-user");
    }

    @Test
    void inaccessibleProjectReturnsNull() throws SQLException {
        when(results.next()).thenReturn(false);

        assertNull(repository.findProject(99, "sample-user"));
        verify(statement).setInt(1, 99);
        verify(statement).setString(2, "sample-user");
    }

    @Test
    void databaseFailurePropagatesToCaller() throws SQLException {
        when(manager.getConnection()).thenThrow(new SQLException("Database unavailable"));

        SQLException thrown = assertThrows(SQLException.class,
                () -> repository.getProjectsByUserId("sample-user"));

        assertEquals("Database unavailable", thrown.getMessage());
    }

    @Test
    void getProjectMemberAccountsByProjectNameReturnsPopulatedAccounts() throws SQLException {
        when(results.next()).thenReturn(true, true, false);
        when(results.getString("UserId")).thenReturn("alice@example.com", "bob@example.com");
        when(results.getString("FirstName")).thenReturn("Alice", "Bob");
        when(results.getString("LastName")).thenReturn("Smith", "Jones");
        when(results.getBoolean("IsStudent")).thenReturn(true, false);

        List<Account> members = repository.getProjectMemberAccountsByProjectName("Capstone");

        assertEquals(2, members.size());
        Account m1 = members.get(0);
        assertEquals("alice@example.com", m1.getUserId());
        assertEquals("Alice", m1.getFirstName());
        assertEquals("Smith", m1.getLastName());
        assertTrue(m1.isStudent());
        assertNull(m1.getPassword());
        assertNull(m1.getAnswer());

        Account m2 = members.get(1);
        assertEquals("bob@example.com", m2.getUserId());
        assertEquals("Bob", m2.getFirstName());
        assertEquals("Jones", m2.getLastName());
        assertFalse(m2.isStudent());
        assertNull(m2.getPassword());
        assertNull(m2.getAnswer());

        verify(statement).setString(1, "Capstone");
        verify(statement).executeQuery();
    }

    @Test
    void getProjectMemberAccountsByProjectNameReturnsEmptyWhenNullOrBlank() throws SQLException {
        assertTrue(repository.getProjectMemberAccountsByProjectName(null).isEmpty());
        assertTrue(repository.getProjectMemberAccountsByProjectName("   ").isEmpty());
        verifyNoInteractions(manager);
    }

    @Test
    void getProjectMemberAccountsByProjectNameReturnsEmptyWhenNoRowsMatch() throws SQLException {
        when(results.next()).thenReturn(false);

        List<Account> members = repository.getProjectMemberAccountsByProjectName("NonExistent");

        assertTrue(members.isEmpty());
        verify(statement).setString(1, "NonExistent");
    }
}
