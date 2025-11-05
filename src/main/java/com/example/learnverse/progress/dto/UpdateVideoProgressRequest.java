package com.example.learnverse.progress.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVideoProgressRequest {
    private String videoId;
    private String videoTitle;
    private Long watchedSeconds;
    private Long totalSeconds;
    private Boolean completed;
}

