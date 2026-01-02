package server.demo.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.demo.repository.ChannelPriceRepository;
import server.demo.repository.RoomTypePricePlanRepository;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChannelPriceWarmupServiceTest {

    @Mock
    private ChannelPriceRepository channelPriceRepository;

    @Mock
    private RoomTypePricePlanRepository roomTypePricePlanRepository;

    @Mock
    private ChannelPriceFallbackService channelPriceFallbackService;

    @InjectMocks
    private ChannelPriceWarmupService channelPriceWarmupService;

    @Test
    void warmupIfNeeded_shouldNoop_whenStoreIdIsNull() {
        channelPriceWarmupService.warmupIfNeeded(null);
        verify(channelPriceRepository, never()).existsByStoreIdAndPriceDateBetween(any(), any(), any());
        verify(roomTypePricePlanRepository, never()).existsByStoreId(any());
        verify(channelPriceFallbackService, never()).generate(any(), any(), anyInt());
    }

    @Test
    void warmupIfNeeded_shouldSkipAndCache_whenPricesAlreadyExistInRange() {
        Long storeId = 5L;
        when(channelPriceRepository.existsByStoreIdAndPriceDateBetween(eq(storeId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(true);

        channelPriceWarmupService.warmupIfNeeded(storeId);
        channelPriceWarmupService.warmupIfNeeded(storeId);

        verify(channelPriceRepository, times(1)).existsByStoreIdAndPriceDateBetween(eq(storeId), any(LocalDate.class), any(LocalDate.class));
        verify(roomTypePricePlanRepository, never()).existsByStoreId(any());
        verify(channelPriceFallbackService, never()).generate(any(), any(), anyInt());
    }

    @Test
    void warmupIfNeeded_shouldNotCache_whenNoRoomTypePricePlansExist() {
        Long storeId = 5L;
        when(channelPriceRepository.existsByStoreIdAndPriceDateBetween(eq(storeId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(false);
        when(roomTypePricePlanRepository.existsByStoreId(storeId)).thenReturn(false);

        channelPriceWarmupService.warmupIfNeeded(storeId);
        channelPriceWarmupService.warmupIfNeeded(storeId);

        verify(channelPriceRepository, times(2)).existsByStoreIdAndPriceDateBetween(eq(storeId), any(LocalDate.class), any(LocalDate.class));
        verify(roomTypePricePlanRepository, times(2)).existsByStoreId(storeId);
        verify(channelPriceFallbackService, never()).generate(any(), any(), anyInt());
    }

    @Test
    void warmupIfNeeded_shouldGenerateAndCache_whenNoPricesExist() {
        Long storeId = 5L;
        LocalDate today = LocalDate.now();
        LocalDate end = today.plusDays(364);

        when(channelPriceRepository.existsByStoreIdAndPriceDateBetween(eq(storeId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(false);
        when(roomTypePricePlanRepository.existsByStoreId(storeId)).thenReturn(true);
        when(channelPriceFallbackService.generate(storeId, today, 365))
                .thenReturn(new ChannelPriceFallbackService.GenerateResult(storeId, today, end, 1, 1, 1, 0, 0));

        channelPriceWarmupService.warmupIfNeeded(storeId);
        channelPriceWarmupService.warmupIfNeeded(storeId);

        verify(channelPriceRepository, times(1)).existsByStoreIdAndPriceDateBetween(eq(storeId), any(LocalDate.class), any(LocalDate.class));
        verify(roomTypePricePlanRepository, times(1)).existsByStoreId(storeId);
        verify(channelPriceFallbackService, times(1)).generate(storeId, today, 365);
    }
}

