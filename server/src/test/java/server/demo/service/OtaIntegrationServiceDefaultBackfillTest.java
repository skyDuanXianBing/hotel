package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import server.demo.config.SuApiConfig;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.OtaIntegrationDTO;
import server.demo.entity.OtaIntegration;
import server.demo.repository.OtaIntegrationRepository;
import server.demo.repository.StoreRepository;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * getAllOtaIntegrations 默认 OTA 渠道幂等补齐测试。
 * QA 缺陷回归：存量门店已有 BOOKING/AIRBNB 两行时，isEmpty 闸门导致
 * EXPEDIA/TRIP/AGODA 集成行永远建不出来，新渠道连接卡片不可见。
 */
class OtaIntegrationServiceDefaultBackfillTest {

    private OtaIntegrationRepository otaIntegrationRepository;
    private OtaIntegrationService service;

    @BeforeEach
    void setUp() {
        StoreContextHolder.setContext(new StoreContext(7L, 10L, "admin"));
        otaIntegrationRepository = Mockito.mock(OtaIntegrationRepository.class);
        service = new OtaIntegrationService(
                otaIntegrationRepository,
                Mockito.mock(SuApiClient.class),
                Mockito.mock(SuContentSyncService.class),
                Mockito.mock(SuAvailabilitySyncService.class),
                Mockito.mock(SuRateSyncService.class),
                Mockito.mock(SuAriSyncService.class),
                Mockito.mock(SuApiConfig.class),
                Mockito.mock(StoreRepository.class),
                Mockito.mock(SuAccessTokenService.class),
                Clock.systemDefaultZone()
        );
        when(otaIntegrationRepository.save(any(OtaIntegration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void existingStoreWithTwoChannels_shouldBackfillThreeNewIntegrations() {
        // 存量门店：已有 BOOKING/AIRBNB 两行
        when(otaIntegrationRepository.findByStoreId(10L))
                .thenReturn(
                        new ArrayList<>(List.of(integration("BOOKING"), integration("AIRBNB"))),
                        new ArrayList<>(List.of(
                                integration("BOOKING"), integration("AIRBNB"), integration("EXPEDIA"),
                                integration("TRIP"), integration("AGODA")))
                );
        when(otaIntegrationRepository.existsByStoreIdAndCode(eq(10L), any()))
                .thenAnswer(invocation -> {
                    String code = invocation.getArgument(1);
                    return "BOOKING".equals(code) || "AIRBNB".equals(code);
                });

        List<OtaIntegrationDTO> result = service.getAllOtaIntegrations();

        // 只补缺失的 EXPEDIA/TRIP/AGODA 三行
        verify(otaIntegrationRepository, times(3)).save(any(OtaIntegration.class));
        verify(otaIntegrationRepository, never()).save(Mockito.argThat(
                ota -> "BOOKING".equals(ota.getCode()) || "AIRBNB".equals(ota.getCode())));
        assertEquals(5, result.size());
        assertTrue(result.stream().anyMatch(dto -> "EXPEDIA".equals(dto.getCode())));
        assertTrue(result.stream().anyMatch(dto -> "TRIP".equals(dto.getCode())));
        assertTrue(result.stream().anyMatch(dto -> "AGODA".equals(dto.getCode())));
    }

    @Test
    void freshStore_shouldInitializeAllFiveIntegrations() {
        when(otaIntegrationRepository.findByStoreId(10L))
                .thenReturn(
                        new ArrayList<>(),
                        new ArrayList<>(List.of(
                                integration("BOOKING"), integration("AIRBNB"), integration("EXPEDIA"),
                                integration("TRIP"), integration("AGODA")))
                );
        when(otaIntegrationRepository.existsByStoreIdAndCode(eq(10L), any())).thenReturn(false);

        List<OtaIntegrationDTO> result = service.getAllOtaIntegrations();

        verify(otaIntegrationRepository, times(5)).save(any(OtaIntegration.class));
        assertEquals(5, result.size());
    }

    @Test
    void storeWithAllChannels_shouldNotSaveAgain() {
        when(otaIntegrationRepository.findByStoreId(10L))
                .thenReturn(new ArrayList<>(List.of(
                        integration("BOOKING"), integration("AIRBNB"), integration("EXPEDIA"),
                        integration("TRIP"), integration("AGODA"))));
        when(otaIntegrationRepository.existsByStoreIdAndCode(eq(10L), any())).thenReturn(true);

        List<OtaIntegrationDTO> result = service.getAllOtaIntegrations();

        verify(otaIntegrationRepository, never()).save(any(OtaIntegration.class));
        assertEquals(5, result.size());
    }

    private static OtaIntegration integration(String code) {
        OtaIntegration integration = new OtaIntegration();
        integration.setCode(code);
        integration.setName(code);
        return integration;
    }
}
