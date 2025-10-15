package com.example.learnverse.auth.refresh;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;

    @Value("${jwt.refresh-token-exp-days:7}")
    private int refreshTokenDurationDays;

    public RefreshToken createRefreshToken(String userId, String deviceInfo) {
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusSeconds(refreshTokenDurationDays * 24 * 60 * 60))
                .createdAt(Instant.now())
                .deviceInfo(deviceInfo)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token expired. Please login again");
        }
        return token;
    }

    @Transactional
    public void deleteByUserId(String userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    @Transactional
    public void deleteByToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }

    public void blacklistToken(String accessToken, Instant expiryDate) {
        BlacklistedToken blacklistedToken = BlacklistedToken.builder()
                .token(accessToken)
                .expiryDate(expiryDate)
                .blacklistedAt(Instant.now())
                .build();

        tokenBlacklistRepository.save(blacklistedToken);
    }

    public boolean isTokenBlacklisted(String token) {
        return tokenBlacklistRepository.findByToken(token).isPresent();
    }

    // Cleanup expired tokens (can be scheduled)
    @Transactional
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();
        refreshTokenRepository.deleteByExpiryDateBefore(now);
        tokenBlacklistRepository.deleteByExpiryDateBefore(now);
    }
}