package com.example.learnverse.ai.service;

import com.example.learnverse.auth.user.AppUser;
import com.example.learnverse.auth.user.UserProfile;
import com.example.learnverse.activity.model.Activity;
import com.example.learnverse.enrollment.model.CourseEnrollment;
import com.example.learnverse.enrollment.service.CourseEnrollmentService;
import com.example.learnverse.learning.model.LearningActivity;
import com.example.learnverse.learning.repository.LearningActivityRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.base-url}")
    private String baseUrl;

    private final WebClient webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
            .build();

    // ** REMOVED THESE DEPENDENCIES FOR NOW - WE'LL ADD THEM GRADUALLY **
    // private final CourseEnrollmentService enrollmentService;
    // private final LearningActivityRepository learningActivityRepository;

    /**
     * Simple AI response for now - we'll enhance it step by step
     */
    public Mono<String> generateResponse(String userQuestion, AppUser user, List<Activity> availableCourses) {
        String prompt = buildBasicPrompt(userQuestion, user, availableCourses);
        return callGeminiAPI(prompt)
                .map(this::extractResponse)
                .onErrorReturn("I'm having trouble right now. Please try again! 📚");
    }

    /**
     * Basic prompt - we'll enhance this later
     */
    private String buildBasicPrompt(String userQuestion, AppUser user, List<Activity> availableCourses) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are LearnVerse AI, a friendly learning assistant for students.\n\n");

        prompt.append("STUDENT: ").append(user.getName()).append("\n");

        // Basic profile info
        if (user.getProfile() != null) {
            UserProfile profile = user.getProfile();
            if (profile.getCareerGoal() != null) {
                prompt.append("CAREER GOAL: ").append(profile.getCareerGoal()).append("\n");
            }
            if (profile.getInterests() != null && !profile.getInterests().isEmpty()) {
                prompt.append("INTERESTS: ").append(String.join(", ", profile.getInterests())).append("\n");
            }
            if (profile.getCurrentFocusArea() != null) {
                prompt.append("CURRENT FOCUS: ").append(profile.getCurrentFocusArea()).append("\n");
            }
        } else {
            // Fallback to basic interests
            if (user.getInterests() != null) {
                prompt.append("INTERESTS: ").append(String.join(", ", user.getInterests())).append("\n");
            }
        }

        // Available courses
        if (!availableCourses.isEmpty()) {
            prompt.append("\nAVAILABLE COURSES:\n");
            for (Activity course : availableCourses.stream().limit(8).collect(Collectors.toList())) {
                prompt.append("• ").append(course.getTitle())
                        .append(" (").append(course.getSubject()).append(", ₹")
                        .append(course.getPricing() != null ? course.getPricing().getPrice() : "Free")
                        .append(")\n");
            }
        }

        prompt.append("\nBe encouraging, recommend specific courses when relevant, and use emojis!\n\n");
        prompt.append("QUESTION: ").append(userQuestion).append("\n\n");
        prompt.append("Response:");

        return prompt.toString();
    }

    private Mono<JsonNode> callGeminiAPI(String prompt) {
        String url = baseUrl + "/models/gemini-2.5-pro:generateContent?key=" + apiKey;

        log.info("🤖 Calling Gemini API");

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "temperature", 0.8,
                        "topK", 64,
                        "topP", 0.95,
                        "maxOutputTokens", 2048
                )
        );

        return webClient.post()
                .uri(url)
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnNext(response -> log.info("✅ AI response generated"))
                .doOnError(error -> log.error("❌ Gemini API error: {}", error.getMessage()));
    }

    private String extractResponse(JsonNode response) {
        try {
            String text = response
                    .path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text")
                    .asText();

            if (text != null && !text.trim().isEmpty()) {
                log.info("✅ Extracted response: {} characters", text.length());
                return text.trim();
            } else {
                log.error("❌ No text found in response");
                return "I couldn't generate a proper response. Please try again! 🤔";
            }

        } catch (Exception e) {
            log.error("❌ Error extracting response: {}", e.getMessage());
            return "I had trouble processing the response. Please try again! 🤔";
        }
    }
}