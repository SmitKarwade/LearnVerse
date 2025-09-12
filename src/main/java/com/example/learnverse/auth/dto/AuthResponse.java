package com.example.learnverse.auth.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        String role,
        String userId
) {}
