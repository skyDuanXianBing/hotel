package server.demo.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.demo.annotation.StoreScoped;
import server.demo.context.StoreContextHolder;
import server.demo.dto.ApiResponse;
import server.demo.dto.SuMessagingTranslationRequest;
import server.demo.dto.SuMessagingTranslationResponse;
import server.demo.service.SuMessagingTranslationService;

@RestController
@RequestMapping("/api/v1/su-messaging")
public class SuMessagingTranslationController {
    private final SuMessagingTranslationService translationService;

    public SuMessagingTranslationController(SuMessagingTranslationService translationService) {
        this.translationService = translationService;
    }

    @PostMapping("/threads/{threadId}/messages/{messageId}/translation")
    @StoreScoped
    public ResponseEntity<ApiResponse<SuMessagingTranslationResponse>> getOrCreateTranslation(
            @PathVariable Long threadId,
            @PathVariable Long messageId,
            @Valid @RequestBody SuMessagingTranslationRequest request
    ) {
        try {
            Long storeId = StoreContextHolder.getContext().getStoreId();
            SuMessagingTranslationResponse response =
                    translationService.getOrCreateTranslation(storeId, threadId, messageId, request);
            return ResponseEntity.ok(ApiResponse.success("消息翻译成功", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("消息翻译失败: " + e.getMessage()));
        }
    }
}
