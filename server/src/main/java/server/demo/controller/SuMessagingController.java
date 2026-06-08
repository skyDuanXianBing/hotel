package server.demo.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.RequirePermission;
import server.demo.annotation.StoreScoped;
import server.demo.context.StoreContextHolder;
import server.demo.dto.ApiResponse;
import server.demo.dto.SuMessagingAiSettingDTO;
import server.demo.dto.SuMessagingMessageDTO;
import server.demo.dto.SuMessagingMessagePageResponse;
import server.demo.dto.SuMessagingSendRequest;
import server.demo.dto.SuMessagingThreadDTO;
import server.demo.dto.SuMessagingThreadPageResponse;
import server.demo.dto.SuMessagingUnreadSummaryDTO;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.service.RegistrationLinkInboxService;
import server.demo.service.SuMessagingAiSettingService;
import server.demo.service.SuMessagingService;

import java.util.List;

/**
 * PMS 收件箱（Su Messaging）接口。
 * <p>
 * 这里只负责消息展示和人工回复，OTA webhook 入库由 {@link SuMessagingWebhookController} 处理。
 */
@RestController
@RequestMapping("/api/v1/su-messaging")
public class SuMessagingController {

    private final SuMessagingService suMessagingService;
    private final SuMessagingAiSettingService suMessagingAiSettingService;
    private final RegistrationLinkInboxService registrationLinkInboxService;

    public SuMessagingController(
            SuMessagingService suMessagingService,
            SuMessagingAiSettingService suMessagingAiSettingService,
            RegistrationLinkInboxService registrationLinkInboxService
    ) {
        this.suMessagingService = suMessagingService;
        this.suMessagingAiSettingService = suMessagingAiSettingService;
        this.registrationLinkInboxService = registrationLinkInboxService;
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

    @GetMapping("/threads/page")
    @StoreScoped
    public ResponseEntity<ApiResponse<SuMessagingThreadPageResponse>> listThreadPage(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String orderKind,
            @RequestParam(required = false) String reservationStatus,
            @RequestParam(required = false) Boolean unread,
            @RequestParam(required = false) Boolean closed,
            @RequestParam(required = false) String search
    ) {
        try {
            Long storeId = StoreContextHolder.getContext().getStoreId();
            SuMessagingThreadPageResponse response = suMessagingService.listThreadPage(
                    storeId,
                    page,
                    size,
                    channel,
                    orderKind,
                    reservationStatus,
                    unread,
                    closed,
                    search
            );
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("获取会话分页失败: " + e.getMessage()));
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

    @GetMapping("/threads/{threadId}/messages/page")
    @StoreScoped
    public ResponseEntity<ApiResponse<SuMessagingMessagePageResponse>> getMessagePage(
            @PathVariable Long threadId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Long beforeMessageId,
            @RequestParam(required = false) Long afterMessageId,
            @RequestParam(required = false) Boolean markRead
    ) {
        try {
            Long storeId = StoreContextHolder.getContext().getStoreId();
            SuMessagingMessagePageResponse response = suMessagingService.getThreadMessagePage(
                    storeId,
                    threadId,
                    limit,
                    beforeMessageId,
                    afterMessageId,
                    markRead
            );
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("获取消息分页失败: " + e.getMessage()));
        }
    }

    @GetMapping("/unread-summary")
    @StoreScoped
    public ResponseEntity<ApiResponse<SuMessagingUnreadSummaryDTO>> getUnreadSummary() {
        try {
            Long storeId = StoreContextHolder.getContext().getStoreId();
            return ResponseEntity.ok(ApiResponse.success(suMessagingService.getUnreadSummary(storeId)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("获取未读摘要失败: " + e.getMessage()));
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

    @PostMapping("/threads/{threadId}/read")
    @StoreScoped
    public ResponseEntity<ApiResponse<Void>> markThreadAsRead(@PathVariable Long threadId) {
        try {
            Long storeId = StoreContextHolder.getContext().getStoreId();
            suMessagingService.markThreadAsRead(storeId, threadId);
            return ResponseEntity.ok(ApiResponse.success("标记消息已读成功", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("标记消息已读失败: " + e.getMessage()));
        }
    }

    @PostMapping("/link-inbox/backfill")
    @StoreScoped
    @RequirePermission(module = PermissionModule.ORDER, action = PermissionAction.MODIFY_ORDER)
    public ResponseEntity<ApiResponse<RegistrationLinkInboxService.BackfillResult>> backfillLinkInbox() {
        try {
            Long storeId = StoreContextHolder.getContext().getStoreId();
            RegistrationLinkInboxService.BackfillResult result = registrationLinkInboxService.backfillMissingForStore(storeId);
            return ResponseEntity.ok(ApiResponse.success("回填登记链接收件箱成功", result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("回填登记链接收件箱失败: " + e.getMessage()));
        }
    }

    @GetMapping("/ai-settings")
    @StoreScoped
    public ResponseEntity<ApiResponse<SuMessagingAiSettingDTO>> getAiSettings() {
        try {
            Long storeId = StoreContextHolder.getContext().getStoreId();
            return ResponseEntity.ok(ApiResponse.success(suMessagingAiSettingService.getOrCreate(storeId)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("获取 AI 自动回复设置失败: " + e.getMessage()));
        }
    }

    @PutMapping("/ai-settings")
    @StoreScoped
    public ResponseEntity<ApiResponse<SuMessagingAiSettingDTO>> updateAiSettings(
            @RequestBody SuMessagingAiSettingDTO request
    ) {
        try {
            Long storeId = StoreContextHolder.getContext().getStoreId();
            return ResponseEntity.ok(ApiResponse.success(suMessagingAiSettingService.update(storeId, request)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("更新 AI 自动回复设置失败: " + e.getMessage()));
        }
    }
}
