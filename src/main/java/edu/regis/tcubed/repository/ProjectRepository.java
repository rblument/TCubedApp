package edu.regis.tcubed.repository;

import edu.regis.tcubed.model.Project;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ProjectRepository {

    private final DatabaseConnectionManager connectionManager;

    @Autowired
    public ProjectRepository(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }
    
    public List<Project> getProjectsByUserId(String userId) throws SQLException {
        List<Project> projects = new ArrayList<>();
        try (Connection conn = connectionManager.getConnection()) {
            // Join PROJECT and PROJECT_MEMBER to only get projects this user belongs to
            String sql = "SELECT p.ProjectId, p.Name, p.Description " +
                         "FROM PROJECT p " +
                         "JOIN PROJECT_MEMBER pm ON p.ProjectId = pm.ProjectId " +
                         "WHERE pm.UserId = ?";
                         
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, userId);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    projects.add(new Project(
                        rs.getInt("ProjectId"),
                        rs.getString("Name"),
                        rs.getString("Description")
                    ));
                }
            }
        }
        return projects;
    }
    
    /**
     * fetch project if the user = member.
     *
     * @param projectId the unique identifier of the project
     * @param userId the ID of the requesting user
     * @return the matching Project, or null if not found or the user is not a
     * member
     * @throws SQLException if a database operation fails
     */
    public Project findProject(int projectId, String userId)
            throws SQLException {

        String sql
                = "SELECT p.ProjectId, p.Name, p.Description "
                + "FROM PROJECT p "
                + "JOIN PROJECT_MEMBER pm ON p.ProjectId = pm.ProjectId "
                + "WHERE p.ProjectId = ? AND pm.UserId = ?";

        try (Connection conn = connectionManager.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

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
     * Create project & register creator as a member.
     *
     * @param name is the name of the new project
     * @param description project's description
     * @param userId  ID of the creating user
     * @return newly created Project
     * @throws SQLException if database operation fails
     *
     */
    public Project createProject(
            String name, String description, String userId)
            throws SQLException {

        String projectSql
                = "INSERT INTO PROJECT (Name, Description) VALUES (?, ?)";

        String memberSql
                = "INSERT INTO PROJECT_MEMBER (ProjectId, UserId) VALUES (?, ?)";

        try (Connection conn = connectionManager.getConnection()) {
            conn.setAutoCommit(false);

            try {
                int projectId;

                try (PreparedStatement stmt = conn.prepareStatement(
                        projectSql, Statement.RETURN_GENERATED_KEYS)) {

                    stmt.setString(1, name);
                    stmt.setString(2, description);
                    stmt.executeUpdate();

                    try (ResultSet keys = stmt.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException(
                                    "Failed to generate project ID"
                            );
                        }
                        projectId = keys.getInt(1);
                    }
                }

                try (PreparedStatement stmt
                        = conn.prepareStatement(memberSql)) {

                    stmt.setInt(1, projectId);
                    stmt.setString(2, userId);
                    stmt.executeUpdate();
                }

                conn.commit();
                return new Project(projectId, name, description);

            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    /**
     * Update project details for an authorized member.
     *
     * @param projectId the unique identifier of the project
     * @param name the updated project name
     * @param description the updated project description
     * @param userId the ID of the requesting user
     * @return the updated Project, or null if no row was updated
     * @throws SQLException if database operation fails
     */
    public Project updateProject(
            int projectId, String name,
            String description, String userId)
            throws SQLException {

        String sql
                = "UPDATE PROJECT SET Name = ?, Description = ? "
                + "WHERE ProjectId = ? AND EXISTS ("
                + "SELECT 1 FROM PROJECT_MEMBER "
                + "WHERE ProjectId = ? AND UserId = ?)";

        try (Connection conn = connectionManager.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

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
     * Delete project
     *
     * @param projectId the unique identifier of the project
     * @param userId the ID of the requesting user
     * @return true if the project was deleted, false if no matching project was
     * deleted
     * @throws SQLException if db operation fails
     */
    public boolean deleteProject(int projectId, String userId)
            throws SQLException {

        String sql = "DELETE FROM PROJECT "
                + "WHERE ProjectId = ? AND EXISTS ("
                + "SELECT 1 FROM PROJECT_MEMBER "
                + "WHERE ProjectId = ? AND UserId = ?)";

        try (Connection conn = connectionManager.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, projectId);
            stmt.setInt(2, projectId);
            stmt.setString(3, userId);

            return stmt.executeUpdate() > 0;
        }
    }
}
