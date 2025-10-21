// src/main/java/com/example/learnverse/ai/service/GeminiService.java
package com.example.learnverse.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String baseUrl;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public GeminiService() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public Flux<String> streamFromGemini(String prompt) {
        String url = baseUrl + "/models/gemini-2.0-flash-exp:streamGenerateContent?alt=sse&key=" + apiKey;

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of(
                        "temperature", 0.7,
                        "topK", 40,
                        "topP", 0.95,
                        "maxOutputTokens", 2048
                )
        );

        log.info("🚀 Starting Gemini streaming");

        return webClient.post()
                .uri(url)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(line -> line.startsWith("data: "))
                .map(line -> line.substring(6).trim())
                .filter(data -> !data.isEmpty() && !data.equals("[DONE]"))
                .mapNotNull(this::extractTextChunk)
                .doOnComplete(() -> log.info("✅ Streaming completed"))
                .doOnError(error -> log.error("❌ Streaming error: {}", error.getMessage()))
                .onErrorResume(error -> Flux.just("I'm having trouble right now. Please try again! 🤔"));
    }

    private String extractTextChunk(String jsonData) {
        try {
            JsonNode root = objectMapper.readTree(jsonData);
            JsonNode textNode = root.path("candidates").path(0)
                    .path("content").path("parts").path(0).path("text");
            return textNode.isMissingNode() ? null : textNode.asText();
        } catch (Exception e) {
            return null;
        }
    }
}