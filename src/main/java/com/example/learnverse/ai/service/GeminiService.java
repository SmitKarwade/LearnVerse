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
        String url = baseUrl + "/models/gemini-2.0-flash:streamGenerateContent?alt=sse&key=" + apiKey;

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

        log.info("🚀 Calling Gemini API");

        return webClient.post()
                .uri(url)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnNext(line -> log.info("📨 Raw: {}", line.substring(0, Math.min(100, line.length()))))
                // ✅ HANDLE BOTH FORMATS: "data: {...}" AND pure "{...}"
                .map(line -> {
                    if (line.startsWith("data: ")) {
                        return line.substring(6).trim();
                    }
                    return line.trim();
                })
                .filter(data -> !data.isEmpty() && !data.equals("[DONE]"))
                .doOnNext(data -> log.info("📦 Processing: {}", data.substring(0, Math.min(100, data.length()))))
                .mapNotNull(this::extractTextChunk)
                .doOnNext(chunk -> log.info("📤 Sending chunk: {} chars", chunk.length()))
                .doOnComplete(() -> log.info("✅ Streaming completed"))
                .doOnError(error -> log.error("❌ Streaming error: {}", error.getMessage(), error))
                .onErrorResume(error -> Flux.just("I'm having trouble right now. Please try again! 🤔"));
    }

    private String extractTextChunk(String jsonData) {
        try {
            JsonNode root = objectMapper.readTree(jsonData);
            JsonNode textNode = root.path("candidates").path(0)
                    .path("content").path("parts").path(0).path("text");

            if (textNode.isMissingNode()) {
                log.warn("⚠️ No text in JSON: {}", jsonData.substring(0, Math.min(200, jsonData.length())));
                return null;
            }

            String text = textNode.asText();

            if (text != null && !text.isEmpty()) {
                log.info("✅ Extracted {} chars", text.length());
                return text;
            }

            return null;
        } catch (Exception e) {
            log.error("❌ Parse error: {}", e.getMessage());
            log.error("Data: {}", jsonData);
            return null;
        }
    }
}