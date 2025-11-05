package com.example.learnverse.progress.service;

import com.example.learnverse.activity.model.Activity;
import com.example.learnverse.activity.repository.ActivityRepository;
import com.example.learnverse.enrollment.model.Enrollment;
import com.example.learnverse.enrollment.repository.EnrollmentRepository;
import com.example.learnverse.progress.dto.ProgressResponse;
import com.example.learnverse.progress.dto.StudentCourseViewResponse;
import com.example.learnverse.progress.dto.UpdateVideoProgressRequest;
import com.example.learnverse.progress.model.UserProgress;
import com.example.learnverse.progress.repository.UserProgressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProgressService {

    private final UserProgressRepository progressRepository;
    private final ActivityRepository activityRepository;
    private final EnrollmentRepository enrollmentRepository;


    /**
     * Initialize progress when student enrolls
     */
    /**
     * Initialize progress when student enrolls (PUBLIC - called by EnrollmentService)
     */
    public UserProgress initializeProgress(String userId, String activityId) {
        log.info("Initializing progress for user: {} in activity: {}", userId, activityId);

        // Check if progress already exists
        Optional<UserProgress> existing = progressRepository
                .findByUserIdAndActivityId(userId, activityId);

        if (existing.isPresent()) {
            log.info("Progress already exists for user: {} in activity: {}", userId, activityId);
            return existing.get();
        }

        // Fetch activity details
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        // Calculate totals
        int totalVideos = 0;
        int totalResources = 0;

        if (activity.getVideoContent() != null &&
                activity.getVideoContent().getRecordedVideos() != null) {
            totalVideos = activity.getVideoContent().getRecordedVideos().size();

            totalResources = activity.getVideoContent().getRecordedVideos().stream()
                    .filter(video -> video.getResources() != null)
                    .mapToInt(video -> video.getResources().size())
                    .sum();
        }

        UserProgress progress = UserProgress.builder()
                .userId(userId)
                .activityId(activityId)
                .activityTitle(activity.getTitle())
                .videoProgress(new ArrayList<>())
                .completedResources(new ArrayList<>())
                .completionPercentage(0.0)
                .totalVideos(totalVideos)
                .totalResources(totalResources)
                .completedVideos(0)
                .completedResourcesCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .lastAccessed(Instant.now())
                .build();

        return progressRepository.save(progress);
    }

    /**
     * Get all enrolled courses with progress (UNIFIED ENDPOINT)
     * Auto-initializes progress if doesn't exist
     */
    public List<ProgressResponse> getMyCoursesWithProgress(String userId) {
        log.info("Fetching my courses with progress for user: {}", userId);

        // Get all enrollments
        List<Enrollment> enrollments = enrollmentRepository.findByUserIdOrderByEnrolledAtDesc(userId);

        if (enrollments.isEmpty()) {
            log.warn("No enrollments found for user: {}", userId);
            return new ArrayList<>();  // ✅ Return empty list, not null
        }

        return enrollments.stream()
                .map(enrollment -> {
                    try {
                        // Get or create progress
                        UserProgress progress = progressRepository
                                .findByUserIdAndActivityId(userId, enrollment.getActivityId())
                                .orElseGet(() -> {
                                    log.info("Progress not found for activity: {}, initializing...", enrollment.getActivityId());
                                    return initializeProgress(userId, enrollment.getActivityId());
                                });

                        // Find last watched video
                        String lastWatchedVideoId = progress.getVideoProgress().stream()
                                .filter(vp -> vp.getLastWatched() != null)  // ✅ Add null check
                                .max((v1, v2) -> v1.getLastWatched().compareTo(v2.getLastWatched()))
                                .map(UserProgress.VideoProgress::getVideoId)
                                .orElse(null);

                        // Find next unwatched video
                        String continueFromVideoId = progress.getVideoProgress().stream()
                                .filter(vp -> !vp.getCompleted())
                                .findFirst()
                                .map(UserProgress.VideoProgress::getVideoId)
                                .orElse(lastWatchedVideoId);

                        return ProgressResponse.builder()
                                .activityId(progress.getActivityId())
                                .activityTitle(progress.getActivityTitle())
                                .completionPercentage(progress.getCompletionPercentage())
                                .totalVideos(progress.getTotalVideos())
                                .completedVideos(progress.getCompletedVideos())
                                .totalResources(progress.getTotalResources())
                                .completedResourcesCount(progress.getCompletedResourcesCount())
                                .videoProgress(progress.getVideoProgress())
                                .completedResources(progress.getCompletedResources())
                                .lastAccessed(progress.getLastAccessed())
                                .lastWatchedVideoId(lastWatchedVideoId)
                                .continueFromVideoId(continueFromVideoId)
                                .build();
                    } catch (Exception e) {
                        log.error("Failed to get progress for enrollment: {}", enrollment.getId(), e);
                        return null;
                    }
                })
                .filter(response -> response != null)
                .collect(Collectors.toList());
    }

    /**
     * Update video watch progress
     */
    @Transactional
    public UserProgress updateVideoProgress(
            String userId,
            String activityId,
            UpdateVideoProgressRequest request
    ) {
        log.info("Updating video progress for user: {} in activity: {}, video: {}",
                userId, activityId, request.getVideoId());

        UserProgress progress = progressRepository
                .findByUserIdAndActivityId(userId, activityId)
                .orElseGet(() -> initializeProgress(userId, activityId));

        // Find existing video progress or create new
        UserProgress.VideoProgress videoProgress = progress.getVideoProgress().stream()
                .filter(vp -> vp.getVideoId().equals(request.getVideoId()))
                .findFirst()
                .orElse(null);

        if (videoProgress == null) {
            // Create new video progress
            videoProgress = UserProgress.VideoProgress.builder()
                    .videoId(request.getVideoId())
                    .videoTitle(request.getVideoTitle())
                    .watchedSeconds(request.getWatchedSeconds())
                    .totalSeconds(request.getTotalSeconds())
                    .completed(request.getCompleted())
                    .lastWatched(Instant.now())
                    .progressPercentage(calculateProgressPercentage(
                            request.getWatchedSeconds(),
                            request.getTotalSeconds()
                    ))
                    .build();
            progress.getVideoProgress().add(videoProgress);
        } else {
            // Update existing video progress
            videoProgress.setWatchedSeconds(request.getWatchedSeconds());
            videoProgress.setTotalSeconds(request.getTotalSeconds());
            videoProgress.setCompleted(request.getCompleted());
            videoProgress.setLastWatched(Instant.now());
            videoProgress.setProgressPercentage(calculateProgressPercentage(
                    request.getWatchedSeconds(),
                    request.getTotalSeconds()
            ));
        }

        // Update completed videos count
        progress.setCompletedVideos((int) progress.getVideoProgress().stream()
                .filter(UserProgress.VideoProgress::getCompleted)
                .count());

        // Update overall completion percentage
        progress.setCompletionPercentage(calculateOverallCompletion(progress));
        progress.setLastAccessed(Instant.now());
        progress.setUpdatedAt(Instant.now());

        return progressRepository.save(progress);
    }

    /**
     * Mark resource as completed
     */
    @Transactional
    public UserProgress markResourceCompleted(
            String userId,
            String activityId,
            String resourceId
    ) {
        log.info("Marking resource {} as completed for user: {} in activity: {}",
                resourceId, userId, activityId);

        UserProgress progress = progressRepository
                .findByUserIdAndActivityId(userId, activityId)
                .orElseGet(() -> initializeProgress(userId, activityId));

        if (!progress.getCompletedResources().contains(resourceId)) {
            progress.getCompletedResources().add(resourceId);
            progress.setCompletedResourcesCount(progress.getCompletedResources().size());
            progress.setCompletionPercentage(calculateOverallCompletion(progress));
            progress.setLastAccessed(Instant.now());
            progress.setUpdatedAt(Instant.now());
        }

        return progressRepository.save(progress);
    }

    /**
     * Get progress for specific activity
     */
    public ProgressResponse getProgress(String userId, String activityId) {
        log.info("Fetching progress for user: {} in activity: {}", userId, activityId);

        UserProgress progress = progressRepository
                .findByUserIdAndActivityId(userId, activityId)
                .orElseGet(() -> initializeProgress(userId, activityId));

        // Find last watched video
        String lastWatchedVideoId = progress.getVideoProgress().stream()
                .max((v1, v2) -> v1.getLastWatched().compareTo(v2.getLastWatched()))
                .map(UserProgress.VideoProgress::getVideoId)
                .orElse(null);

        // Find next unwatched video
        String continueFromVideoId = progress.getVideoProgress().stream()
                .filter(vp -> !vp.getCompleted())
                .findFirst()
                .map(UserProgress.VideoProgress::getVideoId)
                .orElse(lastWatchedVideoId);

        return ProgressResponse.builder()
                .activityId(progress.getActivityId())
                .activityTitle(progress.getActivityTitle())
                .completionPercentage(progress.getCompletionPercentage())
                .totalVideos(progress.getTotalVideos())
                .completedVideos(progress.getCompletedVideos())
                .totalResources(progress.getTotalResources())
                .completedResourcesCount(progress.getCompletedResourcesCount())
                .videoProgress(progress.getVideoProgress())
                .completedResources(progress.getCompletedResources())
                .lastAccessed(progress.getLastAccessed())
                .lastWatchedVideoId(lastWatchedVideoId)
                .continueFromVideoId(continueFromVideoId)
                .build();
    }

    /**
     * Get all enrolled courses with progress
     */
    public List<ProgressResponse> getAllProgress(String userId) {
        log.info("Fetching all progress for user: {}", userId);

        List<UserProgress> progressList = progressRepository
                .findByUserIdOrderByLastAccessedDesc(userId);

        return progressList.stream()
                .map(progress -> getProgress(userId, progress.getActivityId()))
                .collect(Collectors.toList());
    }

    /**
     * Calculate video progress percentage
     */
    private Double calculateProgressPercentage(Long watchedSeconds, Long totalSeconds) {
        if (totalSeconds == null || totalSeconds == 0) {
            return 0.0;
        }
        return (watchedSeconds.doubleValue() / totalSeconds.doubleValue()) * 100.0;
    }

    /**
     * ✅ FIXED: Calculate overall completion percentage
     */
    private Double calculateOverallCompletion(UserProgress progress) {
        // Get total items from progress object itself (already calculated during init)
        int totalItems = (progress.getTotalVideos() != null ? progress.getTotalVideos() : 0) +
                (progress.getTotalResources() != null ? progress.getTotalResources() : 0);

        if (totalItems == 0) {
            return 0.0;
        }

        int completedItems = (progress.getCompletedVideos() != null ? progress.getCompletedVideos() : 0) +
                (progress.getCompletedResourcesCount() != null ? progress.getCompletedResourcesCount() : 0);

        // ✅ FIXED: Cast to double before division
        return ((double) completedItems / (double) totalItems) * 100.0;
    }

    /**
     * Get course detail with student's progress (for enrolled students)
     */
    public StudentCourseViewResponse getStudentCourseView(String userId, String activityId) {
        log.info("Fetching student course view for user: {} in activity: {}", userId, activityId);

        // Verify enrollment
        if (!enrollmentRepository.existsByUserIdAndActivityId(userId, activityId)) {
            throw new RuntimeException("User not enrolled in this activity");
        }

        // Get activity
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        // Get or create progress
        UserProgress progress = progressRepository
                .findByUserIdAndActivityId(userId, activityId)
                .orElseGet(() -> initializeProgress(userId, activityId));

        // Build videos with progress
        List<StudentCourseViewResponse.VideoWithProgress> videosWithProgress = new ArrayList<>();

        if (activity.getVideoContent() != null &&
                activity.getVideoContent().getRecordedVideos() != null) {

            for (Activity.VideoContent.Video video : activity.getVideoContent().getRecordedVideos()) {
                // Find progress for this video
                UserProgress.VideoProgress videoProgress = progress.getVideoProgress().stream()
                        .filter(vp -> vp.getVideoId().equals(video.getVideoId()))
                        .findFirst()
                        .orElse(null);

                // Build video resources
                List<StudentCourseViewResponse.VideoResource> videoResources = new ArrayList<>();
                if (video.getResources() != null) {
                    for (Activity.VideoContent.Video.Resource resource : video.getResources()) {
                        boolean downloaded = progress.getCompletedResources().contains(
                                video.getVideoId() + "_" + resource.getTitle()
                        );

                        videoResources.add(StudentCourseViewResponse.VideoResource.builder()
                                .type(resource.getType())
                                .title(resource.getTitle())
                                .url(resource.getUrl())
                                .downloaded(downloaded)
                                .build());
                    }
                }

                videosWithProgress.add(StudentCourseViewResponse.VideoWithProgress.builder()
                        .videoId(video.getVideoId())
                        .title(video.getTitle())
                        .description(video.getDescription())
                        .duration(video.getDuration())
                        .videoUrl(video.getVideoUrl())
                        .thumbnailUrl(video.getThumbnailUrl())
                        .order(video.getOrder())
                        .isPreview(video.getIsPreview())
                        .completed(videoProgress != null && videoProgress.getCompleted())
                        .watchedSeconds(videoProgress != null ? videoProgress.getWatchedSeconds() : 0L)
                        .progressPercentage(videoProgress != null ? videoProgress.getProgressPercentage() : 0.0)
                        .lastWatched(videoProgress != null ? videoProgress.getLastWatched().toString() : null)
                        .resources(videoResources)
                        .build());
            }
        }

        // Sort videos by order
        videosWithProgress.sort((v1, v2) -> v1.getOrder().compareTo(v2.getOrder()));

        // Find next video to continue
        String continueFromVideoId = null;
        String continueFromVideoTitle = null;

        for (StudentCourseViewResponse.VideoWithProgress video : videosWithProgress) {
            if (!video.getCompleted()) {
                continueFromVideoId = video.getVideoId();
                continueFromVideoTitle = video.getTitle();
                break;
            }
        }

        // If all completed, set to first video
        if (continueFromVideoId == null && !videosWithProgress.isEmpty()) {
            continueFromVideoId = videosWithProgress.get(0).getVideoId();
            continueFromVideoTitle = videosWithProgress.get(0).getTitle();
        }

        // Build meeting info (only for enrolled students)
        StudentCourseViewResponse.MeetingInfo meetingInfo = null;
        if (activity.getVideoContent() != null) {
            meetingInfo = StudentCourseViewResponse.MeetingInfo.builder()
                    .platform(activity.getVideoContent().getPlatform())
                    .meetingLink(activity.getVideoContent().getMeetingLink())
                    .meetingId(activity.getVideoContent().getMeetingId())
                    .passcode(activity.getVideoContent().getPasscode())
                    .build();
        }

        return StudentCourseViewResponse.builder()
                .activityId(activity.getId())
                .activityTitle(activity.getTitle())
                .description(activity.getDescription())
                .tutorName(activity.getTutorName())
                .bannerImageUrl(activity.getBannerImageUrl())
                .completionPercentage(progress.getCompletionPercentage())
                .totalVideos(progress.getTotalVideos())
                .completedVideos(progress.getCompletedVideos())
                .totalResources(progress.getTotalResources())
                .completedResourcesCount(progress.getCompletedResourcesCount())
                .videos(videosWithProgress)
                .resources(new ArrayList<>()) // Add if you have separate resources
                .meetingInfo(meetingInfo)
                .continueFromVideoId(continueFromVideoId)
                .continueFromVideoTitle(continueFromVideoTitle)
                .build();
    }

}