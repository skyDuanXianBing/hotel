package server.demo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import server.demo.annotation.RequirePermission;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.ApiResponse;
import server.demo.dto.SuMessagingMessagePageResponse;
import server.demo.dto.SuMessagingThreadDTO;
import server.demo.dto.SuMessagingThreadPageResponse;
import server.demo.dto.SuMessagingUnreadSummaryDTO;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.service.RegistrationLinkInboxService;
import server.demo.service.SuMessagingAiSettingService;
import server.demo.service.SuMessagingService;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuMessagingControllerTest {

    @Test
    void listThreadPage_shouldExposeEndpointUseStoreContextAndReturnClientContract() throws Exception {
        SuMessagingService suMessagingService = mock(SuMessagingService.class);
        SuMessagingAiSettingService aiSettingService = mock(SuMessagingAiSettingService.class);
        RegistrationLinkInboxService linkInboxService = mock(RegistrationLinkInboxService.class);
        SuMessagingController controller = new SuMessagingController(suMessagingService, aiSettingService, linkInboxService);

        SuMessagingThreadDTO item = new SuMessagingThreadDTO();
        item.setId(1L);
        item.setOrderKind("INQUIRY");
        SuMessagingThreadPageResponse pageResponse = new SuMessagingThreadPageResponse(
                List.of(item),
                0,
                20,
                21,
                2,
                true
        );
        when(suMessagingService.listThreadPage(
                26L,
                0,
                20,
                "AIRBNB",
                "INQUIRY",
                null,
                "INQUIRY,CHECKED_OUT",
                true,
                false,
                "Alice"
        )).thenReturn(pageResponse);

        StoreContextHolder.setContext(new StoreContext(1L, 26L, "OWNER"));
        try {
            ResponseEntity<ApiResponse<SuMessagingThreadPageResponse>> response = controller.listThreadPage(
                    0,
                    20,
                    "AIRBNB",
                    "INQUIRY",
                    null,
                    "INQUIRY,CHECKED_OUT",
                    true,
                    false,
                    "Alice"
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isSuccess());
            assertEquals(1, response.getBody().getData().getItems().size());
            assertEquals(0, response.getBody().getData().getPage());
            assertEquals(20, response.getBody().getData().getSize());
            assertTrue(response.getBody().getData().isHasNext());
            verify(suMessagingService).listThreadPage(
                    26L,
                    0,
                    20,
                    "AIRBNB",
                    "INQUIRY",
                    null,
                    "INQUIRY,CHECKED_OUT",
                    true,
                    false,
                    "Alice"
            );
        } finally {
            StoreContextHolder.clear();
        }

        Method method = SuMessagingController.class.getMethod(
                "listThreadPage",
                Integer.class,
                Integer.class,
                String.class,
                String.class,
                String.class,
                String.class,
                Boolean.class,
                Boolean.class,
                String.class
        );
        GetMapping mapping = method.getAnnotation(GetMapping.class);
        assertNotNull(mapping);
        assertEquals("/threads/page", mapping.value()[0]);
    }

    @Test
    void getMessagePage_shouldExposeEndpointUseStoreContextAndReturnClientContract() throws Exception {
        SuMessagingService suMessagingService = mock(SuMessagingService.class);
        SuMessagingAiSettingService aiSettingService = mock(SuMessagingAiSettingService.class);
        RegistrationLinkInboxService linkInboxService = mock(RegistrationLinkInboxService.class);
        SuMessagingController controller = new SuMessagingController(suMessagingService, aiSettingService, linkInboxService);
        SuMessagingMessagePageResponse pageResponse = new SuMessagingMessagePageResponse(
                List.of(),
                50,
                false,
                null,
                false
        );

        when(suMessagingService.getThreadMessagePage(26L, 77L, 50, 100L, null, false))
                .thenReturn(pageResponse);

        StoreContextHolder.setContext(new StoreContext(1L, 26L, "OWNER"));
        try {
            ResponseEntity<ApiResponse<SuMessagingMessagePageResponse>> response = controller.getMessagePage(
                    77L,
                    50,
                    100L,
                    null,
                    false
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isSuccess());
            assertEquals(0, response.getBody().getData().getItems().size());
            assertEquals(50, response.getBody().getData().getLimit());
            verify(suMessagingService).getThreadMessagePage(26L, 77L, 50, 100L, null, false);
        } finally {
            StoreContextHolder.clear();
        }

        Method method = SuMessagingController.class.getMethod(
                "getMessagePage",
                Long.class,
                Integer.class,
                Long.class,
                Long.class,
                Boolean.class
        );
        GetMapping mapping = method.getAnnotation(GetMapping.class);
        assertNotNull(mapping);
        assertEquals("/threads/{threadId}/messages/page", mapping.value()[0]);
    }

    @Test
    void getUnreadSummary_shouldExposeEndpointUseStoreContextAndReturnTotalUnread() throws Exception {
        SuMessagingService suMessagingService = mock(SuMessagingService.class);
        SuMessagingAiSettingService aiSettingService = mock(SuMessagingAiSettingService.class);
        RegistrationLinkInboxService linkInboxService = mock(RegistrationLinkInboxService.class);
        SuMessagingController controller = new SuMessagingController(suMessagingService, aiSettingService, linkInboxService);
        SuMessagingUnreadSummaryDTO summary = new SuMessagingUnreadSummaryDTO(8, 3, Map.of("AIRBNB", 8L));

        when(suMessagingService.getUnreadSummary(26L)).thenReturn(summary);

        StoreContextHolder.setContext(new StoreContext(1L, 26L, "OWNER"));
        try {
            ResponseEntity<ApiResponse<SuMessagingUnreadSummaryDTO>> response = controller.getUnreadSummary();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isSuccess());
            assertEquals(8, response.getBody().getData().getTotalUnread());
            verify(suMessagingService).getUnreadSummary(26L);
        } finally {
            StoreContextHolder.clear();
        }

        Method method = SuMessagingController.class.getMethod("getUnreadSummary");
        GetMapping mapping = method.getAnnotation(GetMapping.class);
        assertNotNull(mapping);
        assertEquals("/unread-summary", mapping.value()[0]);
    }

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
