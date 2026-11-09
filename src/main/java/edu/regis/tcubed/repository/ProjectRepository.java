package edu.regis.tcubed.repository;

import edu.regis.tcubed.model.Project;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for managing Project persistence, member associations in PROJECT_MEMBER,
 * and workspace lifecycle operations.
 *
 * @author Dylan Cassagnol, Oscar Castillo Saucedo
 */
@Repository
public class ProjectRepository {

    private final DatabaseConnectionManager connectionManager;

    @Autowired
    public ProjectRepository(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }
    
    /**
     * Retrieves all projects associated with a given user ID through the PROJECT_MEMBER table.
     *
     * @param userId the unique identifier of the user
     * @return list of {@link Project} instances the user is a member of
     * @throws SQLException if a database access error occurs
     */
    public List<Project> getProjectsByUserId(String userId) throws SQLException {
        List<Project> projects = new ArrayList<>();
        if (userId == null || userId.isBlank()) {
            return projects;
        }

        try (Connection conn = connectionManager.getConnection()) {
            // Join PROJECT and PROJECT_MEMBER to only get projects this user belongs to
            String sql = "SELECT p.ProjectId, p.Name, p.Description " +
                         "FROM PROJECT p " +
                         "JOIN PROJECT_MEMBER pm ON p.ProjectId = pm.ProjectId " +
                         "WHERE pm.UserId = ?";
                         
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        projects.add(new Project(
                            rs.getInt("ProjectId"),
                            rs.getString("Name"),
                            rs.getString("Description")
                        ));
                    }
                }
            }
        }
        return projects;
    }
    
    /**
     * Fetches a project by its ID if the requesting user is an authorized member.
     *
     * @param projectId the unique identifier of the project
     * @param userId    the ID of the requesting user
     * @return the matching Project, or null if not found or the user is not a member
     * @throws SQLException if a database operation fails
     */
    public Project findProject(int projectId, String userId) throws SQLException {
        String sql = "SELECT p.ProjectId, p.Name, p.Description "
                   + "FROM PROJECT p "
                   + "JOIN PROJECT_MEMBER pm ON p.ProjectId = pm.ProjectId "
                   + "WHERE p.ProjectId = ? AND pm.UserId = ?";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, projectId);
            stmt.setString(2, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Project(
                            rs.getInt("ProjectId"),
                            rs.getString("Name"),
                            rs.getString("Description")
                    );
                }
            }
        }

        return null;
    }

    /**
     * Creates a new project workspace and registers the creator as a member in PROJECT_MEMBER.
     * In the same transaction, creates a default BOARD (name + " Board", "Default Kanban Board")
     * and the four standard BUCKET columns (1: To-Do, 2: In Progress, 3: Done, 4: Trash).
     *
     * @param name        the name of the new project workspace
     * @param description project's description
     * @param userId      ID of the creating user
     * @return newly created Project
     * @throws SQLException if database operation fails
     */
    public Project createProject(String name, String description, String userId) throws SQLException {
        String projectSql = "INSERT INTO PROJECT (Name, Description) VALUES (?, ?)";
        String memberSql  = "INSERT INTO PROJECT_MEMBER (ProjectId, UserId) VALUES (?, ?)";
        String boardSql   = "INSERT INTO BOARD (Name, Description, ProjectId) VALUES (?, ?, ?)";
        String bucketSql  = "INSERT INTO BUCKET (Name, Position, BoardId) VALUES (?, ?, ?)";

        try (Connection conn = connectionManager.getConnection()) {
            conn.setAutoCommit(false);

            try {
                int projectId;
                try (PreparedStatement stmt = conn.prepareStatement(projectSql, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, name);
                    stmt.setString(2, description);
                    stmt.executeUpdate();

                    try (ResultSet keys = stmt.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Failed to generate project ID");
                        }
                        projectId = keys.getInt(1);
                    }
                }

                try (PreparedStatement stmt = conn.prepareStatement(memberSql)) {
                    stmt.setInt(1, projectId);
                    stmt.setString(2, userId);
                    stmt.executeUpdate();
                }

                int boardId;
                try (PreparedStatement stmt = conn.prepareStatement(boardSql, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, name + " Board");
                    stmt.setString(2, "Default Kanban Board");
                    stmt.setInt(3, projectId);
                    stmt.executeUpdate();

                    try (ResultSet keys = stmt.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Failed to generate board ID");
                        }
                        boardId = keys.getInt(1);
                    }
                }

                String[] bucketNames = {"To-Do", "In Progress", "Done", "Trash"};
                try (PreparedStatement stmt = conn.prepareStatement(bucketSql)) {
                    for (int i = 0; i < bucketNames.length; i++) {
                        stmt.setString(1, bucketNames[i]);
                        stmt.setInt(2, i + 1);
                        stmt.setInt(3, boardId);
                        stmt.executeUpdate();
                    }
                }

                conn.commit();
                return new Project(projectId, name, description);

            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Updates project details for an authorized member.
     *
     * @param projectId   the unique identifier of the project
     * @param name        the updated project name
     * @param description the updated project description
     * @param userId      the ID of the requesting user
     * @return the updated Project, or null if no row was updated
     * @throws SQLException if database operation fails
     */
    public Project updateProject(int projectId, String name, String description, String userId) throws SQLException {
        String sql = "UPDATE PROJECT SET Name = ?, Description = ? "
                   + "WHERE ProjectId = ? AND EXISTS ("
                   + "SELECT 1 FROM PROJECT_MEMBER "
                   + "WHERE ProjectId = ? AND UserId = ?)";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);
            stmt.setString(2, description);
            stmt.setInt(3, projectId);
            stmt.setInt(4, projectId);
            stmt.setString(5, userId);

            if (stmt.executeUpdate() == 0) {
                return null;
            }
        }

        return new Project(projectId, name, description);
    }

    /**
     * Cleanly deletes a project workspace and all associated child rows in proper foreign-key
     * order (TASK -> BUCKET -> BOARD -> PROJECT_MEMBER -> PROJECT) within a transaction,
     * after verifying that the requesting user is an authorized member.
     *
     * @param projectId the unique identifier of the project to delete
     * @param userId    the ID of the requesting user
     * @return true if the project and its child records were deleted, false if the user is not a member
     * @throws SQLException if a database error occurs during deletion
     */
    public boolean deleteProject(int projectId, String userId) throws SQLException {
        if (userId == null || userId.isBlank()) {
            return false;
        }

        String verifySql = "SELECT 1 FROM PROJECT_MEMBER WHERE ProjectId = ? AND UserId = ?";

        try (Connection conn = connectionManager.getConnection()) {
            try (PreparedStatement verifyStmt = conn.prepareStatement(verifySql)) {
                verifyStmt.setInt(1, projectId);
                verifyStmt.setString(2, userId);
                try (ResultSet rs = verifyStmt.executeQuery()) {
                    if (!rs.next()) {
                        return false;
                    }
                }
            }

            conn.setAutoCommit(false);
            try {
                // 1. Delete TASK rows belonging to this project
                String deleteTaskSql = "DELETE FROM TASK WHERE BucketId IN ("
                        + "SELECT bu.BucketId FROM BUCKET bu "
                        + "JOIN BOARD bo ON bu.BoardId = bo.BoardId "
                        + "WHERE bo.ProjectId = ?)";
                try (PreparedStatement stmt = conn.prepareStatement(deleteTaskSql)) {
                    stmt.setInt(1, projectId);
                    stmt.executeUpdate();
                }

                // 2. Delete BUCKET rows belonging to this project's boards
                String deleteBucketSql = "DELETE FROM BUCKET WHERE BoardId IN ("
                        + "SELECT bo.BoardId FROM BOARD bo WHERE bo.ProjectId = ?)";
                try (PreparedStatement stmt = conn.prepareStatement(deleteBucketSql)) {
                    stmt.setInt(1, projectId);
                    stmt.executeUpdate();
                }

                // 3. Delete BOARD rows belonging to this project
                String deleteBoardSql = "DELETE FROM BOARD WHERE ProjectId = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteBoardSql)) {
                    stmt.setInt(1, projectId);
                    stmt.executeUpdate();
                }

                // 4. Delete PROJECT_MEMBER rows for this project
                String deleteMemberSql = "DELETE FROM PROJECT_MEMBER WHERE ProjectId = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteMemberSql)) {
                    stmt.setInt(1, projectId);
                    stmt.executeUpdate();
                }

                // 5. Delete PROJECT row itself
                String deleteProjectSql = "DELETE FROM PROJECT WHERE ProjectId = ?";
                int rowsDeleted;
                try (PreparedStatement stmt = conn.prepareStatement(deleteProjectSql)) {
                    stmt.setInt(1, projectId);
                    rowsDeleted = stmt.executeUpdate();
                }

                conn.commit();
                return rowsDeleted > 0;

            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Verifies that the requesting user is a member of the project, then returns all
     * user IDs assigned to the project from PROJECT_MEMBER.
     *
     * @param projectId        the unique identifier of the project workspace
     * @param requestingUserId the user ID of the requesting user
     * @return list of member user IDs, or an empty list if the requesting user is not a member
     * @throws SQLException if a database operation fails
     */
    public List<String> getProjectMembers(int projectId, String requestingUserId) throws SQLException {
        List<String> members = new ArrayList<>();
        if (requestingUserId == null || requestingUserId.isBlank()) {
            return members;
        }

        String verifySql = "SELECT 1 FROM PROJECT_MEMBER WHERE ProjectId = ? AND UserId = ?";
        String querySql = "SELECT UserId FROM PROJECT_MEMBER WHERE ProjectId = ? ORDER BY UserId ASC";

        try (Connection conn = connectionManager.getConnection()) {
            try (PreparedStatement verifyStmt = conn.prepareStatement(verifySql)) {
                verifyStmt.setInt(1, projectId);
                verifyStmt.setString(2, requestingUserId);
                try (ResultSet rs = verifyStmt.executeQuery()) {
                    if (!rs.next()) {
                        return members;
                    }
                }
            }

            try (PreparedStatement queryStmt = conn.prepareStatement(querySql)) {
                queryStmt.setInt(1, projectId);
                try (ResultSet rs = queryStmt.executeQuery()) {
                    while (rs.next()) {
                        members.add(rs.getString("UserId"));
                    }
                }
            }
        }

        return members;
    }

    /**
     * Adds a user to a project workspace. Verifies that the requesting user is an authorized
     * member of the project, checks that targetUserId exists in ACCOUNT (matching UserId or
     * resolving a matching FirstName to UserId), and inserts (projectId, resolvedUserId) into
     * PROJECT_MEMBER if not already present.
     *
     * @param projectId        the unique identifier of the project workspace
     * @param targetUserId     the target user ID or first name to add
     * @param requestingUserId the user ID of the requesting user
     * @return true if the user was added or was already a member; false if the requesting user is not authorized
     * @throws IllegalArgumentException if the target user account cannot be found in ACCOUNT
     * @throws SQLException             if a database operation fails
     */
    public boolean addProjectMember(int projectId, String targetUserId, String requestingUserId) throws SQLException {
        if (requestingUserId == null || requestingUserId.isBlank()) {
            return false;
        }
        if (targetUserId == null || targetUserId.isBlank()) {
            throw new IllegalArgumentException("User account not found");
        }

        String trimmedTarget = targetUserId.trim();

        try (Connection conn = connectionManager.getConnection()) {
            // 1. Verify requesting user is a member
            String verifySql = "SELECT 1 FROM PROJECT_MEMBER WHERE ProjectId = ? AND UserId = ?";
            try (PreparedStatement verifyStmt = conn.prepareStatement(verifySql)) {
                verifyStmt.setInt(1, projectId);
                verifyStmt.setString(2, requestingUserId);
                try (ResultSet rs = verifyStmt.executeQuery()) {
                    if (!rs.next()) {
                        return false;
                    }
                }
            }

            // 2. Resolve targetUserId against ACCOUNT(UserId) or ACCOUNT(FirstName)
            String resolvedUserId = null;
            String findByUserIdSql = "SELECT UserId FROM ACCOUNT WHERE UserId = ?";
            try (PreparedStatement stmt = conn.prepareStatement(findByUserIdSql)) {
                stmt.setString(1, trimmedTarget);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        resolvedUserId = rs.getString("UserId");
                    }
                }
            }

            if (resolvedUserId == null) {
                String findByFirstNameSql = "SELECT UserId FROM ACCOUNT WHERE LOWER(FirstName) = LOWER(?)";
                try (PreparedStatement stmt = conn.prepareStatement(findByFirstNameSql)) {
                    stmt.setString(1, trimmedTarget);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            resolvedUserId = rs.getString("UserId");
                        }
                    }
                }
            }

            if (resolvedUserId == null) {
                throw new IllegalArgumentException("User account not found");
            }

            // 3. Check if already a member
            String checkMemberSql = "SELECT 1 FROM PROJECT_MEMBER WHERE ProjectId = ? AND UserId = ?";
            try (PreparedStatement stmt = conn.prepareStatement(checkMemberSql)) {
                stmt.setInt(1, projectId);
                stmt.setString(2, resolvedUserId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return true;
                    }
                }
            }

            // 4. Insert into PROJECT_MEMBER
            String insertSql = "INSERT INTO PROJECT_MEMBER (ProjectId, UserId) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                stmt.setInt(1, projectId);
                stmt.setString(2, resolvedUserId);
                return stmt.executeUpdate() > 0;
            }
        }
    }

    /**
     * Removes a target user from a project workspace after verifying that the requesting user
     * is an authorized member of the project.
     *
     * @param projectId        the unique identifier of the project workspace
     * @param targetUserId     the user ID of the member to remove
     * @param requestingUserId the user ID of the requesting user
     * @return true if the member was removed; false if requesting user is not a member or target was not found
     * @throws SQLException if a database operation fails
     */
    public boolean removeProjectMember(int projectId, String targetUserId, String requestingUserId) throws SQLException {
        if (requestingUserId == null || requestingUserId.isBlank() || targetUserId == null || targetUserId.isBlank()) {
            return false;
        }

        String verifySql = "SELECT 1 FROM PROJECT_MEMBER WHERE ProjectId = ? AND UserId = ?";
        String deleteSql = "DELETE FROM PROJECT_MEMBER WHERE ProjectId = ? AND UserId = ?";

        try (Connection conn = connectionManager.getConnection()) {
            try (PreparedStatement verifyStmt = conn.prepareStatement(verifySql)) {
                verifyStmt.setInt(1, projectId);
                verifyStmt.setString(2, requestingUserId);
                try (ResultSet rs = verifyStmt.executeQuery()) {
                    if (!rs.next()) {
                        return false;
                    }
                }
            }

            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                deleteStmt.setInt(1, projectId);
                deleteStmt.setString(2, targetUserId.trim());
                return deleteStmt.executeUpdate() > 0;
            }
        }
    }
}
