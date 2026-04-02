package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.AssignRoomTypePricePlanRequest;
import server.demo.entity.PricePlan;
import server.demo.entity.RoomType;
import server.demo.entity.RoomTypePricePlan;
import server.demo.repository.RoomPriceRepository;
import server.demo.repository.RoomTypePricePlanRepository;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PricePlanServiceUpdateRoomTypePricePlanClearOverridesTest {

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void updateRoomTypePricePlan_shouldClearFutureOverrides_whenRequested() {
        StoreContextHolder.setContext(new StoreContext(1L, 7L, "ADMIN"));

        RoomTypePricePlanRepository roomTypePricePlanRepository = Mockito.mock(RoomTypePricePlanRepository.class);
        RoomPriceRepository roomPriceRepository = Mockito.mock(RoomPriceRepository.class);
        SuAriAutoSyncService suAriAutoSyncService = Mockito.mock(SuAriAutoSyncService.class);

        RoomTypePricePlan relation = buildRelation(19L, 7L, 30L, 6L);
        when(roomTypePricePlanRepository.findById(19L)).thenReturn(Optional.of(relation));
        when(roomTypePricePlanRepository.save(relation)).thenReturn(relation);

        PricePlanService service = new PricePlanService();
        ReflectionTestUtils.setField(service, "roomTypePricePlanRepository", roomTypePricePlanRepository);
        ReflectionTestUtils.setField(service, "roomPriceRepository", roomPriceRepository);
        ReflectionTestUtils.setField(service, "suAriAutoSyncService", suAriAutoSyncService);

        AssignRoomTypePricePlanRequest request = new AssignRoomTypePricePlanRequest();
        request.setMaxGuests(2);
        request.setClearFutureOverrides(true);
        request.setClearFromDate(LocalDate.of(2026, 4, 2));

        service.updateRoomTypePricePlan(19L, request);

        verify(roomTypePricePlanRepository).save(relation);
        verify(roomPriceRepository).deleteByStoreIdAndRoomTypeIdAndPricePlanIdAndPriceDateGreaterThanEqual(
                7L,
                30L,
                6L,
                LocalDate.of(2026, 4, 2)
        );
    }

    @Test
    void updateRoomTypePricePlan_shouldNotClearFutureOverrides_whenNotRequested() {
        StoreContextHolder.setContext(new StoreContext(1L, 7L, "ADMIN"));

        RoomTypePricePlanRepository roomTypePricePlanRepository = Mockito.mock(RoomTypePricePlanRepository.class);
        RoomPriceRepository roomPriceRepository = Mockito.mock(RoomPriceRepository.class);
        SuAriAutoSyncService suAriAutoSyncService = Mockito.mock(SuAriAutoSyncService.class);

        RoomTypePricePlan relation = buildRelation(19L, 7L, 30L, 6L);
        when(roomTypePricePlanRepository.findById(19L)).thenReturn(Optional.of(relation));
        when(roomTypePricePlanRepository.save(relation)).thenReturn(relation);

        PricePlanService service = new PricePlanService();
        ReflectionTestUtils.setField(service, "roomTypePricePlanRepository", roomTypePricePlanRepository);
        ReflectionTestUtils.setField(service, "roomPriceRepository", roomPriceRepository);
        ReflectionTestUtils.setField(service, "suAriAutoSyncService", suAriAutoSyncService);

        AssignRoomTypePricePlanRequest request = new AssignRoomTypePricePlanRequest();
        request.setMaxGuests(2);
        request.setClearFutureOverrides(false);

        service.updateRoomTypePricePlan(19L, request);

        verify(roomTypePricePlanRepository).save(relation);
        verify(roomPriceRepository, never()).deleteByStoreIdAndRoomTypeIdAndPricePlanIdAndPriceDateGreaterThanEqual(
                Mockito.anyLong(),
                Mockito.anyLong(),
                Mockito.anyLong(),
                Mockito.any(LocalDate.class)
        );
    }

    @Test
    void updateRoomTypePricePlan_shouldDefaultClearFutureOverrides_whenWeeklyPriceChanged() {
        StoreContextHolder.setContext(new StoreContext(1L, 7L, "ADMIN"));

        RoomTypePricePlanRepository roomTypePricePlanRepository = Mockito.mock(RoomTypePricePlanRepository.class);
        RoomPriceRepository roomPriceRepository = Mockito.mock(RoomPriceRepository.class);
        SuAriAutoSyncService suAriAutoSyncService = Mockito.mock(SuAriAutoSyncService.class);

        RoomTypePricePlan relation = buildRelation(19L, 7L, 30L, 6L);
        relation.setMondayPrice(new BigDecimal("7777"));
        when(roomTypePricePlanRepository.findById(19L)).thenReturn(Optional.of(relation));
        when(roomTypePricePlanRepository.save(relation)).thenReturn(relation);

        PricePlanService service = new PricePlanService();
        ReflectionTestUtils.setField(service, "roomTypePricePlanRepository", roomTypePricePlanRepository);
        ReflectionTestUtils.setField(service, "roomPriceRepository", roomPriceRepository);
        ReflectionTestUtils.setField(service, "suAriAutoSyncService", suAriAutoSyncService);

        AssignRoomTypePricePlanRequest request = new AssignRoomTypePricePlanRequest();
        request.setMaxGuests(2);
        request.setMondayPrice(new BigDecimal("6666"));

        service.updateRoomTypePricePlan(19L, request);

        verify(roomPriceRepository).deleteByStoreIdAndRoomTypeIdAndPricePlanIdAndPriceDateGreaterThanEqual(
                7L,
                30L,
                6L,
                LocalDate.now()
        );
    }

    private RoomTypePricePlan buildRelation(Long id, Long storeId, Long roomTypeId, Long pricePlanId) {
        RoomType roomType = new RoomType();
        roomType.setId(roomTypeId);
        roomType.setStoreId(storeId);

        PricePlan pricePlan = new PricePlan();
        pricePlan.setId(pricePlanId);

        RoomTypePricePlan relation = new RoomTypePricePlan();
        relation.setId(id);
        relation.setStoreId(storeId);
        relation.setRoomType(roomType);
        relation.setPricePlan(pricePlan);
        relation.setMaxGuests(2);
        return relation;
    }
}
