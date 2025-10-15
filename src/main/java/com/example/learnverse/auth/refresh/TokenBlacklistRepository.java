package com.example.learnverse.auth.refresh;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface TokenBlacklistRepository extends MongoRepository<BlacklistedToken, String> {
    Optional<BlacklistedToken> findByToken(String token);
    void deleteByExpiryDateBefore(Instant now);
}
