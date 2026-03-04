package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.ReservationDTO;
import server.demo.entity.Channel;
import server.demo.entity.Reservation;
import server.demo.repository.ChannelRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.repository.UserRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationSearchTest {

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
    void searchReservationsByGuestInfo_shouldReturnEmptyWhenKeywordBlank() {
        StoreContextHolder.setContext(new StoreContext(11L, 7L, "ADMIN"));

        List<ReservationDTO> result = reservationService.searchReservationsByGuestInfo("   ");

        assertTrue(result.isEmpty());
        verifyNoInteractions(reservationRepository);
    }

    @Test
    void searchReservationsByGuestInfo_shouldUseStoreScopeAndTrimmedKeyword() {
        StoreContextHolder.setContext(new StoreContext(11L, 7L, "ADMIN"));

        Channel channel = new Channel();
        channel.setId(2L);
        channel.setName("渠道A");

        Reservation reservation = new Reservation();
        reservation.setId(1001L);
        reservation.setStoreId(7L);
        reservation.setGuestName("张三");
        reservation.setGuestPhone("13800000000");
        reservation.setOrderNumber("RSV-001");
        reservation.setChannelOrderNumber("CH-001");
        reservation.setChannel(channel);

        when(reservationRepository.searchByStoreIdAndKeyword(7L, "RSV-001"))
                .thenReturn(List.of(reservation));

        List<ReservationDTO> result = reservationService.searchReservationsByGuestInfo("  RSV-001  ");

        assertEquals(1, result.size());
        assertEquals(1001L, result.get(0).getId());
        assertEquals("RSV-001", result.get(0).getOrderNumber());
        assertEquals("CH-001", result.get(0).getChannelOrderNumber());
        verify(reservationRepository).searchByStoreIdAndKeyword(7L, "RSV-001");
    }
}
