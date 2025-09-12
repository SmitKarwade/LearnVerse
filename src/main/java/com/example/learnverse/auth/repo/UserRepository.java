package com.example.learnverse.auth.repo;

import com.example.learnverse.auth.user.AppUser;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<AppUser, String> {
    Optional<AppUser> findByEmail(String email);
    Optional<AppUser> findById(String id);
    boolean existsByEmail(String email);
}
