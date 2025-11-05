package com.example.learnverse.progress.controller;

import com.example.learnverse.progress.dto.ProgressResponse;
import com.example.learnverse.progress.dto.StudentCourseViewResponse;
import com.example.learnverse.progress.dto.UpdateVideoProgressRequest;
import com.example.learnverse.progress.model.UserProgress;
import com.example.learnverse.progress.service.UserProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/progress")
@RequiredArgsConstructor
@Slf4j
public class UserProgressController {

    private final UserProgressService progressService;

    /**
     * Get all enrolled courses with progress
     * GET /api/user/progress/my-courses
     */
    @GetMapping("/my-courses")
    public ResponseEntity<List<ProgressResponse>> getMyCourses(
            @AuthenticationPrincipal String userId
    ) {
        log.info("Fetching my courses with progress for user: {}", userId);

        try {
            List<ProgressResponse> courses = progressService.getMyCoursesWithProgress(userId);
            log.info("✅ Returned {} courses for user: {}", courses.size(), userId);
            return ResponseEntity.ok(courses);
        } catch (Exception e) {
            log.error("❌ Failed to fetch my courses", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get progress for specific activity
     * GET /api/user/progress/{activityId}
     */
    @GetMapping("/{activityId}")
    public ResponseEntity<ProgressResponse> getProgress(
            @AuthenticationPrincipal String userId,
            @PathVariable String activityId
    ) {
        log.info("Fetching progress for user: {} in activity: {}", userId, activityId);

        try {
            ProgressResponse progress = progressService.getProgress(userId, activityId);
            return ResponseEntity.ok(progress);
        } catch (Exception e) {
            log.error("Failed to fetch progress", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update video watch progress
     * POST /api/user/progress/{activityId}/video
     */
    @PostMapping("/{activityId}/video")
    public ResponseEntity<UserProgress> updateVideoProgress(
            @AuthenticationPrincipal String userId,
            @PathVariable String activityId,
            @RequestBody UpdateVideoProgressRequest request
    ) {
        log.info("Updating video progress for user: {} in activity: {}", userId, activityId);

        try {
            UserProgress progress = progressService.updateVideoProgress(
                    userId,
                    activityId,
                    request
            );
            return ResponseEntity.ok(progress);
        } catch (Exception e) {
            log.error("Failed to update video progress", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Mark resource as completed
     * POST /api/user/progress/{activityId}/resource/{resourceId}/complete
     */
    @PostMapping("/{activityId}/resource/{resourceId}/complete")
    public ResponseEntity<UserProgress> markResourceCompleted(
            @AuthenticationPrincipal String userId,
            @PathVariable String activityId,
            @PathVariable String resourceId
    ) {
        log.info("Marking resource as completed for user: {} in activity: {}",
                userId, activityId);

        try {
            UserProgress progress = progressService.markResourceCompleted(
                    userId,
                    activityId,
                    resourceId
            );
            return ResponseEntity.ok(progress);
        } catch (Exception e) {
            log.error("Failed to mark resource as completed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get course detail with student's progress
     * GET /api/user/progress/course/{activityId}
     */
    @GetMapping("/course/{activityId}")
    public ResponseEntity<StudentCourseViewResponse> getStudentCourseView(
            @AuthenticationPrincipal String userId,
            @PathVariable String activityId
    ) {
        log.info("Fetching student course view for user: {} in activity: {}", userId, activityId);

        try {
            StudentCourseViewResponse response = progressService.getStudentCourseView(userId, activityId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to fetch student course view", e);
            return ResponseEntity.badRequest().build();
        }
    }

}

