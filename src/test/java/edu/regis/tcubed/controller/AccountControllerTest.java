package edu.regis.tcubed.controller;

import edu.regis.tcubed.model.Account;
import edu.regis.tcubed.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests verifying endpoint handling, session updates, and input validation
 * in {@link AccountController}.
 *
 * @author Oscar Castillo Saucedo
 */
class AccountControllerTest {

    private AccountRepository accountRepository;
    private AccountController controller;
    private MockHttpSession session;
    private ExtendedModelMap model;
    private RedirectAttributesModelMap redirectAttributes;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        controller = new AccountController(accountRepository);
        session = new MockHttpSession();
        model = new ExtendedModelMap();
        redirectAttributes = new RedirectAttributesModelMap();
    }

    @Test
    @DisplayName("showSettings redirects to /login when user is not authenticated")
    void showSettingsUnauthenticatedRedirectsToLogin() {
        String view = controller.showSettings(session, model);
        assertEquals("redirect:/login", view);

        String viewWithNullSession = controller.showSettings(null, model);
        assertEquals("redirect:/login", viewWithNullSession);
    }

    @Test
    @DisplayName("showSettings populates model and refreshes user initials when authenticated")
    void showSettingsAuthenticatedLoadsAccount() throws SQLException {
        session.setAttribute("userId", "test@regis.edu");
        Account mockAccount = new Account("test@regis.edu", null, "Testy", "McTest", 0, "Denver", true);
        when(accountRepository.findByUserId("test@regis.edu")).thenReturn(mockAccount);

        String view = controller.showSettings(session, model);

        assertEquals("settings", view);
        assertEquals("TM", session.getAttribute("userInitials"));
        assertSame(mockAccount, model.get("account"));
        verify(accountRepository).findByUserId("test@regis.edu");
    }

    @Test
    @DisplayName("showSettings redirects to /login when user cannot be found in database")
    void showSettingsUserNotFoundInDbRedirects() throws SQLException {
        session.setAttribute("userId", "deleted@regis.edu");
        when(accountRepository.findByUserId("deleted@regis.edu")).thenReturn(null);

        String view = controller.showSettings(session, model);

        assertEquals("redirect:/login", view);
    }

    @Test
    @DisplayName("showSettings handles database error gracefully")
    void showSettingsDatabaseError() throws SQLException {
        session.setAttribute("userId", "test@regis.edu");
        when(accountRepository.findByUserId("test@regis.edu")).thenThrow(new SQLException("Connection refused"));

        String view = controller.showSettings(session, model);

        assertEquals("settings", view);
        assertTrue(model.containsAttribute("error"));
    }

    @Test
    @DisplayName("updateProfile validates blank required fields and flashes error")
    void updateProfileValidationBlankFields() throws SQLException {
        session.setAttribute("userId", "test@regis.edu");

        String view = controller.updateProfile("", "Castillo", 0, "Denver", false, session, redirectAttributes);

        assertEquals("redirect:/settings", view);
        assertTrue(redirectAttributes.getFlashAttributes().containsKey("error"));
        verify(accountRepository, never()).updateProfile(anyString(), anyString(), anyString(), anyInt(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("updateProfile updates database, syncs session username and initials, and flashes success")
    void updateProfileSuccess() throws SQLException {
        session.setAttribute("userId", "test@regis.edu");
        when(accountRepository.updateProfile("test@regis.edu", "Oscar", "Castillo", 2, "Denver", true))
                .thenReturn(true);

        String view = controller.updateProfile("Oscar", "Castillo", 2, "Denver", true, session, redirectAttributes);

        assertEquals("redirect:/settings", view);
        assertEquals("Oscar", session.getAttribute("username"));
        assertEquals("OC", session.getAttribute("userInitials"));
        assertEquals("Profile updated successfully.", redirectAttributes.getFlashAttributes().get("success"));
        verify(accountRepository).updateProfile("test@regis.edu", "Oscar", "Castillo", 2, "Denver", true);
    }

    @Test
    @DisplayName("updateProfile handles database exception and flashes error")
    void updateProfileDatabaseError() throws SQLException {
        session.setAttribute("userId", "test@regis.edu");
        when(accountRepository.updateProfile(anyString(), anyString(), anyString(), anyInt(), anyString(), anyBoolean()))
                .thenThrow(new SQLException("Disk full"));

        String view = controller.updateProfile("Oscar", "Castillo", 2, "Denver", true, session, redirectAttributes);

        assertEquals("redirect:/settings", view);
        assertTrue(redirectAttributes.getFlashAttributes().containsKey("error"));
    }

    @Test
    @DisplayName("updatePassword flashes error when current password verification fails")
    void updatePasswordCurrentPasswordIncorrect() throws SQLException {
        session.setAttribute("userId", "test@regis.edu");
        when(accountRepository.authenticateUser("test@regis.edu", "wrongPassword")).thenReturn(null);

        String view = controller.updatePassword("wrongPassword", "NewPass123", "NewPass123", session, redirectAttributes);

        assertEquals("redirect:/settings", view);
        assertEquals("Current password is incorrect.", redirectAttributes.getFlashAttributes().get("error"));
        verify(accountRepository, never()).updatePassword(anyString(), anyString());
    }

    @Test
    @DisplayName("updatePassword flashes error when new passwords do not match")
    void updatePasswordMismatch() throws SQLException {
        session.setAttribute("userId", "test@regis.edu");
        Account mockAccount = new Account("test@regis.edu", "hash", "Testy", "McTest", 0, "Denver", true);
        when(accountRepository.authenticateUser("test@regis.edu", "oldPassword")).thenReturn(mockAccount);

        String view = controller.updatePassword("oldPassword", "NewPass123", "DifferentPass123", session, redirectAttributes);

        assertEquals("redirect:/settings", view);
        assertEquals("New passwords do not match.", redirectAttributes.getFlashAttributes().get("error"));
        verify(accountRepository, never()).updatePassword(anyString(), anyString());
    }

    @Test
    @DisplayName("updatePassword flashes error when new password is too short")
    void updatePasswordTooShort() throws SQLException {
        session.setAttribute("userId", "test@regis.edu");
        Account mockAccount = new Account("test@regis.edu", "hash", "Testy", "McTest", 0, "Denver", true);
        when(accountRepository.authenticateUser("test@regis.edu", "oldPassword")).thenReturn(mockAccount);

        String view = controller.updatePassword("oldPassword", "12345", "12345", session, redirectAttributes);

        assertEquals("redirect:/settings", view);
        assertEquals("New password must be at least 6 characters long.", redirectAttributes.getFlashAttributes().get("error"));
        verify(accountRepository, never()).updatePassword(anyString(), anyString());
    }

    @Test
    @DisplayName("updatePassword successfully hashes and updates password and flashes success")
    void updatePasswordSuccess() throws SQLException {
        session.setAttribute("userId", "test@regis.edu");
        Account mockAccount = new Account("test@regis.edu", "hash", "Testy", "McTest", 0, "Denver", true);
        when(accountRepository.authenticateUser("test@regis.edu", "oldPassword")).thenReturn(mockAccount);
        when(accountRepository.updatePassword("test@regis.edu", "ValidNewPass123")).thenReturn(true);

        String view = controller.updatePassword("oldPassword", "ValidNewPass123", "ValidNewPass123", session, redirectAttributes);

        assertEquals("redirect:/settings", view);
        assertEquals("Password updated successfully.", redirectAttributes.getFlashAttributes().get("success"));
        verify(accountRepository).updatePassword("test@regis.edu", "ValidNewPass123");
    }
}
