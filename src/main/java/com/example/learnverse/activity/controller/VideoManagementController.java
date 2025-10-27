package com.example.learnverse.activity.controller;

import com.example.learnverse.activity.dto.VideoDTO;
import com.example.learnverse.activity.model.Activity;
import com.example.learnverse.activity.service.VideoManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tutor/activities")
@PreAuthorize("hasRole('TUTOR')")
@RequiredArgsConstructor
@Slf4j
public class VideoManagementController {

    private final VideoManagementService videoManagementService;

    /**
     * Upload video file and add to activity
     */
    @PostMapping("/{activityId}/videos/upload")
    public ResponseEntity<?> uploadVideo(
            @PathVariable String activityId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("order") Integer order,
            @RequestParam("isPreview") Boolean isPreview,
            Authentication auth) {
        try {
            String tutorId = auth.getName();

            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Video file is required"
                ));
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("video/")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "File must be a video"
                ));
            }

            log.info("📤 Upload request - Activity: {}, File: {} ({}MB)",
                    activityId, file.getOriginalFilename(), file.getSize() / (1024.0 * 1024.0));

            VideoDTO.UploadVideoRequest request = VideoDTO.UploadVideoRequest.builder()
                    .title(title)
                    .description(description)
                    .order(order)
                    .isPreview(isPreview)
                    .build();

            Activity activity = videoManagementService.uploadAndAddVideo(
                    activityId, tutorId, request, file
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Video uploaded successfully",
                    "activity", activity
            ));
        } catch (IOException e) {
            log.error("❌ Upload error: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Upload failed: " + e.getMessage()
            ));
        } catch (Exception e) {
            log.error("❌ Error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Add video with existing URL (manual)
     */
    @PostMapping("/{activityId}/videos")
    public ResponseEntity<?> addVideo(
            @PathVariable String activityId,
            @Valid @RequestBody VideoDTO.AddVideoRequest request,
            Authentication auth) {
        try {
            String tutorId = auth.getName();
            Activity activity = videoManagementService.addVideo(activityId, tutorId, request);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Video added successfully",
                    "activity", activity
            ));
        } catch (Exception e) {
            log.error("❌ Error adding video: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Update existing video
     */
    @PutMapping("/{activityId}/videos/{videoId}")
    public ResponseEntity<?> updateVideo(
            @PathVariable String activityId,
            @PathVariable String videoId,
            @Valid @RequestBody VideoDTO.UpdateVideoRequest request,
            Authentication auth) {
        try {
            String tutorId = auth.getName();
            Activity activity = videoManagementService.updateVideo(activityId, videoId, tutorId, request);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Video updated successfully",
                    "activity", activity
            ));
        } catch (Exception e) {
            log.error("❌ Error updating video: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Delete video
     */
    @DeleteMapping("/{activityId}/videos/{videoId}")
    public ResponseEntity<?> deleteVideo(
            @PathVariable String activityId,
            @PathVariable String videoId,
            Authentication auth) {
        try {
            String tutorId = auth.getName();
            Activity activity = videoManagementService.deleteVideo(activityId, videoId, tutorId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Video deleted successfully",
                    "activity", activity
            ));
        } catch (Exception e) {
            log.error("❌ Error deleting video: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Add resource with URL (manual)
     */
    @PostMapping("/{activityId}/videos/{videoId}/resources")
    public ResponseEntity<?> addResource(
            @PathVariable String activityId,
            @PathVariable String videoId,
            @Valid @RequestBody VideoDTO.AddResourceRequest request,
            Authentication auth) {
        try {
            String tutorId = auth.getName();
            Activity activity = videoManagementService.addResourceToVideo(activityId, videoId, tutorId, request);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Resource added successfully",
                    "activity", activity
            ));
        } catch (Exception e) {
            log.error("❌ Error adding resource: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Upload resource file
     */
    @PostMapping("/{activityId}/videos/{videoId}/resources/upload")
    public ResponseEntity<?> uploadResource(
            @PathVariable String activityId,
            @PathVariable String videoId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String resourceType,
            @RequestParam("title") String resourceTitle,
            Authentication auth) {
        try {
            String tutorId = auth.getName();

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Resource file is required"
                ));
            }

            Activity activity = videoManagementService.uploadResourceToVideo(
                    activityId, videoId, tutorId, resourceType, resourceTitle, file
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Resource uploaded successfully",
                    "activity", activity
            ));
        } catch (IOException e) {
            log.error("❌ Error uploading resource: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Upload failed: " + e.getMessage()
            ));
        } catch (Exception e) {
            log.error("❌ Error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Delete resource from video by URL
     */
    @DeleteMapping("/{activityId}/videos/{videoId}/resources")
    public ResponseEntity<?> deleteResource(
            @PathVariable String activityId,
            @PathVariable String videoId,
            @RequestParam("url") String resourceUrl,  // ← Use URL instead of index
            Authentication auth) {
        try {
            String tutorId = auth.getName();
            Activity activity = videoManagementService.deleteResourceByUrl(
                    activityId, videoId, resourceUrl, tutorId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Resource deleted successfully",
                    "activity", activity
            ));
        } catch (Exception e) {
            log.error("❌ Error deleting resource: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }


    /**
     * Reorder videos
     */
    @PutMapping("/{activityId}/videos/reorder")
    public ResponseEntity<?> reorderVideos(
            @PathVariable String activityId,
            @RequestBody List<String> videoIds,
            Authentication auth) {
        try {
            String tutorId = auth.getName();
            Activity activity = videoManagementService.reorderVideos(activityId, tutorId, videoIds);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Videos reordered successfully",
                    "activity", activity
            ));
        } catch (Exception e) {
            log.error("❌ Error reordering videos: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}
