package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.RoomTypeDeleteBlockInfo;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.enums.ReservationStatus;
import server.demo.exception.RoomTypeDeleteBlockedException;
import server.demo.repository.ChannelPriceRepository;
import server.demo.repository.CleaningTaskRepository;
import server.demo.repository.PriceLabsConnectionRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomBlockoutRepository;
import server.demo.repository.RoomPriceRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypePricePlanRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.repository.PriceChangeHistoryRepository;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoomTypeServiceDeleteRoomTypeBlockedTest {

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void deleteRoomType_shouldReturnBlockingReservationList() {
        StoreContextHolder.setContext(new StoreContext(1L, 7L, "OWNER"));

        RoomTypeRepository roomTypeRepository = Mockito.mock(RoomTypeRepository.class);
        RoomRepository roomRepository = Mockito.mock(RoomRepository.class);
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);

        RoomTypeService service = new RoomTypeService();
        ReflectionTestUtils.setField(service, "roomTypeRepository", roomTypeRepository);
        ReflectionTestUtils.setField(service, "roomRepository", roomRepository);
        ReflectionTestUtils.setField(service, "reservationRepository", reservationRepository);

        RoomType roomType = new RoomType();
        roomType.setId(40L);
        roomType.setStoreId(7L);

        Room room = new Room();
        room.setId(100L);
        room.setRoomNumber("101");

        Reservation reservation = new Reservation();
        reservation.setOrderNumber("ORDER-1");
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setCheckInDate(LocalDate.now().plusDays(1));
        reservation.setCheckOutDate(LocalDate.now().plusDays(2));
        reservation.setRoom(room);

        when(roomTypeRepository.findById(40L)).thenReturn(Optional.of(roomType));
        when(roomRepository.findByStoreIdAndRoomTypeId(7L, 40L)).thenReturn(List.of(room));
        when(reservationRepository.findByStoreIdAndRoomIdInAndDateRangeAndStatuses(
                Mockito.eq(7L),
                Mockito.eq(List.of(100L)),
                Mockito.any(LocalDate.class),
                Mockito.any(LocalDate.class),
                Mockito.anySet()
        )).thenReturn(List.of(reservation));

        RoomTypeDeleteBlockedException ex = assertThrows(RoomTypeDeleteBlockedException.class, () -> service.deleteRoomType(40L));
        RoomTypeDeleteBlockInfo info = ex.getBlockInfo();
        assertNotNull(info);
        assertEquals(1, info.getTotalBlockingReservations());
        assertEquals(1, info.getSample().size());
        assertEquals("ORDER-1", info.getSample().get(0).getOrderNumber());
        assertEquals(ReservationStatus.CONFIRMED, info.getSample().get(0).getStatus());
        assertEquals("101", info.getSample().get(0).getRoomNumber());

        // should not proceed to delete
        verify(roomRepository, never()).deleteAll(Mockito.anyList());
    }

    @Test
    void deleteRoomType_shouldDeleteRoomBlockoutsBeforeDeletingRooms() {
        StoreContextHolder.setContext(new StoreContext(1L, 7L, "OWNER"));

        RoomTypeRepository roomTypeRepository = Mockito.mock(RoomTypeRepository.class);
        RoomRepository roomRepository = Mockito.mock(RoomRepository.class);
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);
        CleaningTaskRepository cleaningTaskRepository = Mockito.mock(CleaningTaskRepository.class);
        RoomBlockoutRepository roomBlockoutRepository = Mockito.mock(RoomBlockoutRepository.class);
        ChannelPriceRepository channelPriceRepository = Mockito.mock(ChannelPriceRepository.class);
        PriceLabsConnectionRepository priceLabsConnectionRepository = Mockito.mock(PriceLabsConnectionRepository.class);
        RoomTypePricePlanRepository roomTypePricePlanRepository = Mockito.mock(RoomTypePricePlanRepository.class);
        RoomPriceRepository roomPriceRepository = Mockito.mock(RoomPriceRepository.class);
        PriceChangeHistoryRepository priceChangeHistoryRepository = Mockito.mock(PriceChangeHistoryRepository.class);

        RoomTypeService service = new RoomTypeService();
        ReflectionTestUtils.setField(service, "roomTypeRepository", roomTypeRepository);
        ReflectionTestUtils.setField(service, "roomRepository", roomRepository);
        ReflectionTestUtils.setField(service, "reservationRepository", reservationRepository);
        ReflectionTestUtils.setField(service, "cleaningTaskRepository", cleaningTaskRepository);
        ReflectionTestUtils.setField(service, "roomBlockoutRepository", roomBlockoutRepository);
        ReflectionTestUtils.setField(service, "channelPriceRepository", channelPriceRepository);
        ReflectionTestUtils.setField(service, "priceLabsConnectionRepository", priceLabsConnectionRepository);
        ReflectionTestUtils.setField(service, "roomTypePricePlanRepository", roomTypePricePlanRepository);
        ReflectionTestUtils.setField(service, "roomPriceRepository", roomPriceRepository);
        ReflectionTestUtils.setField(service, "priceChangeHistoryRepository", priceChangeHistoryRepository);
        ReflectionTestUtils.setField(service, "suRoomTypeAutoSyncEnabled", false);

        RoomType roomType = new RoomType();
        roomType.setId(40L);
        roomType.setStoreId(7L);

        Room room = new Room();
        room.setId(100L);
        room.setRoomNumber("101");

        when(roomTypeRepository.findById(40L)).thenReturn(Optional.of(roomType));
        when(roomRepository.findByStoreIdAndRoomTypeId(7L, 40L)).thenReturn(List.of(room));
        when(reservationRepository.findByStoreIdAndRoomIdInAndDateRangeAndStatuses(
                Mockito.eq(7L),
                Mockito.eq(List.of(100L)),
                Mockito.any(LocalDate.class),
                Mockito.any(LocalDate.class),
                Mockito.anySet()
        )).thenReturn(List.of());

        service.deleteRoomType(40L);

        verify(reservationRepository).clearRoomBindingByStoreIdAndRoomIds(7L, List.of(100L));
        verify(cleaningTaskRepository).deleteByRoomIdIn(new HashSet<>(List.of(100L)));
        verify(roomBlockoutRepository).deleteByStoreIdAndRoom_IdIn(7L, List.of(100L));
        verify(channelPriceRepository).deleteByStoreIdAndRoomTypeId(7L, 40L);
        verify(priceLabsConnectionRepository).deleteByStoreIdAndRoomTypeId(7L, 40L);
        verify(priceChangeHistoryRepository).deleteByRoomTypeId(40L);
        verify(roomPriceRepository).deleteByRoomTypeId(40L);
        verify(roomTypePricePlanRepository).deleteByRoomTypeId(40L);
        verify(roomRepository).deleteAll(List.of(room));
        verify(roomTypeRepository).deleteById(40L);
    }
}
