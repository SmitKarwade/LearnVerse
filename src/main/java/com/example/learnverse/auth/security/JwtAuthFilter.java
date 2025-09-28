package com.example.learnverse.auth.security;

import com.example.learnverse.auth.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extractTokenFromRequest(request);

        if (token != null) {
            try {
                // Use your existing validateAndParse method
                var jws = jwtUtil.validateAndParse(token);
                Claims claims = jws.getPayload(); // Get claims from Jws<Claims>

                String userId = claims.getSubject();
                String role = claims.get("role", String.class);

                log.info("✅ Filter: JWT Valid - UserId: {}, Role: {}", userId, role);

                // Create authorities
                List<GrantedAuthority> authorities = Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + role)
                );

                // Use standard PreAuthenticatedAuthenticationToken
                PreAuthenticatedAuthenticationToken authentication =
                        new PreAuthenticatedAuthenticationToken(userId, null, authorities);

                // Set claims as details for SpEL access
                authentication.setDetails(claims);

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("🔒 Filter: Authentication set with role: ROLE_{}", role);

            } catch (JwtException e) {
                log.error("❌ Invalid JWT token: {}", e.getMessage());
            } catch (Exception e) {
                log.error("❌ Cannot set user authentication: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    // Extract token from Authorization header
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}