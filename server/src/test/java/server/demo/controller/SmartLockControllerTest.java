package server.demo.controller;

import org.junit.jupiter.api.Test;
import server.demo.dto.ApiResponse;
import server.demo.dto.SmartLockBindingDTO;
import server.demo.dto.SmartLockIntegrationDTO;
import server.demo.dto.SmartLockRequests;
import server.demo.service.SmartLockService;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SmartLockControllerTest {
    @Test
    void createBinding_shouldDelegateDualRoleRequestFields() {
        SmartLockService service = mock(SmartLockService.class);
        SmartLockBindingDTO dto = new SmartLockBindingDTO();
        dto.setId(100L);
        dto.setControlDeviceId(20L);
        dto.setPasscodeDeviceId(21L);
        when(service.createBinding(any(SmartLockRequests.CreateBindingRequest.class))).thenReturn(dto);

        SmartLockRequests.CreateBindingRequest request = new SmartLockRequests.CreateBindingRequest();
        request.setRoomId(1L);
        request.setControlDeviceId(20L);
        request.setPasscodeDeviceId(21L);

        SmartLockController controller = new SmartLockController(service);
        ApiResponse<SmartLockBindingDTO> response = controller.createBinding(request);

        assertTrue(response.isSuccess());
        assertEquals(20L, response.getData().getControlDeviceId());
        assertEquals(21L, response.getData().getPasscodeDeviceId());
        verify(service).createBinding(request);
    }

    @Test
    void listIntegrations_shouldRedactSensitiveExceptionMessage() {
        SmartLockService service = mock(SmartLockService.class);
        when(service.listIntegrations()).thenThrow(new RuntimeException(
                "provider rejected token=tok_live_123 secret=sec_live_456 "
                        + "passcode=123456 password=plain_password"
        ));

        SmartLockController controller = new SmartLockController(service);
        ApiResponse<List<SmartLockIntegrationDTO>> response = controller.listIntegrations();

        assertFalse(response.isSuccess());
        assertFalse(response.getMessage().contains("tok_live_123"));
        assertFalse(response.getMessage().contains("sec_live_456"));
        assertFalse(response.getMessage().contains("123456"));
        assertFalse(response.getMessage().contains("plain_password"));
        assertTrue(response.getMessage().contains("获取门锁集成配置失败"));
        assertTrue(response.getMessage().contains("[REDACTED]"));
    }

    @Test
    void publicWebhook_shouldRedactSensitiveExceptionMessage() {
        SmartLockService service = mock(SmartLockService.class);
        when(service.handleSwitchBotWebhook(eq("bad-token"), any())).thenThrow(new RuntimeException(
                "invalid authorization=Bearer bearer_live_123 accessToken=access_live_456"
        ));

        SmartLockPublicWebhookController controller = new SmartLockPublicWebhookController(service);
        ApiResponse<Map<String, Object>> response = controller.handleSwitchBotWebhook("bad-token", Map.of());

        assertFalse(response.isSuccess());
        assertFalse(response.getMessage().contains("bearer_live_123"));
        assertFalse(response.getMessage().contains("access_live_456"));
        assertTrue(response.getMessage().contains("SwitchBot webhook 处理失败"));
        assertTrue(response.getMessage().contains("[REDACTED]"));
    }
}
