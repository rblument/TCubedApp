package edu.regis.tcubed.controller;

import edu.regis.tcubed.model.Task;
import edu.regis.tcubed.repository.ProjectRepository;
import edu.regis.tcubed.repository.TaskRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Controller providing REST API endpoints for workspace trash retention
 * and named archive folders.
 *
 * @author Oscar Castillo Saucedo
 */
@RestController
@RequestMapping("/api/workspace")
public class ArchiveApiController {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;

    @Autowired
    public ArchiveApiController(TaskRepository taskRepository, ProjectRepository projectRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
    }

    private String resolveProjectName(String projectName, HttpSession session) {
        if (projectName != null && !projectName.isBlank()) {
            return projectName;
        }
        String sessionProject = (String) session.getAttribute("activeProjectId");
        if (sessionProject != null && !sessionProject.isBlank()) {
            return sessionProject;
        }
        return "Capstone";
    }

    /**
     * Retrieves all trashed tasks for a given workspace with days remaining before purge.
     */
    @GetMapping("/trash")
    public ResponseEntity<?> getTrashedTasks(
            @RequestParam(name = "projectName", required = false) String projectName,
            HttpSession session) throws SQLException {
        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }

        String project = resolveProjectName(projectName, session);
        List<Task> trashed = taskRepository.getTrashedTasksByProjectName(project);
        return ResponseEntity.ok(trashed);
    }

    /**
     * Retrieves all archive folders and their archived tasks for a given workspace.
     */
    @GetMapping("/archives")
    public ResponseEntity<?> getArchiveFolders(
            @RequestParam(name = "projectName", required = false) String projectName,
            HttpSession session) throws SQLException {
        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }

        String project = resolveProjectName(projectName, session);
        List<Map<String, Object>> archives = taskRepository.getArchiveFoldersWithTasks(project);
        return ResponseEntity.ok(archives);
    }

    /**
     * Creates a new archive folder for a workspace.
     */
    @PostMapping("/archives")
    public ResponseEntity<?> createArchiveFolder(
            @RequestBody(required = false) Map<String, String> body,
            HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }
        if (body == null) {
            return ResponseEntity.badRequest().body("Request body is required");
        }

        String name = body.get("name");
        String project = resolveProjectName(body.get("projectName"), session);

        try {
            Map<String, Object> created = taskRepository.createArchiveFolder(project, name);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    /**
     * Deletes an empty archive folder by its bucketId.
     */
    @DeleteMapping("/archives/{bucketId}")
    public ResponseEntity<?> deleteArchiveFolder(
            @PathVariable int bucketId,
            @RequestParam(name = "projectName", required = false) String projectName,
            HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }

        String project = resolveProjectName(projectName, session);
        try {
            boolean deleted = taskRepository.deleteArchiveFolder(project, bucketId);
            if (deleted) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
