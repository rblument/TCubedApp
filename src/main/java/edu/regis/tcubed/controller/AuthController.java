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
     * Redirects to "/login" if no user is logged in, or to "/my-tasks" if authenticated.
     *
     * @param session the current {@link HttpSession}
     * @return a redirect URL to "/login" or "/my-tasks"
     */
    @GetMapping("/")
    public String handleRoot(HttpSession session) {
        if (session == null || session.getAttribute("userId") == null) {
            return "redirect:/login";
        }
        return "redirect:/my-tasks";
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
     * and the client is redirected to the user's personal task board.
     *
     * @param username the entered user ID / username credential
     * @param password the entered user password credential
     * @param model    the Spring UI {@link Model} used to pass error messages to the view
     * @param session  the {@link HttpSession} used to persist authentication state
     * @return a redirect URL to "/my-tasks" if authentication succeeds, or the "login" view template if it fails
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
                return "redirect:/my-tasks";
            } else {
                model.addAttribute("error", "Invalid Credentials");
            }
        } catch (SQLException e) {
            model.addAttribute("error", "Database Error: " + e.getMessage());
        }
        
        return "login";
    }

    /**
     * Renders the account registration page view (UC-1).
     *
     * @return the name of the Thymeleaf registration view template ("register")
     */
    @GetMapping("/register")
    public String showRegisterPage() {
        return "register";
    }

    /**
     * Processes new account creation requests (UC-1).
     * Validates input parameters, verifies unique UserId, creates the account record,
     * and redirects the user to login or auto-authenticates the session.
     *
     * @param userId          the unique user ID / email
     * @param firstName       the user's first name
     * @param lastName        the user's last name
     * @param password        the chosen password
     * @param confirmPassword confirmation of the chosen password
     * @param question        the selected security question index
     * @param answer          the security question answer
     * @param isStudent       flag indicating whether the user is a student
     * @param model           the Spring UI {@link Model} for passing feedback messages
     * @return redirect to "/login?registered=true" on success, or "register" view on error
     */
    @PostMapping("/register")
    public String handleRegister(
            @RequestParam String userId,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam int question,
            @RequestParam String answer,
            @RequestParam(defaultValue = "false") boolean isStudent,
            Model model) {

        String trimmedUserId = userId != null ? userId.trim() : "";
        if (trimmedUserId.isEmpty() || firstName.isBlank() || lastName.isBlank() || answer.isBlank()) {
            model.addAttribute("error", "All fields are required.");
            return "register";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            return "register";
        }

        if (password.length() < 6) {
            model.addAttribute("error", "Password must be at least 6 characters long.");
            return "register";
        }

        try {
            if (accountRepository.userExists(trimmedUserId)) {
                model.addAttribute("error", "An account with that User ID / Email already exists.");
                return "register";
            }

            Account newAccount = new Account(
                trimmedUserId,
                password,
                firstName.trim(),
                lastName.trim(),
                question,
                answer.trim(),
                isStudent
            );

            accountRepository.createAccount(newAccount);
            return "redirect:/login?registered=true";

        } catch (SQLException e) {
            model.addAttribute("error", "Database Error: " + e.getMessage());
            return "register";
        }
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
