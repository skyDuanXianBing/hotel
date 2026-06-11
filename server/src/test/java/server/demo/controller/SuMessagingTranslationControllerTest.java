package server.demo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import server.demo.annotation.StoreScoped;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.ApiResponse;
import server.demo.dto.SuMessagingTranslationRequest;
import server.demo.dto.SuMessagingTranslationResponse;
import server.demo.entity.SuMessageTranslation;
import server.demo.service.SuMessagingTranslationService;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuMessagingTranslationControllerTest {

    @Test
    void getOrCreateTranslation_shouldExposeEndpointUseStoreContextAndReturnContract() throws Exception {
        SuMessagingTranslationService service = mock(SuMessagingTranslationService.class);
        SuMessagingTranslationController controller = new SuMessagingTranslationController(service);
        SuMessagingTranslationRequest request = new SuMessagingTranslationRequest();
        request.setTargetLanguage("zh-CN");
        SuMessagingTranslationResponse serviceResponse = new SuMessagingTranslationResponse(
                201L,
                "zh-CN",
                "你好",
                "hash",
                SuMessageTranslation.STATUS_SUCCESS,
                true,
                OffsetDateTime.parse("2026-06-10T12:00:00Z")
        );
        when(service.getOrCreateTranslation(26L, 77L, 201L, request)).thenReturn(serviceResponse);

        StoreContextHolder.setContext(new StoreContext(1L, 26L, "OWNER"));
        try {
            ResponseEntity<ApiResponse<SuMessagingTranslationResponse>> response =
                    controller.getOrCreateTranslation(77L, 201L, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isSuccess());
            assertEquals(201L, response.getBody().getData().getMessageId());
            assertEquals("zh-CN", response.getBody().getData().getTargetLanguage());
            assertEquals("你好", response.getBody().getData().getTranslatedContent());
            assertTrue(response.getBody().getData().isCached());
            verify(service).getOrCreateTranslation(26L, 77L, 201L, request);
        } finally {
            StoreContextHolder.clear();
        }

        Method method = SuMessagingTranslationController.class.getMethod(
                "getOrCreateTranslation",
                Long.class,
                Long.class,
                SuMessagingTranslationRequest.class
        );
        PostMapping mapping = method.getAnnotation(PostMapping.class);
        assertNotNull(mapping);
        assertEquals("/threads/{threadId}/messages/{messageId}/translation", mapping.value()[0]);
        assertNotNull(method.getAnnotation(StoreScoped.class));
    }

    @Test
    void getOrCreateTranslation_shouldReturnDiagnosticMessageWhenServiceFails() {
        SuMessagingTranslationService service = mock(SuMessagingTranslationService.class);
        SuMessagingTranslationController controller = new SuMessagingTranslationController(service);
        SuMessagingTranslationRequest request = new SuMessagingTranslationRequest();
        request.setTargetLanguage("zh-CN");

        when(service.getOrCreateTranslation(26L, 77L, 201L, request))
                .thenThrow(new IllegalStateException("TRANSLATION_BLANK_RESULT"));

        StoreContextHolder.setContext(new StoreContext(1L, 26L, "OWNER"));
        try {
            ResponseEntity<ApiResponse<SuMessagingTranslationResponse>> response =
                    controller.getOrCreateTranslation(77L, 201L, request);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("消息翻译失败: TRANSLATION_BLANK_RESULT", response.getBody().getMessage());
        } finally {
            StoreContextHolder.clear();
        }
    }
}
