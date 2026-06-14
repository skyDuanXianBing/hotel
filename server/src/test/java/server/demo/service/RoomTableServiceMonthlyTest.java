package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.RoomTableMonthlyResponse;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.RoomBlockout;
import server.demo.entity.RoomPrice;
import server.demo.entity.RoomType;
import server.demo.enums.ReservationStatus;
import server.demo.enums.RoomBlockoutType;
import server.demo.enums.RoomStatus;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomBlockoutRepository;
import server.demo.repository.RoomPriceRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.repository.StoreRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class RoomTableServiceMonthlyTest {
    private static final Long USER_ID = 7L;
    private static final Long STORE_ID = 26L;
    private static final Long ROOM_TYPE_ID = 10L;

    private RoomTypeRepository roomTypeRepository;
    private RoomRepository roomRepository;
    private ReservationRepository reservationRepository;
    private RoomPriceRepository roomPriceRepository;
    private RoomBlockoutRepository roomBlockoutRepository;
    private StoreRepository storeRepository;
    private RoomTableService service;

    @BeforeEach
    void setUp() {
        StoreContextHolder.setContext(new StoreContext(USER_ID, STORE_ID, "ADMIN"));
        roomTypeRepository = Mockito.mock(RoomTypeRepository.class);
        roomRepository = Mockito.mock(RoomRepository.class);
        reservationRepository = Mockito.mock(ReservationRepository.class);
        roomPriceRepository = Mockito.mock(RoomPriceRepository.class);
        roomBlockoutRepository = Mockito.mock(RoomBlockoutRepository.class);
        storeRepository = Mockito.mock(StoreRepository.class);

        service = new RoomTableService();
        ReflectionTestUtils.setField(service, "roomTypeRepository", roomTypeRepository);
        ReflectionTestUtils.setField(service, "roomRepository", roomRepository);
        ReflectionTestUtils.setField(service, "reservationRepository", reservationRepository);
        ReflectionTestUtils.setField(service, "roomPriceRepository", roomPriceRepository);
        ReflectionTestUtils.setField(service, "roomBlockoutRepository", roomBlockoutRepository);
        ReflectionTestUtils.setField(service, "storeRepository", storeRepository);
    }

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void monthlyRoomTable_shouldSubtractUnassignedOtaAndKeepCheckoutDateExclusive() {
        LocalDate startDate = LocalDate.of(2026, 4, 1);
        LocalDate endDate = LocalDate.of(2026, 4, 2);
        RoomType roomType = roomType();
        Room room101 = room(101L, "101", roomType, RoomStatus.AVAILABLE);
        Room room102 = room(102L, "102", roomType, RoomStatus.AVAILABLE);

        Reservation checkoutExclusive = reservation(
                501L,
                room101,
                startDate.minusDays(1),
                startDate,
                ReservationStatus.CONFIRMED
        );

        mockBaseData(roomType, List.of(room101, room102), startDate, endDate);
        when(reservationRepository.findByStoreIdAndRoomIdInAndDateRangeAndStatusesWithChannel(
                eq(STORE_ID),
                eq(List.of(101L, 102L)),
                eq(startDate),
                eq(endDate),
                anySet()
        )).thenReturn(List.of(checkoutExclusive));
        when(reservationRepository.findMonthlyOccupancyRowsByStoreIdAndDateRangeAndStatuses(
                eq(STORE_ID),
                eq(startDate),
                eq(endDate.plusDays(1)),
                anySet()
        )).thenReturn(List.of(monthlyRow(startDate, startDate.plusDays(1), null, ROOM_TYPE_ID, null)));

        RoomTableMonthlyResponse response = service.getMonthlyRoomTable(startDate, endDate, null);

        RoomTableMonthlyResponse.MonthlyRoomTypeSummaryDTO firstDaySummary = summary(response, startDate);
        assertEquals(1, firstDaySummary.getEffectiveAvailableRooms());
        assertEquals(1, firstDaySummary.getUnassignedOccupiedRooms());
        assertEquals(0, firstDaySummary.getAssignedOccupiedRooms());
        assertEquals(1, countDisplayStatus(response, startDate, "AVAILABLE"));
        assertEquals(1, countDisplayStatus(response, startDate, "FULL"));

        RoomTableMonthlyResponse.MonthlyRoomTypeSummaryDTO checkoutDaySummary = summary(response, endDate);
        assertEquals(2, checkoutDaySummary.getEffectiveAvailableRooms());
        assertEquals(0, checkoutDaySummary.getUnassignedOccupiedRooms());
        assertEquals(2, countDisplayStatus(response, endDate, "AVAILABLE_MANY"));
    }

    @Test
    void monthlyRoomTable_shouldTreatBlockoutAndStaticUnavailableRoomsAsNotSellable() {
        LocalDate date = LocalDate.of(2026, 4, 1);
        RoomType roomType = roomType();
        Room room101 = room(101L, "101", roomType, RoomStatus.AVAILABLE);
        Room room102 = room(102L, "102", roomType, RoomStatus.MAINTENANCE);
        RoomBlockout blockout = new RoomBlockout(
                STORE_ID,
                room101,
                date,
                RoomBlockoutType.STOP,
                "维修"
        );

        mockBaseData(roomType, List.of(room101, room102), date, date);
        when(roomBlockoutRepository.findByStoreIdAndRoom_IdInAndBlockDateBetween(
                eq(STORE_ID),
                eq(List.of(101L, 102L)),
                eq(date),
                eq(date)
        )).thenReturn(List.of(blockout));

        RoomTableMonthlyResponse response = service.getMonthlyRoomTable(date, date, null);

        RoomTableMonthlyResponse.MonthlyRoomTypeSummaryDTO summary = summary(response, date);
        assertEquals(0, summary.getEffectiveAvailableRooms());
        assertEquals(1, summary.getBlockoutRooms());
        assertEquals(1, summary.getStaticUnavailableRooms());
        assertEquals(2, countDisplayStatus(response, date, "FULL"));
        assertTrue(dailyStatus(response, 101L, date).getClosed());
        assertEquals("stop", dailyStatus(response, 101L, date).getCloseType());
        assertEquals("MAINTENANCE", dailyStatus(response, 102L, date).getStatus());
    }

    @Test
    void monthlyRoomTable_shouldApplyAvailableRoomsOverrideAndCloseRoomRestriction() {
        LocalDate startDate = LocalDate.of(2026, 4, 1);
        LocalDate endDate = LocalDate.of(2026, 4, 2);
        RoomType roomType = roomType();
        Room room101 = room(101L, "101", roomType, RoomStatus.AVAILABLE);
        Room room102 = room(102L, "102", roomType, RoomStatus.AVAILABLE);
        RoomPrice limitedPrice = roomPrice(roomType, startDate);
        limitedPrice.setAvailableRooms(1);
        RoomPrice closedPrice = roomPrice(roomType, endDate);
        closedPrice.setCloseRoom(true);

        mockBaseData(roomType, List.of(room101, room102), startDate, endDate);
        when(roomPriceRepository.findByStoreIdAndPriceDateBetweenWithRoomTypeAndPricePlan(
                STORE_ID,
                startDate,
                endDate
        )).thenReturn(List.of(limitedPrice, closedPrice));

        RoomTableMonthlyResponse response = service.getMonthlyRoomTable(startDate, endDate, null);

        RoomTableMonthlyResponse.MonthlyRoomTypeSummaryDTO limitedSummary = summary(response, startDate);
        assertEquals(1, limitedSummary.getInventoryLimit());
        assertEquals(1, limitedSummary.getEffectiveAvailableRooms());
        assertEquals(1, countDisplayStatus(response, startDate, "AVAILABLE"));
        assertEquals(1, countDisplayStatus(response, startDate, "FULL"));

        RoomTableMonthlyResponse.MonthlyRoomTypeSummaryDTO closedSummary = summary(response, endDate);
        assertTrue(closedSummary.getCloseRoom());
        assertEquals(0, closedSummary.getEffectiveAvailableRooms());
        assertEquals(2, countDisplayStatus(response, endDate, "FULL"));
        assertFalse(Boolean.TRUE.equals(dailyStatus(response, 101L, endDate).getSellable()));
    }

    private void mockBaseData(
            RoomType roomType,
            List<Room> rooms,
            LocalDate startDate,
            LocalDate endDate
    ) {
        when(roomTypeRepository.findByStoreIdOrderByName(STORE_ID)).thenReturn(List.of(roomType));
        when(roomRepository.findByStoreIdWithRoomType(STORE_ID)).thenReturn(rooms);
        when(reservationRepository.findByStoreIdAndRoomIdInAndDateRangeAndStatusesWithChannel(
                eq(STORE_ID),
                eq(rooms.stream().map(Room::getId).toList()),
                eq(startDate),
                eq(endDate),
                anySet()
        )).thenReturn(List.of());
        when(reservationRepository.findMonthlyOccupancyRowsByStoreIdAndDateRangeAndStatuses(
                eq(STORE_ID),
                eq(startDate),
                eq(endDate.plusDays(1)),
                anySet()
        )).thenReturn(List.of());
        when(roomBlockoutRepository.findByStoreIdAndRoom_IdInAndBlockDateBetween(
                eq(STORE_ID),
                eq(rooms.stream().map(Room::getId).toList()),
                eq(startDate),
                eq(endDate)
        )).thenReturn(List.of());
        when(roomPriceRepository.findByStoreIdAndPriceDateBetweenWithRoomTypeAndPricePlan(
                STORE_ID,
                startDate,
                endDate
        )).thenReturn(List.of());
    }

    private RoomType roomType() {
        RoomType roomType = new RoomType();
        roomType.setId(ROOM_TYPE_ID);
        roomType.setStoreId(STORE_ID);
        roomType.setName("标准房");
        roomType.setCode("STD");
        roomType.setTotalRooms(2);
        return roomType;
    }

    private Room room(Long id, String roomNumber, RoomType roomType, RoomStatus status) {
        Room room = new Room();
        room.setId(id);
        room.setStoreId(STORE_ID);
        room.setRoomNumber(roomNumber);
        room.setRoomType(roomType);
        room.setStatus(status);
        return room;
    }

    private Reservation reservation(
            Long id,
            Room room,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            ReservationStatus status
    ) {
        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setStoreId(STORE_ID);
        reservation.setGuestName("测试客人");
        reservation.setRoom(room);
        reservation.setCheckInDate(checkInDate);
        reservation.setCheckOutDate(checkOutDate);
        reservation.setStatus(status);
        return reservation;
    }

    private RoomPrice roomPrice(RoomType roomType, LocalDate date) {
        RoomPrice roomPrice = new RoomPrice();
        roomPrice.setStoreId(STORE_ID);
        roomPrice.setRoomType(roomType);
        roomPrice.setPriceDate(date);
        roomPrice.setPrice(new BigDecimal("100.00"));
        return roomPrice;
    }

    private ReservationRepository.MonthlyReservationOccupancyRow monthlyRow(
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Long roomId,
            Long otaRoomTypeId,
            Long assignedRoomTypeId
    ) {
        return new ReservationRepository.MonthlyReservationOccupancyRow() {
            @Override
            public LocalDate getCheckInDate() {
                return checkInDate;
            }

            @Override
            public LocalDate getCheckOutDate() {
                return checkOutDate;
            }

            @Override
            public Long getRoomId() {
                return roomId;
            }

            @Override
            public Long getOtaRoomTypeId() {
                return otaRoomTypeId;
            }

            @Override
            public Long getAssignedRoomTypeId() {
                return assignedRoomTypeId;
            }
        };
    }

    private RoomTableMonthlyResponse.MonthlyRoomTypeSummaryDTO summary(
            RoomTableMonthlyResponse response,
            LocalDate date
    ) {
        return response.getRoomTypeSummaries().stream()
                .filter(item -> date.equals(item.getDate()))
                .findFirst()
                .orElseThrow();
    }

    private long countDisplayStatus(
            RoomTableMonthlyResponse response,
            LocalDate date,
            String displayStatus
    ) {
        return response.getRooms().stream()
                .flatMap(room -> room.getDailyStatus().stream())
                .filter(status -> date.equals(status.getDate()))
                .filter(status -> displayStatus.equals(status.getDisplayStatus()))
                .count();
    }

    private RoomTableMonthlyResponse.MonthlyDailyStatusDTO dailyStatus(
            RoomTableMonthlyResponse response,
            Long roomId,
            LocalDate date
    ) {
        return response.getRooms().stream()
                .filter(room -> roomId.equals(room.getRoomId()))
                .flatMap(room -> room.getDailyStatus().stream())
                .filter(status -> date.equals(status.getDate()))
                .findFirst()
                .orElseThrow();
    }
}
