package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.UpdatePriceByPlanRequest;
import server.demo.entity.PricePlan;
import server.demo.entity.RoomPrice;
import server.demo.entity.RoomType;
import server.demo.entity.RoomTypePricePlan;
import server.demo.repository.PricePlanRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomPriceRepository;
import server.demo.repository.RoomTypePricePlanRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.service.impl.RoomPriceServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoomPriceServiceUpdateByPlanApplyWeekdaysInRangeTest {

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void applyWeekdaysInRange_true_shouldOnlyUpsertMatchingDates() {
        StoreContextHolder.setContext(new StoreContext(1L, 1L, "TEST"));

        RoomPriceRepository roomPriceRepository = Mockito.mock(RoomPriceRepository.class);
        RoomTypeRepository roomTypeRepository = Mockito.mock(RoomTypeRepository.class);
        PricePlanRepository pricePlanRepository = Mockito.mock(PricePlanRepository.class);
        RoomTypePricePlanRepository roomTypePricePlanRepository = Mockito.mock(RoomTypePricePlanRepository.class);
        server.demo.service.PriceChangeHistoryService priceChangeHistoryService = Mockito.mock(server.demo.service.PriceChangeHistoryService.class);
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);

        RoomPriceServiceImpl service = new RoomPriceServiceImpl();
        ReflectionTestUtils.setField(service, "roomPriceRepository", roomPriceRepository);
        ReflectionTestUtils.setField(service, "roomTypeRepository", roomTypeRepository);
        ReflectionTestUtils.setField(service, "pricePlanRepository", pricePlanRepository);
        ReflectionTestUtils.setField(service, "roomTypePricePlanRepository", roomTypePricePlanRepository);
        ReflectionTestUtils.setField(service, "priceChangeHistoryService", priceChangeHistoryService);
        ReflectionTestUtils.setField(service, "reservationRepository", reservationRepository);
        ReflectionTestUtils.setField(service, "priceLabsCalendarSyncDebouncer", null);

        RoomType roomType = new RoomType();
        roomType.setId(11L);
        roomType.setStoreId(1L);
        roomType.setName("RT");
        roomType.setCode("RT1");
        roomType.setTotalRooms(3);

        PricePlan plan = new PricePlan();
        plan.setId(22L);
        plan.setStoreId(1L);
        plan.setName("PP");

        RoomTypePricePlan rtpp = new RoomTypePricePlan();
        rtpp.setId(33L);
        rtpp.setStoreId(1L);
        rtpp.setRoomType(roomType);
        rtpp.setPricePlan(plan);

        when(roomTypeRepository.findByStoreIdAndId(1L, 11L)).thenReturn(Optional.of(roomType));
        when(pricePlanRepository.findByStoreIdAndId(1L, 22L)).thenReturn(Optional.of(plan));
        when(roomTypePricePlanRepository.findByRoomTypeIdAndPricePlanId(11L, 22L)).thenReturn(Optional.of(rtpp));

        LocalDate start = LocalDate.of(2026, 2, 1);
        LocalDate end = start.plusDays(13); // 2 weeks
        List<Integer> weekdays = List.of(1, 3); // Mon, Wed (DayOfWeek.getValue())
        Set<Integer> weekdaySet = new HashSet<>(weekdays);

        Set<LocalDate> expectedDates = new HashSet<>();
        LocalDate d = start;
        while (!d.isAfter(end)) {
            if (weekdaySet.contains(d.getDayOfWeek().getValue())) {
                expectedDates.add(d);
            }
            d = d.plusDays(1);
        }

        when(roomPriceRepository.findByRoomTypeIdAndPricePlanIdAndPriceDate(eq(11L), eq(22L), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(roomPriceRepository.save(any(RoomPrice.class))).thenAnswer(inv -> inv.getArgument(0));

        when(roomTypePricePlanRepository.findByStoreIdWithRoomTypeAndPricePlan(1L)).thenReturn(List.of(rtpp));
        when(roomPriceRepository.findByStoreIdAndPriceDateBetweenWithRoomTypeAndPricePlan(eq(1L), eq(start), eq(end))).thenReturn(List.of());
        when(reservationRepository.findByStoreIdAndDateRange(eq(1L), eq(start), eq(end))).thenReturn(List.of());

        UpdatePriceByPlanRequest req = new UpdatePriceByPlanRequest();
        req.setRoomTypeId(11L);
        req.setPricePlanId(22L);
        req.setStartDate(start);
        req.setEndDate(end);
        req.setWeekdays(weekdays);
        req.setApplyWeekdaysInRange(true);
        req.setPrice(new BigDecimal("123.45"));

        service.updatePriceByPlan(req, "tester");

        ArgumentCaptor<RoomPrice> captor = ArgumentCaptor.forClass(RoomPrice.class);
        verify(roomPriceRepository, times(expectedDates.size())).save(captor.capture());
        for (RoomPrice saved : captor.getAllValues()) {
            assertTrue(expectedDates.contains(saved.getPriceDate()), "should only save matching weekday dates");
            assertEquals(new BigDecimal("123.45"), saved.getPrice());
            assertEquals(1L, saved.getStoreId());
            assertEquals(11L, saved.getRoomType().getId());
            assertEquals(22L, saved.getPricePlan().getId());
        }

        verify(roomTypePricePlanRepository, never()).save(any(RoomTypePricePlan.class));
    }

    @Test
    void applyWeekdaysInRange_false_shouldUpdateWeeklyTemplateOnly() {
        StoreContextHolder.setContext(new StoreContext(1L, 1L, "TEST"));

        RoomPriceRepository roomPriceRepository = Mockito.mock(RoomPriceRepository.class);
        RoomTypeRepository roomTypeRepository = Mockito.mock(RoomTypeRepository.class);
        PricePlanRepository pricePlanRepository = Mockito.mock(PricePlanRepository.class);
        RoomTypePricePlanRepository roomTypePricePlanRepository = Mockito.mock(RoomTypePricePlanRepository.class);
        server.demo.service.PriceChangeHistoryService priceChangeHistoryService = Mockito.mock(server.demo.service.PriceChangeHistoryService.class);
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);

        RoomPriceServiceImpl service = new RoomPriceServiceImpl();
        ReflectionTestUtils.setField(service, "roomPriceRepository", roomPriceRepository);
        ReflectionTestUtils.setField(service, "roomTypeRepository", roomTypeRepository);
        ReflectionTestUtils.setField(service, "pricePlanRepository", pricePlanRepository);
        ReflectionTestUtils.setField(service, "roomTypePricePlanRepository", roomTypePricePlanRepository);
        ReflectionTestUtils.setField(service, "priceChangeHistoryService", priceChangeHistoryService);
        ReflectionTestUtils.setField(service, "reservationRepository", reservationRepository);
        ReflectionTestUtils.setField(service, "priceLabsCalendarSyncDebouncer", null);

        RoomType roomType = new RoomType();
        roomType.setId(11L);
        roomType.setStoreId(1L);
        roomType.setName("RT");
        roomType.setCode("RT1");
        roomType.setTotalRooms(3);

        PricePlan plan = new PricePlan();
        plan.setId(22L);
        plan.setStoreId(1L);
        plan.setName("PP");

        RoomTypePricePlan rtpp = new RoomTypePricePlan();
        rtpp.setId(33L);
        rtpp.setStoreId(1L);
        rtpp.setRoomType(roomType);
        rtpp.setPricePlan(plan);

        when(roomTypeRepository.findByStoreIdAndId(1L, 11L)).thenReturn(Optional.of(roomType));
        when(pricePlanRepository.findByStoreIdAndId(1L, 22L)).thenReturn(Optional.of(plan));
        when(roomTypePricePlanRepository.findByRoomTypeIdAndPricePlanId(11L, 22L)).thenReturn(Optional.of(rtpp));

        LocalDate start = LocalDate.of(2026, 2, 1);
        LocalDate end = start.plusDays(6);
        when(roomTypePricePlanRepository.findByStoreIdWithRoomTypeAndPricePlan(1L)).thenReturn(List.of(rtpp));
        when(roomPriceRepository.findByStoreIdAndPriceDateBetweenWithRoomTypeAndPricePlan(eq(1L), eq(start), eq(end))).thenReturn(List.of());
        when(reservationRepository.findByStoreIdAndDateRange(eq(1L), eq(start), eq(end))).thenReturn(List.of());

        UpdatePriceByPlanRequest req = new UpdatePriceByPlanRequest();
        req.setRoomTypeId(11L);
        req.setPricePlanId(22L);
        req.setStartDate(start);
        req.setEndDate(end);
        req.setWeekdays(List.of(1, 3));
        req.setPrice(new BigDecimal("200.00"));
        // applyWeekdaysInRange is null => legacy behavior

        service.updatePriceByPlan(req, "tester");

        verify(roomTypePricePlanRepository, times(1)).save(any(RoomTypePricePlan.class));
        verify(roomPriceRepository, never()).save(any(RoomPrice.class));
    }
}

