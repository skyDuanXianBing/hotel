package server.demo.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.demo.entity.Store;
import server.demo.repository.ChannelPriceRepository;
import server.demo.repository.ChannelRepository;
import server.demo.repository.StoreRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtaSyncServiceBusinessDateTest {

    @Mock
    private SuApiClient suApiClient;

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private ChannelPriceRepository channelPriceRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private ChannelPriceFallbackService channelPriceFallbackService;

    @Mock
    private SuAccessTokenService suAccessTokenService;

    @Test
    void syncStorePricesToSu_shouldUseStoreBusinessDate_whenStartDateIsNull() {
        Long storeId = 7L;
        LocalDate tokyoDate = LocalDate.of(2026, 4, 8);
        OtaSyncService service = createService("2026-04-07T15:30:00Z");

        mockStore(storeId, "Asia/Tokyo");
        mockNoOtaChannels(storeId);

        OtaSyncService.OtaSyncResult result = service.syncStorePricesToSu(storeId, null, 2);

        assertEquals(tokyoDate, result.startDate());
        assertEquals(tokyoDate.plusDays(1), result.endDate());
        verify(channelPriceFallbackService).generate(
                storeId,
                tokyoDate,
                2,
                List.of("AIRBNB", "BOOKING"),
                null
        );
    }

    @Test
    void syncStorePricesToSu_shouldKeepExplicitStartDate() {
        Long storeId = 7L;
        LocalDate explicitStartDate = LocalDate.of(2026, 3, 1);
        OtaSyncService service = createService("2026-04-07T15:30:00Z");

        mockStore(storeId, "Asia/Tokyo");
        mockNoOtaChannels(storeId);

        OtaSyncService.OtaSyncResult result = service.syncStorePricesToSu(storeId, explicitStartDate, 2);

        assertEquals(explicitStartDate, result.startDate());
        assertEquals(explicitStartDate.plusDays(1), result.endDate());
        verify(channelPriceFallbackService).generate(
                storeId,
                explicitStartDate,
                2,
                List.of("AIRBNB", "BOOKING"),
                null
        );
    }

    private OtaSyncService createService(String instant) {
        Clock clock = Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
        return new OtaSyncService(
                suApiClient,
                channelRepository,
                channelPriceRepository,
                storeRepository,
                channelPriceFallbackService,
                suAccessTokenService,
                clock
        );
    }

    private void mockStore(Long storeId, String timezone) {
        Store store = new Store();
        store.setId(storeId);
        store.setTimezone(timezone);
        store.setSuHotelId("STORE" + storeId);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    }

    private void mockNoOtaChannels(Long storeId) {
        when(channelRepository.findByStoreIdAndCode(storeId, "AIRBNB")).thenReturn(Optional.empty());
        when(channelRepository.findByStoreIdAndCode(storeId, "BOOKING")).thenReturn(Optional.empty());
    }
}
