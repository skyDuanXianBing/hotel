package server.demo.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;
import server.demo.repository.StoreUserRepository;
import server.demo.service.SuMessagingRealtimeGateway;
import server.demo.util.JwtUtil;

@Component
public class SuMessagingWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(SuMessagingWebSocketHandler.class);
    private static final String ATTR_STORE_ID = "storeId";

    private final JwtUtil jwtUtil;
    private final StoreUserRepository storeUserRepository;
    private final SuMessagingRealtimeGateway realtimeGateway;

    public SuMessagingWebSocketHandler(
            JwtUtil jwtUtil,
            StoreUserRepository storeUserRepository,
            SuMessagingRealtimeGateway realtimeGateway
    ) {
        this.jwtUtil = jwtUtil;
        this.storeUserRepository = storeUserRepository;
        this.realtimeGateway = realtimeGateway;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (session.getUri() == null) {
            session.close(CloseStatus.BAD_DATA.withReason("Missing URI"));
            return;
        }

        var params = UriComponentsBuilder.fromUri(session.getUri()).build().getQueryParams();
        String token = params.getFirst("token");
        String storeIdRaw = params.getFirst("storeId");

        if (token == null || token.isBlank() || storeIdRaw == null || storeIdRaw.isBlank()) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Missing auth params"));
            return;
        }

        Long storeId;
        try {
            storeId = Long.parseLong(storeIdRaw.trim());
        } catch (NumberFormatException e) {
            session.close(CloseStatus.BAD_DATA.withReason("Invalid storeId"));
            return;
        }

        if (!jwtUtil.validateToken(token)) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid token"));
            return;
        }

        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null || !storeUserRepository.existsByStoreIdAndUserIdAndIsActiveTrue(storeId, userId)) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Store access denied"));
            return;
        }

        session.getAttributes().put(ATTR_STORE_ID, storeId);
        realtimeGateway.register(storeId, session);
        logger.debug("[SuMessagingRealtime] connected. sessionId={}, storeId={}, userId={}", session.getId(), storeId, userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // server-push only
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        unregister(session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        unregister(session);
    }

    private void unregister(WebSocketSession session) {
        Object storeIdAttr = session.getAttributes().get(ATTR_STORE_ID);
        if (storeIdAttr instanceof Long storeId) {
            realtimeGateway.unregister(storeId, session);
        }
    }
}
