package com.example.learnverse.activity.controller;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import com.example.learnverse.activity.model.Activity;
import com.example.learnverse.activity.model.PagedResponse;
import com.example.learnverse.activity.repository.ActivityRepository;
import com.example.learnverse.activity.service.ActivityService;
import com.example.learnverse.activity.filter.ActivityFilterDto;
import com.example.learnverse.auth.annotation.RequireApprovedTutor;
import com.example.learnverse.community.cloudinary.CloudinaryService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
@Slf4j
public class ActivityController {

    private final ActivityService activityService;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private Cloudinary cloudinary;

    @Data
    public static class NaturalSearchRequest {
        private String text;
        private Double userLatitude;
        private Double userLongitude;
    }

    @PreAuthorize("hasRole('TUTOR')")
    @PostMapping("/create")
    public ResponseEntity<?> createActivity(@RequestBody Activity activity, Authentication auth) {
        String tutorId = auth.getName();

        // No need to check role manually anymore - annotation handles it
        Activity saved = activityService.createActivityByTutor(activity, tutorId);
        return ResponseEntity.ok(saved);
    }

    @PreAuthorize("hasRole('TUTOR')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateActivity(@PathVariable String id, @RequestBody Activity activity, Authentication auth) {
        try {
            String tutorId = auth.getName();

            Activity existingActivity = activityService.getActivityById(id);
            if (!existingActivity.getTutorId().equals(tutorId)) {
                return ResponseEntity.status(403).body("You can only update your own activities");
            }

            Activity updated = activityService.updateActivity(id, activity);
            return ResponseEntity.ok(updated);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * ✅ Get single activity by ID (for all authenticated users)
     */
    @GetMapping("/{activityId}")
    @PreAuthorize("hasAnyRole('USER', 'TUTOR')")
    public ResponseEntity<?> getActivityById(
            @PathVariable String activityId,
            Authentication auth) {
        try {
            Activity activity = activityService.getActivityById(activityId);

            // Check if user can access this activity
            String userId = auth.getName();
            boolean isTutor = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(role -> role.equals("ROLE_TUTOR"));

            // Allow access if:
            // 1. Activity is public
            // 2. User is the tutor who created it
            if (!activity.getIsPublic() && (!isTutor || !activity.getTutorId().equals(userId))) {
                return ResponseEntity.status(403).body("You don't have access to this activity");
            }

            return ResponseEntity.ok(activity);

        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body("Activity not found: " + e.getMessage());
        }
    }


    @PreAuthorize("hasRole('TUTOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteActivity(@PathVariable String id, Authentication auth) {
        try {
            String tutorId = auth.getName();

            Activity existingActivity = activityService.getActivityById(id);
            if (!existingActivity.getTutorId().equals(tutorId)) {
                return ResponseEntity.status(403).body("You can only delete your own activities");
            }

            activityService.deleteActivity(id);
            return ResponseEntity.ok("Activity deleted successfully");

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/by-ids")
    public ResponseEntity<?> getActivitiesByIds(
            @RequestBody List<String> ids,
            Authentication auth) {
        try {
            List<Activity> activities = activityService.getActivitiesByIds(ids);

            // Filter based on access permissions
            String userId = auth.getName();
            boolean isTutor = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(role -> role.equals("ROLE_TUTOR"));

            List<Activity> accessibleActivities = activities.stream()
                    .filter(activity -> {
                        if (activity.getIsPublic()) {
                            return true;
                        }
                        // Private activities only accessible by their tutor
                        return isTutor && activity.getTutorId().equals(userId);
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(accessibleActivities);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching activities: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('TUTOR')")
    @GetMapping("/my-activities")
    public ResponseEntity<?> getMyActivities(Authentication auth) {
        try {
            String tutorId = auth.getName();
            List<Activity> activities = activityService.getActivitiesByTutor(tutorId);
            return ResponseEntity.ok(activities);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/my-feed")
    public ResponseEntity<?> getPersonalizedActivities(Authentication auth) {
        String userId = auth.getName();
        boolean isUser = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_USER"));

        if (!isUser) {
            return ResponseEntity.status(403).body("Only users can fetch personalized activities.");
        }

        try {
            List<Activity> activities = activityService.getActivitiesForUser(userId);
            return ResponseEntity.ok(activities);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllActivities(Authentication auth) {
        boolean isUser = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_USER"));

        if (isUser) {
            List<Activity> activities = activityService.getAllActivitiesForUsers();
            return ResponseEntity.ok(activities);
        } else {
            return ResponseEntity.status(403).body("Only users can fetch activities.");
        }
    }

    // New comprehensive filtering endpoint
    @GetMapping("/filter")
    public ResponseEntity<?> getFilteredActivities(
            @RequestParam(required = false) List<String> subjects,
            @RequestParam(required = false) List<String> activityTypes,
            @RequestParam(required = false) List<String> modes,
            @RequestParam(required = false) List<String> difficulties,
            @RequestParam(required = false) List<String> cities,
            @RequestParam(required = false) List<String> states,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) List<String> priceTypes,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) Integer minDuration,
            @RequestParam(required = false) Integer maxDuration,
            @RequestParam(required = false) Boolean demoAvailable,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) Boolean freeTrialAvailable,
            @RequestParam(required = false) Boolean installmentAvailable,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) List<String> sessionDays,
            @RequestParam(required = false) Boolean flexibleScheduling,
            @RequestParam(required = false) Boolean selfPaced,
            @RequestParam(required = false) String searchQuery,
            @RequestParam(defaultValue = "newest") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            Authentication auth) {

        boolean isUser = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_USER"));

        if (!isUser) {
            return ResponseEntity.status(403).body("Only users can filter activities.");
        }

        try {
            ActivityFilterDto filterDto = ActivityFilterDto.builder()
                    .subjects(subjects)
                    .activityTypes(activityTypes)
                    .modes(modes)
                    .difficulties(difficulties)
                    .cities(cities)
                    .states(states)
                    .minPrice(minPrice)
                    .maxPrice(maxPrice)
                    .priceTypes(priceTypes)
                    .minAge(minAge)
                    .maxAge(maxAge)
                    .minDuration(minDuration)
                    .maxDuration(maxDuration)
                    .demoAvailable(demoAvailable)
                    .featured(featured)
                    .freeTrialAvailable(freeTrialAvailable)
                    .installmentAvailable(installmentAvailable)
                    .minRating(minRating)
                    .sessionDays(sessionDays)
                    .flexibleScheduling(flexibleScheduling)
                    .selfPaced(selfPaced)
                    .searchQuery(searchQuery)
                    .sortBy(sortBy)
                    .sortDirection(sortDirection)
                    .page(page)
                    .size(size)
                    .build();

            Page<Activity> activities = activityService.getFilteredActivities(filterDto);

            PagedResponse<Activity> response = new PagedResponse<>(
                    activities.getContent(),
                    activities.getNumber(),
                    activities.getSize(),
                    activities.getTotalElements(),
                    activities.getTotalPages(),
                    activities.isLast()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error filtering activities: " + e.getMessage());
        }
    }

    @PostMapping("/filter")
    public ResponseEntity<?> getFilteredActivitiesPost(
            @RequestBody ActivityFilterDto filterDto,
            Authentication auth) {

        boolean isUser = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_USER"));

        if (!isUser) {
            return ResponseEntity.status(403).body("Only users can filter activities.");
        }

        try {
            Page<Activity> activities = activityService.getFilteredActivities(filterDto);
            return ResponseEntity.ok(activities);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error filtering activities: " + e.getMessage());
        }
    }

    @GetMapping("/filter/proximity")
    public ResponseEntity<?> getActivitiesByProximity(
            @RequestParam Double userLatitude,
            @RequestParam Double userLongitude,
            @RequestParam(required = false) Double maxDistanceKm,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            Authentication auth) {

        boolean isUser = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_USER"));

        if (!isUser) {
            return ResponseEntity.status(403).body("Only users can filter activities.");
        }

        try {
            ActivityFilterDto filterDto = ActivityFilterDto.builder()
                    .userLatitude(userLatitude)
                    .userLongitude(userLongitude)
                    .maxDistanceKm(maxDistanceKm)
                    .page(page)
                    .size(size)
                    .build();

            Page<Activity> activities = activityService.getActivitiesByProximity(filterDto);

            long totalElements = 0;
            int totalPages = 0;

            if (activities != null) {
                totalElements = activities.getTotalElements() >= 0 ? activities.getTotalElements() : activities.getContent().size();
                totalPages = activities.getTotalPages() > 0 ? activities.getTotalPages() : 1;
            }

            PagedResponse<Activity> response = new PagedResponse<>(
                    activities.getContent(),
                    activities.getNumber(),
                    activities.getSize(),
                    totalElements,
                    totalPages,
                    activities.isLast()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error filtering activities by proximity: " + e.getMessage());
        }
    }

    @PostMapping("/search/natural")
    public ResponseEntity<?> searchActivitiesNaturally(
            @RequestBody NaturalSearchRequest request,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            Authentication auth) {

        boolean isUser = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_USER"));

        if (!isUser) {
            return ResponseEntity.status(403).body("Only users can search activities.");
        }

        try {
            String userId = auth.getName();
            Page<Activity> activities = activityService.getRecommendedActivities(
                    request.getText(),
                    userId,
                    request.getUserLatitude(),
                    request.getUserLongitude(),
                    page,
                    size
            );

            long totalElements = activities.getTotalElements();
            int totalPages = activities.getTotalPages();

            PagedResponse<Activity> response = new PagedResponse<>(
                    activities.getContent(),
                    activities.getNumber(),
                    activities.getSize(),
                    totalElements,
                    totalPages,
                    activities.isLast()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error in natural search: ", e);
            return ResponseEntity.badRequest().body("Error searching activities: " + e.getMessage());
        }
    }

    // ActivityController.java
    @PutMapping("/tutor/activities/{activityId}/banner")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<?> uploadBanner(
            @PathVariable String activityId,
            @RequestParam MultipartFile banner,
            Authentication auth) {

        try {
            String tutorId = auth.getName();
            Activity activity = activityService.getActivityById(activityId);

            if (!activity.getTutorId().equals(tutorId)) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "error", "You can only update your own activities"
                ));
            }

            // Upload to Cloudinary
            Map uploadResult = cloudinary.uploader().upload(
                    banner.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "learnverse/activity-banners",
                            "public_id", activityId + "/banner_" + UUID.randomUUID(),
                            "resource_type", "image",
                            "transformation", new Transformation()
                                    .width(1200).height(300)
                                    .crop("fill")
                                    .quality("auto")
                    )
            );

            String bannerUrl = (String) uploadResult.get("secure_url");
            activity.setBannerImageUrl(bannerUrl);
            activity.setUpdatedAt(new Date());

            activityRepository.save(activity);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "bannerUrl", bannerUrl
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/home-feed")
    public ResponseEntity<?> getHomeFeed(Authentication auth) {
        try {
            String userId = auth != null ? auth.getName() : null;

            Map<String, Object> homeFeed = activityService.getHomeFeed(userId);

            return ResponseEntity.ok(homeFeed);
        } catch (Exception e) {
            log.error("Error fetching home feed", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to fetch home feed: " + e.getMessage()
            ));
        }
    }

}