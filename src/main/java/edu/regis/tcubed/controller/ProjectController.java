package edu.regis.tcubed.controller;

import edu.regis.tcubed.model.Project;
import edu.regis.tcubed.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpSession;
import java.sql.SQLException;
import java.util.List;

@Controller
public class ProjectController {

    private final ProjectRepository projectRepository;

    @Autowired
    public ProjectController(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @GetMapping("/projects")
    public String showProjects(HttpSession session, Model model) {
        // 1. Get the logged-in user's ID from the session
        String userId = (String) session.getAttribute("userId");
        
        try {
            // 2. Fetch their specific projects from the database
            List<Project> userProjects = projectRepository.getProjectsByUserId(userId);
            
            // 3. Add the list to the model so Thymeleaf can read it
            model.addAttribute("projects", userProjects);
            
        } catch (SQLException e) {
            model.addAttribute("error", "Could not load projects: " + e.getMessage());
        }

        return "projects";
    }
}