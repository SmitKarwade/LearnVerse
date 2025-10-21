// Create/Update: src/main/java/com/example/learnverse/ai/service/CourseMatchingService.java
package com.example.learnverse.ai.service;

import com.example.learnverse.activity.model.Activity;
import com.example.learnverse.activity.service.ActivityService;
import com.example.learnverse.auth.user.AppUser;
import com.example.learnverse.auth.user.UserProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseMatchingService {

    private final ActivityService activityService;

    /**
     * Get relevant courses based on user profile, interests, and query context
     * Enhanced version that considers user's learning profile
     */
    public List<Activity> getRelevantCourses(AppUser user, String userQuery, int limit) {
        try {
            // Extract user preferences
            if (userQuery == null || userQuery.trim().isEmpty()) {
                userQuery = "recommended courses"; // Default fallback
            }

            UserProfile profile = user.getProfile();
            List<String> userInterests = getUserInterests(user, profile);
            Set<String> queryKeywords = extractKeywords(userQuery.toLowerCase());

            log.info("🔍 Finding courses for user: {}, interests: {}, keywords: {}",
                    user.getName(), userInterests, queryKeywords);

            // Get all available courses
            List<Activity> allCourses = activityService.getAllPublicActivities();

            if (allCourses.isEmpty()) {
                log.warn("⚠️ No public courses found");
                return List.of();
            }

            // Score and filter courses based on multiple criteria
            return allCourses.stream()
                    .map(course -> new ScoredCourse(course, calculateEnhancedScore(course, user, profile, userInterests, queryKeywords)))
                    .filter(scored -> scored.score > 0)
                    .sorted((a, b) -> Double.compare(b.score, a.score))
                    .limit(limit)
                    .map(scored -> {
                        log.debug("📚 Matched course: {} (Score: {:.1f})", scored.course.getTitle(), scored.score);
                        return scored.course;
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("❌ Error finding relevant courses: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Enhanced scoring algorithm that considers multiple factors
     */
    private double calculateEnhancedScore(Activity course, AppUser user, UserProfile profile,
                                          List<String> userInterests, Set<String> queryKeywords) {
        double score = 0.0;

        // Create searchable course content
        String courseText = buildCourseSearchText(course).toLowerCase();

        // 1. Interest Matching (Highest Priority - 40 points max)
        score += calculateInterestScore(courseText, userInterests);

        // 2. Query Keywords Matching (High Priority - 30 points max)
        score += calculateQueryScore(courseText, queryKeywords);

        // 3. Career Goal Alignment (Medium Priority - 20 points max)
        if (profile != null) {
            score += calculateCareerGoalScore(courseText, profile);
        }

        // 4. Target Skills Matching (Medium Priority - 15 points max)
        if (profile != null) {
            score += calculateTargetSkillsScore(courseText, profile);
        }

        // 5. Current Focus Area (Medium Priority - 10 points max)
        if (profile != null) {
            score += calculateFocusAreaScore(courseText, profile);
        }

        // 6. Difficulty Level Matching (Low Priority - 5 points max)
        if (profile != null) {
            score += calculateDifficultyScore(course, profile);
        }

        // 7. Boost for Free Courses (Small boost - 2 points)
        score += calculatePriceBoost(course);

        // 8. Recency/Popularity Boost (Small boost - 3 points)
        score += calculatePopularityBoost(course);

        return score;
    }

    /**
     * Calculate interest matching score (0-40 points)
     */
    private double calculateInterestScore(String courseText, List<String> userInterests) {
        if (userInterests == null || userInterests.isEmpty()) {
            return 0.0;
        }

        double interestScore = 0.0;
        for (String interest : userInterests) {
            String normalizedInterest = interest.toLowerCase().trim();

            // Exact match in title/subject (higher weight)
            if (courseText.contains(normalizedInterest)) {
                interestScore += 8.0; // Max 8 points per interest
            }

            // Partial word matching
            String[] interestWords = normalizedInterest.split("\\s+");
            for (String word : interestWords) {
                if (word.length() > 3 && courseText.contains(word)) {
                    interestScore += 2.0; // 2 points per word match
                }
            }
        }

        return Math.min(interestScore, 40.0); // Cap at 40 points
    }

    /**
     * Calculate query keywords score (0-30 points)
     */
    private double calculateQueryScore(String courseText, Set<String> queryKeywords) {
        if (queryKeywords == null || queryKeywords.isEmpty()) {
            return 0.0;
        }

        double queryScore = 0.0;
        for (String keyword : queryKeywords) {
            if (courseText.contains(keyword)) {
                // Weight by keyword length (longer keywords are more specific)
                if (keyword.length() >= 6) {
                    queryScore += 5.0; // High value for specific keywords
                } else if (keyword.length() >= 4) {
                    queryScore += 3.0; // Medium value
                } else {
                    queryScore += 1.0; // Low value for short keywords
                }
            }
        }

        return Math.min(queryScore, 30.0); // Cap at 30 points
    }

    /**
     * Calculate career goal alignment score (0-20 points)
     */
    private double calculateCareerGoalScore(String courseText, UserProfile profile) {
        if (profile.getCareerGoal() == null || profile.getCareerGoal().trim().isEmpty()) {
            return 0.0;
        }

        String careerGoal = profile.getCareerGoal().toLowerCase();
        double careerScore = 0.0;

        // Direct career goal matching
        if (courseText.contains(careerGoal)) {
            careerScore += 15.0;
        }

        // Career-related keyword matching
        String[] careerWords = careerGoal.split("\\s+");
        for (String word : careerWords) {
            if (word.length() > 3 && courseText.contains(word)) {
                careerScore += 2.5;
            }
        }

        return Math.min(careerScore, 20.0);
    }

    /**
     * Calculate target skills score (0-15 points)
     */
    private double calculateTargetSkillsScore(String courseText, UserProfile profile) {
        if (profile.getTargetSkills() == null || profile.getTargetSkills().isEmpty()) {
            return 0.0;
        }

        double skillsScore = 0.0;
        for (String skill : profile.getTargetSkills()) {
            String normalizedSkill = skill.toLowerCase().trim();
            if (courseText.contains(normalizedSkill)) {
                skillsScore += 3.0; // 3 points per matching skill
            }
        }

        return Math.min(skillsScore, 15.0);
    }

    /**
     * Calculate current focus area score (0-10 points)
     */
    private double calculateFocusAreaScore(String courseText, UserProfile profile) {
        if (profile.getCurrentFocusArea() == null || profile.getCurrentFocusArea().trim().isEmpty()) {
            return 0.0;
        }

        String focusArea = profile.getCurrentFocusArea().toLowerCase();
        if (courseText.contains(focusArea)) {
            return 10.0; // Full points for focus area match
        }

        // Partial matching
        String[] focusWords = focusArea.split("\\s+");
        double focusScore = 0.0;
        for (String word : focusWords) {
            if (word.length() > 3 && courseText.contains(word)) {
                focusScore += 2.5;
            }
        }

        return Math.min(focusScore, 10.0);
    }

    /**
     * Calculate difficulty level matching (0-5 points)
     */
    private double calculateDifficultyScore(Activity course, UserProfile profile) {
        // For now, give slight preference to beginner courses for new learners
        // This can be enhanced based on user's completed courses count

        if (course.getDifficulty() == null) {
            return 1.0; // Neutral score
        }

        String difficulty = course.getDifficulty().toLowerCase();
        Integer completedCourses = profile.getCompletedCourses();

        if (completedCourses == null || completedCourses == 0) {
            // New learners - prefer beginner courses
            return difficulty.contains("beginner") ? 5.0 : 2.0;
        } else if (completedCourses < 3) {
            // Some experience - prefer beginner/intermediate
            return difficulty.contains("intermediate") ? 5.0 :
                    difficulty.contains("beginner") ? 3.0 : 1.0;
        } else {
            // Experienced - prefer intermediate/advanced
            return difficulty.contains("advanced") ? 5.0 :
                    difficulty.contains("intermediate") ? 4.0 : 2.0;
        }
    }

    /**
     * Calculate price boost (0-2 points)
     */
    private double calculatePriceBoost(Activity course) {
        if (course.getPricing() == null ||
                course.getPricing().getPrice() == null ||
                course.getPricing().getPrice() == 0) {
            return 2.0; // Boost for free courses
        }
        return 0.0;
    }

    /**
     * Calculate popularity boost (0-3 points)
     */
    private double calculatePopularityBoost(Activity course) {
        // This is a placeholder - can be enhanced with actual enrollment/rating data
        if (course.getIsActive() != null && course.getIsActive()) {
            return 1.0;
        }
        return 0.0;
    }

    /**
     * Build comprehensive searchable text from course
     */
    private String buildCourseSearchText(Activity course) {
        StringBuilder text = new StringBuilder();

        if (course.getTitle() != null) text.append(course.getTitle()).append(" ");
        if (course.getSubject() != null) text.append(course.getSubject()).append(" ");
        if (course.getDescription() != null) text.append(course.getDescription()).append(" ");
        if (course.getDifficulty() != null) text.append(course.getDifficulty()).append(" ");
        if (course.getActivityType() != null) text.append(course.getActivityType()).append(" ");
        if (course.getTags() != null) {
            text.append(String.join(" ", course.getTags())).append(" ");
        }

        return text.toString();
    }

    /**
     * Get user interests from profile or fallback to basic interests
     */
    private List<String> getUserInterests(AppUser user, UserProfile profile) {
        if (profile != null && profile.getInterests() != null && !profile.getInterests().isEmpty()) {
            return profile.getInterests();
        } else if (user.getInterests() != null && !user.getInterests().isEmpty()) {
            return user.getInterests();
        }
        return List.of("learning"); // Default fallback
    }

    /**
     * Extract meaningful keywords from user query
     */
    private Set<String> extractKeywords(String query) {
        Set<String> stopWords = Set.of(
                "i", "want", "to", "learn", "how", "what", "is", "the", "a", "an", "and",
                "or", "but", "in", "on", "at", "for", "with", "by", "from", "of", "about",
                "can", "should", "would", "could", "will", "shall", "may", "might",
                "help", "me", "please", "thanks", "thank", "you", "my", "mine"
        );

        return Arrays.stream(query.split("\\s+"))
                .map(String::toLowerCase)
                .filter(word -> word.length() > 2) // Minimum 3 characters
                .filter(word -> !stopWords.contains(word))
                .filter(word -> word.matches("[a-zA-Z]+")) // Only alphabetic words
                .collect(Collectors.toSet());
    }

    /**
     * Inner class for scoring courses
     */
    private static class ScoredCourse {
        final Activity course;
        final double score;

        ScoredCourse(Activity course, double score) {
            this.course = course;
            this.score = score;
        }
    }
}