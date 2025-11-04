package com.example.learnverse.tutor.controller;

import com.example.learnverse.enrollment.model.Enrollment;
import com.example.learnverse.enrollment.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tutor/enrollments")
@RequiredArgsConstructor
@Slf4j
public class TutorEnrollmentController {

    private final EnrollmentService enrollmentService;

    /**
     * Get all enrollments for tutor's activities
     * GET /api/tutor/enrollments
     */
    @GetMapping
    public ResponseEntity<List<Enrollment>> getTutorEnrollments(
            @AuthenticationPrincipal String tutorId
    ) {
        log.info("Fetching enrollments for tutor: {}", tutorId);
        List<Enrollment> enrollments = enrollmentService.getTutorEnrollments(tutorId);
        return ResponseEntity.ok(enrollments);
    }

    /**
     * Get enrollment statistics for tutor
     * GET /api/tutor/enrollments/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getEnrollmentStats(
            @AuthenticationPrincipal String tutorId
    ) {
        log.info("Fetching enrollment stats for tutor: {}", tutorId);

        List<Enrollment> enrollments = enrollmentService.getTutorEnrollments(tutorId);

        long totalEnrollments = enrollments.size();
        long activeEnrollments = enrollments.stream()
                .filter(e -> e.getStatus() == Enrollment.EnrollmentStatus.ENROLLED)
                .count();
        long completedEnrollments = enrollments.stream()
                .filter(e -> e.getStatus() == Enrollment.EnrollmentStatus.COMPLETED)
                .count();

        // Group enrollments by activity
        Map<String, Long> enrollmentsByActivity = enrollments.stream()
                .collect(Collectors.groupingBy(
                        Enrollment::getActivityTitle,
                        Collectors.counting()
                ));

        return ResponseEntity.ok(Map.of(
                "totalEnrollments", totalEnrollments,
                "activeEnrollments", activeEnrollments,
                "completedEnrollments", completedEnrollments,
                "enrollmentsByActivity", enrollmentsByActivity
        ));
    }

    /**
     * Get students enrolled in specific activity
     * GET /api/tutor/enrollments/activity/{activityId}
     */
    @GetMapping("/activity/{activityId}")
    public ResponseEntity<List<Enrollment>> getActivityStudents(
            @AuthenticationPrincipal String tutorId,
            @PathVariable String activityId
    ) {
        log.info("Fetching students for activity: {} (tutor: {})", activityId, tutorId);

        List<Enrollment> enrollments = enrollmentService.getActivityEnrollments(activityId);

        // Filter to ensure tutor owns this activity
        List<Enrollment> tutorEnrollments = enrollments.stream()
                .filter(e -> e.getTutorId().equals(tutorId))
                .collect(Collectors.toList());

        return ResponseEntity.ok(tutorEnrollments);
    }
}

