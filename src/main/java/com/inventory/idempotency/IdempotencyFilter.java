package com.inventory.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.dto.response.ApiResponse;
import com.inventory.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        if ("POST".equals(request.getMethod()) && request.getRequestURI().startsWith("/api/v1/transactions/")) {
            String idempotencyKey = request.getHeader("Idempotency-Key");
            if (idempotencyKey == null || idempotencyKey.isBlank()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json");
                ApiResponse<Void> error = ApiResponse.error(
                        "Idempotency-Key header required",
                        ErrorCode.GEN_003.name());
                objectMapper.writeValue(response.getOutputStream(), error);
                return;
            }
        }

        chain.doFilter(request, response);
    }
}
