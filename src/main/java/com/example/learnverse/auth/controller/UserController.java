package com.example.learnverse.auth.controller;

import com.example.learnverse.auth.service.UserService;
import com.example.learnverse.auth.user.AppUser;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    /**
     * Get current user's interests
     */
    @GetMapping("/interests")
    public ResponseEntity<?> getUserInterests(Authentication auth) {
        try {
            String userId = auth.getName();
            AppUser user = userService.getUserById(userId);

            List<String> interests = user.getInterests() != null ? user.getInterests() : List.of();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("userId", user.getId());
            response.put("email", user.getEmail());
            response.put("name", user.getName());
            response.put("interests", interests);
            response.put("interestCount", interests.size()); // Fixed: safe to call .size() now

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Replace all user interests (PUT - complete replacement)
     */
    @PutMapping("/interests")
    public ResponseEntity<?> replaceUserInterests(
            @RequestBody UpdateInterestsRequest request,
            Authentication auth) {
        try {
            String userId = auth.getName();

            // Validate interests list
            if (request.getInterests() == null || request.getInterests().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Interests list cannot be empty"));
            }

            if (request.getInterests().size() > 10) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Maximum 10 interests allowed"));
            }

            AppUser updatedUser = userService.updateUserInterests(userId, request.getInterests());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Interests updated successfully");
            response.put("userId", updatedUser.getId());
            response.put("interests", updatedUser.getInterests());
            response.put("interestCount", updatedUser.getInterests() != null ? updatedUser.getInterests().size() : 0); // Fixed null check
            response.put("updatedAt", java.time.Instant.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Add interests to existing list (POST - append)
     */
    @PostMapping("/interests/add")
    public ResponseEntity<?> addUserInterests(
            @RequestBody UpdateInterestsRequest request,
            Authentication auth) {
        try {
            String userId = auth.getName();

            if (request.getInterests() == null || request.getInterests().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Interests list cannot be empty"));
            }

            AppUser updatedUser = userService.addUserInterests(userId, request.getInterests());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Interests added successfully");
            response.put("userId", updatedUser.getId());
            response.put("interests", updatedUser.getInterests());
            response.put("interestCount", updatedUser.getInterests() != null ? updatedUser.getInterests().size() : 0); // Fixed null check
            response.put("addedCount", request.getInterests().size());
            response.put("updatedAt", java.time.Instant.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Remove specific interests (DELETE with body)
     */
    @DeleteMapping("/interests/remove")
    public ResponseEntity<?> removeUserInterests(
            @RequestBody UpdateInterestsRequest request,
            Authentication auth) {
        try {
            String userId = auth.getName();

            if (request.getInterests() == null || request.getInterests().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Interests list cannot be empty"));
            }

            AppUser updatedUser = userService.removeUserInterests(userId, request.getInterests());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Interests removed successfully");
            response.put("userId", updatedUser.getId());
            response.put("interests", updatedUser.getInterests());
            response.put("interestCount", updatedUser.getInterests() != null ? updatedUser.getInterests().size() : 0); // Fixed null check
            response.put("removedCount", request.getInterests().size());
            response.put("updatedAt", java.time.Instant.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get user profile info
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(Authentication auth) {
        try {
            String userId = auth.getName();
            AppUser user = userService.getUserById(userId);

            List<String> interests = user.getInterests() != null ? user.getInterests() : List.of();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "name", user.getName(),
                    "role", user.getRole().name(),
                    "interests", interests,
                    "interestCount", interests.size(), // Fixed: safe to call .size() now
                    "createdAt", user.getCreatedAt()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    // Helper method for error responses
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        error.put("timestamp", java.time.Instant.now());
        return error;
    }

    // Request DTO
    @Data
    public static class UpdateInterestsRequest {
        private List<String> interests;
    }
}