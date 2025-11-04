package com.example.learnverse.enrollment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class EnrollmentRequest {

    @NotBlank(message = "Activity ID is required")
    private String activityId;

    @NotBlank(message = "Name is required")
    private String studentName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String studentEmail;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid phone number")
    private String studentPhone;

    private String educationalBackground;
    private String reasonForEnrollment;
}
