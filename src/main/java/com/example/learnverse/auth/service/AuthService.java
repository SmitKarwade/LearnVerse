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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
@RequiredArgsConstructor
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
                Map.of("role", user.getRole().name(), "email", user.getEmail())
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
                user.getId()
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
                Map.of("role", user.getRole().name(), "email", user.getEmail())
        );

        String deviceInfo = getDeviceInfo(request);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId(), deviceInfo);

        return new AuthResponse(
                accessToken,
                refreshToken.getToken(),
                "Bearer",
                jwtUtil.getAccessExpSeconds(),
                user.getRole().name(),
                user.getId()
        );
    }

    @Transactional
    public AuthResponse refreshToken(@Valid TokenRefreshRequest request) {
        String requestRefreshToken = request.refreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUserId)
                .map(userId -> {
                    AppUser user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found"));

                    String newAccessToken = jwtUtil.generateAccessToken(
                            user.getId(),
                            Map.of("role", user.getRole().name(), "email", user.getEmail())
                    );

                    return new AuthResponse(
                            newAccessToken,
                            requestRefreshToken, // Reuse the same refresh token
                            "Bearer",
                            jwtUtil.getAccessExpSeconds(),
                            user.getRole().name(),
                            user.getId()
                    );
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not valid"));
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