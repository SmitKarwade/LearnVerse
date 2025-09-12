package com.example.learnverse.activity.service;

import com.example.learnverse.activity.model.Activity;
import com.example.learnverse.activity.nlp.QueryParser;
import com.example.learnverse.activity.repository.ActivityRepository;
import com.example.learnverse.activity.filter.ActivityFilterDto;
import com.example.learnverse.auth.service.UserService;
import com.example.learnverse.auth.user.AppUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.NearQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityService {

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private TfIdfService tfIdfService;

    @Autowired
    private QueryParser queryParser;

    public Page<Activity> getRecommendedActivities(String naturalQuery, String userId,
                                                   Double userLatitude, Double userLongitude,
                                                   Integer page, Integer size) {
        log.info("Getting recommendations for natural query: {}", naturalQuery);

        // Parse the natural language query
        QueryParser.ParsedQuery parsedQuery = queryParser.parseQuery(naturalQuery);

        // Build base query with hard filters
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        // Base criteria
        criteriaList.add(Criteria.where("isActive").is(true));
        criteriaList.add(Criteria.where("isPublic").is(true));

        // Apply parsed filters
        if (parsedQuery.getPriceMax() != null) {
            criteriaList.add(Criteria.where("pricing.price").lte(parsedQuery.getPriceMax()));
        }

        if (parsedQuery.getMode() != null) {
            criteriaList.add(Criteria.where("mode").regex("^" + Pattern.quote(parsedQuery.getMode()) + "$", "i"));
        }

        if (parsedQuery.getTimePreference() != null && parsedQuery.getTimePreference().equals("weekend")) {
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("schedule.sessionDays").regex("saturday", "i"),
                    Criteria.where("schedule.sessionDays").regex("sunday", "i")
            ));
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        // Get candidate activities
        List<Activity> candidates = mongoTemplate.find(query, Activity.class);
        log.info("Found {} candidate activities after hard filtering", candidates.size());

        if (candidates.isEmpty()) {
            return PageableExecutionUtils.getPage(new ArrayList<>(),
                    PageRequest.of(page != null ? page : 0, size != null ? size : 20), () -> 0);
        }

        // Build TF-IDF index and rank by similarity
        List<Activity> rankedActivities = rankActivitiesBySimilarity(candidates, parsedQuery.getQueryText());

        // Apply proximity filtering if distance is specified
        final List<Activity> finalRankedActivities;
        if (parsedQuery.getDistanceKm() != null && userLatitude != null && userLongitude != null) {
            finalRankedActivities = filterByProximity(rankedActivities, userLatitude, userLongitude, parsedQuery.getDistanceKm());
        } else {
            finalRankedActivities = rankedActivities; // Skip proximity filtering if no location
        }

        // Store the total count in a final variable
        final long totalCount = finalRankedActivities.size();

        // Apply pagination
        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        int start = pageNum * pageSize;
        int end = Math.min(start + pageSize, finalRankedActivities.size());

        List<Activity> paginatedResults = start < finalRankedActivities.size() ?
                finalRankedActivities.subList(start, end) : new ArrayList<>();

        Pageable pageable = PageRequest.of(pageNum, pageSize);

        return PageableExecutionUtils.getPage(paginatedResults, pageable, () -> totalCount);
    }

    private List<Activity> rankActivitiesBySimilarity(List<Activity> activities, String queryText) {
        if (queryText == null || queryText.trim().isEmpty() || activities.isEmpty()) {
            return activities;
        }

        // Build activity texts for TF-IDF
        List<String> activityTexts = activities.stream()
                .map(this::buildActivityText)
                .collect(Collectors.toList());

        // Build TF-IDF index
        tfIdfService.buildIndex(activityTexts);

        // Get query vector
        double[] queryVector = tfIdfService.vectorizeText(queryText);

        // Calculate similarities
        List<ActivitySimilarity> similarities = new ArrayList<>();
        for (int i = 0; i < activities.size(); i++) {
            double[] activityVector = tfIdfService.vectorizeText(activityTexts.get(i));
            double similarity = TfIdfService.cosineSimilarity(queryVector, activityVector);
            similarities.add(new ActivitySimilarity(activities.get(i), similarity));
        }

        // Sort by similarity score (descending)
        similarities.sort((a, b) -> Double.compare(b.similarity, a.similarity));

        log.info("Ranked activities by similarity - top score: {}",
                similarities.isEmpty() ? 0 : similarities.get(0).similarity);

        return similarities.stream()
                .map(as -> as.activity)
                .collect(Collectors.toList());
    }

    private String buildActivityText(Activity activity) {
        StringBuilder text = new StringBuilder();

        // Weight important fields by repetition
        if (activity.getTitle() != null) {
            text.append(activity.getTitle()).append(" ");
            text.append(activity.getTitle()).append(" "); // 2x weight
        }

        if (activity.getSubject() != null) {
            text.append(activity.getSubject()).append(" ");
            text.append(activity.getSubject()).append(" "); // 2x weight
        }

        if (activity.getDescription() != null) {
            text.append(activity.getDescription()).append(" ");
        }

        if (activity.getTags() != null) {
            for (String tag : activity.getTags()) {
                text.append(tag).append(" ");
            }
        }

        if (activity.getActivityType() != null) {
            text.append(activity.getActivityType()).append(" ");
        }

        if (activity.getClassType() != null) {
            text.append(activity.getClassType()).append(" ");
        }

        return text.toString().trim();
    }

    private List<Activity> filterByProximity(List<Activity> activities, double userLat, double userLon, double maxDistanceKm) {
        return activities.stream()
                .filter(activity -> {
                    if (activity.getLocation() == null ||
                            activity.getLocation().getCoordinates() == null ||
                            activity.getLocation().getCoordinates().getCoordinates() == null ||
                            activity.getLocation().getCoordinates().getCoordinates().size() < 2) {
                        return false;
                    }

                    double activityLon = activity.getLocation().getCoordinates().getCoordinates().get(0);
                    double activityLat = activity.getLocation().getCoordinates().getCoordinates().get(1);

                    double distance = calculateDistance(userLat, userLon, activityLat, activityLon);
                    return distance <= maxDistanceKm;
                })
                .collect(Collectors.toList());
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c; // Distance in km

        return distance;
    }

    private static class ActivitySimilarity {
        Activity activity;
        double similarity;

        ActivitySimilarity(Activity activity, double similarity) {
            this.activity = activity;
            this.similarity = similarity;
        }
    }


    // Existing methods...
    public Activity createActivityByTutor(Activity activity, String tutorId) {
        normalizeActivityData(activity);

        if (activity.getLocation() != null && activity.getLocation().getCoordinates() != null) {
            Double lon = activity.getLocation().getCoordinates().getCoordinates().get(0);
            Double lat = activity.getLocation().getCoordinates().getCoordinates().get(1);

            if (lat != null && lon != null) {
                Activity.Location.Coordinates geoJsonCoords = Activity.Location.Coordinates.builder()
                        .type("Point")
                        .coordinates(Arrays.asList(lon, lat))
                        .build();

                activity.getLocation().setCoordinates(geoJsonCoords);
            }
        }


        activity.setTutorId(tutorId);
        activity.setCreatedAt(new java.util.Date());
        activity.setUpdatedAt(new java.util.Date());
        activity.setIsActive(true);
        return activityRepository.save(activity);
    }

    public List<Activity> getActivitiesForUser(String userId) {
        AppUser user = userService.getUserById(userId);
        if (user.getInterests() == null || user.getInterests().isEmpty()) {
            throw new RuntimeException("User must add interests first to see personalized activities");
        }
        log.info("Fetching activities for user: {} with interests: {}", userId, user.getInterests());

        // Create case-insensitive search for user interests
        Query query = new Query();
        List<Criteria> interestCriteria = new ArrayList<>();

        for (String interest : user.getInterests()) {
            String escapedInterest = Pattern.quote(interest.toLowerCase().trim());
            interestCriteria.add(Criteria.where("subject").regex("^" + escapedInterest + "$", "i"));
            interestCriteria.add(Criteria.where("tags").regex(escapedInterest, "i"));
        }

        query.addCriteria(new Criteria().andOperator(
                new Criteria().orOperator(interestCriteria.toArray(new Criteria[0])),
                Criteria.where("isActive").is(true),
                Criteria.where("isPublic").is(true)
        ));

        List<Activity> activities = mongoTemplate.find(query, Activity.class);
        log.info("Found {} activities matching user interests", activities.size());
        return activities;
    }

    public List<Activity> getAllActivitiesForUsers() {
        return activityRepository.findByIsActiveAndIsPublic(true, true);
    }

    public Page<Activity> getActivitiesByProximity(ActivityFilterDto filterDto) {
        if (filterDto.getUserLatitude() == null || filterDto.getUserLongitude() == null) {
            throw new IllegalArgumentException("User latitude and longitude are required for proximity search.");
        }

        Point userLocation = new Point(filterDto.getUserLongitude(), filterDto.getUserLatitude());
        double maxDistance = (filterDto.getMaxDistanceKm() != null ? filterDto.getMaxDistanceKm() : 50); // default 50 km
        Distance maxDistanceMeters = new Distance(maxDistance, Metrics.KILOMETERS);

        NearQuery nearQuery = NearQuery.near(userLocation)
                .maxDistance(maxDistanceMeters)
                .spherical(true); // No distanceField() here

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.geoNear(nearQuery, "distance"),
                Aggregation.match(
                        Criteria.where("isActive").is(true)
                                .and("isPublic").is(true)
                ),
                // Skip and limit for pagination
                Aggregation.skip((filterDto.getPage() != null ? filterDto.getPage() : 0) * (filterDto.getSize() != null ? filterDto.getSize() : 20)),
                Aggregation.limit(filterDto.getSize() != null ? filterDto.getSize() : 20)
        );

        AggregationResults<Activity> results = mongoTemplate.aggregate(aggregation, "activities", Activity.class);

        List<Activity> activities = results.getMappedResults();

        Pageable pageable = PageRequest.of(
                filterDto.getPage() != null ? filterDto.getPage() : 0,
                filterDto.getSize() != null ? filterDto.getSize() : 20
        );

        return PageableExecutionUtils.getPage(activities, pageable, () -> activities.size());
    }

    // Updated comprehensive filtering method with case-insensitive search
    public Page<Activity> getFilteredActivities(ActivityFilterDto filterDto) {
        log.info("Applying filters: {}", filterDto);

        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        // Base criteria - always include
        criteriaList.add(Criteria.where("isActive").is(true));
        criteriaList.add(Criteria.where("isPublic").is(true));

        // Text search - case insensitive
        if (StringUtils.hasText(filterDto.getSearchQuery())) {
            String searchQuery = filterDto.getSearchQuery().toLowerCase().trim();
            TextCriteria textCriteria = TextCriteria.forDefaultLanguage()
                    .matchingAny(searchQuery.split("\\s+"));
            query.addCriteria(textCriteria);
        }

        // Subject filter - case insensitive
        if (filterDto.getSubjects() != null && !filterDto.getSubjects().isEmpty()) {
            List<Criteria> subjectCriteria = filterDto.getSubjects().stream()
                    .filter(subject -> subject != null && !subject.trim().isEmpty())
                    .map(subject -> {
                        String escapedSubject = Pattern.quote(subject.toLowerCase().trim());
                        return Criteria.where("subject").regex("^" + escapedSubject + "$", "i");
                    })
                    .collect(Collectors.toList());

            if (subjectCriteria.size() == 1) {
                criteriaList.add(subjectCriteria.get(0));
            } else {
                criteriaList.add(new Criteria().orOperator(subjectCriteria.toArray(new Criteria[0])));
            }
        }

        // Activity type filter - case insensitive
        if (filterDto.getActivityTypes() != null && !filterDto.getActivityTypes().isEmpty()) {
            List<Criteria> typeCriteria = filterDto.getActivityTypes().stream()
                    .filter(type -> type != null && !type.trim().isEmpty())
                    .map(type -> {
                        String escapedType = Pattern.quote(type.toLowerCase().trim());
                        return Criteria.where("activityType").regex("^" + escapedType + "$", "i");
                    })
                    .collect(Collectors.toList());

            if (typeCriteria.size() == 1) {
                criteriaList.add(typeCriteria.get(0));
            } else {
                criteriaList.add(new Criteria().orOperator(typeCriteria.toArray(new Criteria[0])));
            }
        }

        // Mode filter - case insensitive
        if (filterDto.getModes() != null && !filterDto.getModes().isEmpty()) {
            List<Criteria> modeCriteria = filterDto.getModes().stream()
                    .filter(mode -> mode != null && !mode.trim().isEmpty())
                    .map(mode -> {
                        String escapedMode = Pattern.quote(mode.toLowerCase().trim());
                        return Criteria.where("mode").regex("^" + escapedMode + "$", "i");
                    })
                    .collect(Collectors.toList());

            if (modeCriteria.size() == 1) {
                criteriaList.add(modeCriteria.get(0));
            } else {
                criteriaList.add(new Criteria().orOperator(modeCriteria.toArray(new Criteria[0])));
            }
        }

        // Difficulty filter - case insensitive
        if (filterDto.getDifficulties() != null && !filterDto.getDifficulties().isEmpty()) {
            List<Criteria> difficultyCriteria = filterDto.getDifficulties().stream()
                    .filter(difficulty -> difficulty != null && !difficulty.trim().isEmpty())
                    .map(difficulty -> {
                        String escapedDifficulty = Pattern.quote(difficulty.toLowerCase().trim());
                        return Criteria.where("difficulty").regex("^" + escapedDifficulty + "$", "i");
                    })
                    .collect(Collectors.toList());

            if (difficultyCriteria.size() == 1) {
                criteriaList.add(difficultyCriteria.get(0));
            } else {
                criteriaList.add(new Criteria().orOperator(difficultyCriteria.toArray(new Criteria[0])));
            }
        }

        // Location filters - case insensitive
        if (filterDto.getCities() != null && !filterDto.getCities().isEmpty()) {
            List<Criteria> cityCriteria = filterDto.getCities().stream()
                    .filter(city -> city != null && !city.trim().isEmpty())
                    .map(city -> {
                        String escapedCity = Pattern.quote(city.toLowerCase().trim());
                        return Criteria.where("location.city").regex("^" + escapedCity + "$", "i");
                    })
                    .collect(Collectors.toList());

            if (cityCriteria.size() == 1) {
                criteriaList.add(cityCriteria.get(0));
            } else {
                criteriaList.add(new Criteria().orOperator(cityCriteria.toArray(new Criteria[0])));
            }
        }

        if (filterDto.getStates() != null && !filterDto.getStates().isEmpty()) {
            List<Criteria> stateCriteria = filterDto.getStates().stream()
                    .filter(state -> state != null && !state.trim().isEmpty())
                    .map(state -> {
                        String escapedState = Pattern.quote(state.toLowerCase().trim());
                        return Criteria.where("location.state").regex("^" + escapedState + "$", "i");
                    })
                    .collect(Collectors.toList());

            if (stateCriteria.size() == 1) {
                criteriaList.add(stateCriteria.get(0));
            } else {
                criteriaList.add(new Criteria().orOperator(stateCriteria.toArray(new Criteria[0])));
            }
        }

        // Price range filter
        if (filterDto.getMinPrice() != null || filterDto.getMaxPrice() != null) {
            Criteria priceCriteria = Criteria.where("pricing.price");
            if (filterDto.getMinPrice() != null) {
                priceCriteria = priceCriteria.gte(filterDto.getMinPrice());
            }
            if (filterDto.getMaxPrice() != null) {
                priceCriteria = priceCriteria.lte(filterDto.getMaxPrice());
            }
            criteriaList.add(priceCriteria);
        }

        // Price type filter - case insensitive
        if (filterDto.getPriceTypes() != null && !filterDto.getPriceTypes().isEmpty()) {
            List<Criteria> priceTypeCriteria = filterDto.getPriceTypes().stream()
                    .filter(priceType -> priceType != null && !priceType.trim().isEmpty())
                    .map(priceType -> {
                        String escapedPriceType = Pattern.quote(priceType.toLowerCase().trim());
                        return Criteria.where("pricing.priceType").regex("^" + escapedPriceType + "$", "i");
                    })
                    .collect(Collectors.toList());

            if (priceTypeCriteria.size() == 1) {
                criteriaList.add(priceTypeCriteria.get(0));
            } else {
                criteriaList.add(new Criteria().orOperator(priceTypeCriteria.toArray(new Criteria[0])));
            }
        }

        // Age range filter
        if (filterDto.getMinAge() != null || filterDto.getMaxAge() != null) {
            if (filterDto.getMinAge() != null && filterDto.getMaxAge() != null) {
                criteriaList.add(Criteria.where("suitableAgeGroup.minAge").lte(filterDto.getMaxAge())
                        .and("suitableAgeGroup.maxAge").gte(filterDto.getMinAge()));
            } else if (filterDto.getMinAge() != null) {
                criteriaList.add(Criteria.where("suitableAgeGroup.maxAge").gte(filterDto.getMinAge()));
            } else {
                criteriaList.add(Criteria.where("suitableAgeGroup.minAge").lte(filterDto.getMaxAge()));
            }
        }

        // Duration filter
        if (filterDto.getMinDuration() != null || filterDto.getMaxDuration() != null) {
            Criteria durationCriteria = Criteria.where("duration.totalDuration");
            if (filterDto.getMinDuration() != null) {
                durationCriteria = durationCriteria.gte(filterDto.getMinDuration());
            }
            if (filterDto.getMaxDuration() != null) {
                durationCriteria = durationCriteria.lte(filterDto.getMaxDuration());
            }
            criteriaList.add(durationCriteria);
        }

        // Rating filter
        if (filterDto.getMinRating() != null) {
            criteriaList.add(Criteria.where("reviews.averageRating").gte(filterDto.getMinRating()));
        }

        // Boolean filters
        if (filterDto.getDemoAvailable() != null) {
            criteriaList.add(Criteria.where("demoAvailable").is(filterDto.getDemoAvailable()));
        }

        if (filterDto.getFeatured() != null) {
            criteriaList.add(Criteria.where("featured").is(filterDto.getFeatured()));
        }

        if (filterDto.getFreeTrialAvailable() != null && filterDto.getFreeTrialAvailable()) {
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("pricing.freeTrialDays").gt(0),
                    Criteria.where("demoDetails.freeTrial").is(true)
            ));
        }

        if (filterDto.getInstallmentAvailable() != null) {
            criteriaList.add(Criteria.where("pricing.installmentAvailable").is(filterDto.getInstallmentAvailable()));
        }

        if (filterDto.getFlexibleScheduling() != null) {
            criteriaList.add(Criteria.where("schedule.flexibleScheduling").is(filterDto.getFlexibleScheduling()));
        }

        if (filterDto.getSelfPaced() != null) {
            criteriaList.add(Criteria.where("schedule.selfPaced").is(filterDto.getSelfPaced()));
        }

        if (filterDto.getSessionDays() != null && !filterDto.getSessionDays().isEmpty()) {
            List<Criteria> sessionDayCriteria = filterDto.getSessionDays().stream()
                    .filter(day -> day != null && !day.trim().isEmpty())
                    .map(day -> {
                        String escapedDay = Pattern.quote(day.toLowerCase().trim());
                        return Criteria.where("schedule.sessionDays").regex(escapedDay, "i");
                    })
                    .collect(Collectors.toList());

            if (sessionDayCriteria.size() == 1) {
                criteriaList.add(sessionDayCriteria.get(0));
            } else {
                criteriaList.add(new Criteria().orOperator(sessionDayCriteria.toArray(new Criteria[0])));
            }
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
        }

        // Add sorting
        Pageable pageable = createPageable(filterDto);
        query.with(pageable);

        log.info("Final MongoDB query: {}", query);

        List<Activity> activities = mongoTemplate.find(query, Activity.class);
        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Activity.class);

        log.info("Found {} activities out of {} total", activities.size(), total);

        return PageableExecutionUtils.getPage(activities, pageable, () -> total);
    }

    private Pageable createPageable(ActivityFilterDto filterDto) {
        int page = filterDto.getPage() != null ? filterDto.getPage() : 0;
        int size = filterDto.getSize() != null ? filterDto.getSize() : 20;

        Sort sort = Sort.unsorted();

        if (filterDto.getSortBy() != null) {
            Sort.Direction direction = "desc".equalsIgnoreCase(filterDto.getSortDirection())
                    ? Sort.Direction.DESC : Sort.Direction.ASC;

            switch (filterDto.getSortBy().toLowerCase()) {
                case "price":
                    sort = Sort.by(direction, "pricing.price");
                    break;
                case "rating":
                    sort = Sort.by(direction, "reviews.averageRating");
                    break;
                case "popularity":
                    sort = Sort.by(direction, "enrollmentInfo.enrolledCount");
                    break;
                case "newest":
                    sort = Sort.by(Sort.Direction.DESC, "createdAt");
                    break;
                case "duration":
                    sort = Sort.by(direction, "duration.totalDuration");
                    break;
                case "city":
                    sort = Sort.by(direction, "location.city");
                    break;
                case "state":
                    sort = Sort.by(direction, "location.state");
                    break;
                default:
                    sort = Sort.by(direction, filterDto.getSortBy());
            }
        } else {
            // Default sorting by newest
            sort = Sort.by(Sort.Direction.DESC, "createdAt");
        }

        return PageRequest.of(page, size, sort);
    }

    // Keep the original normalization for data storage
    private void normalizeActivityData(Activity activity) {
        if (activity.getSubject() != null) {
            activity.setSubject(activity.getSubject().toLowerCase().trim());
        }
        if (activity.getMode() != null) {
            activity.setMode(activity.getMode().toLowerCase().trim());
        }
        if (activity.getActivityType() != null) {
            activity.setActivityType(activity.getActivityType().toLowerCase().trim());
        }
        if (activity.getDifficulty() != null) {
            activity.setDifficulty(activity.getDifficulty().toLowerCase().trim());
        }
        if (activity.getTags() != null) {
            List<String> normalizedTags = activity.getTags().stream()
                    .filter(tag -> tag != null && !tag.trim().isEmpty())
                    .map(tag -> tag.toLowerCase().trim())
                    .collect(Collectors.toList());
            activity.setTags(normalizedTags);
        }
        // Normalize location data
        if (activity.getLocation() != null) {
            if (activity.getLocation().getCity() != null) {
                activity.getLocation().setCity(activity.getLocation().getCity().toLowerCase().trim());
            }
            if (activity.getLocation().getState() != null) {
                activity.getLocation().setState(activity.getLocation().getState().toLowerCase().trim());
            }
        }
        // Normalize pricing data
        if (activity.getPricing() != null && activity.getPricing().getPriceType() != null) {
            activity.getPricing().setPriceType(activity.getPricing().getPriceType().toLowerCase().trim());
        }
        // Normalize schedule data
        if (activity.getSchedule() != null && activity.getSchedule().getSessionDays() != null) {
            List<String> normalizedDays = activity.getSchedule().getSessionDays().stream()
                    .filter(day -> day != null && !day.trim().isEmpty())
                    .map(day -> day.toLowerCase().trim())
                    .collect(Collectors.toList());
            activity.getSchedule().setSessionDays(normalizedDays);
        }
    }
}