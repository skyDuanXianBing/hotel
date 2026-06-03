package server.demo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import server.demo.annotation.RequirePermission;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.ApiResponse;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.service.RegistrationLinkInboxService;
import server.demo.service.SuMessagingAiSettingService;
import server.demo.service.SuMessagingService;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuMessagingControllerTest {

    @Test
    void markThreadAsRead_shouldUseStoreContextAndReturnSuccess() {
        SuMessagingService suMessagingService = mock(SuMessagingService.class);
        SuMessagingAiSettingService aiSettingService = mock(SuMessagingAiSettingService.class);
        RegistrationLinkInboxService linkInboxService = mock(RegistrationLinkInboxService.class);
        SuMessagingController controller = new SuMessagingController(suMessagingService, aiSettingService, linkInboxService);

        StoreContextHolder.setContext(new StoreContext(1L, 26L, "OWNER"));
        try {
            ResponseEntity<ApiResponse<Void>> response = controller.markThreadAsRead(77L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isSuccess());
            verify(suMessagingService).markThreadAsRead(26L, 77L);
        } finally {
            StoreContextHolder.clear();
        }
    }

    @Test
    void backfillLinkInbox_shouldRequireOrderModifyPermissionAndUseStoreContext() throws Exception {
        SuMessagingService suMessagingService = mock(SuMessagingService.class);
        SuMessagingAiSettingService aiSettingService = mock(SuMessagingAiSettingService.class);
        RegistrationLinkInboxService linkInboxService = mock(RegistrationLinkInboxService.class);
        SuMessagingController controller = new SuMessagingController(suMessagingService, aiSettingService, linkInboxService);
        RegistrationLinkInboxService.BackfillResult backfillResult =
                new RegistrationLinkInboxService.BackfillResult(3, 2, 1, 1);

        when(linkInboxService.backfillMissingForStore(26L)).thenReturn(backfillResult);

        StoreContextHolder.setContext(new StoreContext(1L, 26L, "OWNER"));
        try {
            ResponseEntity<ApiResponse<RegistrationLinkInboxService.BackfillResult>> response = controller.backfillLinkInbox();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isSuccess());
            assertEquals(backfillResult, response.getBody().getData());
            verify(linkInboxService).backfillMissingForStore(26L);
        } finally {
            StoreContextHolder.clear();
        }

        Method method = SuMessagingController.class.getMethod("backfillLinkInbox");
        RequirePermission permission = method.getAnnotation(RequirePermission.class);
        assertNotNull(permission);
        assertEquals(PermissionModule.ORDER, permission.module());
        assertEquals(PermissionAction.MODIFY_ORDER, permission.action());
    }
}
