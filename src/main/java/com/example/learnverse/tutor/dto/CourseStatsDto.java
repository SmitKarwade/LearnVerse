package com.example.learnverse.tutor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseStatsDto {
    private Integer totalStudents;
    private Integer completedStudents;
    private Integer inProgressStudents;
    private Double totalRevenue;
    private Double averageProgress;
    private Double completionRate;
    private Integer totalVideos;
    private Integer totalResources;
    private String averageCompletionTime;
}
