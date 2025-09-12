package com.example.learnverse.auth.controller;

import com.example.learnverse.auth.dto.AuthResponse;
import com.example.learnverse.auth.dto.LoginRequest;
import com.example.learnverse.auth.dto.RegisterRequest;
import com.example.learnverse.auth.modelenum.Role;
import com.example.learnverse.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/register-user")
    public ResponseEntity<AuthResponse> registerUser(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req, Role.USER));
    }

    @PostMapping("/auth/register-tutor")
    public ResponseEntity<AuthResponse> registerTutor(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req, Role.TUTOR));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }
}

