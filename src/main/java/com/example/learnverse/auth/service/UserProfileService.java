package com.example.learnverse.auth.service;

import com.example.learnverse.auth.dto.ProfileSetupRequest;
import com.example.learnverse.auth.repo.UserRepository;
import com.example.learnverse.auth.user.AppUser;
import com.example.learnverse.auth.user.UserProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final UserRepository userRepository;

    public AppUser setupUserProfile(String userId, ProfileSetupRequest request) {
        try {
            AppUser user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            UserProfile profile = UserProfile.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .gender(request.getGender())
                    .age(request.getAge())
                    .location(request.getLocation())
                    .currentEducationLevel(request.getCurrentEducationLevel())
                    .currentRole(request.getCurrentRole())
                    .interests(request.getInterests())
                    .careerGoal(request.getCareerGoal())
                    .targetSkills(request.getTargetSkills())
                    .currentFocusArea(request.getCurrentFocusArea())
                    .communicationStyle(request.getCommunicationStyle())
                    .wantsStepByStepGuidance(request.getWantsStepByStepGuidance())
                    .completedCourses(0)
                    .profileCreatedAt(LocalDate.now())
                    .lastUpdated(LocalDate.now())
                    .profileCompleted(true)
                    .build();

            user.setProfile(profile);
            user.setProfileCompleted(true);
            user.setAiAssistantEnabled(true);

            // Update basic info for backward compatibility
            user.setName((request.getFirstName() + " " + request.getLastName()).trim());
            user.setInterests(request.getInterests());

            userRepository.save(user);

            log.info("✅ Profile setup completed for user: {}", user.getName());
            return user;

        } catch (Exception e) {
            log.error("❌ Error setting up profile for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to setup profile: " + e.getMessage());
        }
    }

    public UserProfile getUserProfile(String userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getProfile();
    }

    public AppUser updateUserProfile(String userId, ProfileSetupRequest request) {
        try {
            AppUser user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            UserProfile profile = user.getProfile();
            if (profile == null) {
                return setupUserProfile(userId, request);
            }

            // Update fields
            updateProfileFields(profile, request);
            profile.setLastUpdated(LocalDate.now());

            userRepository.save(user);

            log.info("✅ Profile updated for user: {}", user.getName());
            return user;

        } catch (Exception e) {
            log.error("❌ Error updating profile for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to update profile: " + e.getMessage());
        }
    }

    private void updateProfileFields(UserProfile profile, ProfileSetupRequest request) {
        if (request.getFirstName() != null) profile.setFirstName(request.getFirstName());
        if (request.getLastName() != null) profile.setLastName(request.getLastName());
        if (request.getLocation() != null) profile.setLocation(request.getLocation());
        if (request.getCurrentEducationLevel() != null) profile.setCurrentEducationLevel(request.getCurrentEducationLevel());
        if (request.getCurrentRole() != null) profile.setCurrentRole(request.getCurrentRole());
        if (request.getInterests() != null) profile.setInterests(request.getInterests());
        if (request.getCareerGoal() != null) profile.setCareerGoal(request.getCareerGoal());
        if (request.getTargetSkills() != null) profile.setTargetSkills(request.getTargetSkills());
        if (request.getCurrentFocusArea() != null) profile.setCurrentFocusArea(request.getCurrentFocusArea());
        if (request.getCommunicationStyle() != null) profile.setCommunicationStyle(request.getCommunicationStyle());
        if (request.getWantsStepByStepGuidance() != null) profile.setWantsStepByStepGuidance(request.getWantsStepByStepGuidance());
    }
}