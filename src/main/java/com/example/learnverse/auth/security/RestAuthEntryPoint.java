package com.example.learnverse.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class RestAuthEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        // Check if this is an SSE request
        String accept = request.getHeader("Accept");
        String uri = request.getRequestURI();

        log.warn("⚠️ AuthEntryPoint triggered for: {} (Accept: {})", uri, accept);

        // Don't block SSE requests - they need special handling
        if ("text/event-stream".equals(accept)) {
            log.warn("⚠️ SSE request blocked by AuthEntryPoint - this shouldn't happen!");
            // For SSE, we should not interfere - the filter should have handled it
            // Just return without setting error
            return;
        }

        // For regular requests, send 401 error
        log.error("❌ Unauthorized: {} - {}", uri, authException.getMessage());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + authException.getMessage() + "\"}");
    }
}