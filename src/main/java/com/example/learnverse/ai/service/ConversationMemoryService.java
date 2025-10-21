// src/main/java/com/example/learnverse/ai/service/ConversationMemoryService.java
package com.example.learnverse.ai.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ConversationMemoryService {

    private final ConcurrentHashMap<String, ConversationSession> activeSessions = new ConcurrentHashMap<>();

    @Scheduled(fixedDelay = 300000)
    public void cleanupInactiveSessions() {
        AtomicInteger removed = new AtomicInteger(0);  // Change to AtomicInteger

        activeSessions.entrySet().removeIf(entry -> {
            boolean inactive = Duration.between(
                    entry.getValue().getLastActivity(),
                    LocalDateTime.now()
            ).toMinutes() > 30;

            if (inactive) removed.incrementAndGet();  // Use incrementAndGet()
            return inactive;
        });

        if (removed.get() > 0) {  // Use get() to retrieve value
            log.info("🧹 Cleaned up {} inactive sessions", removed.get());
        }
    }

    public void addMessage(String userId, String role, String content) {
        ConversationSession session = activeSessions.computeIfAbsent(userId, k -> {
            log.info("📝 New conversation session for user: {}", userId);
            return new ConversationSession();
        });

        session.addMessage(new Message(role, content));
        session.setLastActivity(LocalDateTime.now());

        if (session.getMessages().size() > 20) {
            session.getMessages().removeFirst();
        }
    }

    public List<Message> getConversationHistory(String userId) {
        ConversationSession session = activeSessions.get(userId);
        return session == null ? List.of() :
                session.getMessages().stream().collect(Collectors.toList());
    }

    public void clearConversation(String userId) {
        activeSessions.remove(userId);
        log.info("🗑️ Cleared conversation for user: {}", userId);
    }

    @Data
    static class ConversationSession {
        private final Deque<Message> messages = new LinkedList<>();
        private LocalDateTime lastActivity = LocalDateTime.now();

        public void addMessage(Message message) {
            messages.addLast(message);
        }
    }

    @Data
    @AllArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }
}