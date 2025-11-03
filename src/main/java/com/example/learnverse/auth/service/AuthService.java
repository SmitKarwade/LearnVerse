package com.example.learnverse.auth.service;

import com.example.learnverse.auth.dto.AuthResponse;
import com.example.learnverse.auth.dto.LoginRequest;
import com.example.learnverse.auth.dto.RegisterRequest;
import com.example.learnverse.auth.jwt.JwtUtil;
import com.example.learnverse.auth.refresh.RefreshToken;
import com.example.learnverse.auth.refresh.RefreshTokenService;
import com.example.learnverse.auth.refresh.dto.TokenRefreshRequest;
import com.example.learnverse.auth.user.AppUser;
import com.example.learnverse.auth.modelenum.Role;
import com.example.learnverse.auth.repo.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthResponse register(@Valid RegisterRequest req, Role role, HttpServletRequest request) {
        if (role != Role.USER) {
            throw new IllegalArgumentException("Direct registration only allowed for USER role");
        }

        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        AppUser user = AppUser.builder()
                .name(req.name())
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(Role.USER)
                .isActive(true)
                .createdAt(Date.from(Instant.now()))
                .build();

        user = userRepository.save(user);

        // Generate access token
        String accessToken = jwtUtil.generateAccessToken(
                user.getId(),
                Map.of("role", user.getRole().name(), "email", user.getEmail(), "name", user.getName())
        );

        // Generate refresh token
        String deviceInfo = getDeviceInfo(request);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId(), deviceInfo);

        return new AuthResponse(
                accessToken,
                refreshToken.getToken(),
                "Bearer",
                jwtUtil.getAccessExpSeconds(),
                user.getRole().name(),
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }

    @Transactional
    public AuthResponse login(@Valid LoginRequest req, HttpServletRequest request) {
        var user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new IllegalArgumentException("Account is deactivated");
        }

        // Delete existing refresh tokens for the user (single session)
        // Comment this line if you want multiple concurrent sessions
        refreshTokenService.deleteByUserId(user.getId());

        // Generate new tokens
        String accessToken = jwtUtil.generateAccessToken(
                user.getId(),
                Map.of("role", user.getRole().name(), "email", user.getEmail(), "name", user.getName())
        );

        String deviceInfo = getDeviceInfo(request);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId(), deviceInfo);

        return new AuthResponse(
                accessToken,
                refreshToken.getToken(),
                "Bearer",
                jwtUtil.getAccessExpSeconds(),
                user.getRole().name(),
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }

    @Transactional
    public AuthResponse refreshToken(@Valid TokenRefreshRequest request, HttpServletRequest httpRequest) {
        String requestRefreshToken = request.refreshToken();

        log.info("🔄 Refresh token request received");

        // 1. Find refresh token
        RefreshToken oldRefreshToken = refreshTokenService.findByToken(requestRefreshToken)
                .orElseThrow(() -> {
                    log.error("❌ Refresh token not found in database");
                    return new RuntimeException("Refresh token is not valid");
                });

        log.info("✅ Refresh token found for userId: {}", oldRefreshToken.getUserId());

        // 2. Check if expired
        if (oldRefreshToken.isExpired()) {
            log.error("❌ Refresh token expired at: {}", oldRefreshToken.getExpiryDate());
            refreshTokenService.deleteByToken(requestRefreshToken);
            throw new RuntimeException("Refresh token expired. Please login again");
        }

        log.info("✅ Refresh token is valid and not expired");

        // 3. Get user
        AppUser user = userRepository.findById(oldRefreshToken.getUserId())
                .orElseThrow(() -> {
                    log.error("❌ User not found for userId: {}", oldRefreshToken.getUserId());
                    return new RuntimeException("User not found");
                });

        log.info("✅ User found: {}", user.getEmail());

        // 4. Generate new access token
        String newAccessToken = jwtUtil.generateAccessToken(
                user.getId(),
                Map.of("role", user.getRole().name(), "email", user.getEmail(), "name", user.getName())
        );

        log.info("✅ New access token generated");

        // 5. Delete old refresh token
        refreshTokenService.deleteByToken(requestRefreshToken);
        log.info("✅ Old refresh token deleted");

        // 6. Create new refresh token
        String deviceInfo = getDeviceInfo(httpRequest);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(
                user.getId(),
                deviceInfo
        );
        log.info("✅ New refresh token created");

        // 7. Return new tokens
        return new AuthResponse(
                newAccessToken,
                newRefreshToken.getToken(),
                "Bearer",
                jwtUtil.getAccessExpSeconds(),
                user.getRole().name(),
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }

    @Transactional
    public void logout(String accessToken, String refreshToken) {
        try {
            // Blacklist the current access token
            Instant tokenExpiry = jwtUtil.getExpirationFromToken(accessToken);
            refreshTokenService.blacklistToken(accessToken, tokenExpiry);
        } catch (Exception e) {
            // Token might be invalid, but we still want to delete refresh token
        }

        // Delete the refresh token
        if (refreshToken != null) {
            refreshTokenService.deleteByToken(refreshToken);
        }
    }

    @Transactional
    public void logoutAllDevices(String userId) {
        refreshTokenService.deleteByUserId(userId);
    }

    public AppUser upgradeUserToTutor(String userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setRole(Role.TUTOR);
        return userRepository.save(user);
    }

    public AppUser getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private String getDeviceInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String remoteAddr = request.getRemoteAddr();
        return (userAgent != null ? userAgent : "Unknown") + " - " + remoteAddr;
    }
}