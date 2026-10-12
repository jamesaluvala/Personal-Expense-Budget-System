package com.college.budget.config;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Small single-instance demo safeguard, not a replacement for provider/WAF rate limiting. */
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private record Window(long expires, int attempts) {}
    private final Map<String,Window> windows = new ConcurrentHashMap<>();
    private synchronized boolean permit(String key, long now, int limit) {
        windows.entrySet().removeIf(entry -> entry.getValue().expires() <= now);
        if (windows.size() >= 10000 && !windows.containsKey(key)) return false;
        Window current = windows.get(key);
        if (current == null) current = new Window(now + 900000L, 0);
        if (current.attempts() >= limit) return false;
        windows.put(key, new Window(current.expires(), current.attempts()+1));
        return true;
    }
    @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (req.getRequestURI().startsWith("/api/")) {
            res.setHeader("Cache-Control", "no-store, private");
            res.setHeader("Pragma", "no-cache");
            if (req.getContentLengthLong() > 32768) {
                res.setStatus(413); res.setContentType("application/json");
                res.getWriter().write("{\"message\":\"Request body is too large.\"}"); return;
            }
        }
        if (req.getMethod().equals("POST")) {
            String key = null; int limit = 15;
            if (req.getRequestURI().equals("/api/auth/login")) {
                String email = Optional.ofNullable(req.getParameter("email")).orElse("").trim().toLowerCase(Locale.ROOT);
                key = "login:" + email;
            } else if (req.getRequestURI().equals("/api/auth/register")) {
                key = "register:" + req.getRemoteAddr(); limit = 30;
            }
            if (key != null && !permit(key, System.currentTimeMillis(), limit)) {
                res.setStatus(429); res.setHeader("Retry-After", "900"); res.setContentType("application/json");
                res.getWriter().write("{\"message\":\"Too many attempts. Please wait 15 minutes.\"}"); return;
            }
        }
        chain.doFilter(req,res);
    }
}
