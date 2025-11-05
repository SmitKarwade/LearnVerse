package com.example.learnverse.progress.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "user_progress")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "user_activity_idx", def = "{'userId': 1, 'activityId': 1}", unique = true)
public class UserProgress {

    @Id
    private String id;

    private String userId;
    private String activityId;
    private String activityTitle;

    @Builder.Default
    private List<VideoProgress> videoProgress = new ArrayList<>();

    @Builder.Default
    private List<String> completedResources = new ArrayList<>();

    @Builder.Default
    private Double completionPercentage = 0.0;

    private Instant lastAccessed;
    private Instant createdAt;
    private Instant updatedAt;

    // Computed fields
    private Integer totalVideos;
    private Integer completedVideos;
    private Integer totalResources;
    private Integer completedResourcesCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VideoProgress {
        private String videoId;
        private String videoTitle;
        private Long watchedSeconds;
        private Long totalSeconds;
        private Boolean completed;
        private Instant lastWatched;
        private Double progressPercentage; // watchedSeconds / totalSeconds * 100
    }
}

