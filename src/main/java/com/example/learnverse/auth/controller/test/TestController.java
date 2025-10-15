package com.example.learnverse.auth.controller.test;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/public")
    public ResponseEntity<Map<String, String>> publicEndpoint() {
        return ResponseEntity.ok(Map.of("message", "This is public"));
    }

    @GetMapping("/protected")
    public ResponseEntity<Map<String, String>> protectedEndpoint(
            @AuthenticationPrincipal String userId,  // ✅ Change to String, not UserDetails
            Authentication authentication) {

        return ResponseEntity.ok(Map.of(
                "message", "Hello User ID: " + userId,
                "authorities", authentication.getAuthorities().toString(),
                "details", authentication.getDetails().toString()
        ));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> adminEndpoint(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(Map.of(
                "message", "Admin only content for user: " + userId
        ));
    }

    // ✅ Alternative approach using Authentication object directly
    @GetMapping("/protected-alt")
    public ResponseEntity<Map<String, String>> protectedAlternative(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();

        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "authorities", authentication.getAuthorities().toString(),
                "authenticated", String.valueOf(authentication.isAuthenticated())
        ));
    }
}