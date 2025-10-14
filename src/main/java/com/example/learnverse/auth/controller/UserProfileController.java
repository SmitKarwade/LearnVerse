package com.example.learnverse.auth.controller;

import com.example.learnverse.auth.dto.ProfileSetupRequest;
import com.example.learnverse.auth.service.UserProfileService;
import com.example.learnverse.auth.user.AppUser;
import com.example.learnverse.auth.user.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
    @GetMapping("/get_profile")
    public ResponseEntity<Map<String, Object>> getProfile(Authentication auth) {
        try {
            UserProfile profile = profileService.getUserProfile(auth.getName());

            if (profile == null) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Please complete your profile setup to get personalized AI assistance.",
                        "profileCompleted", false,
                        "profile", null
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "profile", profile,
                    "profileCompleted", profile.getProfileCompleted() != null ? profile.getProfileCompleted() : false
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
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