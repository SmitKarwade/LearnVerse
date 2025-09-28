package com.example.learnverse.auth.service;

import com.example.learnverse.auth.modelenum.Role;
import com.example.learnverse.auth.repo.UserRepository;
import com.example.learnverse.auth.user.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /**
     * Get user by ID
     */
    public AppUser getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }

    /**
     * Get user by email
     */
    public AppUser getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
    }

    /**
     * Upgrade user to tutor role
     */
    public AppUser upgradeUserToTutor(String userId) {
        AppUser user = getUserById(userId);
        user.setRole(Role.TUTOR);
        return userRepository.save(user);
    }

    /**
     * Update user interests (replace existing)
     */
    public AppUser updateUserInterests(String userId, List<String> interests) {
        AppUser user = getUserById(userId);
        user.setInterests(cleanInterests(interests));
        return userRepository.save(user);
    }

    /**
     * Add interests to existing list - CLEANER VERSION
     */
    public AppUser addUserInterests(String userId, List<String> newInterests) {
        AppUser user = getUserById(userId);

        // Get current interests or empty list
        List<String> currentInterests = user.getInterests() != null
                ? new ArrayList<>(user.getInterests())
                : new ArrayList<>();

        // Clean new interests and add only unique ones
        List<String> cleanedNewInterests = cleanInterests(newInterests);

        for (String interest : cleanedNewInterests) {
            if (!currentInterests.contains(interest)) {
                currentInterests.add(interest);
            }
        }

        // Limit to max 10 interests
        if (currentInterests.size() > 10) {
            currentInterests = currentInterests.subList(0, 10);
        }

        user.setInterests(currentInterests);
        return userRepository.save(user);
    }

    /**
     * Remove specific interests
     */
    public AppUser removeUserInterests(String userId, List<String> interestsToRemove) {
        AppUser user = getUserById(userId);

        if (user.getInterests() == null || user.getInterests().isEmpty()) {
            return user; // Nothing to remove
        }

        List<String> cleanedToRemove = cleanInterests(interestsToRemove);

        List<String> updatedInterests = user.getInterests().stream()
                .filter(interest -> !cleanedToRemove.contains(interest))
                .collect(Collectors.toList());

        user.setInterests(updatedInterests);
        return userRepository.save(user);
    }

    /**
     * Helper method to clean and normalize interests
     */
    private List<String> cleanInterests(List<String> interests) {
        return interests.stream()
                .filter(interest -> interest != null && !interest.trim().isEmpty())
                .map(interest -> interest.toLowerCase().trim())
                .distinct()
                .collect(Collectors.toList());
    }
}