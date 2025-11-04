package com.example.learnverse.tutor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorDashboardStats {

    // Overview stats
    private Double totalEarnings;
    private Double monthlyEarnings;
    private Double pendingEarnings;
    private Integer totalStudents;
    private Integer totalActivities;

    // Activity performance
    private List<ActivityPerformance> topActivities;

    // Recent enrollments
    private List<RecentEnrollment> recentEnrollments;

    // Revenue chart data (last 6 months)
    private List<MonthlyRevenue> revenueChart;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityPerformance {
        private String activityId;
        private String activityTitle;
        private Integer enrolledStudents;
        private Double totalRevenue;
        private Double averageRating;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentEnrollment {
        private String enrollmentId;
        private String studentName;
        private String activityTitle;
        private String enrolledAt;
        private Double amountEarned;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyRevenue {
        private String month;  // "Jan", "Feb", etc.
        private Integer year;
        private Double revenue;
        private Integer enrollments;
    }
}