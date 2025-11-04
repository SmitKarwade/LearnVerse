package com.example.learnverse.enrollment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "enrollments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Enrollment {

    @Id
    private String id;

    private String userId;
    private String userName;
    private String userEmail;
    private String userPhone;

    private String activityId;
    private String activityTitle;

    private String tutorId;
    private String tutorName;

    private String educationalBackground;
    private String reasonForEnrollment;

    private EnrollmentStatus status;
    private Instant enrolledAt;
    private Instant completedAt;
    private Instant droppedAt;
    private Instant pausedAt;

    private Progress progress;

    private String orderId;
    private Double amountPaid;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Progress {
        private Integer completedSessions;
        private Integer totalSessions;
        private Double completionPercentage;
        private Instant lastAccessedAt;
    }

    public enum EnrollmentStatus {
        ENROLLED,
        IN_PROGRESS,
        COMPLETED,
        DROPPED,
        PAUSED,
        SUSPENDED
    }
}