package com.example.learnverse.activity.filter;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityFilterDto {
    // Subject and category filters
    private List<String> subjects;
    private List<String> activityTypes;
    private List<String> modes; // online, offline, hybrid
    private List<String> difficulties; // beginner, intermediate, advanced

    // Location filters
    private List<String> cities;
    private List<String> states;
    private Double userLatitude;
    private Double userLongitude;
    private Double maxDistanceKm;

    // Price filters
    private Integer minPrice;
    private Integer maxPrice;
    private List<String> priceTypes; // per_session, per_course, monthly

    // Age and duration filters
    private Integer minAge;
    private Integer maxAge;
    private Integer minDuration; // in minutes
    private Integer maxDuration;

    // Feature filters
    private Boolean demoAvailable;
    private Boolean featured;
    private Boolean freeTrialAvailable;
    private Boolean installmentAvailable;

    // Rating filter
    private Double minRating;

    // Schedule filters
    private List<String> sessionDays; // monday, tuesday, etc.
    private Boolean flexibleScheduling;
    private Boolean selfPaced;

    // Sorting options
    private String sortBy; // price, rating, popularity, newest, duration
    private String sortDirection; // asc, desc

    // Pagination
    private Integer page;
    private Integer size;

    // Search query
    private String searchQuery;
}

