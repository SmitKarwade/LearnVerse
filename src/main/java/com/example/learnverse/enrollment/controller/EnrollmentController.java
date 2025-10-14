// Update your EnrollmentController.java
package com.example.learnverse.enrollment.controller;

import com.example.learnverse.enrollment.dto.EnrollmentRequest;
import com.example.learnverse.enrollment.model.CourseEnrollment;
import com.example.learnverse.enrollment.service.CourseEnrollmentService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class EnrollmentController {

    private final CourseEnrollmentService enrollmentService;

    /**
     * Enroll in a course
     */
    @PostMapping("/enroll")
    public ResponseEntity<Map<String, Object>> enrollInCourse(
            @RequestBody EnrollmentRequest request,
            Authentication auth) {
        try {
            CourseEnrollment enrollment = enrollmentService.enrollUserInCourse(auth.getName(), request);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Successfully enrolled in " + enrollment.getActivityTitle(),
                    "enrollment", enrollment
            ));
        } catch (Exception e) {
            log.error("❌ Enrollment error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Get user's enrollments
     */
    @GetMapping("/my-enrollments")
    public ResponseEntity<Map<String, Object>> getMyEnrollments(Authentication auth) {
        try {
            List<CourseEnrollment> enrollments = enrollmentService.getUserEnrollments(auth.getName());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "enrollments", enrollments,
                    "total", enrollments.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Get current learning (for AI context)
     */
    @GetMapping("/current-learning")
    public ResponseEntity<Map<String, Object>> getCurrentLearning(Authentication auth) {
        try {
            List<CourseEnrollment> currentLearning = enrollmentService.getUserCurrentLearning(auth.getName());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "currentLearning", currentLearning,
                    "count", currentLearning.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * ✅ FIXED: Update progress with Authentication and ownership validation
     */
    @PutMapping("/progress/{enrollmentId}")
    public ResponseEntity<Map<String, Object>> updateProgress(
            @PathVariable String enrollmentId,
            @RequestBody ProgressUpdateRequest request,
            Authentication auth) { // ✅ Added Authentication parameter
        try {
            log.info("🔄 Progress update request: enrollmentId={}, userId={}, progress={}",
                    enrollmentId, auth.getName(), request.getProgressPercentage());

            // ✅ Add ownership validation
            CourseEnrollment enrollment = enrollmentService.updateProgressWithValidation(
                    enrollmentId,
                    request.getProgressPercentage(),
                    request.getTimeSpentMinutes(),
                    auth.getName() // Pass user ID for ownership check
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Progress updated successfully",
                    "enrollment", enrollment
            ));
        } catch (Exception e) {
            log.error("❌ Progress update error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * ✅ FIXED: Drop enrollment with Authentication
     */
    @PutMapping("/drop/{enrollmentId}")
    public ResponseEntity<Map<String, Object>> dropEnrollment(
            @PathVariable String enrollmentId,
            @RequestBody DropRequest request,
            Authentication auth) { // ✅ Added Authentication parameter
        try {
            CourseEnrollment enrollment = enrollmentService.dropEnrollmentWithValidation(
                    enrollmentId,
                    request.getReason(),
                    auth.getName() // Pass user ID for ownership check
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Enrollment dropped successfully",
                    "enrollment", enrollment
            ));
        } catch (Exception e) {
            log.error("❌ Drop enrollment error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    // DTOs
    @Data
    public static class ProgressUpdateRequest {
        private Double progressPercentage;
        private Integer timeSpentMinutes;
    }

    @Data
    public static class DropRequest {
        private String reason;
    }
}