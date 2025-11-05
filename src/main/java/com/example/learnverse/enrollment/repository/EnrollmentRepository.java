package com.example.learnverse.enrollment.repository;


import com.example.learnverse.enrollment.model.Enrollment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends MongoRepository<Enrollment, String> {

    // Find user's enrollments
    List<Enrollment> findByUserIdOrderByEnrolledAtDesc(String userId);

    // Find enrollments for an activity
    List<Enrollment> findByActivityIdOrderByEnrolledAtDesc(String activityId);

    // Find enrollments for tutor's activities
    List<Enrollment> findByTutorIdOrderByEnrolledAtDesc(String tutorId);

    // Check if user already enrolled
    Optional<Enrollment> findByUserIdAndActivityId(String userId, String activityId);

    boolean existsByUserIdAndActivityId(String userId, String activityId);

    // Count enrollments by status
    long countByActivityIdAndStatus(String activityId, Enrollment.EnrollmentStatus status);

    // Count total enrollments for tutor
    long countByTutorId(String tutorId);
}
