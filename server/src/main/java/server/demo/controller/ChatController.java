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

@RestController
@RequestMapping("/api/v1/chat")
@CrossOrigin(origins = {"http://localhost:8091", "http://127.0.0.1:8091"},
        allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS},
        allowCredentials = "true")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private ChatService chatService;

    @RequestMapping(value = "/message", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> handleOptions() {
        return ResponseEntity.ok()
                .header("Access-Control-Allow-Origin", "http://localhost:8091")
                .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
                .header("Access-Control-Allow-Headers", "Content-Type, Authorization")
                .header("Access-Control-Max-Age", "3600")
                .build();
    }

    @PostMapping("/message")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(@Valid @RequestBody ChatMessageRequest request) {
        logger.info("[AI聊天] 收到请求: sessionId={}", request.getSessionId());

        try {
            ChatMessageResponse response = chatService.processMessage(request);
            if ("success".equals(response.getStatus())) {
                return ResponseEntity.ok(ApiResponse.success("消息发送成功", response));
            }
            return ResponseEntity.ok(ApiResponse.error(response.getErrorMessage(), response));
        } catch (Exception e) {
            logger.error("[AI聊天] 处理异常", e);
            ChatMessageResponse errorResponse = ChatMessageResponse.error("系统繁忙，请稍后再试", request.getSessionId());
            return ResponseEntity.ok(ApiResponse.error("系统错误", errorResponse));
        }
    }

    @GetMapping("/welcome")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> getWelcomeMessage(@RequestParam(required = false) String sessionId) {
        try {
            ChatMessageResponse response = chatService.generateWelcomeMessage(sessionId);
            return ResponseEntity.ok(ApiResponse.success("欢迎消息获取成功", response));
        } catch (Exception e) {
            logger.error("[AI聊天] 获取欢迎消息异常", e);
            ChatMessageResponse errorResponse = ChatMessageResponse.error("无法获取欢迎消息", sessionId);
            return ResponseEntity.ok(ApiResponse.error("系统错误", errorResponse));
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
                return ResponseEntity.ok(ApiResponse.success("服务运行正常", healthInfo));
            }
            return ResponseEntity.ok(ApiResponse.error("服务暂时不可用", healthInfo));
        } catch (Exception e) {
            logger.error("[AI聊天] 健康检查异常", e);
            healthInfo.put("status", "error");
            healthInfo.put("error", e.getMessage());
            healthInfo.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(ApiResponse.error("健康检查失败", healthInfo));
        }
    }

    @GetMapping("/info")
    public ResponseEntity<ApiResponse<Map<String, String>>> getServiceInfo() {
        Map<String, String> info = new HashMap<>();
        info.put("service", "AI聊天客服");
        info.put("version", "1.0.0");
        info.put("provider", "OpenAI GPT (default, fallback DashScope/Mock)");
        info.put("description", "默认使用 OpenAI GPT 的智能客服聊天服务（兼容 DashScope/Mock 回退）");

        return ResponseEntity.ok(ApiResponse.success("服务信息获取成功", info));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleException(Exception e) {
        logger.error("[AI聊天] 控制器异常", e);
        return ResponseEntity.ok(ApiResponse.error("系统错误: " + e.getMessage(), null));
    }
}
