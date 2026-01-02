package server.demo.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.StoreScoped;
import server.demo.context.StoreContextHolder;
import server.demo.dto.ApiResponse;
import server.demo.dto.SuMessagingMessageDTO;
import server.demo.dto.SuMessagingSendRequest;
import server.demo.dto.SuMessagingThreadDTO;
import server.demo.service.SuMessagingService;

import java.util.List;

/**
 * PMS 收件箱（Su Messaging）接口。
 * <p>
 * 说明：仅负责展示/回复；接收 OTA 推送由 SuMessagingWebhookController 处理。
 */
@RestController
@RequestMapping("/api/v1/su-messaging")
public class SuMessagingController {

    private final SuMessagingService suMessagingService;

    public SuMessagingController(SuMessagingService suMessagingService) {
        this.suMessagingService = suMessagingService;
    }

    @GetMapping("/threads")
    @StoreScoped
    public ResponseEntity<ApiResponse<List<SuMessagingThreadDTO>>> listThreads() {
        try {
            Long storeId = StoreContextHolder.getContext().getStoreId();
            return ResponseEntity.ok(ApiResponse.success(suMessagingService.listThreads(storeId)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("获取会话列表失败: " + e.getMessage()));
        }
    }

    @GetMapping("/threads/{threadId}/messages")
    @StoreScoped
    public ResponseEntity<ApiResponse<List<SuMessagingMessageDTO>>> getMessages(@PathVariable Long threadId) {
        try {
            Long storeId = StoreContextHolder.getContext().getStoreId();
            return ResponseEntity.ok(ApiResponse.success(suMessagingService.getThreadMessages(storeId, threadId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("获取消息失败: " + e.getMessage()));
        }
    }

    @GetMapping("/threads/{threadId}/poll")
    @StoreScoped
    public ResponseEntity<ApiResponse<List<SuMessagingMessageDTO>>> pollMessages(
            @PathVariable Long threadId,
            @RequestParam(required = false) String since
    ) {
        try {
            Long storeId = StoreContextHolder.getContext().getStoreId();
            return ResponseEntity.ok(ApiResponse.success(suMessagingService.pollThreadMessages(storeId, threadId, since)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("轮询消息失败: " + e.getMessage()));
        }
    }

    @PostMapping("/threads/{threadId}/send")
    @StoreScoped
    public ResponseEntity<ApiResponse<SuMessagingMessageDTO>> sendMessage(
            @PathVariable Long threadId,
            @Valid @RequestBody SuMessagingSendRequest request
    ) {
        try {
            Long storeId = StoreContextHolder.getContext().getStoreId();
            SuMessagingMessageDTO dto = suMessagingService.sendMessage(storeId, threadId, request);
            return ResponseEntity.ok(ApiResponse.success(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("发送消息失败: " + e.getMessage()));
        }
    }
}

