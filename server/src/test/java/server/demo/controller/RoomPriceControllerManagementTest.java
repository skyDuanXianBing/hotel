package server.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import server.demo.annotation.StoreScoped;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.entity.RoomType;
import server.demo.repository.ChannelPriceRepository;
import server.demo.repository.PricePlanRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomBlockoutRepository;
import server.demo.repository.RoomPriceRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypePricePlanRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.service.impl.RoomPriceServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoomPriceControllerManagementTest {

    private static final Long ROOM_TYPE_ID = 59L;
    private static final LocalDate PRICE_DATE = LocalDate.of(2026, 4, 6);
    private static final Long STORE_ONE_ID = 1L;
    private static final Long STORE_TWO_ID = 2L;
    private static final String IGNORED_STORE_ID_PARAM = "999";

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void managementEndpoint_shouldReturnUnboundReadOnlyRowsFromCurrentStoreContext() throws Exception {
        assertTrue(RoomPriceController.class.isAnnotationPresent(StoreScoped.class));

        RoomTypeRepository roomTypeRepository = mock(RoomTypeRepository.class);
        RoomTypePricePlanRepository roomTypePricePlanRepository = mock(RoomTypePricePlanRepository.class);
        RoomPriceRepository roomPriceRepository = mock(RoomPriceRepository.class);
        ChannelPriceRepository channelPriceRepository = mock(ChannelPriceRepository.class);
        ReservationRepository reservationRepository = mock(ReservationRepository.class);
        RoomRepository roomRepository = mock(RoomRepository.class);
        RoomBlockoutRepository roomBlockoutRepository = mock(RoomBlockoutRepository.class);

        RoomPriceServiceImpl roomPriceService = new RoomPriceServiceImpl();
        ReflectionTestUtils.setField(roomPriceService, "roomTypeRepository", roomTypeRepository);
        ReflectionTestUtils.setField(roomPriceService, "pricePlanRepository", mock(PricePlanRepository.class));
        ReflectionTestUtils.setField(roomPriceService, "roomTypePricePlanRepository", roomTypePricePlanRepository);
        ReflectionTestUtils.setField(roomPriceService, "roomPriceRepository", roomPriceRepository);
        ReflectionTestUtils.setField(roomPriceService, "channelPriceRepository", channelPriceRepository);
        ReflectionTestUtils.setField(roomPriceService, "reservationRepository", reservationRepository);
        ReflectionTestUtils.setField(roomPriceService, "roomRepository", roomRepository);
        ReflectionTestUtils.setField(roomPriceService, "roomBlockoutRepository", roomBlockoutRepository);

        RoomPriceController controller = new RoomPriceController();
        ReflectionTestUtils.setField(controller, "roomPriceService", roomPriceService);
        MockMvc mockMvc = mockMvc(controller);

        when(roomTypeRepository.findByStoreIdAndId(STORE_ONE_ID, ROOM_TYPE_ID))
                .thenReturn(Optional.of(roomType(STORE_ONE_ID)));
        when(roomTypeRepository.findByStoreIdAndId(STORE_TWO_ID, ROOM_TYPE_ID))
                .thenReturn(Optional.of(roomType(STORE_TWO_ID)));
        when(roomTypePricePlanRepository.findByStoreIdWithRoomTypeAndPricePlan(STORE_ONE_ID))
                .thenReturn(List.of());
        when(roomTypePricePlanRepository.findByStoreIdWithRoomTypeAndPricePlan(STORE_TWO_ID))
                .thenReturn(List.of());
        when(roomPriceRepository.findByStoreIdAndPriceDateBetweenWithRoomTypeAndPricePlan(
                STORE_ONE_ID,
                PRICE_DATE,
                PRICE_DATE
        )).thenReturn(List.of());
        when(roomPriceRepository.findByStoreIdAndPriceDateBetweenWithRoomTypeAndPricePlan(
                STORE_TWO_ID,
                PRICE_DATE,
                PRICE_DATE
        )).thenReturn(List.of());
        when(channelPriceRepository.findByStoreIdAndDateRangeWithRelations(STORE_ONE_ID, PRICE_DATE, PRICE_DATE))
                .thenReturn(List.of());
        when(channelPriceRepository.findByStoreIdAndDateRangeWithRelations(STORE_TWO_ID, PRICE_DATE, PRICE_DATE))
                .thenReturn(List.of());
        when(reservationRepository.findByStoreIdAndDateRange(STORE_ONE_ID, PRICE_DATE, PRICE_DATE))
                .thenReturn(List.of());
        when(reservationRepository.findByStoreIdAndDateRange(STORE_TWO_ID, PRICE_DATE, PRICE_DATE))
                .thenReturn(List.of());
        when(roomRepository.findByStoreIdWithRoomType(STORE_ONE_ID)).thenReturn(List.of());
        when(roomRepository.findByStoreIdWithRoomType(STORE_TWO_ID)).thenReturn(List.of());

        StoreContextHolder.setContext(new StoreContext(7L, STORE_ONE_ID, "ADMIN"));
        mockMvc.perform(get("/api/v1/room-prices/management")
                        .param("startDate", "2026-04-06")
                        .param("endDate", "2026-04-06")
                        .param("roomTypeId", ROOM_TYPE_ID.toString())
                        .param("storeId", IGNORED_STORE_ID_PARAM))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取房价管理数据成功"))
                .andExpect(jsonPath("$.data[0].roomTypeId").value(ROOM_TYPE_ID))
                .andExpect(jsonPath("$.data[0].roomTypeName").value("门店1未绑定房型"))
                .andExpect(jsonPath("$.data[0].pricePlanId").value(nullValue()))
                .andExpect(jsonPath("$.data[0].pricePlanName").value(nullValue()))
                .andExpect(jsonPath("$.data[0].bindingState").value("UNBOUND"))
                .andExpect(jsonPath("$.data[0].editable").value(false))
                .andExpect(jsonPath("$.data[0].channelCount").value(0))
                .andExpect(jsonPath("$.data[0].channelRefs").isArray())
                .andExpect(jsonPath("$.data[0].channelRefs").isEmpty());

        StoreContextHolder.setContext(new StoreContext(8L, STORE_TWO_ID, "ADMIN"));
        mockMvc.perform(get("/api/v1/room-prices/management")
                        .param("startDate", "2026-04-06")
                        .param("endDate", "2026-04-06")
                        .param("roomTypeId", ROOM_TYPE_ID.toString())
                        .param("storeId", IGNORED_STORE_ID_PARAM))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].roomTypeName").value("门店2未绑定房型"))
                .andExpect(jsonPath("$.data[0].bindingState").value("UNBOUND"))
                .andExpect(jsonPath("$.data[0].editable").value(false));

        verify(roomTypeRepository).findByStoreIdAndId(STORE_ONE_ID, ROOM_TYPE_ID);
        verify(roomTypeRepository).findByStoreIdAndId(STORE_TWO_ID, ROOM_TYPE_ID);
        verify(roomTypeRepository, never()).findByStoreIdAndId(Long.valueOf(IGNORED_STORE_ID_PARAM), ROOM_TYPE_ID);
    }

    private RoomType roomType(Long storeId) {
        RoomType roomType = new RoomType();
        roomType.setId(ROOM_TYPE_ID);
        roomType.setStoreId(storeId);
        roomType.setName("门店" + storeId + "未绑定房型");
        roomType.setCode("UNBOUND-" + storeId);
        roomType.setTotalRooms(2);
        roomType.setDefaultPrice(new BigDecimal("50000"));
        return roomType;
    }

    private MockMvc mockMvc(RoomPriceController controller) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setConversionService(new DefaultFormattingConversionService())
                .setMessageConverters(jsonConverter())
                .build();
    }

    private MappingJackson2HttpMessageConverter jsonConverter() {
        ObjectMapper objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }
}
