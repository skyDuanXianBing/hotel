package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.repository.RoomTypePricePlanRepository;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PricePlanServiceRoomTypeCountsTest {

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void countRoomTypesByPricePlanForCurrentStore_shouldAggregateGroupedRows() {
        StoreContextHolder.setContext(new StoreContext(1L, 26L, "OWNER"));

        RoomTypePricePlanRepository roomTypePricePlanRepository = mock(RoomTypePricePlanRepository.class);
        when(roomTypePricePlanRepository.countRoomTypesGroupedByPricePlanId(26L))
                .thenReturn(List.of(
                        new Object[]{10L, 3L},
                        new Object[]{11L, 1L}
                ));

        PricePlanService service = new PricePlanService();
        ReflectionTestUtils.setField(service, "roomTypePricePlanRepository", roomTypePricePlanRepository);

        Map<Long, Long> counts = service.countRoomTypesByPricePlanForCurrentStore();

        assertEquals(2, counts.size());
        assertEquals(3L, counts.get(10L));
        assertEquals(1L, counts.get(11L));
    }

    @Test
    void countRoomTypesByPricePlanForCurrentStore_shouldSkipMalformedRows() {
        StoreContextHolder.setContext(new StoreContext(1L, 26L, "OWNER"));

        RoomTypePricePlanRepository roomTypePricePlanRepository = mock(RoomTypePricePlanRepository.class);
        when(roomTypePricePlanRepository.countRoomTypesGroupedByPricePlanId(26L))
                .thenReturn(java.util.Arrays.asList(
                        new Object[]{null, 2L},
                        new Object[]{12L, null},
                        null,
                        new Object[]{13L, 5L}
                ));

        PricePlanService service = new PricePlanService();
        ReflectionTestUtils.setField(service, "roomTypePricePlanRepository", roomTypePricePlanRepository);

        Map<Long, Long> counts = service.countRoomTypesByPricePlanForCurrentStore();

        assertEquals(1, counts.size());
        assertEquals(5L, counts.get(13L));
    }
}
