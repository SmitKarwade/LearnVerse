package com.example.learnverse.progress.dto;

import com.example.learnverse.progress.model.UserProgress;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressResponse {
    private String activityId;
    private String activityTitle;
    private Double completionPercentage;
    private Integer totalVideos;
    private Integer completedVideos;
    private Integer totalResources;
    private Integer completedResourcesCount;
    private List<UserProgress.VideoProgress> videoProgress;
    private List<String> completedResources;
    private Instant lastAccessed;
    private String lastWatchedVideoId;
    private String continueFromVideoId; // Next unwatched video
}

