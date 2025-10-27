package com.example.learnverse.enrollment.service;

import com.example.learnverse.enrollment.model.CourseEnrollment;
import com.example.learnverse.enrollment.model.EnrollmentStatus;
import com.example.learnverse.enrollment.repository.CourseEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentVerificationService {

    private final CourseEnrollmentRepository enrollmentRepository;

    /**
     * Check if user is enrolled in an activity
     */
    public boolean isUserEnrolled(String userId, String activityId) {
        Optional<CourseEnrollment> enrollment = enrollmentRepository
                .findByUserIdAndActivityId(userId, activityId);

        if (enrollment.isEmpty()) {
            return false;
        }

        // Only active enrollments count
        EnrollmentStatus status = enrollment.get().getStatus();
        return status == EnrollmentStatus.ENROLLED ||
                status == EnrollmentStatus.IN_PROGRESS ||
                status == EnrollmentStatus.COMPLETED;
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
