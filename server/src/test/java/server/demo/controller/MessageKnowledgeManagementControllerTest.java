package server.demo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import server.demo.annotation.RequirePermission;
import server.demo.annotation.StoreScoped;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.ApiResponse;
import server.demo.dto.MessageKnowledgeItemDTO;
import server.demo.dto.MessageKnowledgeItemPageResponse;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.service.MessageKnowledgeManagementService;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageKnowledgeManagementControllerTest {

    @Test
    void listItems_shouldUseStoreContextAndSettingsPermission() throws Exception {
        MessageKnowledgeManagementService managementService = mock(MessageKnowledgeManagementService.class);
        MessageKnowledgeManagementController controller =
                new MessageKnowledgeManagementController(managementService);
        MessageKnowledgeItemDTO item = new MessageKnowledgeItemDTO();
        item.setId(9L);
        item.setEmbeddingStatus("READY");
        item.setEmbeddingModel("text-embedding-3-small");
        MessageKnowledgeItemPageResponse pageResponse =
                new MessageKnowledgeItemPageResponse(List.of(item), 0, 20, 1, 1, false);

        when(managementService.listItems(26L, 0, 20, "ACTIVE", "wifi", "ROOM_TYPE", "wifi"))
                .thenReturn(pageResponse);

        StoreContextHolder.setContext(new StoreContext(1L, 26L, "OWNER"));
        try {
            ResponseEntity<ApiResponse<MessageKnowledgeItemPageResponse>> response =
                    controller.listItems(0, 20, "ACTIVE", "wifi", "ROOM_TYPE", "wifi");

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isSuccess());
            assertEquals(pageResponse, response.getBody().getData());
            assertEquals("READY", response.getBody().getData().getContent().get(0).getEmbeddingStatus());
            assertEquals(
                    "text-embedding-3-small",
                    response.getBody().getData().getContent().get(0).getEmbeddingModel()
            );
            verify(managementService).listItems(26L, 0, 20, "ACTIVE", "wifi", "ROOM_TYPE", "wifi");
        } finally {
            StoreContextHolder.clear();
        }

        Method method = MessageKnowledgeManagementController.class.getMethod(
                "listItems",
                Integer.class,
                Integer.class,
                String.class,
                String.class,
                String.class,
                String.class
        );
        GetMapping mapping = method.getAnnotation(GetMapping.class);
        StoreScoped storeScoped = method.getAnnotation(StoreScoped.class);
        RequirePermission permission = method.getAnnotation(RequirePermission.class);
        assertNotNull(mapping);
        assertNotNull(storeScoped);
        assertNotNull(permission);
        assertEquals(PermissionModule.SETTINGS, permission.module());
        assertEquals(PermissionAction.MODIFY_STORE_SETTINGS, permission.action());
    }

    @Test
    void stateEndpoints_shouldExposeOnlyApproveRejectArchiveActions() throws Exception {
        assertStateEndpoint("approve", "/{id}/approve");
        assertStateEndpoint("reject", "/{id}/reject");
        assertStateEndpoint("archive", "/{id}/archive");
    }

    private static void assertStateEndpoint(String methodName, String expectedPath) throws Exception {
        Method method;
        if ("reject".equals(methodName)) {
            method = MessageKnowledgeManagementController.class.getMethod(
                    methodName,
                    Long.class,
                    server.demo.dto.MessageKnowledgeRejectRequest.class
            );
        } else {
            method = MessageKnowledgeManagementController.class.getMethod(methodName, Long.class);
        }

        PostMapping mapping = method.getAnnotation(PostMapping.class);
        StoreScoped storeScoped = method.getAnnotation(StoreScoped.class);
        RequirePermission permission = method.getAnnotation(RequirePermission.class);
        assertNotNull(mapping);
        assertEquals(expectedPath, mapping.value()[0]);
        assertNotNull(storeScoped);
        assertNotNull(permission);
        assertEquals(PermissionModule.SETTINGS, permission.module());
        assertEquals(PermissionAction.MODIFY_STORE_SETTINGS, permission.action());
    }
}
