package server.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import server.demo.dto.SuMessagingMessageDTO;
import server.demo.dto.SuMessagingRealtimeEvent;
import server.demo.enums.SuMessagingSenderType;
import server.demo.i18n.ApiMessages;
import server.demo.service.push.PushDispatchService;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SuMessagingRealtimeGateway {

    private static final Logger logger = LoggerFactory.getLogger(SuMessagingRealtimeGateway.class);
    private static final String EVENT_MESSAGE_CREATED = "MESSAGE_CREATED";
    private static final String EVENT_MESSAGE_UPDATED = "MESSAGE_UPDATED";
    private static final String EVENT_WORKBENCH_INVALIDATED = "WORKBENCH_INVALIDATED";
    private static final String IMAGE_SUMMARY_KEY = "api.t.4f622a639dae";
    private static final String NEW_MESSAGE_TITLE_KEY = "api.t.87a3e5790430";
    private static final int PUSH_BODY_MAX_LENGTH = 120;

    private final ObjectMapper objectMapper;
    private final PushDispatchService pushDispatchService;
    private final Map<Long, Set<WebSocketSession>> storeSessions = new ConcurrentHashMap<>();

    public SuMessagingRealtimeGateway(ObjectMapper objectMapper, PushDispatchService pushDispatchService) {
        this.objectMapper = objectMapper;
        this.pushDispatchService = pushDispatchService;
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
        dispatchGuestMessagePush(storeId, threadId, message);
    }

    /**
     * 客人新消息 → 手机推送（App 未打开也能收到系统弹窗）。员工消息不推。
     */
    private void dispatchGuestMessagePush(Long storeId, Long threadId, SuMessagingMessageDTO message) {
        if (message.getSenderType() != SuMessagingSenderType.GUEST) {
            return;
        }
        try {
            String senderName = message.getSenderName() != null ? message.getSenderName().trim() : "";
            String title = senderName.isEmpty() ? ApiMessages.get(NEW_MESSAGE_TITLE_KEY) : senderName;
            String content = message.getContent() != null ? message.getContent().trim() : "";
            if (content.isEmpty()) {
                content = ApiMessages.get(IMAGE_SUMMARY_KEY);
            }
            if (content.length() > PUSH_BODY_MAX_LENGTH) {
                content = content.substring(0, PUSH_BODY_MAX_LENGTH) + "...";
            }
            pushDispatchService.dispatchToStoreUsers(
                    storeId,
                    PushDispatchService.PushCategory.CHAT,
                    title,
                    content,
                    Map.of("type", "chat", "threadId", String.valueOf(threadId))
            );
        } catch (Exception e) {
            logger.warn("[SuMessagingRealtime] guest message push dispatch failed. storeId={}, threadId={}, err={}",
                    storeId, threadId, e.getMessage());
        }
    }

    public void broadcastMessageUpdated(Long storeId, Long threadId, SuMessagingMessageDTO message) {
        if (storeId == null || threadId == null || message == null) {
            return;
        }

        SuMessagingRealtimeEvent event = new SuMessagingRealtimeEvent(EVENT_MESSAGE_UPDATED, threadId, message);
        broadcast(storeId, event);
    }

    public void broadcastWorkbenchInvalidated(Long storeId, String resourceType) {
        if (storeId == null || resourceType == null || resourceType.isBlank()) {
            return;
        }
        broadcast(storeId, new SuMessagingRealtimeEvent(EVENT_WORKBENCH_INVALIDATED, resourceType));
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
