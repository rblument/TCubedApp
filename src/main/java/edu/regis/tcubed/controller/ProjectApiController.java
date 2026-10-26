package edu.regis.tcubed.controller;

import edu.regis.tcubed.model.Project;
import edu.regis.tcubed.repository.ProjectRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.Map;

/**
 * Project API Controller
 *
 * @author Dylan Cassagnol
 * restful HTTP requests for project management,
 * fetch, create, update, and delete
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectApiController {

    private final ProjectRepository repository;

    public ProjectApiController(ProjectRepository repository) {
        this.repository = repository;
    }

    // fetch authenticated user from session
    private String user(HttpSession session) {
        return (String) session.getAttribute("userId");
    }

    // validate information
    private boolean valid(Map<String, String> body) {
        return body != null
            && body.get("name") != null
            && !body.get("name").isBlank();
    }

    // GET all projects
    @GetMapping
    public ResponseEntity<?> list(HttpSession session)
            throws SQLException {

        if (user(session) == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(
            repository.getProjectsByUserId(user(session))
        );
    }

    // GET project
    @GetMapping("/{id}")
    public ResponseEntity<?> get(
            @PathVariable int id,
            HttpSession session) throws SQLException {

        if (user(session) == null) {
            return ResponseEntity.status(401).build();
        }

        Project project = repository.findProject(
            id,
            user(session)
        );

        return project == null
            ? ResponseEntity.notFound().build()
            : ResponseEntity.ok(project);
    }

    // POST new project
    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody Map<String, String> body,
            HttpSession session) throws SQLException {

        if (user(session) == null) {
            return ResponseEntity.status(401).build();
        }

        if (!valid(body)) {
            return ResponseEntity.badRequest()
                .body("Project name is required");
        }

        Project created = repository.createProject(
            body.get("name"),
            body.get("description"),
            user(session)
        );

        return ResponseEntity.status(201).body(created);
    }

    // PUT existing project
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable int id,
            @RequestBody Map<String, String> body,
            HttpSession session) throws SQLException {

        if (user(session) == null) {
            return ResponseEntity.status(401).build();
        }

        if (!valid(body)) {
            return ResponseEntity.badRequest()
                .body("Project name is required");
        }

        Project updated = repository.updateProject(
            id,
            body.get("name"),
            body.get("description"),
            user(session)
        );

        return updated == null
            ? ResponseEntity.notFound().build()
            : ResponseEntity.ok(updated);
    }

    // DELETE project
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable int id,
            HttpSession session) throws SQLException {

        if (user(session) == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            boolean deleted = repository.deleteProject(
                id,
                user(session)
            );

            return deleted
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();

        } catch (java.sql.SQLIntegrityConstraintViolationException ex) {
            return ResponseEntity.status(409)
                .body("Project contains dependent records");
        }
    }
}
