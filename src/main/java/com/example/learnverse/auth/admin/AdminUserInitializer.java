package com.example.learnverse.auth.admin;

import com.example.learnverse.auth.modelenum.Role;
import com.example.learnverse.auth.repo.UserRepository;
import com.example.learnverse.auth.user.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminUserInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    @Override
    public void run(String... args) throws Exception {
        if (!userRepository.existsByEmail(adminProperties.getEmail())) {
            AppUser admin = AppUser.builder()
                    .name(adminProperties.getName())
                    .email(adminProperties.getEmail())
                    .passwordHash(passwordEncoder.encode(adminProperties.getPassword()))
                    .role(Role.ADMIN)
                    .createdAt(Instant.now())
                    .build();

            userRepository.save(admin);
            log.info("Admin user created with email: {}", adminProperties.getEmail());
        } else {
            log.info("Admin user already exists with email: {}", adminProperties.getEmail());
        }
    }
}