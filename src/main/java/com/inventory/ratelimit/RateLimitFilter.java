package com.inventory.ratelimit;

import com.inventory.observability.MetricsService;
import com.inventory.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final MetricsService metricsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.startsWith("/api/v1/auth/") || path.startsWith("/actuator")
                || path.startsWith("/swagger") || path.startsWith("/v3/api-docs")) {
            chain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal)) {
            chain.doFilter(request, response);
            return;
        }

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        String userId = principal.getId().toString();
        String plan = principal.getPlan();

        boolean isWrite = isWriteMethod(request.getMethod());
        RateLimitResult result = rateLimitService.tryConsume(userId, plan, isWrite);

        response.setHeader("X-RateLimit-Limit", String.valueOf(result.getLimit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.getRemaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(result.getResetInSeconds()));

        if (!result.isAllowed()) {
            log.warn("Rate limit exceeded for userId={}, plan={}, path={}", userId, plan, path);
            metricsService.incrementRateLimitExceeded(path, plan);
            response.setHeader("Retry-After", String.valueOf(result.getResetInSeconds()));
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"success\":false,\"error\":\"Rate limit exceeded\",\"errorCode\":\"RATE_001\"}"
            );
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isWriteMethod(String method) {
        return "POST".equals(method) || "PUT".equals(method)
                || "PATCH".equals(method) || "DELETE".equals(method);
    }
}
