package com.example.learnverse.learning.repository;

import com.example.learnverse.learning.model.LearningActivity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LearningActivityRepository extends MongoRepository<LearningActivity, String> {

    // Get user's learning activities for AI analysis
    List<LearningActivity> findByUserIdOrderByActivityDateDesc(String userId);

    // Get recent activities for AI context
    List<LearningActivity> findByUserIdAndActivityDateAfterOrderByActivityDateDesc(String userId, LocalDateTime after);

    // Get activities by enrollment (for progress tracking)
    List<LearningActivity> findByEnrollmentIdOrderByActivityDateDesc(String enrollmentId);

    // Analytics for tutors
    @Query("{ 'enrollmentId': { $in: ?0 } }")
    List<LearningActivity> findByEnrollmentIds(List<String> enrollmentIds);
}