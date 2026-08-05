package server.demo.service;

import org.junit.jupiter.api.Test;
import server.demo.entity.OtaIntegration;
import server.demo.util.SuChannelCatalog;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OtaIntegrationService 渠道映射逻辑聚焦测试（B1/B2/B3 改造）。
 */
class OtaIntegrationServiceChannelMappingTest {

    @Test
    void resolveSuWidgetChannelId_shouldResolveAllFiveChannels() {
        assertEquals("19", OtaIntegrationService.resolveSuWidgetChannelId("BOOKING"));
        assertEquals("244", OtaIntegrationService.resolveSuWidgetChannelId("AIRBNB"));
        assertEquals("9", OtaIntegrationService.resolveSuWidgetChannelId("EXPEDIA"));
        assertEquals("339", OtaIntegrationService.resolveSuWidgetChannelId("TRIP"));
        assertEquals("189", OtaIntegrationService.resolveSuWidgetChannelId("AGODA"));
    }

    @Test
    void resolveSuWidgetChannelId_shouldResolveAliasesAndIgnoreCase() {
        assertEquals("19", OtaIntegrationService.resolveSuWidgetChannelId("BOOKING.COM"));
        assertEquals("339", OtaIntegrationService.resolveSuWidgetChannelId("CTRIP"));
        assertEquals("339", OtaIntegrationService.resolveSuWidgetChannelId("trip"));
    }

    @Test
    void resolveSuWidgetChannelId_shouldReturnEmptyForUnknownOrNull() {
        assertEquals("", OtaIntegrationService.resolveSuWidgetChannelId(null));
        assertEquals("", OtaIntegrationService.resolveSuWidgetChannelId("UNKNOWN"));
    }

    @Test
    void resolveSuOtaCodes_shouldResolveExplicitChannel() {
        assertEquals(List.of(19), OtaIntegrationService.resolveSuOtaCodes(integration("BOOKING")));
        assertEquals(List.of(19), OtaIntegrationService.resolveSuOtaCodes(integration("BOOKING.COM")));
        assertEquals(List.of(244), OtaIntegrationService.resolveSuOtaCodes(integration("AIRBNB")));
        assertEquals(List.of(9), OtaIntegrationService.resolveSuOtaCodes(integration("EXPEDIA")));
        assertEquals(List.of(339), OtaIntegrationService.resolveSuOtaCodes(integration("TRIP")));
        assertEquals(List.of(339), OtaIntegrationService.resolveSuOtaCodes(integration("CTRIP")));
        assertEquals(List.of(189), OtaIntegrationService.resolveSuOtaCodes(integration("AGODA")));
    }

    @Test
    void resolveSuOtaCodes_shouldFallbackToCatalogAllSuIdsForUnknownChannel() {
        // 阻断级修复：未识别渠道不再默认误推 Booking/Airbnb，退回目录全集
        List<Integer> fallback = OtaIntegrationService.resolveSuOtaCodes(integration("UNKNOWN"));
        assertEquals(SuChannelCatalog.allSuIds(), fallback);
        assertEquals(List.of(19, 244, 9, 339, 189), fallback);
    }

    @Test
    void resolveSuOtaCodes_shouldFallbackToCatalogAllSuIdsForNullIntegrationOrCode() {
        assertEquals(SuChannelCatalog.allSuIds(), OtaIntegrationService.resolveSuOtaCodes(null));
        assertEquals(SuChannelCatalog.allSuIds(), OtaIntegrationService.resolveSuOtaCodes(integration(null)));
    }

    private static OtaIntegration integration(String code) {
        OtaIntegration integration = new OtaIntegration();
        integration.setCode(code);
        return integration;
    }
}
