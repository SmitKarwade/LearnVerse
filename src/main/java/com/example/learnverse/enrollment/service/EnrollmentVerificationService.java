package com.example.learnverse.enrollment.service;

import com.example.learnverse.enrollment.model.Enrollment;
import com.example.learnverse.enrollment.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentVerificationService {

    private final EnrollmentRepository enrollmentRepository;

    /**
     * Check if user is enrolled in an activity
     */
    public boolean isUserEnrolled(String userId, String activityId) {
        Optional<Enrollment> enrollment = enrollmentRepository
                .findByUserIdAndActivityId(userId, activityId);

        if (enrollment.isEmpty()) {
            return false;
        }

        // Only active enrollments count
        Enrollment.EnrollmentStatus status = enrollment.get().getStatus();
        return status == Enrollment.EnrollmentStatus.ENROLLED ||
                status == Enrollment.EnrollmentStatus.IN_PROGRESS ||
                status == Enrollment.EnrollmentStatus.COMPLETED;
    }

    /**
     * Verify enrollment or throw exception
     */
    public void verifyEnrollmentOrThrow(String userId, String activityId) {
        if (!isUserEnrolled(userId, activityId)) {
            throw new RuntimeException("You must enroll in this activity to access this content");
        }
    }
}
