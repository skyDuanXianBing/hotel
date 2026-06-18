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

@RestController
@RequestMapping("/api/v1/su-messaging/knowledge")
public class MessageKnowledgeMaintenanceController {
    private static final int DEFAULT_RECENT_INDEX_DAYS = 365;
    private static final int DEFAULT_MAX_MESSAGES_PER_RUN = 500;
    private static final int MIN_RECENT_INDEX_DAYS = 1;
    private static final int MIN_MESSAGES_PER_RUN = 1;

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
            int lookbackDays = normalizeLookbackDays(safeRequest.getLookbackDays());
            int limit = normalizeMessageLimit(safeRequest.getLimit());
            MessageKnowledgeRebuildResponse response = new MessageKnowledgeRebuildResponse(
                    storeId,
                    lookbackDays,
                    limit,
                    0
            );
            return ResponseEntity.ok(ApiResponse.success(
                    "旧知识库重建已停用，会话级 AI 知识抽取是唯一新主路径",
                    response
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("知识库重建任务执行失败: " + e.getMessage()));
        }
    }

    private static int normalizeLookbackDays(Integer lookbackDays) {
        if (lookbackDays == null) {
            return DEFAULT_RECENT_INDEX_DAYS;
        }
        return Math.max(MIN_RECENT_INDEX_DAYS, Math.min(lookbackDays, DEFAULT_RECENT_INDEX_DAYS));
    }

    private static int normalizeMessageLimit(Integer messageLimit) {
        if (messageLimit == null) {
            return DEFAULT_MAX_MESSAGES_PER_RUN;
        }
        return Math.max(MIN_MESSAGES_PER_RUN, Math.min(messageLimit, DEFAULT_MAX_MESSAGES_PER_RUN));
    }
}
