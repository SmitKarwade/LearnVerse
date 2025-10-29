// Create: src/main/java/com/example/learnverse/enrollment/service/CourseEnrollmentService.java
package com.example.learnverse.enrollment.service;

import com.example.learnverse.activity.repository.ActivityRepository;
import com.example.learnverse.auth.repo.UserRepository;
import com.example.learnverse.auth.service.UserService;
import com.example.learnverse.auth.user.AppUser;
import com.example.learnverse.auth.user.UserProfile;
import com.example.learnverse.activity.model.Activity;
import com.example.learnverse.activity.service.ActivityService;
import com.example.learnverse.enrollment.dto.EnrollmentRequest;
import com.example.learnverse.enrollment.model.CourseEnrollment;
import com.example.learnverse.enrollment.model.EnrollmentStatus;
import com.example.learnverse.enrollment.repository.CourseEnrollmentRepository;
import com.example.learnverse.learning.model.LearningActivity;
import com.example.learnverse.learning.repository.LearningActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseEnrollmentService {

    private final CourseEnrollmentRepository enrollmentRepository;
    private final LearningActivityRepository learningActivityRepository;
    private final UserRepository userRepository;
    private final ActivityService activityService;
    private final MongoTemplate mongoTemplate;
    private final ActivityRepository activityRepository;
    private final UserService userService;

    /**
     * Enroll user in a course
     */
    public CourseEnrollment enrollUserInCourse(String userId, EnrollmentRequest request) {
        try {
            // Check if already enrolled
            Optional<CourseEnrollment> existingEnrollment =
                    enrollmentRepository.findByUserIdAndActivityId(userId, request.getActivityId());

            if (existingEnrollment.isPresent()) {
                CourseEnrollment enrollment = existingEnrollment.get();
                if (enrollment.getStatus() == EnrollmentStatus.DROPPED) {
                    // Re-enroll if previously dropped
                    enrollment.setStatus(EnrollmentStatus.ENROLLED);
                    enrollment.setEnrolledAt(LocalDateTime.now());
                    enrollment.setUpdatedAt(LocalDateTime.now());
                    return enrollmentRepository.save(enrollment);
                } else {
                    throw new RuntimeException("User is already enrolled in this course");
                }
            }


            AppUser user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Activity activity = activityService.getActivityById(request.getActivityId());


            // ✅ ULTRA-SAFE totalSessions handling
            int totalSessions = 10; // Default value
            try {
                if (activity != null && activity.getDuration() != null) {
                    Integer sessionCount = activity.getDuration().getTotalSessions();

                    if (sessionCount != null) {
                        totalSessions = sessionCount.intValue(); // Explicit conversion
                    } else {
                        log.info("🔍 Using default sessions (null value): {}", totalSessions);
                    }
                } else {
                    log.info("🔍 Using default sessions (null activity/duration): {}", totalSessions);
                }
            } catch (Exception e) {
                log.error("❌ Error processing totalSessions, using default: {}", e.getMessage());
                totalSessions = 10; // Fallback
            }

            CourseEnrollment enrollment = CourseEnrollment.builder()
                    .userId(userId)
                    .userName(user.getName())
                    .activityId(request.getActivityId())
                    .activityTitle(activity.getTitle())
                    .tutorId(activity.getTutorId())
                    .tutorName(activity.getTutorName() != null ? activity.getTutorName() : "Unknown")
                    .enrolledAt(LocalDateTime.now())
                    .status(EnrollmentStatus.ENROLLED)
                    .progressPercentage(0.0)
                    .sessionsAttended(0)
                    .totalSessions(totalSessions)
                    .studyHoursSpent(0)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();



            enrollment = enrollmentRepository.save(enrollment);

            incrementEnrollmentCount(request.getActivityId());
            updateTutorStatistics(activity.getTutorId());

            if (user.getProfile() != null && user.getProfile().getCurrentFocusArea() == null) {
                user.getProfile().setCurrentFocusArea(activity.getSubject());
                userRepository.save(user);
            }


            recordLearningActivity(enrollment, "ENROLLED", "Enrolled in course", 0);

            return enrollment;

        } catch (Exception e) {
            throw new RuntimeException("Failed to enroll in course: " + e.getMessage(), e); // Preserve cause
        }
    }

    // Add these methods to CourseEnrollmentService.java
    /**
     * Update progress with user ownership validation
     */
    public CourseEnrollment updateProgressWithValidation(String enrollmentId, double progressPercentage,
                                                         int timeSpentMinutes, String userId) {
        try {
            CourseEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                    .orElseThrow(() -> new RuntimeException("Enrollment not found"));

            // ✅ Validate ownership
            if (!enrollment.getUserId().equals(userId)) {
                throw new RuntimeException("You can only update your own enrollment progress");
            }

            enrollment.setProgressPercentage(progressPercentage);
            enrollment.setStudyHoursSpent(enrollment.getStudyHoursSpent() + (timeSpentMinutes / 60));
            enrollment.setLastActivityDate(LocalDateTime.now());
            enrollment.setUpdatedAt(LocalDateTime.now());

            // Update status based on progress
            if (progressPercentage >= 100.0 && enrollment.getStatus() != EnrollmentStatus.COMPLETED) {
                enrollment.setStatus(EnrollmentStatus.COMPLETED);
                enrollment.setCompletedAt(LocalDateTime.now());
                log.info("🎉 User {} completed course: {}", enrollment.getUserId(), enrollment.getActivityTitle());
            } else if (progressPercentage > 0 && enrollment.getStatus() == EnrollmentStatus.ENROLLED) {
                enrollment.setStatus(EnrollmentStatus.IN_PROGRESS);
            }

            return enrollmentRepository.save(enrollment);

        } catch (Exception e) {
            log.error("❌ Error updating progress for enrollment {}: {}", enrollmentId, e.getMessage());
            throw new RuntimeException("Failed to update progress: " + e.getMessage());
        }
    }

    /**
     * Drop enrollment with user ownership validation
     */
    public CourseEnrollment dropEnrollmentWithValidation(String enrollmentId, String reason, String userId) {
        CourseEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));

        // ✅ Validate ownership
        if (!enrollment.getUserId().equals(userId)) {
            throw new RuntimeException("You can only drop your own enrollment");
        }

        enrollment.setStatus(EnrollmentStatus.DROPPED);
        enrollment.setFeedback(reason);
        enrollment.setUpdatedAt(LocalDateTime.now());

        decrementEnrollmentCount(enrollment.getActivityId());
        updateTutorStatistics(enrollment.getTutorId());

        log.info("📤 User {} dropped course: {} (Reason: {})",
                enrollment.getUserId(), enrollment.getActivityTitle(), reason);

        return enrollmentRepository.save(enrollment);
    }


    /**
     * Auto-increment enrollment count
     */
    private void incrementEnrollmentCount(String activityId) {
        Query query = new Query(Criteria.where("_id").is(activityId));
        Update update = new Update().inc("enrollmentInfo.enrolledCount", 1);
        mongoTemplate.updateFirst(query, update, Activity.class);

        log.info("📊 Enrollment count incremented for activity: {}", activityId);
    }

    /**
     * Auto-decrement enrollment count
     */
    private void decrementEnrollmentCount(String activityId) {
        Query query = new Query(Criteria.where("_id").is(activityId));
        Update update = new Update().inc("enrollmentInfo.enrolledCount", -1);
        mongoTemplate.updateFirst(query, update, Activity.class);

        log.info("📊 Enrollment count decremented for activity: {}", activityId);
    }

    /**
     * Auto-update tutor statistics
     */
    private void updateTutorStatistics(String tutorId) {
        // Count total enrolled students across all tutor's activities
        List<Activity> tutorActivities = activityRepository.findByTutorId(tutorId);

        int totalStudents = tutorActivities.stream()
                .mapToInt(a -> a.getEnrollmentInfo() != null && a.getEnrollmentInfo().getEnrolledCount() != null
                        ? a.getEnrollmentInfo().getEnrolledCount()
                        : 0)
                .sum();

        int totalCourses = (int) tutorActivities.stream()
                .filter(Activity::getIsActive)
                .count();

        // Update all activities' instructor details
        for (Activity activity : tutorActivities) {
            if (activity.getInstructorDetails() != null &&
                    activity.getInstructorDetails().getSocialProof() != null) {

                Query query = new Query(Criteria.where("_id").is(activity.getId()));
                Update update = new Update()
                        .set("instructorDetails.socialProof.studentsCount", totalStudents)
                        .set("instructorDetails.socialProof.coursesCount", totalCourses);

                mongoTemplate.updateFirst(query, update, Activity.class);
            }
        }

        log.info("📊 Tutor statistics updated - Students: {}, Courses: {}", totalStudents, totalCourses);
    }

    /**
     * Get user's current learning (for AI chatbot)
     */
    public List<CourseEnrollment> getUserCurrentLearning(String userId) {
        return enrollmentRepository.findCurrentLearningByUserId(userId);
    }

    /**
     * Get user's completed courses (for AI chatbot)
     */
    public List<CourseEnrollment> getUserCompletedCourses(String userId) {
        return enrollmentRepository.findByUserIdAndStatus(userId, EnrollmentStatus.COMPLETED);
    }

    /**
     * Get all enrollments for a user
     */
    public List<CourseEnrollment> getUserEnrollments(String userId) {
        return enrollmentRepository.findByUserIdOrderByEnrolledAtDesc(userId);
    }

    /**
     * Get enrollments for a tutor (tutor dashboard)
     */
    public List<CourseEnrollment> getTutorEnrollments(String tutorId) {
        return enrollmentRepository.findByTutorIdOrderByEnrolledAtDesc(tutorId);
    }

    /**
     * Get enrollments for a specific activity (admin view)
     */
    public List<CourseEnrollment> getActivityEnrollments(String activityId) {
        return enrollmentRepository.findByActivityIdOrderByEnrolledAtDesc(activityId);
    }


    /**
     * Get enrollment statistics for analytics
     */
    public EnrollmentStats getEnrollmentStats(String tutorId) {
        List<CourseEnrollment> allEnrollments = enrollmentRepository.findByTutorIdOrderByEnrolledAtDesc(tutorId);
        List<CourseEnrollment> completed = enrollmentRepository.findCompletedEnrollmentsByTutorId(tutorId);

        return EnrollmentStats.builder()
                .totalEnrollments(allEnrollments.size())
                .completedEnrollments(completed.size())
                .activeEnrollments((int) allEnrollments.stream().filter(e ->
                        e.getStatus() == EnrollmentStatus.IN_PROGRESS || e.getStatus() == EnrollmentStatus.ENROLLED).count())
                .dropoutRate(allEnrollments.size() > 0 ?
                        (double) allEnrollments.stream().mapToInt(e -> e.getStatus() == EnrollmentStatus.DROPPED ? 1 : 0).sum() / allEnrollments.size() * 100 : 0)
                .build();
    }

    // Helper methods
    private void recordLearningActivity(CourseEnrollment enrollment, String activityType, String notes, int timeSpent) {
        try {
            LearningActivity activity = LearningActivity.builder()
                    .userId(enrollment.getUserId())
                    .enrollmentId(enrollment.getId())
                    .activityId(enrollment.getActivityId())
                    .activityTitle(enrollment.getActivityTitle())
                    .activityType(activityType)
                    .timeSpentMinutes(timeSpent)
                    .activityDate(LocalDateTime.now())
                    .completed(true)
                    .notes(notes)
                    .createdAt(LocalDateTime.now())
                    .build();

            learningActivityRepository.save(activity);
        } catch (Exception e) {
            log.error("❌ Error recording learning activity: {}", e.getMessage());
        }
    }

    private void updateUserCompletedCourses(String userId) {
        try {
            AppUser user = userRepository.findById(userId).orElse(null);
            if (user != null && user.getProfile() != null) {
                UserProfile profile = user.getProfile();
                profile.setCompletedCourses(profile.getCompletedCourses() + 1);
                userRepository.save(user);
            }
        } catch (Exception e) {
            log.error("❌ Error updating user completed courses: {}", e.getMessage());
        }
    }

    // Stats DTO
    @lombok.Data
    @lombok.Builder
    public static class EnrollmentStats {
        private Integer totalEnrollments;
        private Integer completedEnrollments;
        private Integer activeEnrollments;
        private Double dropoutRate;
    }
}