package edu.regis.tcubed;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

    /**
     * Displays the dashboard view for a specified project, populating the model
     * with project details and associated tasks.
     *
     * @param project the name of the project to display, or null to use the default project
     * @param model   the Spring UI {@link Model} used to pass data to the view
     * @return the name of the Thymeleaf view template ("dashboard")
     */
    @GetMapping("/dashboard")
    public String showDashboard(@RequestParam(name="project", required=false) String project, Model model) {
        model.addAttribute("projectName", project != null ? project : "Default Project");

        // Create the Mock Data (Faking the database)
        List<Task> mockTasks = new ArrayList<>();
        mockTasks.add(new Task(101, "Draft ER Diagram", "Design the MySQL schema", 1, 1, "Dylan"));
        mockTasks.add(new Task(102, "Scaffold UI", "Build Thymeleaf board", 1, 2, "Oscar"));
        mockTasks.add(new Task(103, "Setup Git Repo", "Initialize branch structure", 1, 3, "Thomas"));
        mockTasks.add(new Task(104, "Research SortableJS", "Look into drag-and-drop libraries", 2, 1, "Oscar"));

        // Inject the data into the HTML model
        model.addAttribute("tasks", mockTasks);

        return "dashboard";
    }
}
