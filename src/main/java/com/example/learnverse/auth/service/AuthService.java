package com.example.learnverse.auth.service;

import com.example.learnverse.auth.dto.AuthResponse;
import com.example.learnverse.auth.dto.LoginRequest;
import com.example.learnverse.auth.dto.RegisterRequest;
import com.example.learnverse.auth.jwt.JwtUtil;
import com.example.learnverse.auth.user.AppUser;
import com.example.learnverse.auth.modelenum.Role;
import com.example.learnverse.auth.repo.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthResponse register(@Valid RegisterRequest req, Role role) {
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email already registered");
        }
        AppUser user = AppUser.builder()
                .name(req.name())
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(role)
                .createdAt(Instant.now())
                .build();
        user = userRepository.save(user);

        String token = jwtUtil.generateAccessToken(
                user.getId(),
                Map.of("role", user.getRole().name(), "email", user.getEmail())
        );
        return new AuthResponse(token, "Bearer", jwtUtil.getAccessExpSeconds(), user.getRole().name(), user.getId());
    }

    public AuthResponse login(@Valid LoginRequest req) {
        var user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String token = jwtUtil.generateAccessToken(
                user.getId(),
                Map.of("role", user.getRole().name(), "email", user.getEmail())
        );
        return new AuthResponse(token, "Bearer", jwtUtil.getAccessExpSeconds(), user.getRole().name(), user.getId());
    }
}

