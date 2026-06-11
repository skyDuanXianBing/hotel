package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.AssignableRoomsResponse;
import server.demo.dto.ReservationDTO;
import server.demo.entity.Channel;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.enums.OperationType;
import server.demo.enums.ReservationStatus;
import server.demo.enums.RoomStatus;
import server.demo.repository.ChannelRepository;
import server.demo.repository.OrderBoxRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.repository.UserRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationAssignRoomTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderBoxRepository orderBoxRepository;

    @Mock
    private AutoMessageTriggerService autoMessageTriggerService;

    @Mock
    private PriceLabsReservationSyncService priceLabsReservationSyncService;

    @Mock
    private CleaningTaskAutoService cleaningTaskAutoService;

    @Mock
    private OperationLogService operationLogService;

    @InjectMocks
    private ReservationService reservationService;

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void getAssignableRooms_shouldReturnOnlyRoomTypesWithAvailability_andRoomsForSelectedType() {
        StoreContextHolder.setContext(new StoreContext(10L, 7L, "ADMIN"));

        LocalDate checkIn = LocalDate.of(2026, 3, 4);
        LocalDate checkOut = LocalDate.of(2026, 3, 5);

        Reservation reservation = new Reservation();
        reservation.setId(100L);
        reservation.setStoreId(7L);
        reservation.setCheckInDate(checkIn);
        reservation.setCheckOutDate(checkOut);

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        RoomType rtA = new RoomType();
        rtA.setId(1L);
        rtA.setName("A");
        rtA.setCode("A");
        RoomType rtB = new RoomType();
        rtB.setId(2L);
        rtB.setName("B");
        rtB.setCode("B");

        Room r1 = new Room();
        r1.setId(11L);
        r1.setRoomNumber("101");
        r1.setRoomType(rtA);
        r1.setStatus(RoomStatus.AVAILABLE);

        Room r2 = new Room();
        r2.setId(12L);
        r2.setRoomNumber("102");
        r2.setRoomType(rtA);
        r2.setStatus(RoomStatus.AVAILABLE);

        Room r3 = new Room();
        r3.setId(21L);
        r3.setRoomNumber("201");
        r3.setRoomType(rtB);
        r3.setStatus(RoomStatus.AVAILABLE);

        when(roomRepository.findByStoreIdWithRoomType(7L)).thenReturn(List.of(r1, r2, r3));

        // r1 and r3 are occupied by other reservations in the same date range
        Reservation conflict1 = new Reservation();
        conflict1.setId(200L);
        conflict1.setStoreId(7L);
        conflict1.setRoom(r1);
        conflict1.setStatus(ReservationStatus.CONFIRMED);
        Reservation conflict2 = new Reservation();
        conflict2.setId(201L);
        conflict2.setStoreId(7L);
        conflict2.setRoom(r3);
        conflict2.setStatus(ReservationStatus.CHECKED_IN);

        when(reservationRepository.findByStoreIdAndRoomIdInAndDateRangeAndStatuses(
                eq(7L),
                anyList(),
                eq(checkIn),
                eq(checkOut),
                eq(Set.of(ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN, ReservationStatus.REQUESTED))
        )).thenReturn(List.of(conflict1, conflict2));

        AssignableRoomsResponse resp = reservationService.getAssignableRooms(100L, 1L);
        assertEquals(100L, resp.getReservationId());
        assertEquals("2026-03-04", resp.getCheckInDate());
        assertEquals("2026-03-05", resp.getCheckOutDate());

        // Only room type A has availability (room 102)
        assertEquals(1, resp.getRoomTypes().size());
        assertEquals(1L, resp.getRoomTypes().get(0).getId());
        assertEquals(1, resp.getRoomTypes().get(0).getAvailableRooms());

        // Rooms for selected type A: only r2
        assertEquals(1, resp.getRooms().size());
        assertEquals(12L, resp.getRooms().get(0).getId());
        assertEquals("102", resp.getRooms().get(0).getRoomNumber());
        assertEquals(1L, resp.getRooms().get(0).getRoomTypeId());
        assertEquals("A", resp.getRooms().get(0).getRoomTypeName());
    }

    @Test
    void assignRoom_shouldFailWhenConflicted() {
        StoreContextHolder.setContext(new StoreContext(10L, 7L, "ADMIN"));

        LocalDate checkIn = LocalDate.of(2026, 3, 4);
        LocalDate checkOut = LocalDate.of(2026, 3, 5);

        Reservation reservation = new Reservation();
        reservation.setId(100L);
        reservation.setStoreId(7L);
        reservation.setCheckInDate(checkIn);
        reservation.setCheckOutDate(checkOut);
        reservation.setStatus(ReservationStatus.CONFIRMED);

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        RoomType rt = new RoomType();
        rt.setId(1L);
        rt.setName("A");
        rt.setCode("A");
        Room room = new Room();
        room.setId(12L);
        room.setRoomNumber("102");
        room.setRoomType(rt);
        room.setStatus(RoomStatus.AVAILABLE);
        when(roomRepository.findByStoreIdAndIdWithRoomType(7L, 12L)).thenReturn(Optional.of(room));

        Reservation other = new Reservation();
        other.setId(200L);
        other.setStoreId(7L);
        other.setRoom(room);
        other.setStatus(ReservationStatus.CONFIRMED);
        when(reservationRepository.findByStoreIdAndRoomIdAndDateRange(7L, 12L, checkIn, checkOut))
                .thenReturn(List.of(other));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> reservationService.assignRoom(100L, 12L));
        assertNotNull(ex.getMessage());
    }

    @Test
    void assignRoom_shouldUpdateRoomAndWriteOperationLog() {
        StoreContextHolder.setContext(new StoreContext(10L, 7L, "ADMIN"));

        LocalDate checkIn = LocalDate.of(2026, 3, 4);
        LocalDate checkOut = LocalDate.of(2026, 3, 5);

        Channel channel = new Channel();
        channel.setId(1L);
        channel.setName("Su");

        Reservation reservation = new Reservation();
        reservation.setId(100L);
        reservation.setStoreId(7L);
        reservation.setCheckInDate(checkIn);
        reservation.setCheckOutDate(checkOut);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setChannel(channel);
        reservation.setGuestName("G");
        reservation.setGuestPhone("P");

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        RoomType rt = new RoomType();
        rt.setId(1L);
        rt.setName("A");
        rt.setCode("A");
        Room room = new Room();
        room.setId(12L);
        room.setRoomNumber("102");
        room.setRoomType(rt);
        room.setStatus(RoomStatus.AVAILABLE);
        when(roomRepository.findByStoreIdAndIdWithRoomType(7L, 12L)).thenReturn(Optional.of(room));
        when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(rt));

        when(reservationRepository.findByStoreIdAndRoomIdAndDateRange(7L, 12L, checkIn, checkOut))
                .thenReturn(List.of());

        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationDTO dto = reservationService.assignRoom(100L, 12L);
        assertEquals(12L, dto.getRoomId());
        assertEquals("102", dto.getRoomNumber());

        verify(operationLogService).logOperation(
                eq(100L),
                eq(OperationType.ORDER),
                eq("分配房间"),
                any(),
                any(),
                any(List.class)
        );
        verify(cleaningTaskAutoService).syncTaskForReservation(any(Reservation.class));
        verify(priceLabsReservationSyncService).pushReservationById(eq(7L), eq(100L));
    }
}
