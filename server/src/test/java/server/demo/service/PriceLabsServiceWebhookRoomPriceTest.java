package server.demo.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.dto.PriceLabsWebhookRequest;
import server.demo.entity.PriceLabsConnection;
import server.demo.entity.PriceLabsIntegration;
import server.demo.entity.PricePlan;
import server.demo.entity.PriceChangeHistory;
import server.demo.entity.RoomPrice;
import server.demo.entity.RoomType;
import server.demo.repository.ChannelPriceRepository;
import server.demo.repository.ChannelRepository;
import server.demo.repository.PriceChangeHistoryRepository;
import server.demo.repository.PriceLabsConnectionRepository;
import server.demo.repository.PriceLabsIntegrationRepository;
import server.demo.repository.PriceLabsSyncLogRepository;
import server.demo.repository.RoomPriceRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PriceLabsServiceWebhookRoomPriceTest {

    @Test
    void handleWebhookPriceUpdate_shouldUpsertRoomPriceEvenWhenNoChannelsEnabled() {
        PriceLabsConnectionRepository connectionRepository = mock(PriceLabsConnectionRepository.class);
        ChannelRepository channelRepository = mock(ChannelRepository.class);
        ChannelPriceRepository channelPriceRepository = mock(ChannelPriceRepository.class);
        RoomPriceRepository roomPriceRepository = mock(RoomPriceRepository.class);
        PriceChangeHistoryRepository priceChangeHistoryRepository = mock(PriceChangeHistoryRepository.class);
        PriceLabsSyncLogRepository syncLogRepository = mock(PriceLabsSyncLogRepository.class);
        PriceLabsIntegrationRepository integrationRepository = mock(PriceLabsIntegrationRepository.class);

        RoomType roomType = new RoomType();
        roomType.setId(35L);
        roomType.setName("Room 35");
        roomType.setCode("RT35");
        roomType.setStoreId(5L);

        PricePlan pricePlan = new PricePlan();
        pricePlan.setId(10L);
        pricePlan.setName("Plan 10");
        pricePlan.setStoreId(5L);

        PriceLabsConnection connection = new PriceLabsConnection();
        connection.setId(100L);
        connection.setStoreId(5L);
        connection.setRoomType(roomType);
        connection.setPricePlan(pricePlan);
        connection.setIsEnabled(true);

        when(connectionRepository.findByRoomTypeIdAndPricePlanId(35L, 10L)).thenReturn(Optional.of(connection));
        when(channelRepository.findByStoreId(5L)).thenReturn(List.of());
        when(integrationRepository.findByStoreId(5L)).thenReturn(Optional.of(new PriceLabsIntegration(5L)));

        LocalDate date = LocalDate.of(2026, 1, 15);
        when(roomPriceRepository.findByRoomTypeIdAndPricePlanIdAndPriceDate(35L, 10L, date)).thenReturn(Optional.empty());
        when(roomPriceRepository.save(any(RoomPrice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(priceChangeHistoryRepository.save(any(PriceChangeHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        PriceLabsService service = new PriceLabsService();
        ReflectionTestUtils.setField(service, "connectionRepository", connectionRepository);
        ReflectionTestUtils.setField(service, "channelRepository", channelRepository);
        ReflectionTestUtils.setField(service, "channelPriceRepository", channelPriceRepository);
        ReflectionTestUtils.setField(service, "roomPriceRepository", roomPriceRepository);
        ReflectionTestUtils.setField(service, "priceChangeHistoryRepository", priceChangeHistoryRepository);
        ReflectionTestUtils.setField(service, "syncLogRepository", syncLogRepository);
        ReflectionTestUtils.setField(service, "integrationRepository", integrationRepository);

        PriceLabsWebhookRequest req = new PriceLabsWebhookRequest();
        req.setType("price_update");
        PriceLabsWebhookRequest.ListingData listing = new PriceLabsWebhookRequest.ListingData();
        listing.setListingId("store_5_room_type_35");
        listing.setRatePlanId("plan_10");
        PriceLabsWebhookRequest.CalendarData cal = new PriceLabsWebhookRequest.CalendarData();
        cal.setDate("2026-01-15");
        cal.setPrice(new BigDecimal("13442"));
        listing.setCalendar(List.of(cal));
        req.setData(List.of(listing));

        service.handleWebhookPriceUpdate(req);

        verify(roomPriceRepository, times(1))
                .save(any(RoomPrice.class));
        verify(priceChangeHistoryRepository, times(1)).save(any(PriceChangeHistory.class));
        verify(channelPriceRepository, never()).save(any());
    }

    @Test
    void handleWebhookPriceUpdate_shouldNotCreateHistoryWhenPriceUnchanged() {
        PriceLabsConnectionRepository connectionRepository = mock(PriceLabsConnectionRepository.class);
        ChannelRepository channelRepository = mock(ChannelRepository.class);
        ChannelPriceRepository channelPriceRepository = mock(ChannelPriceRepository.class);
        RoomPriceRepository roomPriceRepository = mock(RoomPriceRepository.class);
        PriceChangeHistoryRepository priceChangeHistoryRepository = mock(PriceChangeHistoryRepository.class);
        PriceLabsSyncLogRepository syncLogRepository = mock(PriceLabsSyncLogRepository.class);
        PriceLabsIntegrationRepository integrationRepository = mock(PriceLabsIntegrationRepository.class);

        RoomType roomType = new RoomType();
        roomType.setId(35L);
        roomType.setStoreId(5L);

        PricePlan pricePlan = new PricePlan();
        pricePlan.setId(10L);
        pricePlan.setStoreId(5L);

        PriceLabsConnection connection = new PriceLabsConnection();
        connection.setId(100L);
        connection.setStoreId(5L);
        connection.setRoomType(roomType);
        connection.setPricePlan(pricePlan);
        connection.setIsEnabled(true);

        when(connectionRepository.findByRoomTypeIdAndPricePlanId(35L, 10L)).thenReturn(Optional.of(connection));
        when(channelRepository.findByStoreId(5L)).thenReturn(List.of());
        when(integrationRepository.findByStoreId(5L)).thenReturn(Optional.of(new PriceLabsIntegration(5L)));

        LocalDate date = LocalDate.of(2026, 1, 15);
        RoomPrice existing = new RoomPrice(roomType, pricePlan, date, new BigDecimal("13442"));
        when(roomPriceRepository.findByRoomTypeIdAndPricePlanIdAndPriceDate(35L, 10L, date)).thenReturn(Optional.of(existing));
        when(roomPriceRepository.save(any(RoomPrice.class))).thenAnswer(inv -> inv.getArgument(0));

        PriceLabsService service = new PriceLabsService();
        ReflectionTestUtils.setField(service, "connectionRepository", connectionRepository);
        ReflectionTestUtils.setField(service, "channelRepository", channelRepository);
        ReflectionTestUtils.setField(service, "channelPriceRepository", channelPriceRepository);
        ReflectionTestUtils.setField(service, "roomPriceRepository", roomPriceRepository);
        ReflectionTestUtils.setField(service, "priceChangeHistoryRepository", priceChangeHistoryRepository);
        ReflectionTestUtils.setField(service, "syncLogRepository", syncLogRepository);
        ReflectionTestUtils.setField(service, "integrationRepository", integrationRepository);

        PriceLabsWebhookRequest req = new PriceLabsWebhookRequest();
        req.setType("price_update");
        PriceLabsWebhookRequest.ListingData listing = new PriceLabsWebhookRequest.ListingData();
        listing.setListingId("store_5_room_type_35");
        listing.setRatePlanId("plan_10");
        PriceLabsWebhookRequest.CalendarData cal = new PriceLabsWebhookRequest.CalendarData();
        cal.setDate("2026-01-15");
        cal.setPrice(new BigDecimal("13442"));
        listing.setCalendar(List.of(cal));
        req.setData(List.of(listing));

        service.handleWebhookPriceUpdate(req);

        verify(roomPriceRepository, times(1)).save(any(RoomPrice.class));
        verify(priceChangeHistoryRepository, never()).save(any(PriceChangeHistory.class));
        verify(channelPriceRepository, never()).save(any());
    }

    @Test
    void handleWebhookPriceUpdate_shouldClampMinStayToOne() {
        PriceLabsConnectionRepository connectionRepository = mock(PriceLabsConnectionRepository.class);
        ChannelRepository channelRepository = mock(ChannelRepository.class);
        ChannelPriceRepository channelPriceRepository = mock(ChannelPriceRepository.class);
        RoomPriceRepository roomPriceRepository = mock(RoomPriceRepository.class);
        PriceChangeHistoryRepository priceChangeHistoryRepository = mock(PriceChangeHistoryRepository.class);
        PriceLabsSyncLogRepository syncLogRepository = mock(PriceLabsSyncLogRepository.class);
        PriceLabsIntegrationRepository integrationRepository = mock(PriceLabsIntegrationRepository.class);

        RoomType roomType = new RoomType();
        roomType.setId(35L);
        roomType.setStoreId(5L);

        PricePlan pricePlan = new PricePlan();
        pricePlan.setId(10L);
        pricePlan.setStoreId(5L);

        PriceLabsConnection connection = new PriceLabsConnection();
        connection.setId(100L);
        connection.setStoreId(5L);
        connection.setRoomType(roomType);
        connection.setPricePlan(pricePlan);
        connection.setIsEnabled(true);

        when(connectionRepository.findByRoomTypeIdAndPricePlanId(35L, 10L)).thenReturn(Optional.of(connection));
        when(channelRepository.findByStoreId(5L)).thenReturn(List.of());
        when(integrationRepository.findByStoreId(5L)).thenReturn(Optional.of(new PriceLabsIntegration(5L)));

        LocalDate date = LocalDate.of(2026, 1, 15);
        when(roomPriceRepository.findByRoomTypeIdAndPricePlanIdAndPriceDate(35L, 10L, date)).thenReturn(Optional.empty());
        when(roomPriceRepository.save(any(RoomPrice.class))).thenAnswer(inv -> inv.getArgument(0));

        PriceLabsService service = new PriceLabsService();
        ReflectionTestUtils.setField(service, "connectionRepository", connectionRepository);
        ReflectionTestUtils.setField(service, "channelRepository", channelRepository);
        ReflectionTestUtils.setField(service, "channelPriceRepository", channelPriceRepository);
        ReflectionTestUtils.setField(service, "roomPriceRepository", roomPriceRepository);
        ReflectionTestUtils.setField(service, "priceChangeHistoryRepository", priceChangeHistoryRepository);
        ReflectionTestUtils.setField(service, "syncLogRepository", syncLogRepository);
        ReflectionTestUtils.setField(service, "integrationRepository", integrationRepository);

        PriceLabsWebhookRequest req = new PriceLabsWebhookRequest();
        req.setType("price_update");
        PriceLabsWebhookRequest.ListingData listing = new PriceLabsWebhookRequest.ListingData();
        listing.setListingId("store_5_room_type_35");
        listing.setRatePlanId("plan_10");
        PriceLabsWebhookRequest.CalendarData cal = new PriceLabsWebhookRequest.CalendarData();
        cal.setDate("2026-01-15");
        cal.setPrice(new BigDecimal("13442"));
        cal.setMinStay(0);
        listing.setCalendar(List.of(cal));
        req.setData(List.of(listing));

        service.handleWebhookPriceUpdate(req);

        var captor = forClass(RoomPrice.class);
        verify(roomPriceRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getMinStay());
    }

    @Test
    void handleWebhookPriceUpdate_shouldRejectMissingListingId() {
        PriceLabsService service = new PriceLabsService();
        PriceLabsWebhookRequest req = new PriceLabsWebhookRequest();
        PriceLabsWebhookRequest.ListingData listing = new PriceLabsWebhookRequest.ListingData();
        listing.setListingId(" ");
        req.setData(List.of(listing));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.handleWebhookPriceUpdate(req));
        assertNotNull(ex.getMessage());
    }
}
