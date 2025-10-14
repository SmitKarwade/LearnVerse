package com.example.learnverse.enrollment.dto;

import lombok.Data;

@Data
public class EnrollmentRequest {
    private String activityId;
    private String message; // Optional message from student
}