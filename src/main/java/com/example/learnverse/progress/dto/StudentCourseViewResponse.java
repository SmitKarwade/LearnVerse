package com.example.learnverse.progress.dto;

import com.example.learnverse.activity.model.Activity;
import com.example.learnverse.progress.model.UserProgress;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentCourseViewResponse {

    // Activity Info
    private String activityId;
    private String activityTitle;
    private String description;
    private String tutorName;
    private String bannerImageUrl;

    // Progress Info
    private Double completionPercentage;
    private Integer totalVideos;
    private Integer completedVideos;
    private Integer totalResources;
    private Integer completedResourcesCount;

    // Videos with Progress
    private List<VideoWithProgress> videos;

    // Resources
    private List<ResourceWithProgress> resources;

    // Meeting Info
    private MeetingInfo meetingInfo;

    // Next Video to Watch
    private String continueFromVideoId;
    private String continueFromVideoTitle;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VideoWithProgress {
        private String videoId;
        private String title;
        private String description;
        private Integer duration; // in seconds
        private String videoUrl;
        private String thumbnailUrl;
        private Integer order;
        private Boolean isPreview;

        // Progress Data
        private Boolean completed;
        private Long watchedSeconds;
        private Double progressPercentage;
        private String lastWatched;

        // Resources in this video
        private List<VideoResource> resources;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VideoResource {
        private String type;
        private String title;
        private String url;
        private Boolean downloaded; // Student downloaded it or not
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceWithProgress {
        private String resourceId;
        private String type;
        private String title;
        private String url;
        private Boolean completed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MeetingInfo {
        private String platform;
        private String meetingLink;
        private String meetingId;
        private String passcode;
    }
}