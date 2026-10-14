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
}