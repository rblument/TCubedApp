package edu.regis.tcubed;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC configuration for registering application interceptors and
 * configuring route mappings.
 *
 * @author Oscar Castillo Saucedo
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    /**
     * Constructs a {@code WebConfig} instance with dependency-injected {@link AuthInterceptor}.
     *
     * @param authInterceptor the authentication interceptor to register
     */
    @Autowired
    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    /**
     * Registers application interceptors to secure specific endpoints.
     * Applies {@link AuthInterceptor} to "/dashboard" and "/projects",
     * while explicitly excluding "/login", "/", and static resources ("/css/**", "/js/**").
     *
     * @param registry the {@link InterceptorRegistry} to configure
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/dashboard", "/projects")
                .excludePathPatterns("/login", "/", "/css/**", "/js/**");
    }
}
