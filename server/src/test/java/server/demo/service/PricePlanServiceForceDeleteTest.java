package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.entity.PricePlan;
import server.demo.repository.ChannelPriceRepository;
import server.demo.repository.PriceChangeHistoryRepository;
import server.demo.repository.PriceLabsConnectionRepository;
import server.demo.repository.PricePlanRepository;
import server.demo.repository.RoomPriceRepository;
import server.demo.repository.RoomTypePricePlanRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PricePlanServiceForceDeleteTest {

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void forceDeletePricePlan_shouldOnlyCleanupChannelPrices_thenReuseDeleteFlow() {
        StoreContextHolder.setContext(new StoreContext(1L, 7L, "ADMIN"));

        PricePlanRepository pricePlanRepository = Mockito.mock(PricePlanRepository.class);
        RoomTypePricePlanRepository roomTypePricePlanRepository = Mockito.mock(RoomTypePricePlanRepository.class);
        RoomTypeRepository roomTypeRepository = Mockito.mock(RoomTypeRepository.class);
        RoomPriceRepository roomPriceRepository = Mockito.mock(RoomPriceRepository.class);
        ChannelPriceRepository channelPriceRepository = Mockito.mock(ChannelPriceRepository.class);
        PriceChangeHistoryRepository priceChangeHistoryRepository = Mockito.mock(PriceChangeHistoryRepository.class);
        PriceLabsConnectionRepository priceLabsConnectionRepository = Mockito.mock(PriceLabsConnectionRepository.class);
        UserRepository userRepository = Mockito.mock(UserRepository.class);

        PricePlan plan = new PricePlan();
        plan.setId(4L);
        plan.setName("dd");

        when(pricePlanRepository.findByStoreIdAndId(7L, 4L)).thenReturn(Optional.of(plan));
        when(roomPriceRepository.existsByStoreIdAndPricePlanId(7L, 4L)).thenReturn(false);
        when(channelPriceRepository.existsByStoreIdAndPricePlanId(7L, 4L)).thenReturn(false);
        when(priceChangeHistoryRepository.existsByStoreIdAndPricePlanId(7L, 4L)).thenReturn(false);
        when(priceLabsConnectionRepository.existsByStoreIdAndPricePlanId(7L, 4L)).thenReturn(false);
        when(channelPriceRepository.deleteByStoreIdAndPricePlanId(7L, 4L)).thenReturn(1);

        PricePlanService service = new PricePlanService();
        ReflectionTestUtils.setField(service, "pricePlanRepository", pricePlanRepository);
        ReflectionTestUtils.setField(service, "roomTypePricePlanRepository", roomTypePricePlanRepository);
        ReflectionTestUtils.setField(service, "roomTypeRepository", roomTypeRepository);
        ReflectionTestUtils.setField(service, "roomPriceRepository", roomPriceRepository);
        ReflectionTestUtils.setField(service, "channelPriceRepository", channelPriceRepository);
        ReflectionTestUtils.setField(service, "priceChangeHistoryRepository", priceChangeHistoryRepository);
        ReflectionTestUtils.setField(service, "priceLabsConnectionRepository", priceLabsConnectionRepository);
        ReflectionTestUtils.setField(service, "userRepository", userRepository);

        service.forceDeletePricePlan(4L, true);

        verify(channelPriceRepository).deleteByStoreIdAndPricePlanId(7L, 4L);
        verify(roomTypePricePlanRepository).deleteByStoreIdAndPricePlanId(7L, 4L);
        verify(pricePlanRepository).delete(plan);
    }

    @Test
    void forceDeletePricePlan_shouldRequireConfirm() {
        StoreContextHolder.setContext(new StoreContext(1L, 7L, "ADMIN"));

        PricePlanService service = new PricePlanService();
        assertThrows(RuntimeException.class, () -> service.forceDeletePricePlan(4L, false));
    }
}

