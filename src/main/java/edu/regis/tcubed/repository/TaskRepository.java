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
 * @author Thomas Wintenburg, Oscar Castillo Saucedo, Dylan Cassagnol
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
            int position = i + 1;
            try (PreparedStatement checkBucket = conn.prepareStatement(
                    "SELECT BucketId FROM BUCKET WHERE BoardId = ? AND Position = ?")) {
                checkBucket.setInt(1, boardId);
                checkBucket.setInt(2, position);
                ResultSet rs = checkBucket.executeQuery();
                if (!rs.next()) {
                    try (PreparedStatement insertBucket = conn.prepareStatement(
                            "INSERT INTO BUCKET (Name, Position, BoardId) VALUES (?, ?, ?)")) {
                        insertBucket.setString(1, bucketNames[i]);
                        insertBucket.setInt(2, position);
                        insertBucket.setInt(3, boardId);
                        insertBucket.executeUpdate();
                    }
                }
            }
        }
    }

    /**
     * Resolves the actual auto-incremented database BucketId corresponding to a project's
     * logical bucket position (1: To-Do, 2: In Progress, 3: Done, 4: Trash).
     *
     * @param conn           active JDBC connection
     * @param projectName    the active project name
     * @param bucketPosition logical bucket position (1..4)
     * @return the database BucketId foreign key
     * @throws SQLException if the bucket cannot be found or resolved
     */
    private int resolveDatabaseBucketId(Connection conn, String projectName, int bucketPosition) throws SQLException {
        ensureBoardBucketsExist(conn, projectName);

        String sql = "SELECT bu.BucketId " +
                     "FROM BUCKET bu " +
                     "JOIN BOARD bo ON bu.BoardId = bo.BoardId " +
                     "JOIN PROJECT p ON bo.ProjectId = p.ProjectId " +
                     "WHERE p.Name = ? AND bu.Position = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, projectName);
            stmt.setInt(2, bucketPosition);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("BucketId");
                }
            }
        }
        throw new SQLException("Could not resolve database BucketId for project: " + projectName + ", position: " + bucketPosition);
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

            String sql = "SELECT t.TaskId, t.Title, t.Body, t.Position, bu.Position AS LogicalBucketId, t.Assignee, p.Name AS ProjectName " +
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
                    Task task = new Task(
                        rs.getInt("TaskId"),
                        rs.getString("Title"),
                        rs.getString("Body"),
                        rs.getInt("Position"),
                        rs.getInt("LogicalBucketId"),
                        rs.getString("Assignee")
                    );
                    task.setProjectName(rs.getString("ProjectName"));
                    tasks.add(task);
                }
            }
        }
        return tasks;
    }

    /**
     * Fetches all tasks assigned to a specific user across all projects.
     *
     * @param assignee the name of the assigned user
     * @return list of tasks assigned to the user
     * @throws SQLException if a database error occurs
     */
    public List<Task> getTasksByAssignee(String assignee) throws SQLException {
        List<Task> tasks = new ArrayList<>();

        try (Connection conn = connectionManager.getConnection()) {
            String sql = "SELECT t.TaskId, t.Title, t.Body, t.Position, bu.Position AS LogicalBucketId, t.Assignee, p.Name AS ProjectName " +
                         "FROM TASK t " +
                         "JOIN BUCKET bu ON t.BucketId = bu.BucketId " +
                         "JOIN BOARD bo ON bu.BoardId = bo.BoardId " +
                         "JOIN PROJECT p ON bo.ProjectId = p.ProjectId " +
                         "WHERE LOWER(t.Assignee) = LOWER(?) " +
                         "ORDER BY t.Position ASC, t.TaskId ASC";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, assignee);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    Task task = new Task(
                        rs.getInt("TaskId"),
                        rs.getString("Title"),
                        rs.getString("Body"),
                        rs.getInt("Position"),
                        rs.getInt("LogicalBucketId"),
                        rs.getString("Assignee")
                    );
                    task.setProjectName(rs.getString("ProjectName"));
                    tasks.add(task);
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

            int logicalBucket = task.getBucketId() > 0 ? task.getBucketId() : 1;
            int dbBucketId = resolveDatabaseBucketId(conn, projectName, logicalBucket);

            int nextPosition = 1;
            String posSql = "SELECT COALESCE(MAX(Position), 0) + 1 AS NextPos FROM TASK WHERE BucketId = ?";
            try (PreparedStatement posStmt = conn.prepareStatement(posSql)) {
                posStmt.setInt(1, dbBucketId);
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
                stmt.setInt(4, dbBucketId);
                stmt.setString(5, task.getAssignee());
                stmt.executeUpdate();

                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        task.setTaskId(generatedKeys.getInt(1));
                    }
                }
            }
            task.setBucketId(logicalBucket);
            task.setProjectName(projectName);
        }
        return task;
    }

    /**
     * Updates a task's bucket and vertical position when it is dragged and dropped.
     *
     * @param taskId                   the ID of the task being moved
     * @param newLogicalBucketPosition the destination logical bucket position (1..4)
     * @param newPosition              the new vertical position index within the bucket
     * @throws SQLException if a database error occurs
     */
    public void updateTaskPosition(int taskId, int newLogicalBucketPosition, int newPosition) throws SQLException {
        try (Connection conn = connectionManager.getConnection()) {
            int boardId = -1;
            String findBoardSql = "SELECT bu.BoardId FROM TASK t JOIN BUCKET bu ON t.BucketId = bu.BucketId WHERE t.TaskId = ?";
            try (PreparedStatement stmt = conn.prepareStatement(findBoardSql)) {
                stmt.setInt(1, taskId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        boardId = rs.getInt("BoardId");
                    }
                }
            }

            int targetBucketId = -1;
            if (boardId != -1) {
                String findBucketSql = "SELECT BucketId FROM BUCKET WHERE BoardId = ? AND Position = ?";
                try (PreparedStatement stmt = conn.prepareStatement(findBucketSql)) {
                    stmt.setInt(1, boardId);
                    stmt.setInt(2, newLogicalBucketPosition);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            targetBucketId = rs.getInt("BucketId");
                        }
                    }
                }
            }

            if (targetBucketId == -1) {
                targetBucketId = newLogicalBucketPosition;
            }

            String sql = "UPDATE TASK SET BucketId = ?, Position = ? WHERE TaskId = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, targetBucketId);
                stmt.setInt(2, newPosition);
                stmt.setInt(3, taskId);
                stmt.executeUpdate();
            }
        }
    }
    
    /**
     * Fetches tasks.
     *  
     */
    public List<Task> getTasksForMember(
            String userId, String projectId) throws SQLException {

        List<Task> tasks = new ArrayList<>();

        String sql =
            "SELECT t.TaskId, t.Title, t.Body, t.Position, " +
            "bu.Position AS LogicalBucketId, t.Assignee, " +
            "p.Name AS ProjectName " +
            "FROM TASK t " +
            "JOIN BUCKET bu ON t.BucketId = bu.BucketId " +
            "JOIN BOARD bo ON bu.BoardId = bo.BoardId " +
            "JOIN PROJECT p ON bo.ProjectId = p.ProjectId " +
            "JOIN PROJECT_MEMBER pm ON p.ProjectId = pm.ProjectId " +
            "WHERE pm.UserId = ? AND p.ProjectId = ? " +
            "ORDER BY t.Position, t.TaskId";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userId);
            stmt.setInt(2, Integer.parseInt(projectId));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Task task = new Task(
                        rs.getInt("TaskId"),
                        rs.getString("Title"),
                        rs.getString("Body"),
                        rs.getInt("Position"),
                        rs.getInt("LogicalBucketId"),
                        rs.getString("Assignee")
                    );

                    task.setProjectName(rs.getString("ProjectName"));
                    tasks.add(task);
                }
            }
        }

        return tasks;
    }

    /**
     * Fetches a task only when the user belongs
     * to the task's project
     *
     */
    public Task findTaskForMember(
            int taskId, String userId, String projectId)
            throws SQLException {

        String sql =
            "SELECT t.TaskId, t.Title, t.Body, t.Position, " +
            "bu.Position AS LogicalBucketId, t.Assignee, " +
            "p.Name AS ProjectName " +
            "FROM TASK t " +
            "JOIN BUCKET bu ON t.BucketId = bu.BucketId " +
            "JOIN BOARD bo ON bu.BoardId = bo.BoardId " +
            "JOIN PROJECT p ON bo.ProjectId = p.ProjectId " +
            "JOIN PROJECT_MEMBER pm ON p.ProjectId = pm.ProjectId " +
            "WHERE t.TaskId = ? AND pm.UserId = ? " +
            "AND p.ProjectId = ?";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, taskId);
            stmt.setString(2, userId);
            stmt.setInt(3, Integer.parseInt(projectId));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Task task = new Task(
                        rs.getInt("TaskId"),
                        rs.getString("Title"),
                        rs.getString("Body"),
                        rs.getInt("Position"),
                        rs.getInt("LogicalBucketId"),
                        rs.getString("Assignee")
                    );

                    task.setProjectName(rs.getString("ProjectName"));
                    return task;
                }
            }
        }

        return null;
    }
    
    /**
     * Updates task details for an authorized project member.
     *
     */
    public Task updateTaskForMember(
            int taskId, Task task, String userId,
            String projectId) throws SQLException {

        String sql =
            "UPDATE TASK t " +
            "JOIN BUCKET bu ON t.BucketId = bu.BucketId " +
            "JOIN BOARD bo ON bu.BoardId = bo.BoardId " +
            "JOIN PROJECT_MEMBER pm ON bo.ProjectId = pm.ProjectId " +
            "SET t.Title = ?, t.Body = ?, t.Assignee = ? " +
            "WHERE t.TaskId = ? AND pm.UserId = ? " +
            "AND bo.ProjectId = ?";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, task.getTitle());
            stmt.setString(2, task.getBody());
            stmt.setString(3, task.getAssignee());
            stmt.setInt(4, taskId);
            stmt.setString(5, userId);
            stmt.setInt(6, Integer.parseInt(projectId));

            if (stmt.executeUpdate() == 0) {
                return null;
            }
        }

        return findTaskForMember(taskId, userId, projectId);
    }

    /**
     * Delete task only if permitted access to project.
     *
     */
    public boolean deleteTaskForMember(
            int taskId, String userId, String projectId)
            throws SQLException {

        String sql =
            "DELETE t FROM TASK t " +
            "JOIN BUCKET bu ON t.BucketId = bu.BucketId " +
            "JOIN BOARD bo ON bu.BoardId = bo.BoardId " +
            "JOIN PROJECT_MEMBER pm ON bo.ProjectId = pm.ProjectId " +
            "WHERE t.TaskId = ? AND pm.UserId = ? " +
            "AND bo.ProjectId = ?";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, taskId);
            stmt.setString(2, userId);
            stmt.setInt(3, Integer.parseInt(projectId));

            return stmt.executeUpdate() > 0;
        }
    }


}