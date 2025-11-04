package com.example.learnverse.enrollment.controller;

import com.example.learnverse.activity.model.Activity;
import com.example.learnverse.activity.service.ActivityService;
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
public class EnrollmentManagementController {

    private final ActivityService activityService;
    private final MongoTemplate mongoTemplate;

    /**
     * Open enrollment for activity
     */
    @PutMapping("/{activityId}/enrollment/open")
    public ResponseEntity<?> openEnrollment(
            @PathVariable String activityId,
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

            // Update enrollment status
            Query query = new Query(Criteria.where("_id").is(activityId));
            Update update = new Update().set("enrollmentInfo.enrollmentStatus", "open");
            mongoTemplate.updateFirst(query, update, Activity.class);

            log.info("✅ Enrollment opened for activity: {}", activityId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Enrollment is now open"
            ));

        } catch (Exception e) {
            log.error("❌ Error opening enrollment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Close enrollment for activity
     */
    @PutMapping("/{activityId}/enrollment/close")
    public ResponseEntity<?> closeEnrollment(
            @PathVariable String activityId,
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

            // Update enrollment status
            Query query = new Query(Criteria.where("_id").is(activityId));
            Update update = new Update().set("enrollmentInfo.enrollmentStatus", "closed");
            mongoTemplate.updateFirst(query, update, Activity.class);

            log.info("✅ Enrollment closed for activity: {}", activityId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Enrollment is now closed"
            ));

        } catch (Exception e) {
            log.error("❌ Error closing enrollment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Set max capacity
     */
    @PutMapping("/{activityId}/enrollment/capacity")
    public ResponseEntity<?> setCapacity(
            @PathVariable String activityId,
            @RequestBody CapacityRequest request,
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

            // Update max capacity
            Query query = new Query(Criteria.where("_id").is(activityId));
            Update update = new Update().set("enrollmentInfo.maxCapacity", request.getMaxCapacity());
            mongoTemplate.updateFirst(query, update, Activity.class);

            log.info("✅ Max capacity set to {} for activity: {}", request.getMaxCapacity(), activityId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Max capacity updated",
                    "maxCapacity", request.getMaxCapacity()
            ));

        } catch (Exception e) {
            log.error("❌ Error setting capacity: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @Data
    public static class CapacityRequest {
        private Integer maxCapacity;
    }
}
