package com.example.learnverse.auth.service;

import com.example.learnverse.auth.dto.UserInterestDto;
import com.example.learnverse.auth.modelenum.Role;
import com.example.learnverse.auth.repo.UserRepository;
import com.example.learnverse.auth.user.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    public AppUser addUserInterests(String userId, UserInterestDto interestDto) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Check if user has USER role
        if (user.getRole() != Role.USER) {
            throw new RuntimeException("Only users with USER role can add interests");
        }

        // Normalize and validate interests
        List<String> normalizedInterests = interestDto.getInterests().stream()
                .map(String::toLowerCase)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());

        if (normalizedInterests.size() < 3) {
            throw new RuntimeException("User must have at least 3 unique interests");
        }

        user.setInterests(normalizedInterests);
        AppUser savedUser = userRepository.save(user);

        log.info("Added interests for user {}: {}", userId, normalizedInterests);
        return savedUser;
    }

    public AppUser updateUserInterests(String userId, UserInterestDto interestDto) {
        return addUserInterests(userId, interestDto); // Same logic
    }

    public List<String> getUserInterests(String userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        if (user.getRole() != Role.USER) {
            throw new RuntimeException("Only users with USER role have interests");
        }

        return user.getInterests();
    }

    public AppUser getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }
}

