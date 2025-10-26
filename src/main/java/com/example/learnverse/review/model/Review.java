package com.example.learnverse.review.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "reviews")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    private String id;

    // Core Information
    private String activityId;
    private String userId;
    private String userName; // For quick display

    // Review Content
    private Integer rating; // 1-5
    private String feedback; // Optional comment

    // Metadata
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isEdited;

    // Verification (optional - can be used later)
    private Boolean isVerifiedEnrollment; // True if user was enrolled when reviewing
}
