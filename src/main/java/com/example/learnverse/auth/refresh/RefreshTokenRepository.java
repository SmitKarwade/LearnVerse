package com.example.learnverse.auth.refresh;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {
    Optional<RefreshToken> findByToken(String token);
    List<RefreshToken> findByUserId(String userId);
    void deleteByToken(String token);
    void deleteByUserId(String userId);
    void deleteByExpiryDateBefore(Instant now); // Cleanup expired tokens
}
