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

import java.util.Map;

@RestController
@RequestMapping("/api/tutor/activities")
@PreAuthorize("hasRole('TUTOR')")
@RequiredArgsConstructor
@Slf4j
public class MeetingManagementController {

    private final ActivityService activityService;
    private final MongoTemplate mongoTemplate;

    /**
     * Add/Update meeting details
     */
    @PutMapping("/{activityId}/meeting")
    public ResponseEntity<?> updateMeetingDetails(
            @PathVariable String activityId,
            @Valid @RequestBody MeetingDetailsRequest request,
            Authentication auth) {
        try {
            String tutorId = auth.getName();

            // Verify ownership
            Activity activity = activityService.getActivityById(activityId);
            if (!activity.getTutorId().equals(tutorId)) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "error", "You can only manage your own activities"
                ));
            }

            // Update meeting details
            Query query = new Query(Criteria.where("_id").is(activityId));
            Update update = new Update();

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

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Meeting details updated successfully",
                    "meeting", Map.of(
                            "platform", updated.getVideoContent().getPlatform(),
                            "meetingLink", updated.getVideoContent().getMeetingLink(),
                            "meetingId", updated.getVideoContent().getMeetingId(),
                            "passcode", updated.getVideoContent().getPasscode()
                    )
            ));

        } catch (Exception e) {
            log.error("❌ Error updating meeting details: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Get meeting details (tutor only)
     */
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
                        "meeting", Map.of(),
                        "message", "No meeting details set"
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "meeting", Map.of(
                            "platform", activity.getVideoContent().getPlatform(),
                            "meetingLink", activity.getVideoContent().getMeetingLink(),
                            "meetingId", activity.getVideoContent().getMeetingId(),
                            "passcode", activity.getVideoContent().getPasscode()
                    )
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