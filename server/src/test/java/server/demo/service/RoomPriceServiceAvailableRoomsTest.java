package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.RoomPriceManagementDTO;
import server.demo.entity.PricePlan;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.RoomPrice;
import server.demo.entity.RoomType;
import server.demo.entity.RoomTypePricePlan;
import server.demo.enums.ReservationStatus;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class RoomPriceServiceAvailableRoomsTest {

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void getRoomPriceManagementData_shouldSubtractOccupancyFromManualAvailableRooms() {
        StoreContextHolder.setContext(new StoreContext(1L, 1L, "TEST"));

        RoomPriceRepository roomPriceRepository = Mockito.mock(RoomPriceRepository.class);
        RoomTypeRepository roomTypeRepository = Mockito.mock(RoomTypeRepository.class);
        PricePlanRepository pricePlanRepository = Mockito.mock(PricePlanRepository.class);
        RoomTypePricePlanRepository roomTypePricePlanRepository = Mockito.mock(RoomTypePricePlanRepository.class);
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);
        RoomRepository roomRepository = Mockito.mock(RoomRepository.class);
        RoomBlockoutRepository roomBlockoutRepository = Mockito.mock(RoomBlockoutRepository.class);

        RoomPriceServiceImpl service = new RoomPriceServiceImpl();
        ReflectionTestUtils.setField(service, "roomPriceRepository", roomPriceRepository);
        ReflectionTestUtils.setField(service, "roomTypeRepository", roomTypeRepository);
        ReflectionTestUtils.setField(service, "pricePlanRepository", pricePlanRepository);
        ReflectionTestUtils.setField(service, "roomTypePricePlanRepository", roomTypePricePlanRepository);
        ReflectionTestUtils.setField(service, "reservationRepository", reservationRepository);
        ReflectionTestUtils.setField(service, "roomRepository", roomRepository);
        ReflectionTestUtils.setField(service, "roomBlockoutRepository", roomBlockoutRepository);
        ReflectionTestUtils.setField(service, "priceLabsCalendarSyncDebouncer", null);

        LocalDate date = LocalDate.of(2026, 4, 6);

        RoomType roomType = new RoomType();
        roomType.setId(59L);
        roomType.setStoreId(1L);
        roomType.setName("楽途ホテル　東十条　趣　0101");
        roomType.setCode("010");
        roomType.setTotalRooms(1);
        roomType.setDefaultPrice(new BigDecimal("60000"));

        Room room = new Room();
        room.setId(118L);
        room.setRoomNumber("0101");
        room.setRoomType(roomType);

        PricePlan pricePlan = new PricePlan();
        pricePlan.setId(18L);
        pricePlan.setStoreId(1L);
        pricePlan.setName("标准定价");

        RoomTypePricePlan roomTypePricePlan = new RoomTypePricePlan();
        roomTypePricePlan.setId(88L);
        roomTypePricePlan.setStoreId(1L);
        roomTypePricePlan.setRoomType(roomType);
        roomTypePricePlan.setPricePlan(pricePlan);
        roomTypePricePlan.setMondayPrice(new BigDecimal("60000"));

        RoomPrice roomPrice = new RoomPrice();
        roomPrice.setId(9092L);
        roomPrice.setStoreId(1L);
        roomPrice.setRoomType(roomType);
        roomPrice.setPricePlan(pricePlan);
        roomPrice.setPriceDate(date);
        roomPrice.setPrice(new BigDecimal("60000"));
        roomPrice.setAvailableRooms(1);

        Reservation reservation = new Reservation();
        reservation.setId(405L);
        reservation.setStatus(ReservationStatus.REQUESTED);
        reservation.setCheckInDate(date);
        reservation.setCheckOutDate(date.plusDays(2));
        reservation.setRoom(room);

        when(roomTypeRepository.findByStoreIdAndId(1L, 59L)).thenReturn(Optional.of(roomType));
        when(roomTypePricePlanRepository.findByStoreIdWithRoomTypeAndPricePlan(1L)).thenReturn(List.of(roomTypePricePlan));
        when(roomPriceRepository.findByStoreIdAndPriceDateBetweenWithRoomTypeAndPricePlan(1L, date, date)).thenReturn(List.of(roomPrice));
        when(reservationRepository.findByStoreIdAndDateRange(1L, date, date)).thenReturn(List.of(reservation));
        when(roomRepository.findByStoreIdWithRoomType(1L)).thenReturn(List.of(room));
        when(roomBlockoutRepository.findByStoreIdAndRoom_IdInAndBlockDateBetween(1L, List.of(118L), date, date))
                .thenReturn(List.of());

        List<RoomPriceManagementDTO> result = service.getRoomPriceManagementData(date, date, 59L);

        assertEquals(1, result.size());
        assertEquals(0, result.get(0).getAvailableRooms());
    }

    @Test
    void getRoomPriceByPlan_shouldReturnEffectiveAvailableRooms() {
        StoreContextHolder.setContext(new StoreContext(1L, 1L, "TEST"));

        RoomPriceRepository roomPriceRepository = Mockito.mock(RoomPriceRepository.class);
        RoomTypeRepository roomTypeRepository = Mockito.mock(RoomTypeRepository.class);
        PricePlanRepository pricePlanRepository = Mockito.mock(PricePlanRepository.class);
        RoomTypePricePlanRepository roomTypePricePlanRepository = Mockito.mock(RoomTypePricePlanRepository.class);
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);
        RoomRepository roomRepository = Mockito.mock(RoomRepository.class);
        RoomBlockoutRepository roomBlockoutRepository = Mockito.mock(RoomBlockoutRepository.class);

        RoomPriceServiceImpl service = new RoomPriceServiceImpl();
        ReflectionTestUtils.setField(service, "roomPriceRepository", roomPriceRepository);
        ReflectionTestUtils.setField(service, "roomTypeRepository", roomTypeRepository);
        ReflectionTestUtils.setField(service, "pricePlanRepository", pricePlanRepository);
        ReflectionTestUtils.setField(service, "roomTypePricePlanRepository", roomTypePricePlanRepository);
        ReflectionTestUtils.setField(service, "reservationRepository", reservationRepository);
        ReflectionTestUtils.setField(service, "roomRepository", roomRepository);
        ReflectionTestUtils.setField(service, "roomBlockoutRepository", roomBlockoutRepository);

        LocalDate date = LocalDate.of(2026, 4, 6);

        RoomType roomType = new RoomType();
        roomType.setId(59L);
        roomType.setStoreId(1L);
        roomType.setName("楽途ホテル　東十条　趣　0101");
        roomType.setCode("010");
        roomType.setTotalRooms(1);

        Room room = new Room();
        room.setId(118L);
        room.setRoomNumber("0101");
        room.setRoomType(roomType);

        PricePlan pricePlan = new PricePlan();
        pricePlan.setId(18L);
        pricePlan.setStoreId(1L);
        pricePlan.setName("标准定价");

        RoomPrice roomPrice = new RoomPrice();
        roomPrice.setId(9092L);
        roomPrice.setStoreId(1L);
        roomPrice.setRoomType(roomType);
        roomPrice.setPricePlan(pricePlan);
        roomPrice.setPriceDate(date);
        roomPrice.setPrice(new BigDecimal("60000"));
        roomPrice.setAvailableRooms(1);

        Reservation reservation = new Reservation();
        reservation.setId(405L);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setCheckInDate(date);
        reservation.setCheckOutDate(date.plusDays(1));
        reservation.setRoom(room);

        when(roomTypeRepository.findByStoreIdAndId(1L, 59L)).thenReturn(Optional.of(roomType));
        when(pricePlanRepository.findByStoreIdAndId(1L, 18L)).thenReturn(Optional.of(pricePlan));
        when(roomPriceRepository.findByRoomTypeIdAndPricePlanIdAndPriceDate(59L, 18L, date)).thenReturn(Optional.of(roomPrice));
        when(reservationRepository.findByStoreIdAndRoomTypeIdOverlappingDateRangeWithRoomType(1L, 59L, date, date))
                .thenReturn(List.of(reservation));
        when(roomRepository.findByStoreIdAndRoomTypeId(1L, 59L)).thenReturn(List.of(room));
        when(roomBlockoutRepository.findByStoreIdAndRoom_IdInAndBlockDateBetween(eq(1L), eq(List.of(118L)), eq(date), eq(date)))
                .thenReturn(List.of());

        Optional<RoomPriceManagementDTO> result = service.getRoomPriceByPlan(59L, 18L, date);

        assertTrue(result.isPresent());
        assertEquals(0, result.get().getAvailableRooms());
    }
}
