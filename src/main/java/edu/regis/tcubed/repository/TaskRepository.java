package edu.regis.tcubed.repository;

import edu.regis.tcubed.model.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class TaskRepository {

   private final DatabaseConnectionManager connectionManager;

    @Autowired
    public TaskRepository(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }
    
    /**
     * Fetches all tasks associated with a specific project by joining the 
     * TASK, BUCKET, BOARD, and PROJECT tables.
     */
    public List<Task> getTasksByProjectName(String projectName) throws SQLException {
        List<Task> tasks = new ArrayList<>();
        
        try (Connection conn = connectionManager.getConnection()) {
            // This join traces the hierarchy: Project -> Board -> Bucket -> Task
            String sql = "SELECT t.TaskId, t.Title, t.Body, t.Position, t.BucketId, t.Assignee " +
                         "FROM TASK t " +
                         "JOIN BUCKET bu ON t.BucketId = bu.BucketId " +
                         "JOIN BOARD bo ON bu.BoardId = bo.BoardId " +
                         "JOIN PROJECT p ON bo.ProjectId = p.ProjectId " +
                         "WHERE p.Name = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, projectName);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    tasks.add(new Task(
                        rs.getInt("TaskId"),
                        rs.getString("Title"),
                        rs.getString("Body"),
                        rs.getInt("Position"),
                        rs.getInt("BucketId"),
                        rs.getString("Assignee") 
                    ));
                }
            }
        }
        return tasks;
    }

    /**
     * Updates a task's bucket and vertical position when it is dragged and dropped.
     */
    public void updateTaskPosition(int taskId, int newBucketId, int newPosition) throws SQLException {
        try (Connection conn = connectionManager.getConnection()) {
            String sql = "UPDATE TASK SET BucketId = ?, Position = ? WHERE TaskId = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, newBucketId);
                stmt.setInt(2, newPosition);
                stmt.setInt(3, taskId);
                stmt.executeUpdate();
            }
        }
    }
}