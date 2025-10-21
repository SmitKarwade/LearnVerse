package com.example.learnverse.ai.controller;

import com.example.learnverse.ai.service.StreamingAIService;
import com.example.learnverse.auth.service.UserService;
import com.example.learnverse.auth.user.AppUser;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AIAssistantController {

    private final StreamingAIService streamingAIService;
    private final UserService userService;

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
                    .data("Sorry, I encountered an error. Please try again!")
                    .build());
        }
    }

    @Data
    public static class ChatRequest {
        private String message;
    }
}