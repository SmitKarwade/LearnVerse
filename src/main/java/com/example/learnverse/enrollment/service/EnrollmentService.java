package com.example.learnverse.enrollment.service;

import com.example.learnverse.activity.model.Activity;
import com.example.learnverse.activity.repository.ActivityRepository;
import com.example.learnverse.auth.user.AppUser;
import com.example.learnverse.auth.repo.UserRepository;
import com.example.learnverse.enrollment.dto.EnrollmentRequest;
import com.example.learnverse.enrollment.model.Enrollment;
import com.example.learnverse.enrollment.repository.EnrollmentRepository;
import com.example.learnverse.payment.model.Order;
import com.example.learnverse.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;

    /**
     * Initiate enrollment - creates enrollment after payment verification
     */
    @Transactional
    public Enrollment createEnrollment(String userId, String orderId) {
        log.info("Creating enrollment for user: {} and order: {}", userId, orderId);

        // Get order details
        Order order = paymentService.getOrderById(orderId);

        if (order.getStatus() != Order.OrderStatus.COMPLETED) {
            throw new IllegalStateException("Cannot create enrollment - payment not completed");
        }

        // Check if already enrolled
        if (enrollmentRepository.findByUserIdAndActivityId(userId, order.getActivityId()).isPresent()) {
            throw new IllegalStateException("User already enrolled in this activity");
        }

        // Get activity details
        Activity activity = activityRepository.findById(order.getActivityId())
                .orElseThrow(() -> new IllegalArgumentException("Activity not found"));

        // Get user details
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Create enrollment
        Enrollment enrollment = Enrollment.builder()
                .userId(userId)
                .userName(order.getUserName())
                .userEmail(order.getUserEmail())
                .userPhone(order.getUserPhone())
                .activityId(activity.getId())
                .activityTitle(activity.getTitle())
                .tutorId(activity.getTutorId())
                .tutorName(activity.getTutorName())
                .educationalBackground(order.getEducationalBackground())
                .reasonForEnrollment(order.getReasonForEnrollment())
                .status(Enrollment.EnrollmentStatus.ENROLLED)
                .enrolledAt(Instant.now())
                .orderId(orderId)
                .amountPaid(order.getTotalAmount())
                .progress(Enrollment.Progress.builder()
                        .completedSessions(0)
                        .totalSessions(activity.getDuration() != null ?
                                activity.getDuration().getTotalSessions() : 0)
                        .completionPercentage(0.0)
                        .lastAccessedAt(Instant.now())
                        .build())
                .build();

        enrollmentRepository.save(enrollment);

        log.info("Enrollment created successfully: {}", enrollment.getId());
        return enrollment;
    }

    /**
     * Enroll user in free activity (no payment required)
     */
    @Transactional
    public Enrollment enrollInFreeActivity(String userId, EnrollmentRequest request) {
        log.info("Creating free enrollment for user: {} and activity: {}", userId, request.getActivityId());

        // Check if already enrolled
        if (enrollmentRepository.findByUserIdAndActivityId(userId, request.getActivityId()).isPresent()) {
            throw new IllegalStateException("User already enrolled in this activity");
        }

        // Get activity details
        Activity activity = activityRepository.findById(request.getActivityId())
                .orElseThrow(() -> new IllegalArgumentException("Activity not found"));

        // Verify activity is free
        if (activity.getPricing() != null && activity.getPricing().getPrice() != null &&
                activity.getPricing().getPrice() > 0) {
            throw new IllegalStateException("This is a paid activity. Please use payment flow.");
        }

        // Create enrollment
        Enrollment enrollment = Enrollment.builder()
                .userId(userId)
                .userName(request.getStudentName())
                .userEmail(request.getStudentEmail())
                .userPhone(request.getStudentPhone())
                .activityId(activity.getId())
                .activityTitle(activity.getTitle())
                .tutorId(activity.getTutorId())
                .tutorName(activity.getTutorName())
                .educationalBackground(request.getEducationalBackground())
                .reasonForEnrollment(request.getReasonForEnrollment())
                .status(Enrollment.EnrollmentStatus.ENROLLED)
                .enrolledAt(Instant.now())
                .amountPaid(0.0)
                .progress(Enrollment.Progress.builder()
                        .completedSessions(0)
                        .totalSessions(activity.getDuration() != null ?
                                activity.getDuration().getTotalSessions() : 0)
                        .completionPercentage(0.0)
                        .lastAccessedAt(Instant.now())
                        .build())
                .build();

        enrollmentRepository.save(enrollment);
        log.info("Free enrollment created successfully: {}", enrollment.getId());
        return enrollment;
    }

    /**
     * Get user's enrollments
     */
    public List<Enrollment> getUserEnrollments(String userId) {
        return enrollmentRepository.findByUserIdOrderByEnrolledAtDesc(userId);
    }

    /**
     * Get enrollments for an activity
     */
    public List<Enrollment> getActivityEnrollments(String activityId) {
        return enrollmentRepository.findByActivityIdOrderByEnrolledAtDesc(activityId);
    }

    /**
     * Get enrollments for tutor's activities
     */
    public List<Enrollment> getTutorEnrollments(String tutorId) {
        return enrollmentRepository.findByTutorIdOrderByEnrolledAtDesc(tutorId);
    }

    /**
     * Update enrollment progress
     */
    @Transactional
    public void updateProgress(String enrollmentId, int completedSessions) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found"));

        int totalSessions = enrollment.getProgress().getTotalSessions();
        double completionPercentage = totalSessions > 0 ?
                (completedSessions * 100.0 / totalSessions) : 0.0;

        enrollment.getProgress().setCompletedSessions(completedSessions);
        enrollment.getProgress().setCompletionPercentage(completionPercentage);
        enrollment.getProgress().setLastAccessedAt(Instant.now());

        // Mark as completed if 100%
        if (completionPercentage >= 100.0) {
            enrollment.setStatus(Enrollment.EnrollmentStatus.COMPLETED);
            enrollment.setCompletedAt(Instant.now());
        }

        enrollmentRepository.save(enrollment);
    }

    /**
     * Check if user is enrolled in activity
     */
    public boolean isUserEnrolled(String userId, String activityId) {
        return enrollmentRepository.findByUserIdAndActivityId(userId, activityId).isPresent();
    }
}