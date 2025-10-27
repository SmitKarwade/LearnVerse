package com.example.learnverse.activity.service;

import com.example.learnverse.activity.dto.VideoDTO;
import com.example.learnverse.activity.model.Activity;
import com.example.learnverse.activity.repository.ActivityRepository;
import com.example.learnverse.community.cloudinary.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoManagementService {

    private final ActivityRepository activityRepository;
    private final CloudinaryService cloudinaryService;

    /**
     * Verify tutor owns the activity
     */
    private Activity verifyOwnership(String activityId, String tutorId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        if (!activity.getTutorId().equals(tutorId)) {
            throw new RuntimeException("You can only manage videos for your own activities");
        }

        return activity;
    }

    /**
     * Upload video file and add to activity
     */
    @Transactional
    public Activity uploadAndAddVideo(
            String activityId,
            String tutorId,
            VideoDTO.UploadVideoRequest request,
            MultipartFile videoFile) throws IOException {

        Activity activity = verifyOwnership(activityId, tutorId);

        // Upload to Cloudinary - BOTH duration and thumbnail auto-extracted
        CloudinaryService.VideoUploadResult uploadResult =
                cloudinaryService.uploadVideo(videoFile, activityId);

        // Initialize collections
        if (activity.getVideoContent() == null) {
            activity.setVideoContent(new Activity.VideoContent());
        }
        if (activity.getVideoContent().getRecordedVideos() == null) {
            activity.getVideoContent().setRecordedVideos(new ArrayList<>());
        }

        // Create video with AUTO-GENERATED values
        Activity.VideoContent.Video newVideo = Activity.VideoContent.Video.builder()
                .videoId(UUID.randomUUID().toString())
                .title(request.getTitle())
                .description(request.getDescription())
                .duration(uploadResult.getDurationMinutes() != null
                        ? uploadResult.getDurationMinutes()
                        : 0) // ✅ Auto-detected
                .videoUrl(uploadResult.getVideoUrl())
                .thumbnailUrl(uploadResult.getThumbnailUrl()) // ✅ Auto-generated!
                .order(request.getOrder())
                .isPreview(request.getIsPreview())
                .resources(new ArrayList<>())
                .build();

        activity.getVideoContent().getRecordedVideos().add(newVideo);
        updateVideoStats(activity);

        Activity saved = activityRepository.save(activity);
        log.info("✅ Video uploaded - Duration: {} mins, Thumbnail: {}",
                newVideo.getDuration(), newVideo.getThumbnailUrl());

        return saved;
    }
    /**
     * Add video with existing URL (manual)
     */
    @Transactional
    public Activity addVideo(String activityId, String tutorId, VideoDTO.AddVideoRequest request) {
        log.info("🎥 Adding video to activity: {} by tutor: {}", activityId, tutorId);

        Activity activity = verifyOwnership(activityId, tutorId);

        if (activity.getVideoContent() == null) {
            activity.setVideoContent(new Activity.VideoContent());
        }

        if (activity.getVideoContent().getRecordedVideos() == null) {
            activity.getVideoContent().setRecordedVideos(new ArrayList<>());
        }

        Activity.VideoContent.Video newVideo = Activity.VideoContent.Video.builder()
                .videoId(UUID.randomUUID().toString())
                .title(request.getTitle())
                .description(request.getDescription())
                .duration(0)
                .videoUrl(request.getVideoUrl())
                .thumbnailUrl(null)
                .order(request.getOrder())
                .isPreview(request.getIsPreview())
                .resources(convertResources(request.getResources()))
                .build();

        activity.getVideoContent().getRecordedVideos().add(newVideo);
        updateVideoStats(activity);

        Activity saved = activityRepository.save(activity);
        log.info("✅ Video added: {}", newVideo.getVideoId());

        return saved;
    }

    /**
     * Update existing video
     */
    @Transactional
    public Activity updateVideo(String activityId, String videoId, String tutorId, VideoDTO.UpdateVideoRequest request) {
        log.info("✏️ Updating video: {} in activity: {}", videoId, activityId);

        Activity activity = verifyOwnership(activityId, tutorId);

        if (activity.getVideoContent() == null || activity.getVideoContent().getRecordedVideos() == null) {
            throw new RuntimeException("No videos found in this activity");
        }

        Activity.VideoContent.Video video = activity.getVideoContent().getRecordedVideos().stream()
                .filter(v -> v.getVideoId().equals(videoId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Video not found"));

        // Update fields
        if (request.getTitle() != null) video.setTitle(request.getTitle());
        if (request.getDescription() != null) video.setDescription(request.getDescription());
        if (request.getDuration() != null) video.setDuration(request.getDuration());
        if (request.getVideoUrl() != null) video.setVideoUrl(request.getVideoUrl());
        if (request.getThumbnailUrl() != null) video.setThumbnailUrl(request.getThumbnailUrl());
        if (request.getOrder() != null) video.setOrder(request.getOrder());
        if (request.getIsPreview() != null) video.setIsPreview(request.getIsPreview());
        if (request.getResources() != null) video.setResources(convertResources(request.getResources()));

        updateVideoStats(activity);

        Activity saved = activityRepository.save(activity);
        log.info("✅ Video updated: {}", videoId);

        return saved;
    }

    /**
     * Delete video
     */
    @Transactional
    public Activity deleteVideo(String activityId, String videoId, String tutorId) throws IOException {
        log.info("🗑️ Deleting video: {} from activity: {}", videoId, activityId);

        Activity activity = verifyOwnership(activityId, tutorId);

        if (activity.getVideoContent() == null || activity.getVideoContent().getRecordedVideos() == null) {
            throw new RuntimeException("No videos found in this activity");
        }

        Activity.VideoContent.Video videoToDelete = activity.getVideoContent().getRecordedVideos().stream()
                .filter(v -> v.getVideoId().equals(videoId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Video not found"));

        // Try to delete from Cloudinary
        String publicId = extractPublicIdFromUrl(videoToDelete.getVideoUrl());
        if (publicId != null) {
            try {
                cloudinaryService.deleteVideo(publicId);
            } catch (Exception e) {
                log.warn("⚠️ Failed to delete from Cloudinary: {}", e.getMessage());
            }
        }

        activity.getVideoContent().getRecordedVideos().remove(videoToDelete);
        updateVideoStats(activity);

        Activity saved = activityRepository.save(activity);
        log.info("✅ Video deleted: {}", videoId);

        return saved;
    }

    /**
     * Add resource to video
     */
    @Transactional
    public Activity addResourceToVideo(String activityId, String videoId, String tutorId, VideoDTO.AddResourceRequest request) {
        log.info("📎 Adding resource to video: {}", videoId);

        Activity activity = verifyOwnership(activityId, tutorId);

        if (activity.getVideoContent() == null || activity.getVideoContent().getRecordedVideos() == null) {
            throw new RuntimeException("No videos found");
        }

        Activity.VideoContent.Video video = activity.getVideoContent().getRecordedVideos().stream()
                .filter(v -> v.getVideoId().equals(videoId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Video not found"));

        if (video.getResources() == null) {
            video.setResources(new ArrayList<>());
        }

        Activity.VideoContent.Video.Resource newResource = Activity.VideoContent.Video.Resource.builder()
                .type(request.getType())
                .title(request.getTitle())
                .url(request.getUrl())
                .build();

        video.getResources().add(newResource);

        Activity saved = activityRepository.save(activity);
        log.info("✅ Resource added");

        return saved;
    }

    /**
     * Upload resource file
     */
    @Transactional
    public Activity uploadResourceToVideo(
            String activityId,
            String videoId,
            String tutorId,
            String resourceType,
            String resourceTitle,
            MultipartFile resourceFile) throws IOException {

        log.info("📎 Uploading resource to video: {}", videoId);

        Activity activity = verifyOwnership(activityId, tutorId);

        if (activity.getVideoContent() == null || activity.getVideoContent().getRecordedVideos() == null) {
            throw new RuntimeException("No videos found");
        }

        Activity.VideoContent.Video video = activity.getVideoContent().getRecordedVideos().stream()
                .filter(v -> v.getVideoId().equals(videoId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Video not found"));

        String resourceUrl = cloudinaryService.uploadResource(resourceFile, activityId, resourceType);

        if (video.getResources() == null) {
            video.setResources(new ArrayList<>());
        }

        Activity.VideoContent.Video.Resource newResource = Activity.VideoContent.Video.Resource.builder()
                .type(resourceType)
                .title(resourceTitle)
                .url(resourceUrl)
                .build();

        video.getResources().add(newResource);

        Activity saved = activityRepository.save(activity);
        log.info("✅ Resource uploaded");

        return saved;
    }

    /**
     * Delete resource by URL
     */
    @Transactional
    public Activity deleteResourceByUrl(String activityId, String videoId, String resourceUrl, String tutorId) {
        log.info("🗑️ Deleting resource with URL: {} from video: {}", resourceUrl, videoId);

        Activity activity = verifyOwnership(activityId, tutorId);

        if (activity.getVideoContent() == null || activity.getVideoContent().getRecordedVideos() == null) {
            throw new RuntimeException("No videos found");
        }

        // Find video
        Activity.VideoContent.Video video = activity.getVideoContent().getRecordedVideos().stream()
                .filter(v -> v.getVideoId().equals(videoId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Video not found"));

        if (video.getResources() == null || video.getResources().isEmpty()) {
            throw new RuntimeException("No resources found");
        }

        // Find and remove resource by URL
        boolean removed = video.getResources().removeIf(r -> r.getUrl().equals(resourceUrl));

        if (!removed) {
            throw new RuntimeException("Resource not found with URL: " + resourceUrl);
        }

        Activity saved = activityRepository.save(activity);
        log.info("✅ Resource deleted");

        return saved;
    }

    /**
     * Reorder videos
     */
    @Transactional
    public Activity reorderVideos(String activityId, String tutorId, List<String> videoIds) {
        log.info("🔄 Reordering videos");

        Activity activity = verifyOwnership(activityId, tutorId);

        if (activity.getVideoContent() == null || activity.getVideoContent().getRecordedVideos() == null) {
            throw new RuntimeException("No videos found");
        }

        List<Activity.VideoContent.Video> currentVideos = activity.getVideoContent().getRecordedVideos();

        if (videoIds.size() != currentVideos.size()) {
            throw new RuntimeException("Video count mismatch");
        }

        List<Activity.VideoContent.Video> reorderedVideos = new ArrayList<>();
        for (int i = 0; i < videoIds.size(); i++) {
            String videoId = videoIds.get(i);
            Activity.VideoContent.Video video = currentVideos.stream()
                    .filter(v -> v.getVideoId().equals(videoId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));

            video.setOrder(i + 1);
            reorderedVideos.add(video);
        }

        activity.getVideoContent().setRecordedVideos(reorderedVideos);

        Activity saved = activityRepository.save(activity);
        log.info("✅ Videos reordered");

        return saved;
    }

    /**
     * Auto-calculate video stats
     */
    private void updateVideoStats(Activity activity) {
        if (activity.getVideoContent() == null) return;

        List<Activity.VideoContent.Video> videos = activity.getVideoContent().getRecordedVideos();

        if (videos == null || videos.isEmpty()) {
            activity.getVideoContent().setTotalVideoCount(0);
            activity.getVideoContent().setTotalVideoDuration(0);
            return;
        }

        int count = videos.size();
        int totalDuration = videos.stream()
                .mapToInt(Activity.VideoContent.Video::getDuration)
                .sum();

        activity.getVideoContent().setTotalVideoCount(count);
        activity.getVideoContent().setTotalVideoDuration(totalDuration);

        log.info("📊 Video stats - Count: {}, Duration: {} mins", count, totalDuration);
    }

    /**
     * Convert DTO resources to model
     */
    private List<Activity.VideoContent.Video.Resource> convertResources(List<VideoDTO.ResourceRequest> requests) {
        if (requests == null) return null;

        return requests.stream()
                .map(r -> Activity.VideoContent.Video.Resource.builder()
                        .type(r.getType())
                        .title(r.getTitle())
                        .url(r.getUrl())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Extract Cloudinary public_id from URL
     */
    private String extractPublicIdFromUrl(String url) {
        try {
            String[] parts = url.split("/upload/");
            if (parts.length < 2) return null;

            String afterUpload = parts[1];
            String withoutVersion = afterUpload.replaceFirst("v\\d+/", "");
            return withoutVersion.replaceFirst("\\.[^.]+$", "");
        } catch (Exception e) {
            log.error("Failed to extract public_id: {}", url);
            return null;
        }
    }
}
