package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.PriceLabsWebhookRequest;
import server.demo.entity.PriceLabsConnection;
import server.demo.entity.PricePlan;
import server.demo.entity.RoomType;
import server.demo.repository.PriceLabsConnectionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PriceLabsSyncServiceGetPricesTest {

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void pullPricesForNextDays_shouldCallHandleWebhookWithConvertedData() {
        StoreContextHolder.setContext(new StoreContext(1L, 5L, "ADMIN"));

        PriceLabsApiClient apiClient = mock(PriceLabsApiClient.class);
        PriceLabsService priceLabsService = mock(PriceLabsService.class);
        PriceLabsConnectionRepository connectionRepository = mock(PriceLabsConnectionRepository.class);

        RoomType roomType = new RoomType();
        roomType.setId(35L);
        roomType.setStoreId(5L);

        PricePlan pricePlan = new PricePlan();
        pricePlan.setId(10L);
        pricePlan.setStoreId(5L);

        PriceLabsConnection conn = new PriceLabsConnection();
        conn.setId(100L);
        conn.setStoreId(5L);
        conn.setRoomType(roomType);
        conn.setPricePlan(pricePlan);
        conn.setIsEnabled(true);
        conn.setPriceLabsListingId("store_5_room_type_35");

        when(connectionRepository.findByStoreId(5L)).thenReturn(List.of(conn));

        PriceLabsApiClient.PullSyncPriceEntry entry = new PriceLabsApiClient.PullSyncPriceEntry();
        entry.setDate(LocalDate.now().plusDays(1).toString());
        entry.setPrice(new BigDecimal("12345"));
        entry.setMinStay(2);
        entry.setCheckIn(true);
        entry.setCheckOut(false);

        PriceLabsApiClient.PullSyncGetPricesResponse resp = new PriceLabsApiClient.PullSyncGetPricesResponse();
        resp.setData(List.of(entry));

        when(apiClient.pullSyncGetPrices(any(), any(), any()))
                .thenReturn(resp);

        PriceLabsSyncService service = new PriceLabsSyncService();
        ReflectionTestUtils.setField(service, "apiClient", apiClient);
        ReflectionTestUtils.setField(service, "priceLabsService", priceLabsService);
        ReflectionTestUtils.setField(service, "connectionRepo", connectionRepository);

        service.pullPricesForNextDaysPullSync(365);

        verify(priceLabsService, times(1)).handleWebhookPriceUpdate(any(PriceLabsWebhookRequest.class));
    }
}
