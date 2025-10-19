package com.example.learnverse.community.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class CommunityWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private WebSocketNotificationService webSocketService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        webSocketService.addSession(session);
        System.out.println("WebSocket connection established: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        webSocketService.removeSession(session);
        System.out.println("WebSocket connection closed: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Handle incoming messages if needed
        System.out.println("Received: " + message.getPayload());
    }
}
