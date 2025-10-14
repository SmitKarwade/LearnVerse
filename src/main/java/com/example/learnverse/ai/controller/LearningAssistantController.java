// Update: src/main/java/com/example/learnverse/ai/controller/LearningAssistantController.java
package com.example.learnverse.ai.controller;

import com.example.learnverse.ai.service.CourseMatchingService;
import com.example.learnverse.ai.service.GeminiService;
import com.example.learnverse.auth.service.UserService;
import com.example.learnverse.auth.user.AppUser;
import com.example.learnverse.activity.model.Activity;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/learning-assistant")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LearningAssistantController {

    private final GeminiService geminiService;
    private final UserService userService;
    private final CourseMatchingService courseMatchingService;

    /**
     * Main AI chat endpoint - now checks for profile completion
     */
    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> askAssistant(
            @RequestBody AskRequest request,
            Authentication auth) {

        try {
            AppUser user = userService.getUserById(auth.getName());

            // Check if profile is completed
            if (user.getProfile() == null || !Boolean.TRUE.equals(user.getProfileCompleted())) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "needsProfile", true,
                        "message", "Hi! To provide you with personalized learning assistance, please complete your profile setup first. This helps me understand your goals and give you better recommendations! 🎯",
                        "profileSetupUrl", "/api/user/profile/setup"
                ));
            }

            // Get relevant courses for the question
            List<Activity> courses = courseMatchingService.getRelevantCourses(user, request.getQuestion(), 8);

            // Generate AI response - now synchronous to avoid auth issues
            String answer = geminiService.generateResponse(request.getQuestion(), user, courses)
                    .block(Duration.ofSeconds(30));

            if (answer == null) {
                answer = "I'm having trouble generating a response right now. Please try again! 📚";
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "question", request.getQuestion(),
                    "answer", answer,
                    "studentName", user.getName(),
                    "timestamp", java.time.Instant.now()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "timestamp", java.time.Instant.now()
            ));
        }
    }

    /**
     * Get personalized course recommendations
     */
    @GetMapping("/recommend-courses")
    public ResponseEntity<Map<String, Object>> recommendCourses(Authentication auth) {
        try {
            AppUser user = userService.getUserById(auth.getName());

            // Check profile completion
            if (user.getProfile() == null) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "needsProfile", true,
                        "message", "Please complete your profile to get personalized course recommendations!"
                ));
            }

            String query = "recommend courses for " + String.join(" ",
                    user.getProfile().getInterests() != null ? user.getProfile().getInterests() : List.of("learning"));
            List<Activity> courses = courseMatchingService.getRelevantCourses(user, query, 10);

            String recommendations = geminiService.generateResponse("Recommend the best courses for me based on my interests and goals", user, courses)
                    .block(Duration.ofSeconds(30));

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "recommendations", recommendations,
                    "studentName", user.getName(),
                    "basedOnProfile", true,
                    "timestamp", java.time.Instant.now()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Create personalized study plan
     */
    @PostMapping("/create-study-plan")
    public ResponseEntity<Map<String, Object>> createStudyPlan(
            @RequestBody StudyPlanRequest request,
            Authentication auth) {

        try {
            AppUser user = userService.getUserById(auth.getName());

            String planQuery = String.format(
                    "Create a detailed %d-week study plan for %s to learn %s, dedicating %d hours per week. " +
                            "Consider their background and goals for personalized recommendations.",
                    request.getWeeks(), user.getName(), request.getSubject(), request.getHoursPerWeek()
            );

            String studyPlan = geminiService.generateResponse(planQuery, user, List.of())
                    .block(Duration.ofSeconds(30));

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "studyPlan", studyPlan,
                    "subject", request.getSubject(),
                    "weeks", request.getWeeks(),
                    "hoursPerWeek", request.getHoursPerWeek(),
                    "personalized", user.getProfile() != null,
                    "timestamp", java.time.Instant.now()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // DTOs
    @Data
    public static class AskRequest {
        private String question;
    }

    @Data
    public static class StudyPlanRequest {
        private String subject;
        private Integer weeks;
        private Integer hoursPerWeek;
    }
}