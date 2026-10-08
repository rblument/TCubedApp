package edu.regis.tcubed;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor that enforces session-based authentication on protected routes.
 * Redirects unauthenticated requests to the login page.
 *
 * @author Oscar Castillo Saucedo
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /**
     * Default constructor for {@code AuthInterceptor}.
     */
    public AuthInterceptor() {
    }

    /**
     * Intercepts incoming HTTP requests before reaching the controller handler.
     * Checks if the active session contains a valid "userId" attribute.
     * If unauthenticated, redirects to "/login" and returns {@code false}.
     *
     * @param request  the current HTTP request
     * @param response the current HTTP response
     * @param handler  the chosen handler to execute
     * @return {@code true} if authenticated; {@code false} if redirected to "/login"
     * @throws Exception in case of redirection errors
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession();
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect("/login");
            return false;
        }
        return true;
    }
}
