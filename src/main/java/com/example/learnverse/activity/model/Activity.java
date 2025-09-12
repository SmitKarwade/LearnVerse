package com.example.learnverse.activity.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.lang.Nullable;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Document(collection = "activities")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Activity {
    @Id
    private String id;

    private String tutorId;
    private String tutorName;

    private String title;
    private String description;
    private String subject;
    private String classType;

    private String activityType;
    private String mode;

    // Nullable if not offline/hybrid
    @Nullable
    private Location location;

    // Nullable if not online/recorded
    @Nullable
    private VideoContent videoContent;

    @Nullable
    private SuitableAgeGroup suitableAgeGroup;
    private String difficulty;
    @Nullable
    private List<String> prerequisites;

    @Nullable
    private InstructorDetails instructorDetails;
    @Nullable
    private Reviews reviews;
    @Nullable
    private Pricing pricing;
    @Nullable
    private DurationInfo duration;
    @Nullable
    private Schedule schedule;
    @Nullable
    private EnrollmentInfo enrollmentInfo;

    private Boolean demoAvailable;
    @Nullable
    private DemoDetails demoDetails;

    @Nullable
    private ContactInfo contactInfo;

    @Nullable
    private List<String> tags;
    private Boolean isActive;
    private Boolean isPublic;
    private Boolean featured;
    private Date createdAt;
    private Date updatedAt;
    private Date publishedAt;

    // === Embedded Classes ===

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Location {
        private String address;
        private String city;
        private String state;
        @Nullable
        private Coordinates coordinates;
        @Nullable
        private Integer proximityRadius;
        @Nullable
        private String landmark;
        @Nullable
        private List<String> facilities;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class Coordinates {
            @NotNull
            private String type;

            @NotNull
            private List<Double> coordinates;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VideoContent {
        @Nullable
        private String platform;
        @Nullable
        private String meetingLink;
        @Nullable
        private String meetingId;
        @Nullable
        private String passcode;
        @Nullable
        private List<Video> recordedVideos;
        @Nullable
        private Integer totalVideoCount;
        @Nullable
        private Integer totalVideoDuration;
        @Nullable
        private List<String> streamingQuality;
        @Nullable
        private Boolean downloadAllowed;
        @Nullable
        private Boolean offlineViewing;
        @Nullable
        private Boolean subtitlesAvailable;
        @Nullable
        private List<String> languages;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class Video {
            private String videoId;
            private String title;
            private String description;
            private Integer duration;
            private String videoUrl;
            @Nullable
            private String thumbnailUrl;
            private Integer order;
            private Boolean isPreview;
            @Nullable
            private List<Resource> resources; // PDF/slides etc.

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            @Builder
            public static class Resource {
                private String type;
                private String title;
                private String url;
            }
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SuitableAgeGroup {
        private Integer minAge;
        private Integer maxAge;
        @Nullable
        private String ageDescription;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InstructorDetails {
        private String bio;
        @Nullable
        private List<String> qualifications;
        @Nullable
        private String experience;
        @Nullable
        private List<String> specializations;
        @Nullable
        private String profileImage;
        @Nullable
        private SocialProof socialProof;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class SocialProof {
            private Integer studentsCount;
            private Integer coursesCount;
            private Integer yearsTeaching;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Reviews {
        private Double averageRating;
        private Integer totalReviews;
        @Nullable
        private Map<String, Integer> ratingDistribution;
        @Nullable
        private List<RecentReview> recentReviews;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class RecentReview {
            private String studentName;
            private Integer rating;
            private String comment;
            private String date; // ISO format (2025-08-10)
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Pricing {
        private Integer price;
        private String currency;
        @Nullable
        private Integer discountPrice;
        @Nullable
        private String priceType;
        @Nullable
        private Boolean installmentAvailable;
        @Nullable
        private Integer freeTrialDays;
        @Nullable
        private Integer moneyBackGuarantee;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DurationInfo {
        private Integer totalDuration;
        private Integer estimatedDuration;
        private Integer totalSessions;
        private String durationDescription;
        @Nullable
        private Boolean lifetimeAccess;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Schedule {
        @Nullable
        private Integer timingsPerWeek;
        @Nullable
        private List<String> sessionDays;
        @Nullable
        private String sessionTime;
        @Nullable
        private String timezone;
        @Nullable
        private String startDate;
        @Nullable
        private String endDate;
        @Nullable
        private Boolean flexibleScheduling;
        @Nullable
        private Boolean selfPaced;
        @Nullable
        private Integer accessDuration;
        @Nullable
        private String completionDeadline;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EnrollmentInfo {
        private Integer enrolledCount;
        @Nullable
        private Integer maxCapacity;
        @Nullable
        private Integer waitlistCount;
        private String enrollmentStatus;
        @Nullable
        private Boolean autoEnrollment;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DemoDetails {
        @Nullable
        private String demoVideoUrl;
        @Nullable
        private String demoSessionDate;
        @Nullable
        private Integer demoDuration;
        private Boolean freeTrial;
        @Nullable
        private Integer trialDuration;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ContactInfo {
        private String enrollmentLink;
        @Nullable
        private String whatsappNumber;
        @Nullable
        private String email;
        @Nullable
        private SocialLinks socialLinks;
        @Nullable
        private String supportHours;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class SocialLinks {
            @Nullable
            private String youtube;
            @Nullable
            private String instagram;
        }
    }
}

