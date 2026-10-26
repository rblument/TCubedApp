package edu.regis.tcubed.controller;

import edu.regis.tcubed.model.Task;
import edu.regis.tcubed.repository.TaskRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.sql.SQLException;

/**
 * TCubed - Task API Controller
 *
 * restful requests for task management Restricts task operations to
 * authenticated project members.
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskApiController {

    private final TaskRepository repository;

    public TaskApiController(TaskRepository repository) {
        this.repository = repository;
    }

    private String user(HttpSession session) {
        return (String) session.getAttribute("userId");
    }

    private String project(HttpSession session) {
        Object value = session.getAttribute("activeProjectId");

        return value instanceof String
                && !((String) value).isBlank()
                ? (String) value
                : null;
    }

    // GET - Retrieve all tasks for active project
    @GetMapping
    public ResponseEntity<?> list(HttpSession session)
            throws SQLException {

        if (project(session) == null) {
            return ResponseEntity.badRequest()
                    .body("Select an active project first");
        }

        return ResponseEntity.ok(
                repository.getTasksForMember(
                        user(session),
                        project(session)
                )
        );
    }

    // GET - Retrieve individual task
    @GetMapping("/{id}")
    public ResponseEntity<?> get(
            @PathVariable int id,
            HttpSession session) throws SQLException {

        if (project(session) == null) {
            return ResponseEntity.badRequest()
                    .body("Select an active project first");
        }

        Task task = repository.findTaskForMember(
                id,
                user(session),
                project(session)
        );

        return task == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(task);
    }

    // PUT - Update existing task
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable int id,
            @RequestBody Task task,
            HttpSession session) throws SQLException {

        if (project(session) == null) {
            return ResponseEntity.badRequest()
                    .body("Select an active project first");
        }

        if (task == null
                || task.getTitle() == null
                || task.getTitle().isBlank()) {

            return ResponseEntity.badRequest()
                    .body("Task title is required");
        }

        Task updated = repository.updateTaskForMember(
                id,
                task,
                user(session),
                project(session)
        );

        return updated == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(updated);
    }

    // DELETE - Delete task
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable int id,
            HttpSession session) throws SQLException {

        if (project(session) == null) {
            return ResponseEntity.badRequest()
                    .body("Select an active project first");
        }

        return repository.deleteTaskForMember(
                id,
                user(session),
                project(session)
        )
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
