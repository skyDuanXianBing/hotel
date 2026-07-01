package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.ChannelPriceAdjustmentDTO;
import server.demo.entity.Channel;
import server.demo.enums.PriceAdjustmentType;
import server.demo.repository.ChannelRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PriceLabsServiceChannelAdjustmentSyncContainmentTest {

    private static final Long USER_ID = 1L;
    private static final Long STORE_ID = 5L;
    private static final Long CHANNEL_ID = 43L;

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void updateChannelPriceAdjustment_shouldSaveLocalSettingWithoutLegacySuSync() {
        StoreContextHolder.setContext(new StoreContext(USER_ID, STORE_ID, "ADMIN"));

        ChannelRepository channelRepository = mock(ChannelRepository.class);
        SuMappingMultiplierSyncService syncService = mock(SuMappingMultiplierSyncService.class);
        Channel channel = channel("AIRBNB");

        when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(channel));
        when(channelRepository.save(any(Channel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PriceLabsService service = createService(channelRepository, syncService);

        ChannelPriceAdjustmentDTO result = service.updateChannelPriceAdjustment(
                CHANNEL_ID,
                PriceAdjustmentType.PERCENTAGE,
                new BigDecimal("12"),
                true
        );

        assertEquals(PriceAdjustmentType.PERCENTAGE, channel.getPriceAdjustmentType());
        assertEquals(new BigDecimal("12"), channel.getPriceAdjustmentValue());
        assertEquals(Boolean.TRUE, channel.getAutoSyncPrice());
        assertSkippedMappingSync(result);
        assertEquals(new BigDecimal("1.12"), result.getSuMappingSync().getRequestedMultiplier());
        assertEquals(BigDecimal.ZERO, result.getSuMappingSync().getRequestedSurcharge());
        verify(syncService, never()).syncForChannel(any(), any());
    }

    @Test
    void batchUpdateChannelPriceAdjustments_shouldSaveLocalSettingsWithoutLegacySuSync() {
        StoreContextHolder.setContext(new StoreContext(USER_ID, STORE_ID, "ADMIN"));

        ChannelRepository channelRepository = mock(ChannelRepository.class);
        SuMappingMultiplierSyncService syncService = mock(SuMappingMultiplierSyncService.class);
        Channel channel = channel("BOOKING");
        ChannelPriceAdjustmentDTO request = new ChannelPriceAdjustmentDTO();
        request.setChannelId(CHANNEL_ID);
        request.setAdjustmentType(PriceAdjustmentType.FIXED);
        request.setAdjustmentValue(new BigDecimal("25"));
        request.setAutoSyncPrice(false);

        when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(channel));
        when(channelRepository.save(any(Channel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PriceLabsService service = createService(channelRepository, syncService);

        List<ChannelPriceAdjustmentDTO> results = service.batchUpdateChannelPriceAdjustments(List.of(request));

        assertEquals(1, results.size());
        assertEquals(PriceAdjustmentType.FIXED, channel.getPriceAdjustmentType());
        assertEquals(new BigDecimal("25"), channel.getPriceAdjustmentValue());
        assertEquals(Boolean.FALSE, channel.getAutoSyncPrice());
        assertSkippedMappingSync(results.get(0));
        assertEquals(BigDecimal.ONE, results.get(0).getSuMappingSync().getRequestedMultiplier());
        assertEquals(new BigDecimal("25"), results.get(0).getSuMappingSync().getRequestedSurcharge());
        verify(syncService, never()).syncForChannel(any(), any());
    }

    private PriceLabsService createService(
            ChannelRepository channelRepository,
            SuMappingMultiplierSyncService syncService
    ) {
        PriceLabsService service = new PriceLabsService();
        ReflectionTestUtils.setField(service, "channelRepository", channelRepository);
        ReflectionTestUtils.setField(service, "suMappingMultiplierSyncService", syncService);
        return service;
    }

    private Channel channel(String channelCode) {
        Channel channel = new Channel();
        channel.setId(CHANNEL_ID);
        channel.setStoreId(STORE_ID);
        channel.setName(channelCode);
        channel.setCode(channelCode);
        channel.setAutoSyncPrice(true);
        channel.setPriceAdjustmentType(PriceAdjustmentType.PERCENTAGE);
        channel.setPriceAdjustmentValue(BigDecimal.ZERO);
        return channel;
    }

    private void assertSkippedMappingSync(ChannelPriceAdjustmentDTO dto) {
        assertNotNull(dto);
        assertNotNull(dto.getSuMappingSync());
        assertEquals("SKIPPED", dto.getSuMappingSync().getStatus());
        assertNotEquals("SUCCESS", dto.getSuMappingSync().getStatus());
        assertEquals(0, dto.getSuMappingSync().getSuccessCount());
        assertEquals(0, dto.getSuMappingSync().getTotalCount());
        assertTrue(dto.getSuMappingSync().getMessage().contains("映射级"));
        assertTrue(dto.getSuMappingSync().getMessage().contains("逐行保存"));
    }
}
