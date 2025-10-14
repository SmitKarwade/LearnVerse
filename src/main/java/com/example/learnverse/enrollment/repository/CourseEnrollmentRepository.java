package com.example.learnverse.enrollment.repository;

import com.example.learnverse.enrollment.model.CourseEnrollment;
import com.example.learnverse.enrollment.model.EnrollmentStatus; // ✅ correct import
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseEnrollmentRepository extends MongoRepository<CourseEnrollment, String> {

    // Find enrollments by user
    List<CourseEnrollment> findByUserIdOrderByEnrolledAtDesc(String userId);

    // Find enrollments by tutor (for tutor dashboard)
    List<CourseEnrollment> findByTutorIdOrderByEnrolledAtDesc(String tutorId);

    // Find enrollments by activity (for admin/tutor to see who enrolled)
    List<CourseEnrollment> findByActivityIdOrderByEnrolledAtDesc(String activityId);

    // Find active enrollments for a user
    List<CourseEnrollment> findByUserIdAndStatusInOrderByEnrolledAtDesc(String userId, List<EnrollmentStatus> statuses); // ✅ fixed

    // Check if user is already enrolled in an activity
    Optional<CourseEnrollment> findByUserIdAndActivityId(String userId, String activityId);

    // Get user's current learning (for AI chatbot context)
    @Query("{ 'userId': ?0, 'status': { $in: ['IN_PROGRESS', 'ENROLLED'] } }")
    List<CourseEnrollment> findCurrentLearningByUserId(String userId);

    // Get completed courses (for AI chatbot context)
    List<CourseEnrollment> findByUserIdAndStatus(String userId, EnrollmentStatus status); // ✅ fixed

    // Analytics queries
    @Query("{ 'tutorId': ?0, 'status': 'COMPLETED' }")
    List<CourseEnrollment> findCompletedEnrollmentsByTutorId(String tutorId);

    @Query("{ 'activityId': ?0 }")
    Long countByActivityId(String activityId);
}