// src/main/java/com/example/learnverse/ai/service/StreamingAIService.java
package com.example.learnverse.ai.service;

import com.example.learnverse.activity.model.Activity;
import com.example.learnverse.auth.user.AppUser;
import com.example.learnverse.auth.user.UserProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreamingAIService {

    private final ConversationMemoryService conversationMemory;
    private final CourseMatchingService courseMatchingService;
    private final GeminiService geminiService;

    private final ConcurrentHashMap<String, StringBuilder> responseBuffers = new ConcurrentHashMap<>();

    public Flux<ServerSentEvent<String>> streamResponse(String userMessage, AppUser user) {
        String userId = user.getId();

        conversationMemory.addMessage(userId, "user", userMessage);

        List<ConversationMemoryService.Message> history = conversationMemory.getConversationHistory(userId);

        List<Activity> relevantCourses = courseMatchingService.getRelevantCourses(user, userMessage, 6);

        String prompt = buildContextualPrompt(userMessage, user, history, relevantCourses);

        responseBuffers.put(userId, new StringBuilder());

        log.info("🤖 Streaming for user: {}", user.getName());

        return geminiService.streamFromGemini(prompt)
                .map(chunk -> {
                    responseBuffers.get(userId).append(chunk);
                    return ServerSentEvent.<String>builder().data(chunk).build();
                })
                .doOnComplete(() -> {
                    String completeResponse = responseBuffers.get(userId).toString();
                    conversationMemory.addMessage(userId, "assistant", completeResponse);
                    responseBuffers.remove(userId);
                    log.info("✅ Completed for user: {}", user.getName());
                })
                .doOnError(error -> {
                    log.error("❌ Error: {}", error.getMessage());
                    responseBuffers.remove(userId);
                });
    }

    private String buildContextualPrompt(String userMessage, AppUser user,
                                         List<ConversationMemoryService.Message> history,
                                         List<Activity> courses) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are LearnVerse AI, a natural conversational learning assistant.\n\n");

        prompt.append("You are LearnVerse AI, a natural conversational learning assistant.\n\n");

        prompt.append("⚠️ CRITICAL FORMATTING RULES - FOLLOW EXACTLY:\n\n");
        prompt.append("1. Headings: **Heading Text:**\n");
        prompt.append("   - Must be on their OWN line\n");
        prompt.append("   - Add blank line AFTER heading\n");
        prompt.append("   - Example: **Programming Language:**\n\n");
        prompt.append("2. Bullet Points: * Item text\n");
        prompt.append("   - Start with * and space\n");
        prompt.append("   - Each bullet on NEW line\n");
        prompt.append("   - Example: * Learn Kotlin basics\n\n");
        prompt.append("3. Bold Text: Only use **word** for emphasis\n");
        prompt.append("   - NOT for entire lines\n");
        prompt.append("   - Example: Practice with **simple projects**\n\n");
        prompt.append("4. Structure:\n");
        prompt.append("   **Section Name:**\n\n");
        prompt.append("   * First point here\n");
        prompt.append("   * Second point here\n\n");
        prompt.append("   **Next Section:**\n\n");
        prompt.append("   * Another point\n\n");




        prompt.append("STUDENT PROFILE:\n");
        UserProfile profile = user.getProfile();

        if (profile != null) {
            if (profile.getCareerGoal() != null && !profile.getCareerGoal().isEmpty()) {
                prompt.append("• Career Goal: ").append(profile.getCareerGoal()).append("\n");
            }
            if (profile.getCurrentFocusArea() != null) {
                prompt.append("• Current Focus: ").append(profile.getCurrentFocusArea()).append("\n");
            }
            if (profile.getInterests() != null && !profile.getInterests().isEmpty()) {
                prompt.append("• Interests: ").append(String.join(", ", profile.getInterests())).append("\n");
            }
            if (profile.getTargetSkills() != null && !profile.getTargetSkills().isEmpty()) {
                prompt.append("• Target Skills: ").append(String.join(", ", profile.getTargetSkills())).append("\n");
            }
        }
        prompt.append("\n");

        if (courses != null && !courses.isEmpty()) {
            prompt.append("AVAILABLE COURSES:\n");
            for (Activity course : courses) {
                prompt.append("• ").append(course.getTitle());
                if (course.getPricing() != null && course.getPricing().getPrice() != null) {
                    prompt.append(" - ₹").append(course.getPricing().getPrice());
                }
                prompt.append("\n");
            }
            prompt.append("\n");
        }

        if (!history.isEmpty()) {
            prompt.append("RECENT CONVERSATION:\n");
            int startIdx = Math.max(0, history.size() - 6); // Only last 6 messages
            for (int i = startIdx; i < history.size(); i++) {
                ConversationMemoryService.Message msg = history.get(i);
                prompt.append(msg.getRole().equalsIgnoreCase("user") ? "USER" : "AI");
                prompt.append(": ").append(msg.getContent()).append("\n");
            }
            prompt.append("\n");
        }

        prompt.append("USER QUESTION: ").append(userMessage).append("\n\n");
        prompt.append("RESPONSE (use markdown formatting):\n");

        return prompt.toString();
    }
}