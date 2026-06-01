package server.demo.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.entity.OtaIntegration;
import server.demo.service.ChannelE2ETestSupportService.TestSupportAccessException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChannelE2ETestSupportServiceTest {

    private static final String VALID_KEY = "local-test-key";

    @Test
    void validateTestSupportAccess_defaultDisabledRejectsAccess() {
        ChannelE2ETestSupportService service = newService(false, VALID_KEY);

        TestSupportAccessException exception = assertThrows(
                TestSupportAccessException.class,
                () -> service.validateTestSupportAccess(VALID_KEY)
        );

        assertEquals(403, exception.getStatusCode());
        assertEquals("本地渠道E2E test-support 未启用", exception.getMessage());
    }

    @Test
    void validateTestSupportAccess_enabledButEmptyKeyRejectsAccess() {
        ChannelE2ETestSupportService service = newService(true, " ");

        TestSupportAccessException exception = assertThrows(
                TestSupportAccessException.class,
                () -> service.validateTestSupportAccess(VALID_KEY)
        );

        assertEquals(403, exception.getStatusCode());
        assertEquals("本地渠道E2E test-support key 未配置", exception.getMessage());
    }

    @Test
    void validateTestSupportAccess_missingHeaderRejectsAccess() {
        ChannelE2ETestSupportService service = newService(true, VALID_KEY);

        TestSupportAccessException exception = assertThrows(
                TestSupportAccessException.class,
                () -> service.validateTestSupportAccess(null)
        );

        assertEquals(400, exception.getStatusCode());
        assertEquals("缺少 X-Test-Support-Key", exception.getMessage());
    }

    @Test
    void validateTestSupportAccess_blankHeaderRejectsAccess() {
        ChannelE2ETestSupportService service = newService(true, VALID_KEY);

        TestSupportAccessException exception = assertThrows(
                TestSupportAccessException.class,
                () -> service.validateTestSupportAccess(" ")
        );

        assertEquals(400, exception.getStatusCode());
        assertEquals("缺少 X-Test-Support-Key", exception.getMessage());
    }

    @Test
    void validateTestSupportAccess_wrongKeyRejectsAccess() {
        ChannelE2ETestSupportService service = newService(true, VALID_KEY);

        TestSupportAccessException exception = assertThrows(
                TestSupportAccessException.class,
                () -> service.validateTestSupportAccess("wrong-key")
        );

        assertEquals(403, exception.getStatusCode());
        assertEquals("X-Test-Support-Key 不匹配", exception.getMessage());
    }

    @Test
    void validateTestSupportAccess_correctKeyAllowsAccess() {
        ChannelE2ETestSupportService service = newService(true, VALID_KEY);

        assertDoesNotThrow(() -> service.validateTestSupportAccess(" " + VALID_KEY + " "));
    }

    @Test
    void localSetupStoreTimezoneDefaultsToTokyo() {
        assertEquals(
                "Asia/Tokyo",
                ReflectionTestUtils.getField(ChannelE2ETestSupportService.class, "LOCAL_SETUP_TIME_ZONE")
        );
    }

    @Test
    void hasSupportedOtaIntegration_requiresSupportedEnabledAndConnectedIntegration() {
        ChannelE2ETestSupportService service = newService(true, VALID_KEY);

        Boolean supported = ReflectionTestUtils.invokeMethod(
                service,
                "hasSupportedOtaIntegration",
                List.of(buildIntegration("BOOKING", true, true))
        );

        assertTrue(Boolean.TRUE.equals(supported));
    }

    @Test
    void hasSupportedOtaIntegration_rejectsDisconnectedSupportedIntegration() {
        ChannelE2ETestSupportService service = newService(true, VALID_KEY);

        Boolean supported = ReflectionTestUtils.invokeMethod(
                service,
                "hasSupportedOtaIntegration",
                List.of(buildIntegration("BOOKING", true, false))
        );

        assertFalse(Boolean.TRUE.equals(supported));
    }

    @Test
    void hasSupportedOtaIntegration_rejectsDisabledAndUnsupportedIntegrations() {
        ChannelE2ETestSupportService service = newService(true, VALID_KEY);

        Boolean disabledSupported = ReflectionTestUtils.invokeMethod(
                service,
                "hasSupportedOtaIntegration",
                List.of(buildIntegration("AIRBNB", false, true))
        );
        Boolean unsupported = ReflectionTestUtils.invokeMethod(
                service,
                "hasSupportedOtaIntegration",
                List.of(buildIntegration("EXPEDIA", true, true))
        );

        assertFalse(Boolean.TRUE.equals(disabledSupported));
        assertFalse(Boolean.TRUE.equals(unsupported));
    }

    private ChannelE2ETestSupportService newService(boolean localE2EEnabled, String testSupportKey) {
        ChannelE2ETestSupportService service = new ChannelE2ETestSupportService(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        ReflectionTestUtils.setField(service, "localE2EEnabled", localE2EEnabled);
        ReflectionTestUtils.setField(service, "testSupportKey", testSupportKey);
        return service;
    }

    private OtaIntegration buildIntegration(String code, boolean enabled, boolean connected) {
        OtaIntegration integration = new OtaIntegration();
        integration.setCode(code);
        integration.setEnabled(enabled);
        integration.setIsConnected(connected);
        return integration;
    }
}
