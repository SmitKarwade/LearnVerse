package com.example.learnverse.activity.controller;

import com.example.learnverse.activity.model.Activity;
import com.example.learnverse.activity.service.ActivityService;
import com.example.learnverse.enrollment.service.EnrollmentVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
@Slf4j
public class ActivityAccessController {

    private final ActivityService activityService;
    private final EnrollmentVerificationService enrollmentVerificationService;

    /**
     * Tab 1: Public Activity Info (No auth required)
     * Shows: Basic info, pricing, instructor, preview videos only
     */
    @GetMapping("/{activityId}/info")
    public ResponseEntity<?> getActivityInfo(@PathVariable String activityId) {
        try {
            Activity activity = activityService.getActivityById(activityId);

            // Build public info response
            Map<String, Object> publicInfo = new HashMap<>();
            publicInfo.put("id", activity.getId());
            publicInfo.put("title", activity.getTitle());
            publicInfo.put("description", activity.getDescription());
            publicInfo.put("subject", activity.getSubject());
            publicInfo.put("activityType", activity.getActivityType());
            publicInfo.put("mode", activity.getMode());
            publicInfo.put("difficulty", activity.getDifficulty());
            publicInfo.put("tutorName", activity.getTutorName());
            publicInfo.put("suitableAgeGroup", activity.getSuitableAgeGroup());
            publicInfo.put("prerequisites", activity.getPrerequisites());
            publicInfo.put("instructorDetails", activity.getInstructorDetails());
            publicInfo.put("pricing", activity.getPricing());
            publicInfo.put("duration", activity.getDuration());
            publicInfo.put("schedule", activity.getSchedule());
            publicInfo.put("demoAvailable", activity.getDemoAvailable());
            publicInfo.put("demoDetails", activity.getDemoDetails());
            publicInfo.put("tags", activity.getTags());
            publicInfo.put("featured", activity.getFeatured());

            // Add preview videos only (isPreview = true)
            if (activity.getVideoContent() != null &&
                    activity.getVideoContent().getRecordedVideos() != null) {

                List<Activity.VideoContent.Video> previewVideos = activity.getVideoContent()
                        .getRecordedVideos()
                        .stream()
                        .filter(video -> video.getIsPreview() != null && video.getIsPreview())
                        .collect(Collectors.toList());

                publicInfo.put("previewVideos", previewVideos);
                publicInfo.put("totalVideoCount", activity.getVideoContent().getTotalVideoCount());
                publicInfo.put("totalVideoDuration", activity.getVideoContent().getTotalVideoDuration());
            }

            // Add review summary (not full reviews)
            if (activity.getReviews() != null) {
                Map<String, Object> reviewSummary = new HashMap<>();
                reviewSummary.put("averageRating", activity.getReviews().getAverageRating());
                reviewSummary.put("totalReviews", activity.getReviews().getTotalReviews());
                reviewSummary.put("ratingDistribution", activity.getReviews().getRatingDistribution());
                publicInfo.put("reviews", reviewSummary);
            }

            // Add enrollment info (without sensitive data)
            if (activity.getEnrollmentInfo() != null) {
                Map<String, Object> enrollmentSummary = new HashMap<>();
                enrollmentSummary.put("enrolledCount", activity.getEnrollmentInfo().getEnrolledCount());
                enrollmentSummary.put("enrollmentStatus", activity.getEnrollmentInfo().getEnrollmentStatus());
                enrollmentSummary.put("maxCapacity", activity.getEnrollmentInfo().getMaxCapacity());
                publicInfo.put("enrollmentInfo", enrollmentSummary);
            }

            return ResponseEntity.ok(publicInfo);

        } catch (Exception e) {
            log.error("❌ Error fetching activity info: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Tab 2: Videos & Resources (Enrolled users only)
     */
    @GetMapping("/{activityId}/videos")
    public ResponseEntity<?> getActivityVideos(
            @PathVariable String activityId,
            Authentication auth) {
        try {
            String userId = auth.getName();

            // Verify enrollment
            enrollmentVerificationService.verifyEnrollmentOrThrow(userId, activityId);

            Activity activity = activityService.getActivityById(activityId);

            if (activity.getVideoContent() == null) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "videos", List.of(),
                        "message", "No videos available yet"
                ));
            }

            // Build response with null-safe handling
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("videos", activity.getVideoContent().getRecordedVideos() != null
                    ? activity.getVideoContent().getRecordedVideos()
                    : List.of());
            response.put("totalVideoCount", activity.getVideoContent().getTotalVideoCount() != null
                    ? activity.getVideoContent().getTotalVideoCount()
                    : 0);
            response.put("totalVideoDuration", activity.getVideoContent().getTotalVideoDuration() != null
                    ? activity.getVideoContent().getTotalVideoDuration()
                    : 0);

            // Optional fields - only add if not null
            if (activity.getVideoContent().getStreamingQuality() != null) {
                response.put("streamingQuality", activity.getVideoContent().getStreamingQuality());
            }
            if (activity.getVideoContent().getDownloadAllowed() != null) {
                response.put("downloadAllowed", activity.getVideoContent().getDownloadAllowed());
            }
            if (activity.getVideoContent().getSubtitlesAvailable() != null) {
                response.put("subtitlesAvailable", activity.getVideoContent().getSubtitlesAvailable());
            }

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.warn("⚠️ Unauthorized access attempt to videos: {}", e.getMessage());
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("❌ Error fetching videos: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Tab 3: Meeting Info (Enrolled users only)
     */
    @GetMapping("/{activityId}/meeting")
    public ResponseEntity<?> getMeetingInfo(
            @PathVariable String activityId,
            Authentication auth) {
        try {
            String userId = auth.getName();

            // Verify enrollment
            enrollmentVerificationService.verifyEnrollmentOrThrow(userId, activityId);

            Activity activity = activityService.getActivityById(activityId);

            if (activity.getVideoContent() == null) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "No meeting details available"
                ));
            }

            Map<String, Object> meetingInfo = new HashMap<>();
            meetingInfo.put("platform", activity.getVideoContent().getPlatform());
            meetingInfo.put("meetingLink", activity.getVideoContent().getMeetingLink());
            meetingInfo.put("meetingId", activity.getVideoContent().getMeetingId());
            meetingInfo.put("passcode", activity.getVideoContent().getPasscode());

            // Add schedule info
            meetingInfo.put("schedule", activity.getSchedule());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "meeting", meetingInfo
            ));

        } catch (RuntimeException e) {
            log.warn("⚠️ Unauthorized access attempt to meeting: {}", e.getMessage());
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("❌ Error fetching meeting info: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Check if current user is enrolled (utility endpoint)
     */
    @GetMapping("/{activityId}/enrollment-status")
    public ResponseEntity<?> checkEnrollmentStatus(
            @PathVariable String activityId,
            Authentication auth) {
        try {
            String userId = auth.getName();
            boolean isEnrolled = enrollmentVerificationService.isUserEnrolled(userId, activityId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "isEnrolled", isEnrolled,
                    "activityId", activityId
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}
