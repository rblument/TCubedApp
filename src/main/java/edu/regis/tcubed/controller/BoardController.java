package edu.regis.tcubed.controller;

import edu.regis.tcubed.model.Task;
import edu.regis.tcubed.repository.TaskRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for handling dashboard and task board view operations,
 * including asynchronous REST endpoints for task creation and drag-and-drop movement.
 *
 * @author Oscar Castillo Saucedo
 */
@Controller
public class BoardController {

    private final TaskRepository taskRepository;

    private final List<Task> mockTasks = new ArrayList<>(List.of(
        new Task(101, "Draft ER Diagram", "Design the MySQL schema", 1, 1, "Dylan"),
        new Task(102, "Scaffold UI", "Build Thymeleaf board", 2, 1, "Oscar"),
        new Task(103, "Setup Git Repo", "Initialize branch structure", 3, 1, "Thomas"),
        new Task(104, "Research SortableJS", "Look into drag-and-drop libraries", 1, 2, "Oscar")
    ));

    @Autowired
    public BoardController(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    /**
     * Displays the dashboard view for a specified project, loading tasks from the database.
     *
     * @param project the name of the project to display, or null to use the session active project
     * @param model   the Spring UI {@link Model} used to pass data to the view
     * @param session the current {@link HttpSession} used to store active project state
     * @return the name of the Thymeleaf view template ("dashboard")
     */
    @GetMapping("/dashboard")
    public String showDashboard(@RequestParam(name = "project", required = false) String project, Model model, HttpSession session) {
        String activeProject = project != null ? project : (String) session.getAttribute("activeProjectId");
        if (activeProject == null || activeProject.isBlank()) {
            activeProject = "Capstone";
        }
        session.setAttribute("activeProjectId", activeProject);
        model.addAttribute("projectName", activeProject);

        try {
            List<Task> dbTasks = taskRepository.getTasksByProjectName(activeProject);
            model.addAttribute("tasks", dbTasks);
        } catch (SQLException e) {
            model.addAttribute("error", "Database warning: using fallback tasks (" + e.getMessage() + ")");
            model.addAttribute("tasks", mockTasks);
        }

        return "dashboard";
    }

    /**
     * REST endpoint to create a new task asynchronously via POST and persist it in the database.
     *
     * @param newTask the {@link Task} payload deserialized from the JSON request body
     * @param session the active {@link HttpSession} containing the current project context
     * @return HTTP 201 Created with the persisted {@link Task} JSON object, or HTTP 400/500 on error
     */
    @PostMapping("/api/tasks")
    @ResponseBody
    public ResponseEntity<?> createTask(@RequestBody Task newTask, HttpSession session) {
        if (newTask.getTitle() == null || newTask.getTitle().isBlank()) {
            return ResponseEntity.badRequest().body("Task title is required.");
        }

        if (newTask.getBucketId() <= 0) {
            newTask.setBucketId(1); // Default to To-Do bucket
        }

        if (newTask.getAssignee() == null || newTask.getAssignee().isBlank()) {
            String sessionUser = (String) session.getAttribute("username");
            newTask.setAssignee(sessionUser != null ? sessionUser : "Unassigned");
        }

        String activeProject = (String) session.getAttribute("activeProjectId");
        if (activeProject == null || activeProject.isBlank()) {
            activeProject = "Capstone";
        }

        try {
            Task created = taskRepository.createTask(newTask, activeProject);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to create task in database: " + e.getMessage());
        }
    }

    /**
     * REST endpoint to handle asynchronous drag-and-drop task movements.
     * Updates the task's bucket assignment and vertical position in the database.
     *
     * @param taskId      the ID of the task being moved
     * @param newBucketId the ID of the destination bucket
     * @param newPosition the new vertical position index within the bucket
     * @return HTTP 200 OK on success
     */
    @PutMapping("/api/tasks/{taskId}/move")
    @ResponseBody
    public ResponseEntity<String> moveTask(
            @PathVariable int taskId,
            @RequestParam int newBucketId,
            @RequestParam int newPosition) {

        try {
            taskRepository.updateTaskPosition(taskId, newBucketId, newPosition);
            return ResponseEntity.ok("Task moved successfully");
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating task position: " + e.getMessage());
        }
    }
}
