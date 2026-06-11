package server.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.demo.annotation.RequirePermission;
import server.demo.annotation.StoreScoped;
import server.demo.context.StoreContextHolder;
import server.demo.dto.ApiResponse;
import server.demo.dto.MessageKnowledgeRebuildRequest;
import server.demo.dto.MessageKnowledgeRebuildResponse;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.service.MessageKnowledgeIndexService;

@RestController
@RequestMapping("/api/v1/su-messaging/knowledge")
public class MessageKnowledgeMaintenanceController {

    private final MessageKnowledgeIndexService indexService;

    public MessageKnowledgeMaintenanceController(MessageKnowledgeIndexService indexService) {
        this.indexService = indexService;
    }

    @PostMapping("/rebuild")
    @StoreScoped
    @RequirePermission(module = PermissionModule.ORDER, action = PermissionAction.MODIFY_ORDER)
    public ResponseEntity<ApiResponse<MessageKnowledgeRebuildResponse>> rebuildKnowledge(
            @RequestBody(required = false) MessageKnowledgeRebuildRequest request
    ) {
        try {
            MessageKnowledgeRebuildRequest safeRequest =
                    request == null ? new MessageKnowledgeRebuildRequest() : request;
            Long storeId = StoreContextHolder.getContext().getStoreId();
            int lookbackDays = MessageKnowledgeIndexService.normalizeLookbackDays(safeRequest.getLookbackDays());
            int limit = MessageKnowledgeIndexService.normalizeMessageLimit(safeRequest.getLimit());
            int attemptedCount = indexService.indexRecentStoreMessages(storeId, lookbackDays, limit);
            MessageKnowledgeRebuildResponse response = new MessageKnowledgeRebuildResponse(
                    storeId,
                    lookbackDays,
                    limit,
                    attemptedCount
            );
            return ResponseEntity.ok(ApiResponse.success("知识库重建任务执行成功", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("知识库重建任务执行失败: " + e.getMessage()));
        }
    }
}
