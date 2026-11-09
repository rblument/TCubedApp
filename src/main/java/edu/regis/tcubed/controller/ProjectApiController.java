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
 * @author Dylan Cassagnol, Oscar Castillo Saucedo
 * RESTful HTTP endpoints for project workspace management, member associations,
 * task retrieval, creation, updates, and deletion.
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

    /**
     * Lists all projects the authenticated user belongs to.
     *
     * @param session the current HTTP session
     * @return 200 OK with project list, or 401 if unauthenticated
     * @throws SQLException if a database error occurs
     */
    @GetMapping
    public ResponseEntity<?> list(HttpSession session) throws SQLException {
        if (user(session) == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(
            repository.getProjectsByUserId(user(session))
        );
    }

    /**
     * Retrieves a single project by ID if the authenticated user is a member.
     *
     * @param id      the project ID
     * @param session the current HTTP session
     * @return 200 OK with project details, 404 if not found, or 401 if unauthenticated
     * @throws SQLException if a database error occurs
     */
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

    /**
     * Creates a new project workspace, establishes membership for the creator, and builds
     * default board and bucket structures.
     *
     * @param body    the request payload with name and optional description
     * @param session the current HTTP session
     * @return 201 Created with the new Project, 400 Bad Request if invalid, or 401 if unauthenticated
     * @throws SQLException if a database error occurs
     */
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

    /**
     * Updates an existing project workspace name and description.
     *
     * @param id      the project ID
     * @param body    the request payload containing updated name and description
     * @param session the current HTTP session
     * @return 200 OK with updated Project, 404 if not found, 400 if invalid, or 401 if unauthenticated
     * @throws SQLException if a database error occurs
     */
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

    /**
     * Deletes a project workspace and cascades deletion through tasks, buckets, boards,
     * and member rows in foreign key order.
     *
     * @param id      the project ID to delete
     * @param session the current HTTP session
     * @return 204 No Content if deleted, 404 if not found, 409 on integrity conflict, or 401 if unauthenticated
     * @throws SQLException if a database error occurs
     */
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

    /**
     * Retrieves all member user IDs assigned to a project workspace.
     *
     * @param id      the project ID
     * @param session the current HTTP session
     * @return 200 OK with the member user ID list, or 401 if unauthenticated
     * @throws SQLException if a database error occurs
     */
    @GetMapping("/{id}/members")
    public ResponseEntity<?> getMembers(
            @PathVariable int id,
            HttpSession session) throws SQLException {

        if (user(session) == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(repository.getProjectMembers(id, user(session)));
    }

    /**
     * Adds a new member to a project workspace, resolving either a direct UserId (email)
     * or a matching FirstName against ACCOUNT.
     *
     * @param id      the project ID
     * @param body    the request payload containing "userId"
     * @param session the current HTTP session
     * @return 200 OK with the updated member list, 400 Bad Request if user account not found or invalid,
     *         403 Forbidden if unauthorized, or 401 if unauthenticated
     * @throws SQLException if a database error occurs
     */
    @PostMapping("/{id}/members")
    public ResponseEntity<?> addMember(
            @PathVariable int id,
            @RequestBody Map<String, String> body,
            HttpSession session) throws SQLException {

        if (user(session) == null) {
            return ResponseEntity.status(401).build();
        }

        if (body == null || body.get("userId") == null || body.get("userId").isBlank()) {
            return ResponseEntity.badRequest().body("User ID is required");
        }

        try {
            boolean success = repository.addProjectMember(id, body.get("userId").trim(), user(session));
            if (!success) {
                return ResponseEntity.status(403).body("Not authorized to modify this project");
            }
            return ResponseEntity.ok(repository.getProjectMembers(id, user(session)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /**
     * Removes an existing member from a project workspace.
     *
     * @param id       the project ID
     * @param memberId the target member's user ID to remove
     * @param session  the current HTTP session
     * @return 200 OK with updated member list, 404 if not found, or 401 if unauthenticated
     * @throws SQLException if a database error occurs
     */
    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<?> removeMember(
            @PathVariable int id,
            @PathVariable String memberId,
            HttpSession session) throws SQLException {

        if (user(session) == null) {
            return ResponseEntity.status(401).build();
        }

        boolean removed = repository.removeProjectMember(id, memberId, user(session));
        if (!removed) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(repository.getProjectMembers(id, user(session)));
    }
}
