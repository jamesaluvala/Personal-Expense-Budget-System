package com.college.budget.config;
import com.college.budget.service.UserService;
import tools.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import java.util.Map;
@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean UserDetailsService userDetails(UserService users) {
        return email -> {
            var user = users.find(email).orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
            return User.withUsername(user.email()).password(user.passwordHash()).roles("USER").build();
        };
    }
    @Bean SecurityFilterChain security(HttpSecurity http, UserService users, ObjectMapper json) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/health", "/api/meta", "/api/auth/csrf", "/api/auth/register", "/api/auth/login").permitAll()
                .requestMatchers("/api/**").authenticated().anyRequest().permitAll())
            // Keep default CSRF protection, including login, registration and logout.
            .formLogin(form -> form.loginPage("/").loginProcessingUrl("/api/auth/login")
                .usernameParameter("email").passwordParameter("password")
                .successHandler((request,response,authentication) -> {
                    response.setContentType("application/json");
                    json.writeValue(response.getOutputStream(), users.profile(authentication));
                })
                .failureHandler((request,response,exception) -> {
                    response.setStatus(401); response.setContentType("application/json");
                    json.writeValue(response.getOutputStream(), Map.of("message","Email or password is incorrect."));
                }))
            .logout(logout -> logout.logoutUrl("/api/auth/logout").deleteCookies("LEDGER_SESSION")
                .logoutSuccessHandler((request,response,authentication) -> response.setStatus(204)))
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request,response,exception) -> {
                    response.setStatus(401); response.setContentType("application/json");
                    json.writeValue(response.getOutputStream(), Map.of("message","Please sign in to continue."));
                })
                .accessDeniedHandler((request,response,exception) -> {
                    response.setStatus(403); response.setContentType("application/json");
                    json.writeValue(response.getOutputStream(), Map.of("message","Permission or security token invalid. Refresh and sign in again."));
                }))
            .headers(headers -> headers.frameOptions(frame -> frame.deny())
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'")))
            .addFilterBefore(new AuthRateLimitFilter(), CsrfFilter.class);
        // SecurityContext session persistence and session fixation protection are handled by Spring Security.
        return http.build();
    }
}
