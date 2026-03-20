package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.CreateReservationRequest;
import server.demo.entity.Channel;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.entity.User;
import server.demo.enums.ReservationStatus;
import server.demo.repository.ChannelRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServicePriceLabsCalendarSyncTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private ChannelRepository channelRepository;
    @Mock private RoomTypeRepository roomTypeRepository;
    @Mock private UserRepository userRepository;
    @Mock private AutoMessageTriggerService autoMessageTriggerService;
    @Mock private PriceLabsReservationSyncService priceLabsReservationSyncService;
    @Mock private PriceLabsCalendarSyncDebouncer priceLabsCalendarSyncDebouncer;
    @Mock private CleaningTaskAutoService cleaningTaskAutoService;
    @Mock private OperationLogService operationLogService;
    @Mock private OrderNotificationDispatchService orderNotificationDispatchService;

    @InjectMocks
    private ReservationService reservationService;

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void createReservation_shouldTriggerPriceLabsCalendarSyncForNightsOnly() {
        StoreContextHolder.setContext(new StoreContext(7L, 25L, "ADMIN"));

        RoomType roomType = new RoomType();
        roomType.setId(45L);
        roomType.setName("RT");
        roomType.setCode("RT");

        Room room = new Room();
        room.setId(100L);
        room.setStoreId(25L);
        room.setUserId(7L);
        room.setRoomNumber("101");
        room.setRoomType(roomType);

        Channel channel = new Channel();
        channel.setId(1L);

        User user = new User();
        user.setId(7L);

        LocalDate checkIn = LocalDate.of(2026, 3, 17);
        LocalDate checkOut = LocalDate.of(2026, 3, 19);

        CreateReservationRequest req = new CreateReservationRequest();
        req.setGuestName("Test");
        req.setRoomId(100L);
        req.setChannelId(1L);
        req.setCheckInDate(checkIn);
        req.setCheckOutDate(checkOut);
        req.setAdults(1);
        req.setChildren(0);
        req.setTotalAmount(new BigDecimal("100.00"));

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(roomRepository.findByStoreIdAndId(25L, 100L)).thenReturn(Optional.of(room));
        when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
        when(roomTypeRepository.findById(45L)).thenReturn(Optional.of(roomType));
        when(reservationRepository.findByStoreIdAndRoomIdAndDateRange(25L, 100L, checkIn, checkOut)).thenReturn(List.of());

        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> {
            Reservation saved = inv.getArgument(0);
            saved.setId(999L);
            return saved;
        });

        reservationService.createReservation(req);

        // nights-only: 2026-03-17 ~ 2026-03-18 (checkOut-1)
        verify(priceLabsCalendarSyncDebouncer).requestSyncAfterCommit(
                eq(25L),
                eq(45L),
                eq(LocalDate.of(2026, 3, 17)),
                eq(LocalDate.of(2026, 3, 18))
        );
    }

    @Test
    void checkOut_shouldEnsureCheckoutCleaningTask() {
        StoreContextHolder.setContext(new StoreContext(7L, 25L, "ADMIN"));

        RoomType roomType = new RoomType();
        roomType.setId(45L);
        roomType.setName("RT");

        Room room = new Room();
        room.setId(100L);
        room.setStoreId(25L);
        room.setRoomNumber("101");
        room.setRoomType(roomType);

        Reservation reservation = new Reservation();
        reservation.setId(999L);
        reservation.setStoreId(25L);
        reservation.setRoom(room);
        Channel channel = new Channel();
        channel.setId(1L);
        channel.setName("渠道A");
        reservation.setChannel(channel);
        reservation.setGuestName("测试客人");
        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservation.setCheckInDate(LocalDate.of(2026, 3, 17));
        reservation.setCheckOutDate(LocalDate.of(2026, 3, 19));

        when(reservationRepository.findById(999L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roomTypeRepository.findById(45L)).thenReturn(Optional.of(roomType));

        reservationService.checkOut(999L);

        verify(cleaningTaskAutoService).ensureCheckoutTaskForReservation(any(Reservation.class));
    }
}
