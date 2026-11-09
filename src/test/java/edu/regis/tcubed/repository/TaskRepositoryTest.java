
package edu.regis.tcubed.repository;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests TaskRepository using mocked JDBC connections.
 * Does not require a running MySQL database.
 */
class TaskRepositoryTest {

    @Test
    void returnsEmptyListWhenNoRowsMatch() throws Exception {

        DatabaseConnectionManager manager =
                mock(DatabaseConnectionManager.class);

        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet results = mock(ResultSet.class);

        when(manager.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString()))
                .thenReturn(statement);
        when(statement.executeQuery()).thenReturn(results);
        when(results.next()).thenReturn(false);

        TaskRepository repository = new TaskRepository(manager);

        assertTrue(
            repository.getTasksForMember("sample-user", "42").isEmpty()
        );

        verify(statement).setString(1, "sample-user");
        verify(statement).setInt(2, 42);
        verify(statement).executeQuery();
    }
}
