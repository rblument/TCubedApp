package edu.regis.tcubed.repository;

import edu.regis.tcubed.model.Task;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Tests member-scoped queries using mocked JDBC, without a live MySQL database. */
class TaskRepositoryTest {
    private DatabaseConnectionManager manager;
    private Connection connection;
    private PreparedStatement statement;
    private ResultSet results;
    private TaskRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        manager = mock(DatabaseConnectionManager.class);
        connection = mock(Connection.class);
        statement = mock(PreparedStatement.class);
        results = mock(ResultSet.class);
        when(manager.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(results);
        repository = new TaskRepository(manager);
    }

    @Test
    void returnsEmptyListWhenNoRowsMatch() throws Exception {
        when(results.next()).thenReturn(false);
        assertTrue(repository.getTasksForMember("sample-user", "42").isEmpty());
        verify(statement).setString(1, "sample-user");
        verify(statement).setInt(2, 42);
        verify(statement).executeQuery();
    }

    @Test
    void mapsTasksFromMemberScopedQuery() throws Exception {
        when(results.next()).thenReturn(true, false);
        when(results.getInt("TaskId")).thenReturn(12);
        when(results.getInt("Position")).thenReturn(2);
        when(results.getInt("LogicalBucketId")).thenReturn(3);
        when(results.getString("Title")).thenReturn("Implement tests");
        when(results.getString("Body")).thenReturn("Controller coverage");
        when(results.getString("Assignee")).thenReturn("sample-user");
        when(results.getString("ProjectName")).thenReturn("Capstone");
        List<Task> tasks = repository.getTasksForMember("sample-user", "42");
        assertEquals(1, tasks.size());
        assertEquals(12, tasks.get(0).getTaskId());
        assertEquals("Implement tests", tasks.get(0).getTitle());
        assertEquals(3, tasks.get(0).getBucketId());
        assertEquals("Capstone", tasks.get(0).getProjectName());
    }

    @Test
    void findsTaskWithMembershipAndProjectIdConditions() throws Exception {
        when(results.next()).thenReturn(true);
        when(results.getInt("TaskId")).thenReturn(12);
        when(results.getString("Title")).thenReturn("Write tests");
        Task task = repository.findTaskForMember(12, "sample-user", "42");
        assertNotNull(task);
        assertEquals("Write tests", task.getTitle());
        verify(statement).setInt(1, 12);
        verify(statement).setString(2, "sample-user");
        verify(statement).setInt(3, 42);
    }

    @Test
    void missingOrInaccessibleTaskReturnsNull() throws Exception {
        when(results.next()).thenReturn(false);
        assertNull(repository.findTaskForMember(999, "sample-user", "42"));
    }

    @Test
    void databaseFailuresAreNotSilentlyIgnored() throws Exception {
        when(manager.getConnection()).thenThrow(new SQLException("Connection unavailable"));
        assertThrows(SQLException.class,
                () -> repository.getTasksForMember("sample-user", "42"));
    }
}
