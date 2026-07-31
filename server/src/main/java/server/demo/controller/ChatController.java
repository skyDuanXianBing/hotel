package server.demo.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.dto.ApiResponse;
import server.demo.dto.ChatMessageRequest;
import server.demo.dto.ChatMessageResponse;
import server.demo.service.ChatService;

import java.util.HashMap;
import java.util.Map;

import server.demo.i18n.ApiMessages;
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private ChatService chatService;

    @PostMapping("/message")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(@Valid @RequestBody ChatMessageRequest request) {
        logger.info("[AI聊天] 收到请求: sessionId={}", request.getSessionId());

        try {
            ChatMessageResponse response = chatService.processMessage(request);
            if ("success".equals(response.getStatus())) {
                return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.786f98d1c4da"), response));
            }
            return ResponseEntity.ok(ApiResponse.error(response.getErrorMessage(), response));
        } catch (Exception e) {
            logger.error("[AI聊天] 处理异常", e);
            ChatMessageResponse errorResponse = ChatMessageResponse.error(ApiMessages.get("api.t.9bc2dbed831a"), request.getSessionId());
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.eda9b06d3994"), errorResponse));
        }
    }

    @GetMapping("/welcome")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> getWelcomeMessage(@RequestParam(required = false) String sessionId) {
        try {
            ChatMessageResponse response = chatService.generateWelcomeMessage(sessionId);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.8352f5301f61"), response));
        } catch (Exception e) {
            logger.error("[AI聊天] 获取欢迎消息异常", e);
            ChatMessageResponse errorResponse = ChatMessageResponse.error(ApiMessages.get("api.t.32a6b321161b"), sessionId);
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.eda9b06d3994"), errorResponse));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkHealth() {
        Map<String, Object> healthInfo = new HashMap<>();

        try {
            boolean isAvailable = chatService.isServiceAvailable();
            String serviceInfo = chatService.getServiceInfo();

            healthInfo.put("status", isAvailable ? "healthy" : "unhealthy");
            healthInfo.put("service", serviceInfo);
            healthInfo.put("timestamp", System.currentTimeMillis());

            if (isAvailable) {
                return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.b59909664023"), healthInfo));
            }
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.b397a2585988"), healthInfo));
        } catch (Exception e) {
            logger.error("[AI聊天] 健康检查异常", e);
            healthInfo.put("status", "error");
            healthInfo.put("error", e.getMessage());
            healthInfo.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.515450304fa0"), healthInfo));
        }
    }

    @GetMapping("/info")
    public ResponseEntity<ApiResponse<Map<String, String>>> getServiceInfo() {
        Map<String, String> info = new HashMap<>();
        info.put("service", ApiMessages.get("api.t.db0bb816a4d3"));
        info.put("version", "1.0.0");
        info.put("provider", "OpenAI GPT (default, fallback DashScope/Mock)");
        info.put("description", ApiMessages.get("api.t.14f57a7b0004"));

        return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.dcd7284e51f0"), info));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleException(Exception e) {
        logger.error("[AI聊天] 控制器异常", e);
        return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.55ed45c78c2a") + e.getMessage(), null));
    }
}
