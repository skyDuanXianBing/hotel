package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.entity.PriceLabsConnection;
import server.demo.entity.PricePlan;
import server.demo.entity.RoomType;
import server.demo.repository.PriceLabsConnectionRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PriceLabsServiceConnectionPolicyTest {

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void createConnection_shouldRejectWhenRoomTypeAlreadyHasEnabledConnection() {
        StoreContextHolder.setContext(new StoreContext(1L, 5L, "ADMIN"));

        PriceLabsConnectionRepository connectionRepository = mock(PriceLabsConnectionRepository.class);

        RoomType rt = new RoomType();
        rt.setId(34L);

        PricePlan plan = new PricePlan();
        plan.setId(10L);
        plan.setName("主计划");

        PriceLabsConnection existing = new PriceLabsConnection();
        existing.setId(100L);
        existing.setStoreId(5L);
        existing.setRoomType(rt);
        existing.setPricePlan(plan);
        existing.setIsEnabled(true);

        when(connectionRepository.findByStoreIdAndRoomTypeIdAndIsEnabledTrue(5L, 34L))
                .thenReturn(Optional.of(existing));

        PriceLabsService service = new PriceLabsService();
        ReflectionTestUtils.setField(service, "connectionRepository", connectionRepository);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.createConnection(34L, 11L));
        assertTrue(ex.getMessage().contains("已绑定价格计划"));

        verify(connectionRepository, times(1)).findByStoreIdAndRoomTypeIdAndIsEnabledTrue(5L, 34L);
        verify(connectionRepository, never()).save(any());
    }

    @Test
    void enableConnection_shouldRejectWhenAnotherEnabledConnectionExists() {
        StoreContextHolder.setContext(new StoreContext(1L, 5L, "ADMIN"));

        PriceLabsConnectionRepository connectionRepository = mock(PriceLabsConnectionRepository.class);

        RoomType rt = new RoomType();
        rt.setId(34L);

        PricePlan planToEnable = new PricePlan();
        planToEnable.setId(11L);
        planToEnable.setName("周末计划");

        PriceLabsConnection target = new PriceLabsConnection();
        target.setId(200L);
        target.setStoreId(5L);
        target.setRoomType(rt);
        target.setPricePlan(planToEnable);
        target.setIsEnabled(false);

        PricePlan existingPlan = new PricePlan();
        existingPlan.setId(10L);
        existingPlan.setName("主计划");

        PriceLabsConnection existingEnabled = new PriceLabsConnection();
        existingEnabled.setId(100L);
        existingEnabled.setStoreId(5L);
        existingEnabled.setRoomType(rt);
        existingEnabled.setPricePlan(existingPlan);
        existingEnabled.setIsEnabled(true);

        when(connectionRepository.findById(200L)).thenReturn(Optional.of(target));
        when(connectionRepository.findByStoreIdAndRoomTypeIdAndIsEnabledTrue(5L, 34L))
                .thenReturn(Optional.of(existingEnabled));

        PriceLabsService service = new PriceLabsService();
        ReflectionTestUtils.setField(service, "connectionRepository", connectionRepository);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.updateConnectionStatus(200L, true));
        assertTrue(ex.getMessage().contains("已绑定价格计划"));

        verify(connectionRepository, times(1)).findById(200L);
        verify(connectionRepository, times(1)).findByStoreIdAndRoomTypeIdAndIsEnabledTrue(5L, 34L);
        verify(connectionRepository, never()).save(any());
    }
}

