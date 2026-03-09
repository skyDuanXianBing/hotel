package server.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import server.demo.dto.SuMessagingMessageDTO;
import server.demo.dto.SuMessagingRealtimeEvent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SuMessagingRealtimeGateway {

    private static final Logger logger = LoggerFactory.getLogger(SuMessagingRealtimeGateway.class);
    private static final String EVENT_MESSAGE_CREATED = "MESSAGE_CREATED";
    private static final String EVENT_MESSAGE_UPDATED = "MESSAGE_UPDATED";

    private final ObjectMapper objectMapper;
    private final Map<Long, Set<WebSocketSession>> storeSessions = new ConcurrentHashMap<>();

    public SuMessagingRealtimeGateway(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void register(Long storeId, WebSocketSession session) {
        storeSessions.computeIfAbsent(storeId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregister(Long storeId, WebSocketSession session) {
        if (storeId == null) {
            return;
        }
        Set<WebSocketSession> sessions = storeSessions.get(storeId);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            storeSessions.remove(storeId);
        }
    }

    public void broadcastMessageCreated(Long storeId, Long threadId, SuMessagingMessageDTO message) {
        if (storeId == null || threadId == null || message == null) {
            return;
        }

        SuMessagingRealtimeEvent event = new SuMessagingRealtimeEvent(EVENT_MESSAGE_CREATED, threadId, message);
        broadcast(storeId, event);
    }

    public void broadcastMessageUpdated(Long storeId, Long threadId, SuMessagingMessageDTO message) {
        if (storeId == null || threadId == null || message == null) {
            return;
        }

        SuMessagingRealtimeEvent event = new SuMessagingRealtimeEvent(EVENT_MESSAGE_UPDATED, threadId, message);
        broadcast(storeId, event);
    }

    private void broadcast(Long storeId, SuMessagingRealtimeEvent event) {
        Set<WebSocketSession> sessions = storeSessions.get(storeId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            logger.error("[SuMessagingRealtime] serialize event failed. storeId={}, err={}", storeId, e.getMessage(), e);
            return;
        }

        TextMessage textMessage = new TextMessage(payload);
        for (WebSocketSession session : sessions) {
            if (session == null || !session.isOpen()) {
                unregister(storeId, session);
                continue;
            }
            try {
                synchronized (session) {
                    session.sendMessage(textMessage);
                }
            } catch (Exception e) {
                logger.warn("[SuMessagingRealtime] send failed. storeId={}, sessionId={}, err={}",
                        storeId, session.getId(), e.getMessage());
                unregister(storeId, session);
            }
        }
    }
}
