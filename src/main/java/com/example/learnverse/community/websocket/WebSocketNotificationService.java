package com.example.learnverse.community.websocket;


import com.example.learnverse.community.model.Post;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebSocketNotificationService {

    private final Set<WebSocketSession> activeSessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void addSession(WebSocketSession session) {
        activeSessions.add(session);
    }

    public void removeSession(WebSocketSession session) {
        activeSessions.remove(session);
    }

    public void broadcastNewPost(Post post) {
        Map<String, Object> message = Map.of(
                "type", "NEW_POST",
                "data", post
        );
        broadcast(message);
    }

    public void broadcastNewComment(String postId, Post.Comment comment) {
        Map<String, Object> message = Map.of(
                "type", "NEW_COMMENT",
                "postId", postId,
                "data", comment
        );
        broadcast(message);
    }

    public void broadcastPostLike(String postId, String userId) {
        Map<String, Object> message = Map.of(
                "type", "POST_LIKE",
                "postId", postId,
                "userId", userId
        );
        broadcast(message);
    }

    public void broadcastPostShare(String postId, String userId) {
        Map<String, Object> message = Map.of(
                "type", "POST_SHARE",
                "postId", postId,
                "userId", userId
        );
        broadcast(message);
    }

    private void broadcast(Object message) {
        activeSessions.removeIf(session -> !session.isOpen());

        activeSessions.forEach(session -> {
            try {
                String jsonMessage = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(jsonMessage));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}