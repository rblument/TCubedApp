package edu.regis.tcubed.controller;

import edu.regis.tcubed.model.Account;
import edu.regis.tcubed.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit tests verifying login handling, registration routing, and initial avatar session bindings
 * in {@link AuthController}.
 *
 * @author Oscar Castillo Saucedo
 */
class AuthControllerTest {

    private AccountRepository accountRepository;
    private AuthController controller;
    private MockHttpSession session;
    private ExtendedModelMap model;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        controller = new AuthController(accountRepository);
        session = new MockHttpSession();
        model = new ExtendedModelMap();
    }

    @Test
    @DisplayName("handleLogin binds username, userId, and userInitials upon successful authentication")
    void handleLoginSuccessfulBindsInitials() throws SQLException {
        Account user = new Account("test@regis.edu", "hashed", "Oscar", "Castillo", 0, "Denver", true);
        when(accountRepository.authenticateUser("test@regis.edu", "secret")).thenReturn(user);

        String view = controller.handleLogin("test@regis.edu", "secret", model, session);

        assertEquals("redirect:/my-tasks", view);
        assertEquals("Oscar", session.getAttribute("username"));
        assertEquals("test@regis.edu", session.getAttribute("userId"));
        assertEquals("OC", session.getAttribute("userInitials"));
    }

    @Test
    @DisplayName("handleLogin adds error model attribute on failed authentication")
    void handleLoginFailedAuthentication() throws SQLException {
        when(accountRepository.authenticateUser("unknown@regis.edu", "badpass")).thenReturn(null);

        String view = controller.handleLogin("unknown@regis.edu", "badpass", model, session);

        assertEquals("login", view);
        assertEquals("Invalid Credentials", model.get("error"));
    }
}
