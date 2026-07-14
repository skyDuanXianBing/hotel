package server.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import server.demo.annotation.StoreScoped;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.ApiResponse;
import server.demo.dto.SuMessagingTranslationSettingDTO;
import server.demo.service.SuMessagingTranslationSettingService;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuMessagingTranslationSettingControllerTest {

    @Test
    void getTranslationSetting_shouldUseTrustedContextUserAndExposeStoreScopedEndpoint() throws Exception {
        SuMessagingTranslationSettingService service = mock(SuMessagingTranslationSettingService.class);
        SuMessagingTranslationSettingController controller =
                new SuMessagingTranslationSettingController(service);
        when(service.get(91L)).thenReturn(new SuMessagingTranslationSettingDTO(false, "zh-CN", false));

        StoreContextHolder.setContext(new StoreContext(91L, 26L, "OWNER"));
        try {
            ResponseEntity<ApiResponse<SuMessagingTranslationSettingDTO>> response =
                    controller.getTranslationSetting();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isSuccess());
            assertFalse(response.getBody().getData().getEnabled());
            assertEquals("zh-CN", response.getBody().getData().getTargetLanguage());
            assertFalse(response.getBody().getData().getConfigured());
            verify(service).get(91L);
        } finally {
            StoreContextHolder.clear();
        }

        RequestMapping controllerMapping = SuMessagingTranslationSettingController.class
                .getAnnotation(RequestMapping.class);
        assertNotNull(controllerMapping);
        assertEquals("/api/v1/su-messaging/translation-settings", controllerMapping.value()[0]);

        Method method = SuMessagingTranslationSettingController.class.getMethod("getTranslationSetting");
        assertNotNull(method.getAnnotation(GetMapping.class));
        assertNotNull(method.getAnnotation(StoreScoped.class));
    }

    @Test
    void updateTranslationSetting_shouldUseTrustedContextUserAndReturnEnvelope() throws Exception {
        SuMessagingTranslationSettingService service = mock(SuMessagingTranslationSettingService.class);
        SuMessagingTranslationSettingController controller =
                new SuMessagingTranslationSettingController(service);
        SuMessagingTranslationSettingDTO request = new SuMessagingTranslationSettingDTO(true, "ja");
        when(service.update(91L, request)).thenReturn(new SuMessagingTranslationSettingDTO(true, "ja", true));

        StoreContextHolder.setContext(new StoreContext(91L, 26L, "OWNER"));
        try {
            ResponseEntity<ApiResponse<SuMessagingTranslationSettingDTO>> response =
                    controller.updateTranslationSetting(request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isSuccess());
            assertTrue(response.getBody().getData().getEnabled());
            assertEquals("ja", response.getBody().getData().getTargetLanguage());
            assertTrue(response.getBody().getData().getConfigured());
            verify(service).update(91L, request);
        } finally {
            StoreContextHolder.clear();
        }

        Method method = SuMessagingTranslationSettingController.class.getMethod(
                "updateTranslationSetting",
                SuMessagingTranslationSettingDTO.class
        );
        assertNotNull(method.getAnnotation(PutMapping.class));
        assertNotNull(method.getAnnotation(StoreScoped.class));
    }

    @Test
    void updateTranslationSetting_shouldReturnBadRequestForUnsupportedLanguage() {
        SuMessagingTranslationSettingService service = mock(SuMessagingTranslationSettingService.class);
        SuMessagingTranslationSettingController controller =
                new SuMessagingTranslationSettingController(service);
        SuMessagingTranslationSettingDTO request = new SuMessagingTranslationSettingDTO(true, "fr");
        when(service.update(91L, request)).thenThrow(new IllegalArgumentException("目标语言仅支持 zh-CN、en、ja、ko"));

        StoreContextHolder.setContext(new StoreContext(91L, 26L, "OWNER"));
        try {
            ResponseEntity<ApiResponse<SuMessagingTranslationSettingDTO>> response =
                    controller.updateTranslationSetting(request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertFalse(response.getBody().isSuccess());
            assertEquals("目标语言仅支持 zh-CN、en、ja、ko", response.getBody().getMessage());
        } finally {
            StoreContextHolder.clear();
        }
    }

    @Test
    void requestContract_shouldNotExposeClientControlledUserId() {
        assertThrows(
                NoSuchFieldException.class,
                () -> SuMessagingTranslationSettingDTO.class.getDeclaredField("userId")
        );
    }

    @Test
    void requestContract_shouldIgnoreClientControlledConfiguredValue() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        SuMessagingTranslationSettingDTO request = objectMapper.readValue(
                "{\"enabled\":false,\"targetLanguage\":\"zh-CN\",\"configured\":true}",
                SuMessagingTranslationSettingDTO.class
        );

        assertFalse(request.getEnabled());
        assertEquals("zh-CN", request.getTargetLanguage());
        assertNull(request.getConfigured());
    }
}
