package com.example.learnverse.auth.security;

import com.example.learnverse.auth.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        System.out.println("🔍 Filter: Processing request to " + request.getRequestURI());

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        System.out.println("🔍 Filter: Auth Header = " + (header != null ? "Bearer ***" : "null"));

        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            System.out.println("❌ Filter: No Bearer token");
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        System.out.println("Token: " + token);
        try {
            System.out.println("Done0");
            Jws<Claims> jws = jwtUtil.validateAndParse(token);
            System.out.println("Done1");
            Claims claims = jws.getPayload();
            System.out.println("Done2");
            String userId = claims.getSubject();
            System.out.println("Done3");
            String role = claims.get("role", String.class);
            System.out.println("✅ Filter: JWT Valid - UserId: " + userId + ", Role: " + role);

            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userId, token, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
            System.out.println("🔒 Filter: Authentication set with role: ROLE_" + role);

        } catch (Exception e) {
            System.out.println("❌ Filter: JWT validation failed: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/auth/") || path.startsWith("/actuator/health");
    }
}