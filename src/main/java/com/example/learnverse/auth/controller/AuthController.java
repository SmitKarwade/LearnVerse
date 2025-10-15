package com.example.learnverse.auth.controller;

import com.example.learnverse.auth.dto.AuthResponse;
import com.example.learnverse.auth.dto.LoginRequest;
import com.example.learnverse.auth.dto.RegisterRequest;
import com.example.learnverse.auth.modelenum.Role;
import com.example.learnverse.auth.refresh.dto.TokenRefreshRequest;
import com.example.learnverse.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req,
                                                 HttpServletRequest request) {
        return ResponseEntity.ok(authService.register(req, Role.USER, request));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req,
                                              HttpServletRequest request) {
        return ResponseEntity.ok(authService.login(req, request));
    }

    @PostMapping("/auth/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Map<String, String>> logout(
            HttpServletRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) Map<String, String> logoutRequest) {

        String accessToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }

        String refreshToken = logoutRequest != null ? logoutRequest.get("refreshToken") : null;

        authService.logout(accessToken, refreshToken);

        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/auth/logout-all")
    public ResponseEntity<Map<String, String>> logoutAllDevices(
            @AuthenticationPrincipal String userId) {

        authService.logoutAllDevices(userId);

        return ResponseEntity.ok(Map.of("message", "Logged out from all devices"));
    }
}