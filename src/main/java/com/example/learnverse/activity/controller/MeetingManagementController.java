package com.example.learnverse.activity.controller;

import com.example.learnverse.activity.model.Activity;
import com.example.learnverse.activity.service.ActivityService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/tutor/activities")
@PreAuthorize("hasRole('TUTOR')")
@RequiredArgsConstructor
@Slf4j
public class MeetingManagementController {

    private final ActivityService activityService;
    private final MongoTemplate mongoTemplate;

    @PutMapping("/{activityId}/meeting")
    public ResponseEntity<?> updateMeetingDetails(
            @PathVariable String activityId,
            @Valid @RequestBody MeetingDetailsRequest request,
            Authentication auth) {
        try {
            String tutorId = auth.getName();

            Activity activity = activityService.getActivityById(activityId);
            if (!activity.getTutorId().equals(tutorId)) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "error", "You can only manage your own activities"
                ));
            }

            // ✅ Initialize videoContent if null
            Query query = new Query(Criteria.where("_id").is(activityId));
            Update update = new Update();

            // Ensure videoContent exists
            if (activity.getVideoContent() == null) {
                update.set("videoContent", new Activity.VideoContent());
            }

            // Update meeting details
            if (request.getPlatform() != null) {
                update.set("videoContent.platform", request.getPlatform());
            }
            if (request.getMeetingLink() != null) {
                update.set("videoContent.meetingLink", request.getMeetingLink());
            }
            if (request.getMeetingId() != null) {
                update.set("videoContent.meetingId", request.getMeetingId());
            }
            if (request.getPasscode() != null) {
                update.set("videoContent.passcode", request.getPasscode());
            }

            mongoTemplate.updateFirst(query, update, Activity.class);

            // Fetch updated activity
            Activity updated = activityService.getActivityById(activityId);

            log.info("✅ Meeting details updated for activity: {}", activityId);

            // Build safe response
            Map<String, Object> meetingInfo = new HashMap<>();
            if (updated.getVideoContent() != null) {
                if (updated.getVideoContent().getPlatform() != null) {
                    meetingInfo.put("platform", updated.getVideoContent().getPlatform());
                }
                if (updated.getVideoContent().getMeetingLink() != null) {
                    meetingInfo.put("meetingLink", updated.getVideoContent().getMeetingLink());
                }
                if (updated.getVideoContent().getMeetingId() != null) {
                    meetingInfo.put("meetingId", updated.getVideoContent().getMeetingId());
                }
                if (updated.getVideoContent().getPasscode() != null) {
                    meetingInfo.put("passcode", updated.getVideoContent().getPasscode());
                }
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Meeting details updated successfully",
                    "meeting", meetingInfo
            ));

        } catch (Exception e) {
            log.error("❌ Error updating meeting details: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/{activityId}/meeting")
    public ResponseEntity<?> getMeetingDetails(
            @PathVariable String activityId,
            Authentication auth) {
        try {
            String tutorId = auth.getName();

            Activity activity = activityService.getActivityById(activityId);

            if (!activity.getTutorId().equals(tutorId)) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "error", "You can only view your own activity details"
                ));
            }

            if (activity.getVideoContent() == null) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "No meeting details set",
                        "meeting", Map.of()
                ));
            }

            // Build response with null-safe handling
            Map<String, Object> meetingInfo = new HashMap<>();
            if (activity.getVideoContent().getPlatform() != null) {
                meetingInfo.put("platform", activity.getVideoContent().getPlatform());
            }
            if (activity.getVideoContent().getMeetingLink() != null) {
                meetingInfo.put("meetingLink", activity.getVideoContent().getMeetingLink());
            }
            if (activity.getVideoContent().getMeetingId() != null) {
                meetingInfo.put("meetingId", activity.getVideoContent().getMeetingId());
            }
            if (activity.getVideoContent().getPasscode() != null) {
                meetingInfo.put("passcode", activity.getVideoContent().getPasscode());
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "meeting", meetingInfo
            ));

        } catch (Exception e) {
            log.error("❌ Error fetching meeting details: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Delete meeting details
     */
    @DeleteMapping("/{activityId}/meeting")
    public ResponseEntity<?> deleteMeetingDetails(
            @PathVariable String activityId,
            Authentication auth) {
        try {
            String tutorId = auth.getName();

            Activity activity = activityService.getActivityById(activityId);

            if (!activity.getTutorId().equals(tutorId)) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "error", "You can only delete your own meeting details"
                ));
            }

            // Remove meeting details
            Query query = new Query(Criteria.where("_id").is(activityId));
            Update update = new Update()
                    .unset("videoContent.platform")
                    .unset("videoContent.meetingLink")
                    .unset("videoContent.meetingId")
                    .unset("videoContent.passcode");

            mongoTemplate.updateFirst(query, update, Activity.class);

            log.info("✅ Meeting details deleted for activity: {}", activityId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Meeting details deleted successfully"
            ));

        } catch (Exception e) {
            log.error("❌ Error deleting meeting details: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @Data
    public static class MeetingDetailsRequest {
        private String platform;      // "Zoom", "Google Meet", "Microsoft Teams"
        private String meetingLink;   // Full URL
        private String meetingId;     // Meeting ID/Room number
        private String passcode;      // Password/Passcode
    }
}