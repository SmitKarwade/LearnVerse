package com.example.learnverse.ai.controller;

import com.example.learnverse.ai.service.StreamingAIService;
import com.example.learnverse.auth.service.UserService;
import com.example.learnverse.auth.user.AppUser;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
@CrossOrigin(
        origins = "*",
        allowedHeaders = "*",
        exposedHeaders = {"Content-Type", "Authorization"},
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS}
)
public class AIAssistantController {

    private final StreamingAIService streamingAIService;
    private final UserService userService;

    @GetMapping("/test-auth")
    public ResponseEntity<Map<String, Object>> testAuth(Authentication auth) {
        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "userId", auth.getName(),
                "message", "✅ Your token is valid!"
        ));
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(
            @RequestBody ChatRequest request,
            Authentication auth) {

        try {
            AppUser user = userService.getUserById(auth.getName());

            if (user.getProfile() == null || !Boolean.TRUE.equals(user.getProfileCompleted())) {
                return Flux.just(ServerSentEvent.<String>builder()
                        .data("Hi! 👋 Please complete your profile first to get personalized assistance!")
                        .build());
            }

            return streamingAIService.streamResponse(request.getMessage(), user);

        } catch (Exception e) {
            return Flux.just(ServerSentEvent.<String>builder()
                    .data("Sorry, I encountered an error: " + e.getMessage())
                    .build());
        }
    }

    @Data
    public static class ChatRequest {
        private String message;
    }
}