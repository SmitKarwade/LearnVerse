package com.example.learnverse.activity.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class VideoDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddVideoRequest {
        @NotBlank(message = "Video title is required")
        private String title;

        @NotBlank(message = "Video description is required")
        private String description;

        @NotBlank(message = "Video URL is required")
        private String videoUrl;

        @NotNull(message = "Order is required")
        @Min(value = 1, message = "Order must be at least 1")
        private Integer order;

        @NotNull(message = "isPreview flag is required")
        private Boolean isPreview;

        private List<ResourceRequest> resources;
    }

    /**
     * Upload Video Request (with file upload)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadVideoRequest {
        @NotBlank(message = "Video title is required")
        private String title;

        @NotBlank(message = "Video description is required")
        private String description;

        @NotNull(message = "Order is required")
        @Min(value = 1, message = "Order must be at least 1")
        private Integer order;

        @NotNull(message = "isPreview flag is required")
        private Boolean isPreview;
    }

    /**
     * Update Video Request
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateVideoRequest {
        private String title;
        private String description;

        @Min(value = 1, message = "Duration must be at least 1 minute")
        private Integer duration;

        private String videoUrl;
        private String thumbnailUrl;

        @Min(value = 1, message = "Order must be at least 1")
        private Integer order;

        private Boolean isPreview;
        private List<ResourceRequest> resources;
    }

    /**
     * Resource Request (for adding resources when creating video)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceRequest {
        @NotBlank(message = "Resource type is required")
        private String type; // "PDF", "SLIDES", "CODE", "DOCUMENT"

        @NotBlank(message = "Resource title is required")
        private String title;

        @NotBlank(message = "Resource URL is required")
        private String url;
    }

    /**
     * Add Resource to Existing Video
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddResourceRequest {
        @NotBlank(message = "Resource type is required")
        private String type;

        @NotBlank(message = "Resource title is required")
        private String title;

        @NotBlank(message = "Resource URL is required")
        private String url;
    }
}
