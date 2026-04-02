package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.entity.PricePlan;
import server.demo.entity.RoomType;
import server.demo.entity.RoomTypePricePlan;
import server.demo.repository.RoomPriceRepository;
import server.demo.repository.RoomTypePricePlanRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PricePlanServiceDeleteRoomTypePricePlanTest {

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void deleteRoomTypePricePlan_shouldOnlyDeleteRelation_whenClearOverridesIsFalse() {
        StoreContextHolder.setContext(new StoreContext(1L, 7L, "ADMIN"));

        RoomTypePricePlanRepository roomTypePricePlanRepository = Mockito.mock(RoomTypePricePlanRepository.class);
        RoomPriceRepository roomPriceRepository = Mockito.mock(RoomPriceRepository.class);

        RoomTypePricePlan relation = buildRelation(99L, 7L, 12L, 30L);
        when(roomTypePricePlanRepository.findById(99L)).thenReturn(Optional.of(relation));

        PricePlanService service = new PricePlanService();
        ReflectionTestUtils.setField(service, "roomTypePricePlanRepository", roomTypePricePlanRepository);
        ReflectionTestUtils.setField(service, "roomPriceRepository", roomPriceRepository);

        long clearedCount = service.deleteRoomTypePricePlan(99L, false);

        assertEquals(0L, clearedCount);
        verify(roomTypePricePlanRepository).delete(relation);
        verify(roomPriceRepository, never()).deleteByStoreIdAndRoomTypeIdAndPricePlanId(7L, 12L, 30L);
    }

    @Test
    void deleteRoomTypePricePlan_shouldClearOverridesBeforeDelete_whenClearOverridesIsTrue() {
        StoreContextHolder.setContext(new StoreContext(1L, 7L, "ADMIN"));

        RoomTypePricePlanRepository roomTypePricePlanRepository = Mockito.mock(RoomTypePricePlanRepository.class);
        RoomPriceRepository roomPriceRepository = Mockito.mock(RoomPriceRepository.class);

        RoomTypePricePlan relation = buildRelation(99L, 7L, 12L, 30L);
        when(roomTypePricePlanRepository.findById(99L)).thenReturn(Optional.of(relation));
        when(roomPriceRepository.deleteByStoreIdAndRoomTypeIdAndPricePlanId(7L, 12L, 30L)).thenReturn(6);

        PricePlanService service = new PricePlanService();
        ReflectionTestUtils.setField(service, "roomTypePricePlanRepository", roomTypePricePlanRepository);
        ReflectionTestUtils.setField(service, "roomPriceRepository", roomPriceRepository);

        long clearedCount = service.deleteRoomTypePricePlan(99L, true);

        assertEquals(6L, clearedCount);
        verify(roomPriceRepository).deleteByStoreIdAndRoomTypeIdAndPricePlanId(7L, 12L, 30L);
        verify(roomTypePricePlanRepository).delete(relation);
    }

    @Test
    void deleteRoomTypePricePlan_shouldRejectWhenStoreMismatch() {
        StoreContextHolder.setContext(new StoreContext(1L, 7L, "ADMIN"));

        RoomTypePricePlanRepository roomTypePricePlanRepository = Mockito.mock(RoomTypePricePlanRepository.class);
        RoomPriceRepository roomPriceRepository = Mockito.mock(RoomPriceRepository.class);

        RoomTypePricePlan relation = buildRelation(99L, 8L, 12L, 30L);
        when(roomTypePricePlanRepository.findById(99L)).thenReturn(Optional.of(relation));

        PricePlanService service = new PricePlanService();
        ReflectionTestUtils.setField(service, "roomTypePricePlanRepository", roomTypePricePlanRepository);
        ReflectionTestUtils.setField(service, "roomPriceRepository", roomPriceRepository);

        assertThrows(RuntimeException.class, () -> service.deleteRoomTypePricePlan(99L, true));
        verify(roomTypePricePlanRepository, never()).delete(relation);
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
        return relation;
    }
}
