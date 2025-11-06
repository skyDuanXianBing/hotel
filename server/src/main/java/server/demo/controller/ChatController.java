package server.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import server.demo.dto.ApiResponse;
import server.demo.dto.ChatMessageRequest;
import server.demo.dto.ChatMessageResponse;
import server.demo.service.ChatService;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * AI聊天控制器
 * 提供AI自动回复的REST API接口
 * 
 * @author AI Assistant
 */
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

    /**
     * 处理OPTIONS预检请求
     */
    @RequestMapping(value = "/message", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> handleOptions() {
        return ResponseEntity.ok()
                .header("Access-Control-Allow-Origin", "http://localhost:8091")
                .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
                .header("Access-Control-Allow-Headers", "Content-Type, Authorization")
                .header("Access-Control-Max-Age", "3600")
                .build();
    }

    /**
     * 发送消息并获取AI回复
     * 
     * @param request 聊天消息请求
     * @return AI回复响应
     */
    @PostMapping("/message")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
            @Valid @RequestBody ChatMessageRequest request) {
        
        logger.warn("【AI聊天】收到AI聊天请求 - 消息: '{}', 会话ID: {}", request.getMessage(), request.getSessionId());
        logger.warn("【AI聊天】警告：如果这是自动触发的请求，请检查调用来源！");
        
        // 添加调用栈信息以便追踪来源
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        logger.info("【AI聊天】调用栈前5层:");
        for (int i = 0; i < Math.min(5, stackTrace.length); i++) {
            logger.info("  [{}] {}.{}({}:{})", i, stackTrace[i].getClassName(), 
                       stackTrace[i].getMethodName(), stackTrace[i].getFileName(), stackTrace[i].getLineNumber());
        }
        
        try {
            ChatMessageResponse response = chatService.processMessage(request);
            
            if ("success".equals(response.getStatus())) {
                logger.info("【AI聊天】AI回复生成成功: sessionId={}, 回复长度: {}", 
                           response.getSessionId(), response.getReply().length());
                return ResponseEntity.ok(ApiResponse.success("消息发送成功", response));
            } else {
                logger.warn("【AI聊天】AI回复生成失败: sessionId={}, error={}", 
                    response.getSessionId(), response.getErrorMessage());
                return ResponseEntity.ok(ApiResponse.error(response.getErrorMessage(), response));
            }
            
        } catch (Exception e) {
            logger.error("【AI聊天】AI聊天处理异常", e);
            ChatMessageResponse errorResponse = ChatMessageResponse.error(
                "系统繁忙，请稍后再试", request.getSessionId()
            );
            return ResponseEntity.ok(ApiResponse.error("系统错误", errorResponse));
        }
    }

    /**
     * 获取欢迎消息
     * 
     * @param sessionId 会话ID（可选）
     * @return 欢迎消息
     */
    @GetMapping("/welcome")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> getWelcomeMessage(
            @RequestParam(required = false) String sessionId) {
        
        logger.info("获取欢迎消息请求: sessionId={}", sessionId);
        
        try {
            ChatMessageResponse response = chatService.generateWelcomeMessage(sessionId);
            return ResponseEntity.ok(ApiResponse.success("欢迎消息获取成功", response));
            
        } catch (Exception e) {
            logger.error("获取欢迎消息异常", e);
            ChatMessageResponse errorResponse = ChatMessageResponse.error(
                "无法获取欢迎消息", sessionId
            );
            return ResponseEntity.ok(ApiResponse.error("系统错误", errorResponse));
        }
    }

    /**
     * 检查聊天服务健康状态
     * 
     * @return 服务状态信息
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkHealth() {
        logger.info("检查聊天服务健康状态");
        
        Map<String, Object> healthInfo = new HashMap<>();
        
        try {
            boolean isAvailable = chatService.isServiceAvailable();
            String serviceInfo = chatService.getServiceInfo();
            
            healthInfo.put("status", isAvailable ? "healthy" : "unhealthy");
            healthInfo.put("service", serviceInfo);
            healthInfo.put("timestamp", System.currentTimeMillis());
            
            if (isAvailable) {
                return ResponseEntity.ok(ApiResponse.success("服务运行正常", healthInfo));
            } else {
                return ResponseEntity.ok(ApiResponse.error("服务暂时不可用", healthInfo));
            }
            
        } catch (Exception e) {
            logger.error("健康检查异常", e);
            healthInfo.put("status", "error");
            healthInfo.put("error", e.getMessage());
            healthInfo.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(ApiResponse.error("健康检查失败", healthInfo));
        }
    }

    /**
     * 获取聊天服务信息
     * 
     * @return 服务信息
     */
    @GetMapping("/info")
    public ResponseEntity<ApiResponse<Map<String, String>>> getServiceInfo() {
        logger.info("获取聊天服务信息");
        
        Map<String, String> info = new HashMap<>();
        info.put("service", "AI聊天客服");
        info.put("version", "1.0.0");
        info.put("provider", "LangChain4j + DashScope");
        info.put("description", "基于千问模型的智能客服聊天服务");
        
        return ResponseEntity.ok(ApiResponse.success("服务信息获取成功", info));
    }

    /**
     * 全局异常处理
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleException(Exception e) {
        logger.error("聊天控制器异常", e);
        return ResponseEntity.ok(ApiResponse.error("系统错误: " + e.getMessage(), null));
    }
}