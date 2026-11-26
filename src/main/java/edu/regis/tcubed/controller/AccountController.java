package edu.regis.tcubed.controller;

import edu.regis.tcubed.model.Account;
import edu.regis.tcubed.repository.AccountRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.SQLException;

/**
 * Controller for managing user account settings, profile updates, and password changes.
 *
 * @author Oscar Castillo Saucedo
 */
@Controller
public class AccountController {

    private final AccountRepository accountRepository;

    /**
     * Constructs an {@code AccountController} with the required {@link AccountRepository} dependency.
     *
     * @param accountRepository the repository for managing account persistence
     */
    @Autowired
    public AccountController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * Renders the account settings page view (/settings).
     * Retrieves the current user's account details, refreshes initials in session,
     * and binds the account to the view model.
     *
     * @param session the current {@link HttpSession}
     * @param model   the Spring UI {@link Model}
     * @return the Thymeleaf template view name ("settings") or redirect to "/login" if unauthenticated
     */
    @GetMapping("/settings")
    public String showSettings(HttpSession session, Model model) {
        if (session == null) {
            return "redirect:/login";
        }
        String userId = (String) session.getAttribute("userId");
        if (userId == null || userId.isBlank()) {
            return "redirect:/login";
        }

        try {
            Account account = accountRepository.findByUserId(userId);
            if (account == null) {
                return "redirect:/login";
            }

            session.setAttribute("userInitials", account.getInitials());
            model.addAttribute("account", account);
            return "settings";
        } catch (SQLException e) {
            model.addAttribute("error", "Database error loading account settings: " + e.getMessage());
            return "settings";
        }
    }

    /**
     * Processes profile and security detail updates for the authenticated user (/settings/profile).
     * Validates non-blank inputs, updates the database record, synchronizes session attributes,
     * and redirects back to the settings view with a status flash message.
     *
     * @param firstName          the user's updated first name
     * @param lastName           the user's updated last name
     * @param question           the index of the selected security question
     * @param answer             the updated answer to the security question
     * @param isStudent          flag indicating whether the user is a student
     * @param session            the active {@link HttpSession}
     * @param redirectAttributes redirect attributes used to pass flash messages
     * @return redirect view URL to "/settings"
     */
    @PostMapping("/settings/profile")
    public String updateProfile(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam int question,
            @RequestParam String answer,
            @RequestParam(defaultValue = "false") boolean isStudent,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (session == null) {
            return "redirect:/login";
        }
        String userId = (String) session.getAttribute("userId");
        if (userId == null || userId.isBlank()) {
            return "redirect:/login";
        }

        String trimmedFirst = firstName != null ? firstName.trim() : "";
        String trimmedLast = lastName != null ? lastName.trim() : "";
        String trimmedAnswer = answer != null ? answer.trim() : "";

        if (trimmedFirst.isBlank() || trimmedLast.isBlank() || trimmedAnswer.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "First name, last name, and security answer cannot be blank.");
            return "redirect:/settings";
        }

        try {
            boolean updated = accountRepository.updateProfile(userId, trimmedFirst, trimmedLast, question, trimmedAnswer, isStudent);
            if (updated) {
                Account updatedAccount = new Account(userId, null, trimmedFirst, trimmedLast, question, trimmedAnswer, isStudent);
                session.setAttribute("username", trimmedFirst);
                session.setAttribute("userInitials", updatedAccount.getInitials());
                redirectAttributes.addFlashAttribute("success", "Profile updated successfully.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Unable to update profile. User not found.");
            }
        } catch (SQLException e) {
            redirectAttributes.addFlashAttribute("error", "Database error updating profile: " + e.getMessage());
        }

        return "redirect:/settings";
    }

    /**
     * Processes password change requests for the authenticated user (/settings/password).
     * Validates current credentials, checks matching and minimum length of the new password,
     * updates the database record, and redirects with a status flash message.
     *
     * @param currentPassword    the user's current plaintext password for verification
     * @param newPassword        the new plaintext password
     * @param confirmPassword    confirmation matching the new password
     * @param session            the active {@link HttpSession}
     * @param redirectAttributes redirect attributes used to pass flash messages
     * @return redirect view URL to "/settings"
     */
    @PostMapping("/settings/password")
    public String updatePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (session == null) {
            return "redirect:/login";
        }
        String userId = (String) session.getAttribute("userId");
        if (userId == null || userId.isBlank()) {
            return "redirect:/login";
        }

        try {
            Account authenticated = accountRepository.authenticateUser(userId, currentPassword);
            if (authenticated == null) {
                redirectAttributes.addFlashAttribute("error", "Current password is incorrect.");
                return "redirect:/settings";
            }

            if (newPassword == null || !newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "New passwords do not match.");
                return "redirect:/settings";
            }

            if (newPassword.length() < 6) {
                redirectAttributes.addFlashAttribute("error", "New password must be at least 6 characters long.");
                return "redirect:/settings";
            }

            boolean updated = accountRepository.updatePassword(userId, newPassword);
            if (updated) {
                redirectAttributes.addFlashAttribute("success", "Password updated successfully.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Unable to update password.");
            }
        } catch (SQLException e) {
            redirectAttributes.addFlashAttribute("error", "Database error updating password: " + e.getMessage());
        }

        return "redirect:/settings";
    }
}
