package com.example.learnverse.progress.repository;

import com.example.learnverse.progress.model.UserProgress;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProgressRepository extends MongoRepository<UserProgress, String> {

    /**
     * Find progress for specific user and activity
     */
    Optional<UserProgress> findByUserIdAndActivityId(String userId, String activityId);

    /**
     * Find all progress records for a user
     */
    List<UserProgress> findByUserIdOrderByLastAccessedDesc(String userId);

    /**
     * Find in-progress courses (completion < 100%)
     */
    List<UserProgress> findByUserIdAndCompletionPercentageLessThan(
            String userId,
            Double percentage
    );

    /**
     * Find completed courses
     */
    List<UserProgress> findByUserIdAndCompletionPercentageEquals(
            String userId,
            Double percentage
    );

    /**
     * Check if user has progress in activity
     */
    boolean existsByUserIdAndActivityId(String userId, String activityId);

    /**
     * Delete progress when enrollment is cancelled
     */
    void deleteByUserIdAndActivityId(String userId, String activityId);
}

