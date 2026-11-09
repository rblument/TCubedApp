package edu.regis.tcubed.repository;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Mocked JDBC: does not connect to a real MySQL database. */
class ProjectRepositoryTest {
    @Test
    void returnsEmptyListWhenNoRowsMatch() throws Exception {
        DatabaseConnectionManager manager = mock(DatabaseConnectionManager.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet results = mock(ResultSet.class);
        when(manager.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(results);
        when(results.next()).thenReturn(false);

        ProjectRepository repository = new ProjectRepository(manager);
        assertTrue(repository.getProjectsByUserId("sample-user").isEmpty());
        verify(statement).setString(1, "sample-user");
        verify(statement).executeQuery();
    }
}
