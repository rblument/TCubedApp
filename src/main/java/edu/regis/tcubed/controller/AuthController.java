package edu.regis.tcubed.controller;

import edu.regis.tcubed.model.Account;
import edu.regis.tcubed.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.sql.SQLException;

/**
 * Controller handling user authentication, session management, and routing
 * between the login page and projects dashboard.
 *
 * @author Oscar Castillo Saucedo, Thomas Wintenburg
 */
@Controller
public class AuthController {
    
    private final AccountRepository accountRepository;
    
    @Autowired
    public AuthController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }
    
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
    public String handleLogin(@RequestParam String username, @RequestParam String password, Model model, HttpSession session) {
        try {
            // Ask the repository for the Account object
            Account loggedInUser = accountRepository.authenticateUser(username, password);

            if (loggedInUser != null) {
                // Use the Account object's getters to set up the session
                session.setAttribute("username", loggedInUser.getFirstName());
                session.setAttribute("userId", loggedInUser.getUserId());
                return "redirect:/projects";
            } else {
                model.addAttribute("error", "Invalid Credentials");
            }
        } catch (SQLException e) {
            model.addAttribute("error", "Database Error: " + e.getMessage());
        }
        
        return "login";
    }
    
    /**
     * Logs the current user out by invalidating their active HTTP session.
     *
     * @param request the {@link HttpServletRequest} containing the active session
     * @return a redirect URL to the login page ("redirect:/login")
     */
    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate(); // Destroys the session data
        }
        return "redirect:/login"; // Routes back to the login page
    }  
}
