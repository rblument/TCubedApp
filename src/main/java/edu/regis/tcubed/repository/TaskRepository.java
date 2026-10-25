package edu.regis.tcubed.repository;

import edu.regis.tcubed.model.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for managing Task persistence and board hierarchy queries in the database.
 *
 * @author Thomas Wintenburg, Oscar Castillo Saucedo
 */
@Repository
public class TaskRepository {

    private final DatabaseConnectionManager connectionManager;

    @Autowired
    public TaskRepository(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /**
     * Ensures that the default project, board, and buckets (1: To-Do, 2: In Progress,
     * 3: Done, 4: Trash) exist in the database so foreign key constraints on TASK succeed.
     *
     * @param conn        active JDBC connection
     * @param projectName the active project name
     * @throws SQLException if a database error occurs
     */
    private void ensureBoardBucketsExist(Connection conn, String projectName) throws SQLException {
        int projectId = -1;
        try (PreparedStatement checkProj = conn.prepareStatement("SELECT ProjectId FROM PROJECT WHERE Name = ?")) {
            checkProj.setString(1, projectName);
            ResultSet rs = checkProj.executeQuery();
            if (rs.next()) {
                projectId = rs.getInt("ProjectId");
            }
        }

        if (projectId == -1) {
            try (PreparedStatement insertProj = conn.prepareStatement(
                    "INSERT INTO PROJECT (Name, Description) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                insertProj.setString(1, projectName);
                insertProj.setString(2, projectName + " Workspace");
                insertProj.executeUpdate();
                ResultSet keys = insertProj.getGeneratedKeys();
                if (keys.next()) {
                    projectId = keys.getInt(1);
                }
            }
        }

        int boardId = -1;
        try (PreparedStatement checkBoard = conn.prepareStatement("SELECT BoardId FROM BOARD WHERE ProjectId = ?")) {
            checkBoard.setInt(1, projectId);
            ResultSet rs = checkBoard.executeQuery();
            if (rs.next()) {
                boardId = rs.getInt("BoardId");
            }
        }

        if (boardId == -1) {
            try (PreparedStatement insertBoard = conn.prepareStatement(
                    "INSERT INTO BOARD (Name, Description, ProjectId) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                insertBoard.setString(1, projectName + " Board");
                insertBoard.setString(2, "Default Kanban Board");
                insertBoard.setInt(3, projectId);
                insertBoard.executeUpdate();
                ResultSet keys = insertBoard.getGeneratedKeys();
                if (keys.next()) {
                    boardId = keys.getInt(1);
                }
            }
        }

        String[] bucketNames = {"To-Do", "In Progress", "Done", "Trash"};
        for (int i = 0; i < bucketNames.length; i++) {
            int bucketId = i + 1;
            try (PreparedStatement checkBucket = conn.prepareStatement("SELECT BucketId FROM BUCKET WHERE BucketId = ?")) {
                checkBucket.setInt(1, bucketId);
                ResultSet rs = checkBucket.executeQuery();
                if (!rs.next()) {
                    try (PreparedStatement insertBucket = conn.prepareStatement(
                            "INSERT INTO BUCKET (BucketId, Name, Position, BoardId) VALUES (?, ?, ?, ?)")) {
                        insertBucket.setInt(1, bucketId);
                        insertBucket.setString(2, bucketNames[i]);
                        insertBucket.setInt(3, bucketId);
                        insertBucket.setInt(4, boardId);
                        insertBucket.executeUpdate();
                    }
                }
            }
        }
    }

    /**
     * Fetches all tasks associated with a specific project, ordered by position.
     *
     * @param projectName the name of the project
     * @return list of tasks belonging to the project
     * @throws SQLException if a database error occurs
     */
    public List<Task> getTasksByProjectName(String projectName) throws SQLException {
        List<Task> tasks = new ArrayList<>();

        try (Connection conn = connectionManager.getConnection()) {
            ensureBoardBucketsExist(conn, projectName);

            String sql = "SELECT t.TaskId, t.Title, t.Body, t.Position, t.BucketId, t.Assignee " +
                         "FROM TASK t " +
                         "JOIN BUCKET bu ON t.BucketId = bu.BucketId " +
                         "JOIN BOARD bo ON bu.BoardId = bo.BoardId " +
                         "JOIN PROJECT p ON bo.ProjectId = p.ProjectId " +
                         "WHERE p.Name = ? " +
                         "ORDER BY t.Position ASC, t.TaskId ASC";

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
     * Inserts a new task into the database TASK table and returns the generated TaskId.
     *
     * @param task        the task to persist
     * @param projectName the active project name
     * @return the newly generated Task with its database-assigned taskId and position
     * @throws SQLException if a database access error occurs
     */
    public Task createTask(Task task, String projectName) throws SQLException {
        try (Connection conn = connectionManager.getConnection()) {
            ensureBoardBucketsExist(conn, projectName);

            int nextPosition = 1;
            String posSql = "SELECT COALESCE(MAX(Position), 0) + 1 AS NextPos FROM TASK WHERE BucketId = ?";
            try (PreparedStatement posStmt = conn.prepareStatement(posSql)) {
                posStmt.setInt(1, task.getBucketId());
                ResultSet rs = posStmt.executeQuery();
                if (rs.next()) {
                    nextPosition = rs.getInt("NextPos");
                }
            }
            task.setPosition(nextPosition);

            String sql = "INSERT INTO TASK (Title, Body, Position, BucketId, Assignee) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, task.getTitle());
                stmt.setString(2, task.getBody());
                stmt.setInt(3, task.getPosition());
                stmt.setInt(4, task.getBucketId());
                stmt.setString(5, task.getAssignee());
                stmt.executeUpdate();

                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        task.setTaskId(generatedKeys.getInt(1));
                    }
                }
            }
        }
        return task;
    }

    /**
     * Updates a task's bucket and vertical position when it is dragged and dropped.
     *
     * @param taskId      the ID of the task being moved
     * @param newBucketId the ID of the destination bucket
     * @param newPosition the new vertical position index within the bucket
     * @throws SQLException if a database error occurs
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