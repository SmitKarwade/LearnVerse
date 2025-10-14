package com.example.learnverse.auth.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    // Basic Identification
    private String firstName;
    private String lastName;
    private String location; // City, Country
    private Integer age;
    private String gender;

    // Education/Professional Snapshot
    private String currentEducationLevel; // HIGH_SCHOOL, UNDERGRADUATE, GRADUATE, WORKING_PROFESSIONAL
    private String currentRole; // Student, Developer, Manager, etc.

    // Learning/Career Summary
    private List<String> interests; // Programming, AI, Marketing, etc.
    private String careerGoal; // Full-stack Developer, Data Scientist, etc.
    private List<String> targetSkills; // React, Python, Leadership, etc.

    // Progress Tracking
    private Integer completedCourses;
    private String currentFocusArea; // Current main learning topic
    private LocalDate profileCreatedAt;
    private LocalDate lastUpdated;

    // AI Chatbot Preferences
    private String communicationStyle; // FRIENDLY, FORMAL, MOTIVATIONAL
    private Boolean wantsStepByStepGuidance;
    private Boolean profileCompleted;
}
