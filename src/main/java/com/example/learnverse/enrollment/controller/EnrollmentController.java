package com.example.learnverse.enrollment.controller;

import com.example.learnverse.enrollment.dto.EnrollmentRequest;
import com.example.learnverse.enrollment.model.Enrollment;
import com.example.learnverse.enrollment.service.EnrollmentService;
import com.example.learnverse.payment.dto.OrderResponse;
import com.example.learnverse.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
@Slf4j
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final PaymentService paymentService;

    /**
     * Initiate enrollment - creates order for paid activities
     * POST /api/enrollments/initiate
     */
    @PostMapping("/initiate")
    public ResponseEntity<OrderResponse> initiateEnrollment(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody EnrollmentRequest request
    ) {
        log.info("Initiating enrollment for user: {}", userId);

        try {
            // This creates an order and returns Razorpay details
            OrderResponse orderResponse = paymentService.createOrder(userId, request);
            return ResponseEntity.ok(orderResponse);
        } catch (Exception e) {
            log.error("Failed to initiate enrollment", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Complete enrollment after successful payment
     * POST /api/enrollments/complete
     */
    @PostMapping("/complete")
    public ResponseEntity<Enrollment> completeEnrollment(
            @AuthenticationPrincipal String userId,
            @RequestBody Map<String, String> request
    ) {
        log.info("Completing enrollment for user: {}", userId);

        try {
            String orderId = request.get("orderId");
            Enrollment enrollment = enrollmentService.createEnrollment(userId, orderId);
            return ResponseEntity.ok(enrollment);
        } catch (Exception e) {
            log.error("Failed to complete enrollment", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Enroll in free activity (no payment required)
     * POST /api/enrollments/free
     */
    @PostMapping("/free")
    public ResponseEntity<Enrollment> enrollInFreeActivity(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody EnrollmentRequest request
    ) {
        log.info("Enrolling user in free activity: {}", userId);

        try {
            Enrollment enrollment = enrollmentService.enrollInFreeActivity(userId, request);
            return ResponseEntity.ok(enrollment);
        } catch (Exception e) {
            log.error("Failed to enroll in free activity", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get user's enrollments
     * GET /api/enrollments/my-enrollments
     */
    @GetMapping("/my-enrollments")
    public ResponseEntity<List<Enrollment>> getMyEnrollments(
            @AuthenticationPrincipal String userId
    ) {
        log.info("Fetching enrollments for user: {}", userId);
        List<Enrollment> enrollments = enrollmentService.getUserEnrollments(userId);
        return ResponseEntity.ok(enrollments);
    }

    /**
     * Get enrollments for a specific activity
     * GET /api/enrollments/activity/{activityId}
     */
    @GetMapping("/activity/{activityId}")
    public ResponseEntity<List<Enrollment>> getActivityEnrollments(
            @PathVariable String activityId
    ) {
        log.info("Fetching enrollments for activity: {}", activityId);
        List<Enrollment> enrollments = enrollmentService.getActivityEnrollments(activityId);
        return ResponseEntity.ok(enrollments);
    }

    /**
     * Check if user is enrolled in activity
     * GET /api/enrollments/check/{activityId}
     */
    @GetMapping("/check/{activityId}")
    public ResponseEntity<Map<String, Boolean>> checkEnrollment(
            @AuthenticationPrincipal String userId,
            @PathVariable String activityId
    ) {
        log.info("Checking enrollment status for user: {} in activity: {}", userId, activityId);
        boolean isEnrolled = enrollmentService.isUserEnrolled(userId, activityId);
        return ResponseEntity.ok(Map.of("isEnrolled", isEnrolled));
    }

    /**
     * Update enrollment progress
     * PUT /api/enrollments/{enrollmentId}/progress
     */
    @PutMapping("/{enrollmentId}/progress")
    public ResponseEntity<Void> updateProgress(
            @PathVariable String enrollmentId,
            @RequestBody Map<String, Integer> request
    ) {
        log.info("Updating progress for enrollment: {}", enrollmentId);

        try {
            int completedSessions = request.get("completedSessions");
            enrollmentService.updateProgress(enrollmentId, completedSessions);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to update progress", e);
            return ResponseEntity.badRequest().build();
        }
    }
}