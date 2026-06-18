package server.demo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import server.demo.annotation.RequirePermission;
import server.demo.annotation.StoreScoped;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.ApiResponse;
import server.demo.dto.MessageKnowledgeRebuildRequest;
import server.demo.dto.MessageKnowledgeRebuildResponse;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageKnowledgeMaintenanceControllerTest {

    @Test
    void rebuildKnowledge_shouldReturnDisabledNoopResponseAndKeepStoreScopedEndpoint() throws Exception {
        MessageKnowledgeMaintenanceController controller = new MessageKnowledgeMaintenanceController();
        MessageKnowledgeRebuildRequest request = new MessageKnowledgeRebuildRequest();
        request.setLookbackDays(30);
        request.setLimit(25);

        StoreContextHolder.setContext(new StoreContext(1L, 26L, "OWNER"));
        try {
            ResponseEntity<ApiResponse<MessageKnowledgeRebuildResponse>> response =
                    controller.rebuildKnowledge(request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isSuccess());
            assertEquals(26L, response.getBody().getData().getStoreId());
            assertEquals(30, response.getBody().getData().getLookbackDays());
            assertEquals(25, response.getBody().getData().getLimit());
            assertEquals(0, response.getBody().getData().getAttemptedCount());
            assertEquals(
                    "旧知识库重建已停用，会话级 AI 知识抽取是唯一新主路径",
                    response.getBody().getMessage()
            );
        } finally {
            StoreContextHolder.clear();
        }

        Method method = MessageKnowledgeMaintenanceController.class.getMethod(
                "rebuildKnowledge",
                MessageKnowledgeRebuildRequest.class
        );
        PostMapping mapping = method.getAnnotation(PostMapping.class);
        StoreScoped storeScoped = method.getAnnotation(StoreScoped.class);
        RequirePermission permission = method.getAnnotation(RequirePermission.class);
        assertNotNull(mapping);
        assertNotNull(storeScoped);
        assertNotNull(permission);
        assertEquals("/rebuild", mapping.value()[0]);
        assertEquals(PermissionModule.ORDER, permission.module());
        assertEquals(PermissionAction.MODIFY_ORDER, permission.action());
    }

    @Test
    void rebuildKnowledge_shouldNormalizeNullRequestWithoutLegacyWrites() {
        MessageKnowledgeMaintenanceController controller = new MessageKnowledgeMaintenanceController();

        StoreContextHolder.setContext(new StoreContext(1L, 26L, "OWNER"));
        try {
            ResponseEntity<ApiResponse<MessageKnowledgeRebuildResponse>> response =
                    controller.rebuildKnowledge(null);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isSuccess());
            assertEquals(26L, response.getBody().getData().getStoreId());
            assertEquals(0, response.getBody().getData().getAttemptedCount());
        } finally {
            StoreContextHolder.clear();
        }
    }
}
