package server.demo.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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

import server.demo.i18n.ApiMessages;
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
            return ResponseEntity.status(500).body(ApiResponse.error(ApiMessages.get("api.t.70301e9ce242") + e.getMessage()));
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
            @RequestParam(required = false) String orderStatuses,
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
                    orderStatuses,
                    unread,
                    closed,
                    search
            );
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(ApiMessages.get("api.t.f52835933dc4") + e.getMessage()));
        }
    }

    @GetMapping("/threads/{threadId}")
    @StoreScoped
    public ResponseEntity<ApiResponse<SuMessagingThreadDTO>> getThread(@PathVariable Long threadId) {
        try {
            Long storeId = StoreContextHolder.getContext().getStoreId();
            return ResponseEntity.ok(ApiResponse.success(suMessagingService.getThread(storeId, threadId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(ApiMessages.get("api.t.e37923d0e4e9") + e.getMessage()));
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
            return ResponseEntity.status(500).body(ApiResponse.error(ApiMessages.get("api.t.e3068035d0f0") + e.getMessage()));
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
            return ResponseEntity.status(500).body(ApiResponse.error(ApiMessages.get("api.t.60d1176545dd") + e.getMessage()));
        }
    }

    @GetMapping("/unread-summary")
    @StoreScoped
    public ResponseEntity<ApiResponse<SuMessagingUnreadSummaryDTO>> getUnreadSummary() {
        try {
            Long storeId = StoreContextHolder.getContext().getStoreId();
            return ResponseEntity.ok(ApiResponse.success(suMessagingService.getUnreadSummary(storeId)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(ApiMessages.get("api.t.8bbc1cefdc47") + e.getMessage()));
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
            return ResponseEntity.status(500).body(ApiResponse.error(ApiMessages.get("api.t.1d3a66d26b52") + e.getMessage()));
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
            return ResponseEntity.status(500).body(ApiResponse.error(ApiMessages.get("api.t.5c00823f5372") + e.getMessage()));
        }
    }

    @PostMapping(value = "/threads/{threadId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @StoreScoped
    public ResponseEntity<ApiResponse<SuMessagingMessageDTO>> sendAttachment(
            @PathVariable Long threadId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String senderName
    ) {
        try {
            Long storeId = StoreContextHolder.getContext().getStoreId();
            SuMessagingMessageDTO dto = suMessagingService.sendAttachment(storeId, threadId, file, senderName);
            return ResponseEntity.ok(ApiResponse.success(dto));
        } catch (SuMessagingService.BookingChannelHotelMappingConflictException e) {
            return ResponseEntity.status(409).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(ApiMessages.get("api.t.9d8382247607") + e.getMessage()));
        }
    }

    @GetMapping("/threads/{threadId}/messages/{messageId}/attachments/{attachmentId}")
    @StoreScoped
    public ResponseEntity<byte[]> downloadAttachment(
            @PathVariable Long threadId,
            @PathVariable Long messageId,
            @PathVariable Long attachmentId
    ) {
        try {
            Long storeId = StoreContextHolder.getContext().getStoreId();
            SuMessagingService.AttachmentContent content = suMessagingService.downloadAttachment(
                    storeId,
                    threadId,
                    messageId,
                    attachmentId
            );
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .contentType(MediaType.parseMediaType(content.mimeType()))
                    .body(content.bytes());
        } catch (SuMessagingService.BookingChannelHotelMappingConflictException e) {
            return ResponseEntity.status(409).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(502).build();
        }
    }

    @PostMapping("/threads/{threadId}/read")
    @StoreScoped
    public ResponseEntity<ApiResponse<Void>> markThreadAsRead(@PathVariable Long threadId) {
        try {
            Long storeId = StoreContextHolder.getContext().getStoreId();
            suMessagingService.markThreadAsRead(storeId, threadId);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.08fbf3c13510"), null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(ApiMessages.get("api.t.e3b1b978c40d") + e.getMessage()));
        }
    }

    @PostMapping("/link-inbox/backfill")
    @StoreScoped
    @RequirePermission(module = PermissionModule.ORDER, action = PermissionAction.MODIFY_ORDER)
    public ResponseEntity<ApiResponse<RegistrationLinkInboxService.BackfillResult>> backfillLinkInbox() {
        try {
            Long storeId = StoreContextHolder.getContext().getStoreId();
            RegistrationLinkInboxService.BackfillResult result = registrationLinkInboxService.backfillMissingForStore(storeId);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.e7d8f5efe373"), result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(ApiMessages.get("api.t.ad5d145b6316") + e.getMessage()));
        }
    }

    @GetMapping("/ai-settings")
    @StoreScoped
    public ResponseEntity<ApiResponse<SuMessagingAiSettingDTO>> getAiSettings() {
        try {
            Long storeId = StoreContextHolder.getContext().getStoreId();
            return ResponseEntity.ok(ApiResponse.success(suMessagingAiSettingService.getOrCreate(storeId)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(ApiMessages.get("api.t.7d40945f5e2a") + e.getMessage()));
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
            return ResponseEntity.status(500).body(ApiResponse.error(ApiMessages.get("api.t.591efa484c25") + e.getMessage()));
        }
    }
}
