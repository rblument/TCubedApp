//Edited by Dylan Cassagnol for initial sprint, week two

/*
 *  T^3: TCubed Task Tracking Tool
 * 
 *   (C) Richard Blumenthal, All rights reserved
 * 
 *   Unauthorized use, duplication or distribution without the authors'
 *   permission is strictly prohibited.
 * 
 *   Unless required by applicable law or agreed to in writing, this
 *   software is distributed on an "AS IS" basis without warranties
 *   or conditions of any kind, either expressed or implied.
 */

// Thomas Wintenburg - Initial Sprint
// Oscar Castillo - Initial Sprint Setup

package edu.regis.tcubed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.sql.*;
import jakarta.servlet.http.HttpSession;


/**
 * Main entry point for the TCubed Task Tracking Tool Spring Boot application.
 * Bootstraps the application context and embedded web server.
 *
 * @author Oscar Castillo Saucedo
 */
@SpringBootApplication
public class TCubedApp {

    /**
     * Launches the TCubed Spring Boot application.
     *
     * @param args command-line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(TCubedApp.class, args);
    }
}

/**
 * Controller handling user authentication, session management, and routing
 * between the login page and projects dashboard.
 *
 * @author Oscar Castillo Saucedo
 */
@Controller
class AuthController {
    
    // Reads the NetBeans VM Option if present; falls back to Koyeb's Environment Variable in production
    private final String dbUrl = System.getProperty("DB_URL") != null ? 
            System.getProperty("DB_URL") : System.getenv("DB_URL"); 
            
    private final String dbUser = System.getProperty("DB_USER") != null ? 
            System.getProperty("DB_USER") : System.getenv("DB_USER");
            
    private final String dbPassword = System.getProperty("DB_PASSWORD") != null ? 
            System.getProperty("DB_PASSWORD") : System.getenv("DB_PASSWORD");

    /**
     * Handles requests to the root URL ("/").
     * Redirects to "/login" if no user is logged in, or to "/projects" if authenticated.
     *
     * @param session the current {@link HttpSession}
     * @return a redirect URL to "/login" or "/projects"
     */
    @GetMapping("/")
    public String handleRoot(HttpSession session) {
        if (session == null || session.getAttribute("userId") == null) {
            return "redirect:/login";
        }
        return "redirect:/projects";
    }

    /**
     * Renders the login page view.
     *
     * @return the name of the Thymeleaf login view template ("login")
     */
    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    /**
     * Processes user login requests by validating credentials against the database.
     * Upon successful authentication, the user ID is stored in the HTTP session
     * and the client is redirected to the projects view.
     *
     * @param username the entered user ID / username credential
     * @param password the entered user password credential
     * @param model    the Spring UI {@link Model} used to pass error messages to the view
     * @param session  the {@link HttpSession} used to persist authentication state
     * @return a redirect URL to "/projects" if authentication succeeds, or the "login" view template if it fails
     */
    @PostMapping("/login")
    // Added HttpSession session to the parameters
    public String handleLogin(@RequestParam String username, @RequestParam String password, Model model, HttpSession session) {
        String secureUrl = dbUrl + (dbUrl.contains("?") ? "&" : "?") + "sslmode=REQUIRED";

        try (Connection conn = DriverManager.getConnection(secureUrl, dbUser, dbPassword)) {
            String sql = "SELECT * FROM Account WHERE UserId = ? AND Password = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                stmt.setString(2, password);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    // Save the username to the session instead of the model
                    session.setAttribute("username", rs.getString("userId"));
                    session.setAttribute("userId", rs.getString("userId"));
                    return "redirect:/projects"; 
                }
            }
        } catch (SQLException e) {
            model.addAttribute("error", "Database Error: " + e.getMessage());
            return "login";
        }
        model.addAttribute("error", "Invalid Credentials!");
        return "login";
    }
    
    /**
     * Renders the projects view for authenticated users.
     *
     * @return the name of the Thymeleaf projects view template ("projects")
     */
    @GetMapping("/projects")
    public String showProjects() {
        return "projects";
    }
    
    /**
     * Logs the current user out by invalidating their active HTTP session.
     *
     * @param session the {@link HttpSession} to invalidate
     * @return a redirect URL to the login page ("redirect:/login")
     */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // Destroys the session data
        return "redirect:/login";  // Routes back to the login page
    }
}

