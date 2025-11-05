package com.example.learnverse.tutor.controller;

import com.example.learnverse.activity.model.Activity;
import com.example.learnverse.activity.service.ActivityService;
import com.example.learnverse.enrollment.model.Enrollment;
import com.example.learnverse.enrollment.service.EnrollmentService;
import com.example.learnverse.progress.model.UserProgress;
import com.example.learnverse.progress.repository.UserProgressRepository;
import com.example.learnverse.tutor.dto.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tutor/courses")
@PreAuthorize("hasRole('TUTOR')")
@RequiredArgsConstructor
@Slf4j
public class TutorCourseManagementController {

    private final ActivityService activityService;
    private final EnrollmentService enrollmentService;
    private final UserProgressRepository userProgressRepository;
    private final MongoTemplate mongoTemplate;

    /**
     * Get complete course management data
     * GET /api/tutor/courses/{activityId}/management
     */
    @GetMapping("/{activityId}/management")
    public ResponseEntity<TutorCourseDetailResponse> getCourseManagementData(
            @PathVariable String activityId,
            @AuthenticationPrincipal String tutorId) {

        log.info("Fetching course management data for activity: {}", activityId);

        try {
            // Get activity
            Activity activity = activityService.getActivityById(activityId);

            // Verify tutor owns this activity
            if (!activity.getTutorId().equals(tutorId)) {
                return ResponseEntity.status(403).build();
            }

            // Get enrolled students
            List<Enrollment> enrollments = enrollmentService.getActivityEnrollments(activityId);

            // Get progress for each student
            List<EnrolledStudentDto> students = enrollments.stream()
                    .map(enrollment -> {
                        UserProgress progress = userProgressRepository
                                .findByUserIdAndActivityId(enrollment.getUserId(), activityId)
                                .orElse(null);

                        return EnrolledStudentDto.builder()
                                .userId(enrollment.getUserId())
                                .name(enrollment.getUserName())
                                .email(enrollment.getUserEmail())
                                .enrolledAt(enrollment.getEnrolledAt().toString())
                                .completionPercentage(progress != null ? progress.getCompletionPercentage() : 0.0)
                                // ✅ FIXED: Use lastAccessed from UserProgress
                                .lastAccessed(progress != null && progress.getLastAccessed() != null ?
                                        progress.getLastAccessed().toString() :
                                        enrollment.getEnrolledAt().toString())
                                // ✅ FIXED: Use completedVideos field directly
                                .totalVideosWatched(progress != null && progress.getCompletedVideos() != null ?
                                        progress.getCompletedVideos() : 0)
                                // ✅ FIXED: Use completedResourcesCount field directly
                                .totalResourcesCompleted(progress != null && progress.getCompletedResourcesCount() != null ?
                                        progress.getCompletedResourcesCount() : 0)
                                .build();
                    })
                    .collect(Collectors.toList());

            // Calculate stats
            CourseStatsDto stats = calculateCourseStats(activityId, enrollments, activity);

            // Build response
            TutorCourseDetailResponse response = TutorCourseDetailResponse.builder()
                    .activity(activity)
                    .enrolledStudents(students)
                    .stats(stats)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching course management data", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get enrolled students
     * GET /api/tutor/courses/{activityId}/students
     */
    @GetMapping("/{activityId}/students")
    public ResponseEntity<List<EnrolledStudentDto>> getEnrolledStudents(
            @PathVariable String activityId,
            @AuthenticationPrincipal String tutorId) {

        log.info("Fetching enrolled students for activity: {}", activityId);

        try {
            Activity activity = activityService.getActivityById(activityId);

            if (!activity.getTutorId().equals(tutorId)) {
                return ResponseEntity.status(403).build();
            }

            List<Enrollment> enrollments = enrollmentService.getActivityEnrollments(activityId);

            List<EnrolledStudentDto> students = enrollments.stream()
                    .map(enrollment -> {
                        UserProgress progress = userProgressRepository
                                .findByUserIdAndActivityId(enrollment.getUserId(), activityId)
                                .orElse(null);

                        return EnrolledStudentDto.builder()
                                .userId(enrollment.getUserId())
                                .name(enrollment.getUserName())
                                .email(enrollment.getUserEmail())
                                .enrolledAt(enrollment.getEnrolledAt().toString())
                                .completionPercentage(progress != null ? progress.getCompletionPercentage() : 0.0)
                                // ✅ FIXED: Use lastAccessed from UserProgress
                                .lastAccessed(progress != null && progress.getLastAccessed() != null ?
                                        progress.getLastAccessed().toString() :
                                        enrollment.getEnrolledAt().toString())
                                // ✅ FIXED: Use completedVideos field
                                .totalVideosWatched(progress != null && progress.getCompletedVideos() != null ?
                                        progress.getCompletedVideos() : 0)
                                // ✅ FIXED: Use completedResourcesCount field
                                .totalResourcesCompleted(progress != null && progress.getCompletedResourcesCount() != null ?
                                        progress.getCompletedResourcesCount() : 0)
                                .build();
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(students);

        } catch (Exception e) {
            log.error("Error fetching students", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get course statistics
     * GET /api/tutor/courses/{activityId}/stats
     */
    @GetMapping("/{activityId}/stats")
    public ResponseEntity<CourseStatsDto> getCourseStats(
            @PathVariable String activityId,
            @AuthenticationPrincipal String tutorId) {

        log.info("Fetching stats for activity: {}", activityId);

        try {
            Activity activity = activityService.getActivityById(activityId);

            if (!activity.getTutorId().equals(tutorId)) {
                return ResponseEntity.status(403).build();
            }

            List<Enrollment> enrollments = enrollmentService.getActivityEnrollments(activityId);
            CourseStatsDto stats = calculateCourseStats(activityId, enrollments, activity);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Error fetching stats", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update course status (active/inactive, public/private)
     * PATCH /api/tutor/courses/{activityId}/status
     */
    @PatchMapping("/{activityId}/status")
    public ResponseEntity<?> updateCourseStatus(
            @PathVariable String activityId,
            @RequestBody CourseStatusUpdateRequest request,
            @AuthenticationPrincipal String tutorId) {

        log.info("Updating course status for activity: {}", activityId);

        try {
            Activity activity = activityService.getActivityById(activityId);

            if (!activity.getTutorId().equals(tutorId)) {
                return ResponseEntity.status(403).build();
            }

            Query query = new Query(Criteria.where("_id").is(activityId));
            Update update = new Update();

            if (request.getIsActive() != null) {
                update.set("isActive", request.getIsActive());
            }
            if (request.getIsPublic() != null) {
                update.set("isPublic", request.getIsPublic());
            }

            mongoTemplate.updateFirst(query, update, Activity.class);

            log.info("Course status updated successfully");
            return ResponseEntity.ok(Map.of("success", true, "message", "Status updated"));

        } catch (Exception e) {
            log.error("Error updating course status", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Calculate course statistics
     */
    private CourseStatsDto calculateCourseStats(String activityId, List<Enrollment> enrollments, Activity activity) {
        int totalStudents = enrollments.size();

        // Get all progress records
        List<UserProgress> allProgress = enrollments.stream()
                .map(e -> userProgressRepository.findByUserIdAndActivityId(e.getUserId(), activityId))
                .filter(opt -> opt.isPresent())
                .map(opt -> opt.get())
                .collect(Collectors.toList());

        int completedStudents = (int) allProgress.stream()
                .filter(p -> p.getCompletionPercentage() != null && p.getCompletionPercentage() >= 100.0)
                .count();

        int inProgressStudents = totalStudents - completedStudents;

        double averageProgress = totalStudents > 0 ?
                allProgress.stream()
                        .mapToDouble(p -> p.getCompletionPercentage() != null ? p.getCompletionPercentage() : 0.0)
                        .average()
                        .orElse(0.0) : 0.0;

        double completionRate = totalStudents > 0 ?
                (completedStudents * 100.0 / totalStudents) : 0.0;

        // Calculate revenue
        double totalRevenue = enrollments.stream()
                .mapToDouble(e -> e.getAmountPaid() != null ? e.getAmountPaid() : 0.0)
                .sum();

        // ✅ FIXED: Get videos from videoContent.recordedVideos
        int totalVideos = 0;
        if (activity.getVideoContent() != null && activity.getVideoContent().getRecordedVideos() != null) {
            totalVideos = activity.getVideoContent().getRecordedVideos().size();
        }

        // ✅ FIXED: Count resources from ALL videos (nested inside each video)
        int totalResources = 0;
        if (activity.getVideoContent() != null && activity.getVideoContent().getRecordedVideos() != null) {
            totalResources = activity.getVideoContent().getRecordedVideos().stream()
                    .filter(video -> video.getResources() != null)
                    .mapToInt(video -> video.getResources().size())
                    .sum();
        }

        return CourseStatsDto.builder()
                .totalStudents(totalStudents)
                .completedStudents(completedStudents)
                .inProgressStudents(inProgressStudents)
                .totalRevenue(totalRevenue)
                .averageProgress(averageProgress)
                .completionRate(completionRate)
                .totalVideos(totalVideos)
                .totalResources(totalResources)
                .averageCompletionTime(null) // Can be calculated if you track completion dates
                .build();
    }


    // DTO Classes
    @Data
    public static class CourseStatusUpdateRequest {
        private Boolean isActive;
        private Boolean isPublic;
    }
}