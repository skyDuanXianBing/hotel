package server.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.demo.annotation.StoreScoped;
import server.demo.context.StoreContextHolder;
import server.demo.dto.ApiResponse;
import server.demo.dto.SuMessagingAiReplyDraftRequest;
import server.demo.dto.SuMessagingAiReplyDraftResponse;
import server.demo.service.SuMessagingAiReplyDraftService;

@RestController
@RequestMapping("/api/v1/su-messaging")
public class SuMessagingAiReplyDraftController {

    private final SuMessagingAiReplyDraftService aiReplyDraftService;

    public SuMessagingAiReplyDraftController(SuMessagingAiReplyDraftService aiReplyDraftService) {
        this.aiReplyDraftService = aiReplyDraftService;
    }

    @PostMapping("/threads/{threadId}/ai-reply-draft")
    @StoreScoped
    public ResponseEntity<ApiResponse<SuMessagingAiReplyDraftResponse>> generateReplyDraft(
            @PathVariable Long threadId,
            @RequestBody(required = false) SuMessagingAiReplyDraftRequest request
    ) {
        try {
            Long storeId = StoreContextHolder.getContext().getStoreId();
            SuMessagingAiReplyDraftResponse response =
                    aiReplyDraftService.generateDraft(storeId, threadId, request);
            return ResponseEntity.ok(ApiResponse.success("AI 回复草稿生成成功", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("生成 AI 回复草稿失败: " + e.getMessage()));
        }
    }
}
