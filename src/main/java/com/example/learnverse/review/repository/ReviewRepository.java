package com.example.learnverse.review.repository;

import com.example.learnverse.review.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends MongoRepository<Review, String> {

    // Find all reviews for an activity
    Page<Review> findByActivityIdOrderByCreatedAtDesc(String activityId, Pageable pageable);

    List<Review> findByActivityId(String activityId);

    // Check if user already reviewed
    Optional<Review> findByActivityIdAndUserId(String activityId, String userId);

    // Count reviews for an activity
    long countByActivityId(String activityId);

    // Get user's reviews
    List<Review> findByUserIdOrderByCreatedAtDesc(String userId);

    // Delete all reviews for an activity (when activity is deleted)
    void deleteByActivityId(String activityId);

    // Aggregate queries for statistics
    @Query(value = "{ 'activityId': ?0 }", count = true)
    long countReviewsByActivity(String activityId);
}
