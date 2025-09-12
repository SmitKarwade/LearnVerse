package com.example.learnverse.activity.controller;

import com.example.learnverse.activity.model.Activity;
import com.example.learnverse.activity.model.PagedResponse;
import com.example.learnverse.activity.service.ActivityService;
import com.example.learnverse.activity.filter.ActivityFilterDto;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
@Slf4j
public class ActivityController {

    private final ActivityService activityService;

    @Data
    public static class NaturalSearchRequest {
        private String text;
        private Double userLatitude;
        private Double userLongitude;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createActivity(@RequestBody Activity activity, Authentication auth) {
        String tutorId = auth.getName();
        boolean isTutor = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_TUTOR"));

        if (isTutor) {
            Activity saved = activityService.createActivityByTutor(activity, tutorId);
            return ResponseEntity.ok(saved);
        } else {
            return ResponseEntity.status(403).body("Only tutors can create activities.");
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
}