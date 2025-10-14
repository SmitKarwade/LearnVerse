package com.example.learnverse.learning.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "learning_activities")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningActivity {

    @Id
    private String id;

    // Core Information
    private String userId;
    private String enrollmentId;
    private String activityId;
    private String activityTitle;

    // Activity Details
    private String activityType; // VIDEO_WATCHED, QUIZ_COMPLETED, ASSIGNMENT_SUBMITTED, etc.
    private String topicCovered;
    private Integer timeSpentMinutes;
    private LocalDateTime activityDate;

    // Performance
    private Double score; // For quizzes, assignments
    private Boolean completed;
    private String notes; // Student notes

    // AI Analysis
    private String difficultyLevel; // EASY, MEDIUM, HARD (AI assessed)
    private String comprehensionLevel; // HIGH, MEDIUM, LOW (AI assessed)
    private String recommendedNextAction; // AI suggestion

    private LocalDateTime createdAt;
}