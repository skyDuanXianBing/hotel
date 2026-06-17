package server.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import server.demo.annotation.RequirePermission;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.dto.MessageKnowledgeEvidenceResponse;
import server.demo.dto.MessageKnowledgeItemDTO;
import server.demo.dto.MessageKnowledgeItemPageResponse;
import server.demo.dto.MessageKnowledgeRejectRequest;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.service.MessageKnowledgeManagementService;
import server.demo.util.StoreContextUtils;

@RestController
@RequestMapping("/api/v1/su-messaging/knowledge-items")
public class MessageKnowledgeManagementController {
    private final MessageKnowledgeManagementService managementService;

    public MessageKnowledgeManagementController(MessageKnowledgeManagementService managementService) {
        this.managementService = managementService;
    }

    @GetMapping
    @StoreScoped
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MODIFY_STORE_SETTINGS)
    public ResponseEntity<ApiResponse<MessageKnowledgeItemPageResponse>> listItems(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String scopeType,
            @RequestParam(required = false) String topicCode
    ) {
        try {
            Long storeId = StoreContextUtils.requireStoreId();
            MessageKnowledgeItemPageResponse response = managementService.listItems(
                    storeId,
                    page,
                    size,
                    status,
                    keyword,
                    scopeType,
                    topicCode
            );
            return ResponseEntity.ok(ApiResponse.success("知识项列表获取成功", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("知识项列表获取失败: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/evidence")
    @StoreScoped
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MODIFY_STORE_SETTINGS)
    public ResponseEntity<ApiResponse<MessageKnowledgeEvidenceResponse>> getEvidence(@PathVariable Long id) {
        try {
            Long storeId = StoreContextUtils.requireStoreId();
            MessageKnowledgeEvidenceResponse response = managementService.getEvidence(storeId, id);
            return ResponseEntity.ok(ApiResponse.success("知识项证据获取成功", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("知识项证据获取失败: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/approve")
    @StoreScoped
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MODIFY_STORE_SETTINGS)
    public ResponseEntity<ApiResponse<MessageKnowledgeItemDTO>> approve(@PathVariable Long id) {
        try {
            Long storeId = StoreContextUtils.requireStoreId();
            MessageKnowledgeItemDTO response = managementService.approve(storeId, id);
            return ResponseEntity.ok(ApiResponse.success("知识项已通过", response));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("知识项通过失败: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/reject")
    @StoreScoped
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MODIFY_STORE_SETTINGS)
    public ResponseEntity<ApiResponse<MessageKnowledgeItemDTO>> reject(
            @PathVariable Long id,
            @RequestBody(required = false) MessageKnowledgeRejectRequest request
    ) {
        try {
            Long storeId = StoreContextUtils.requireStoreId();
            MessageKnowledgeItemDTO response = managementService.reject(storeId, id, request);
            return ResponseEntity.ok(ApiResponse.success("知识项已拒绝", response));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("知识项拒绝失败: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/archive")
    @StoreScoped
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MODIFY_STORE_SETTINGS)
    public ResponseEntity<ApiResponse<MessageKnowledgeItemDTO>> archive(@PathVariable Long id) {
        try {
            Long storeId = StoreContextUtils.requireStoreId();
            MessageKnowledgeItemDTO response = managementService.archive(storeId, id);
            return ResponseEntity.ok(ApiResponse.success("知识项已归档", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("知识项归档失败: " + e.getMessage()));
        }
    }
}
