package com.example.learnverse.tutor.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import com.example.learnverse.tutor.model.TutorVerification;
import com.example.learnverse.tutor.enumcl.VerificationStatus;
import java.util.Optional;
import java.util.List;

@Repository
public interface TutorVerificationRepository extends MongoRepository<TutorVerification, String> {

    // Find by email (to prevent duplicate registrations)
    Optional<TutorVerification> findByEmail(String email);

    // Find all pending verifications for admin panel
    List<TutorVerification> findByStatus(VerificationStatus status);

    // Find by email and status (useful for checking approval status)
    Optional<TutorVerification> findByEmailAndStatus(String email, VerificationStatus status);

    // Count by status (for admin dashboard statistics)
    long countByStatus(VerificationStatus status);

    // Find recent applications (for admin panel)
    @Query(value = "{}", sort = "{ 'createdAt' : -1 }")
    List<TutorVerification> findAllOrderByCreatedAtDesc();
}

