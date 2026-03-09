package server.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import server.demo.websocket.SuMessagingWebSocketHandler;

@Configuration
@EnableWebSocket
public class SuMessagingWebSocketConfig implements WebSocketConfigurer {

    private final SuMessagingWebSocketHandler handler;

    public SuMessagingWebSocketConfig(SuMessagingWebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/api/v1/ws/su-messaging")
                .setAllowedOriginPatterns("*");
    }
}
