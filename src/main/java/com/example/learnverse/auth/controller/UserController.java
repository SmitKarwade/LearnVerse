package com.example.learnverse.auth.controller;

import com.example.learnverse.auth.dto.UserInterestDto;
import com.example.learnverse.auth.service.UserService;
import com.example.learnverse.auth.user.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/interests/add")
    public ResponseEntity<?> addInterests(@Valid @RequestBody UserInterestDto interestDto,
                                          Authentication auth) {
        String userId = auth.getName();

        // Check if user has USER role
        boolean isUser = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_USER"));

        if (!isUser) {
            return ResponseEntity.status(403).body("Only users with USER role can add interests");
        }

        try {
            AppUser updatedUser = userService.addUserInterests(userId, interestDto);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/interests/edit")
    public ResponseEntity<?> updateInterests(@Valid @RequestBody UserInterestDto interestDto,
                                             Authentication auth) {
        String userId = auth.getName();

        boolean isUser = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_USER"));

        if (!isUser) {
            return ResponseEntity.status(403).body("Only users with USER role can update interests");
        }

        try {
            AppUser updatedUser = userService.updateUserInterests(userId, interestDto);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/interests/get")
    public ResponseEntity<?> getInterests(Authentication auth) {
        String userId = auth.getName();

        boolean isUser = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_USER"));

        if (!isUser) {
            return ResponseEntity.status(403).body("Only users with USER role have interests");
        }

        try {
            List<String> interests = userService.getUserInterests(userId);
            return ResponseEntity.ok(interests);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

