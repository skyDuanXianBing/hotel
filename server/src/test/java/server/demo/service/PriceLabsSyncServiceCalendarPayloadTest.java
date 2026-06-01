package server.demo.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.constants.PriceLabsSyncDefaults;
import server.demo.entity.PriceLabsIntegration;
import server.demo.entity.PricePlan;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.entity.RoomTypePricePlan;
import server.demo.entity.Store;
import server.demo.enums.ReservationStatus;
import server.demo.repository.PriceLabsConnectionRepository;
import server.demo.repository.PriceLabsIntegrationRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomPriceRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypePricePlanRepository;
import server.demo.repository.StoreRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PriceLabsSyncServiceCalendarPayloadTest {

    @Test
    void syncListingRatePlanAndCalendar_shouldPushCalendarWithMultiUnitAndInitialRange() {
        PriceLabsApiClient apiClient = mock(PriceLabsApiClient.class);
        StoreRepository storeRepository = mock(StoreRepository.class);
        PriceLabsIntegrationRepository integrationRepository = mock(PriceLabsIntegrationRepository.class);
        PriceLabsConnectionRepository connectionRepository = mock(PriceLabsConnectionRepository.class);
        RoomTypePricePlanRepository roomTypePricePlanRepository = mock(RoomTypePricePlanRepository.class);
        RoomPriceRepository roomPriceRepository = mock(RoomPriceRepository.class);
        RoomRepository roomRepository = mock(RoomRepository.class);
        ReservationRepository reservationRepository = mock(ReservationRepository.class);
        GoogleGeocodingService googleGeocodingService = mock(GoogleGeocodingService.class);

        Store store = new Store();
        store.setId(7L);
        store.setCity("Foshan");
        store.setAddress("No.1 Test Road");
        store.setCountry("CHN");
        store.setCurrency("CNY");
        store.setTimezone("Asia/Tokyo");

        PriceLabsIntegration integration = new PriceLabsIntegration(7L);
        integration.setIsEnabled(true);
        integration.setPriceLabsEmail("test@example.com");

        RoomType roomType = new RoomType();
        roomType.setId(30L);
        roomType.setStoreId(7L);
        roomType.setName("Room 30");
        roomType.setTotalRooms(2);

        PricePlan selectedPlan = new PricePlan();
        selectedPlan.setId(4L);
        selectedPlan.setStoreId(7L);
        selectedPlan.setName("Plan 4");
        selectedPlan.setMinNights(1);

        RoomTypePricePlan mapping = new RoomTypePricePlan();
        mapping.setId(100L);
        mapping.setRoomType(roomType);
        mapping.setPricePlan(selectedPlan);
        mapping.setStoreId(7L);

        Room room1 = new Room();
        room1.setRoomNumber("A101");
        room1.setRoomType(roomType);
        room1.setStoreId(7L);

        Room room2 = new Room();
        room2.setRoomNumber("A102");
        room2.setRoomType(roomType);
        room2.setStoreId(7L);

        when(storeRepository.findById(7L)).thenReturn(Optional.of(store));
        when(integrationRepository.findByStoreId(7L)).thenReturn(Optional.of(integration));
        when(integrationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(connectionRepository.findByStoreIdAndRoomTypeIdAndPricePlanId(7L, 30L, 4L))
                .thenReturn(Optional.empty());
        when(roomTypePricePlanRepository.findByRoomTypeId(30L)).thenReturn(List.of(mapping));
        when(roomRepository.findByStoreIdAndRoomTypeId(7L, 30L)).thenReturn(List.of(room1, room2));
        when(roomPriceRepository.findByStoreIdAndRoomTypeIdAndPriceDateBetween(anyLong(), anyLong(), any(), any()))
                .thenReturn(List.of());
        when(reservationRepository.findOccupancyRowsByStoreIdAndDateRangeAndStatuses(anyLong(), any(), any(), any()))
                .thenReturn(List.of());
        when(googleGeocodingService.geocodeCoordinates(any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        PriceLabsApiClient.PriceLabsResponse successResponse = new PriceLabsApiClient.PriceLabsResponse();
        successResponse.setSuccess(List.of("ok"));
        successResponse.setFailure(List.of());

        @SuppressWarnings("unchecked")
        var calendarCaptor = org.mockito.ArgumentCaptor.forClass((Class<List<PriceLabsApiClient.CalendarData>>) (Class<?>) List.class);

        when(apiClient.pushListings(any())).thenReturn(successResponse);
        when(apiClient.pushRatePlans(any())).thenReturn(successResponse);
        when(apiClient.pushCalendar(calendarCaptor.capture())).thenReturn(successResponse);

        PriceLabsSyncService service = new PriceLabsSyncService();
        ReflectionTestUtils.setField(service, "apiClient", apiClient);
        ReflectionTestUtils.setField(service, "storeRepo", storeRepository);
        ReflectionTestUtils.setField(service, "integrationRepo", integrationRepository);
        ReflectionTestUtils.setField(service, "connectionRepo", connectionRepository);
        ReflectionTestUtils.setField(service, "rtppRepo", roomTypePricePlanRepository);
        ReflectionTestUtils.setField(service, "roomPriceRepo", roomPriceRepository);
        ReflectionTestUtils.setField(service, "roomRepo", roomRepository);
        ReflectionTestUtils.setField(service, "reservationRepository", reservationRepository);
        ReflectionTestUtils.setField(service, "googleGeocodingService", googleGeocodingService);
        ReflectionTestUtils.setField(
                service,
                "clock",
                Clock.fixed(Instant.parse("2026-04-07T15:30:00Z"), ZoneOffset.UTC)
        );

        service.syncListingRatePlanAndCalendar(7L, roomType, selectedPlan, null, PriceLabsSyncDefaults.DEFAULT_SYNC_DAYS);

        verify(apiClient).pushCalendar(any());

        org.mockito.ArgumentCaptor<java.util.Set> statusesCaptor = org.mockito.ArgumentCaptor.forClass(java.util.Set.class);
        verify(reservationRepository, atLeastOnce()).findOccupancyRowsByStoreIdAndDateRangeAndStatuses(
                eq(7L),
                any(),
                any(),
                statusesCaptor.capture()
        );
        @SuppressWarnings("unchecked")
        java.util.Set<ReservationStatus> statuses = (java.util.Set<ReservationStatus>) statusesCaptor.getValue();
        assertTrue(statuses.contains(ReservationStatus.CONFIRMED));
        assertTrue(statuses.contains(ReservationStatus.CHECKED_IN));
        assertTrue(statuses.contains(ReservationStatus.CHECKED_OUT));
        assertTrue(!statuses.contains(ReservationStatus.REQUESTED));

        List<PriceLabsApiClient.CalendarData> pushedCalendars = calendarCaptor.getValue();
        assertNotNull(pushedCalendars);
        assertEquals(1, pushedCalendars.size());

        PriceLabsApiClient.CalendarData calendarData = pushedCalendars.get(0);
        assertNotNull(calendarData.getMultiUnit());
        assertEquals(2, calendarData.getMultiUnit().getTotalUnits());
        assertEquals(2, calendarData.getMultiUnit().getUnitIds().size());
        assertEquals("2026-04-08", calendarData.getCalendar().get(0).getDate());
        assertTrue(calendarData.getCalendar().size() >= PriceLabsSyncDefaults.INITIAL_SYNC_DAYS);
    }
}
