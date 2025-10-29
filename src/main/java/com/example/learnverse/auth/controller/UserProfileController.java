package com.example.learnverse.auth.controller;

import com.example.learnverse.auth.dto.ProfileSetupRequest;
import com.example.learnverse.auth.service.UserProfileService;
import com.example.learnverse.auth.user.AppUser;
import com.example.learnverse.auth.user.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/user/profile")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserProfileController {

    private final UserProfileService profileService;

    /**
     * Setup user profile (first time)
     */
    @PostMapping("/setup")
    public ResponseEntity<Map<String, Object>> setupProfile(
            @RequestBody ProfileSetupRequest request,
            Authentication auth) {
        try {
            AppUser user = profileService.setupUserProfile(auth.getName(), request);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Profile setup completed successfully!",
                    "profile", user.getProfile(),
                    "profileCompleted", user.getProfileCompleted(),
                    "aiAssistantEnabled", user.getAiAssistantEnabled()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Get user profile
     */
    // UserProfileController.java
    @GetMapping("/get_profile")
    @PreAuthorize("hasAnyRole('USER', 'TUTOR', 'ADMIN')")
    public ResponseEntity<?> getProfile(Authentication auth) {
        try {
            String userId = auth.getName();

            Optional<UserProfile> profileOpt = Optional.ofNullable(profileService.getUserProfile(userId));

            if (profileOpt.isEmpty()) {
                // ✅ FIX: Use HashMap instead of Map.of() to allow null values
                return ResponseEntity.ok(new HashMap<String, Object>() {{
                    put("success", true);
                    put("profile", null);  // null is allowed in HashMap
                    put("message", "Profile not set up yet");
                }});
            }

            UserProfile profile = profileOpt.get();

            // ✅ FIX: Use HashMap or ensure no null values
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("profile", profile);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();

            // ✅ FIX: Use HashMap for error response too
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }


    /**
     * Update user profile
     */
    @PutMapping("/update_profile")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @RequestBody ProfileSetupRequest request,
            Authentication auth) {
        try {
            AppUser user = profileService.updateUserProfile(auth.getName(), request);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Profile updated successfully!",
                    "profile", user.getProfile(),
                    "profileCompleted", user.getProfileCompleted()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}