package com.example.learnverse.tutor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrolledStudentDto {
    private String userId;
    private String name;
    private String email;
    private String enrolledAt;
    private Double completionPercentage;
    private String lastAccessed;
    private Integer totalVideosWatched;
    private Integer totalResourcesCompleted;
}
