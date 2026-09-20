package edu.regis.tcubed;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
public class BoardController {

    @GetMapping("/dashboard")
    public String showDashboard(@RequestParam(name="project", required=false) String project, Model model) {
        model.addAttribute("projectName", project != null ? project : "Default Project");

        // Create the Mock Data (Faking the database)
        List<Task> mockTasks = new ArrayList<>();
        mockTasks.add(new Task("TP-101", "Draft ER Diagram", "Design the MySQL schema", "To-Do", "Dylan"));
        mockTasks.add(new Task("TP-102", "Scaffold UI", "Build Thymeleaf board", "In Progress", "Oscar"));
        mockTasks.add(new Task("TP-103", "Setup Git Repo", "Initialize branch structure", "Done", "Thomas"));
        mockTasks.add(new Task("TP-104", "Research SortableJS", "Look into drag-and-drop libraries", "To-Do", "Oscar"));

        // Inject the data into the HTML model
        model.addAttribute("tasks", mockTasks);

        return "dashboard";
    }
}
