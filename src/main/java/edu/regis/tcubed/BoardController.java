package edu.regis.tcubed;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for handling dashboard and task board view operations.
 * Manages presentation of tasks and project boards in the user interface.
 *
 * @author Oscar Castillo Saucedo
 */
@Controller
public class BoardController {

    // Create the Mock Data (Faking the database)
    private final List<Task> mockTasks = new ArrayList<>(List.of(
        new Task(101, "Draft ER Diagram", "Design the MySQL schema", 1, 1, "Dylan"),
        new Task(102, "Scaffold UI", "Build Thymeleaf board", 1, 2, "Oscar"),
        new Task(103, "Setup Git Repo", "Initialize branch structure", 1, 3, "Thomas"),
        new Task(104, "Research SortableJS", "Look into drag-and-drop libraries", 2, 1, "Oscar")
    ));

    /**
     * Displays the dashboard view for a specified project, populating the model
     * with project details and associated tasks, and saving the active project to the session.
     *
     * @param project the name of the project to display, or null to use the default project
     * @param model   the Spring UI {@link Model} used to pass data to the view
     * @param session the current {@link HttpSession} used to store active project state
     * @return the name of the Thymeleaf view template ("dashboard")
     */
    @GetMapping("/dashboard")
    public String showDashboard(@RequestParam(name="project", required=false) String project, Model model, HttpSession session) {
        String activeProject = project != null ? project : (String) session.getAttribute("activeProjectId");
        if (activeProject == null) {
            activeProject = "Default Project";
        }
        session.setAttribute("activeProjectId", activeProject);
        model.addAttribute("projectName", activeProject);

        // Inject the data into the HTML model
        model.addAttribute("tasks", mockTasks);

        return "dashboard";
    }

    /**
     * REST endpoint to handle asynchronous drag-and-drop task movements.
     * Updates the task's bucket assignment and vertical position.
     * 
     * @param taskId the ID of the task being moved
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
        
        // Locate the mock task and update its properties
        for (Task task : mockTasks) {
            if (task.getTaskId() == taskId) {
                task.setBucketId(newBucketId);
                task.setPosition(newPosition);
                
                // In the future, this is where you will call:
                // taskRepository.save(task);
                
                return ResponseEntity.ok("Task moved successfully");
            }
        }
        return ResponseEntity.notFound().build();
    }
}
