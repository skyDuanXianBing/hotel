package server.demo.controller;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.entity.RoomStatusShare;
import server.demo.entity.Store;
import server.demo.repository.StoreRepository;
import server.demo.service.RoomStatusShareService;
import server.demo.service.RoomStatusService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoomStatusShareControllerBusinessDateTest {

    @Test
    void publicRoomStatusDefaultRange_shouldUseShareStoreTimezone() {
        RoomStatusShareService shareService = Mockito.mock(RoomStatusShareService.class);
        RoomStatusService roomStatusService = Mockito.mock(RoomStatusService.class);
        StoreRepository storeRepository = Mockito.mock(StoreRepository.class);

        RoomStatusShareController controller = new RoomStatusShareController();
        ReflectionTestUtils.setField(controller, "roomStatusShareService", shareService);
        ReflectionTestUtils.setField(controller, "roomStatusService", roomStatusService);
        ReflectionTestUtils.setField(controller, "storeRepository", storeRepository);
        ReflectionTestUtils.setField(
                controller,
                "clock",
                Clock.fixed(Instant.parse("2026-04-07T15:30:00Z"), ZoneOffset.UTC)
        );

        RoomStatusShare share = new RoomStatusShare();
        share.setUserId(7L);
        share.setStoreId(26L);

        Store store = new Store();
        store.setId(26L);
        store.setTimezone("Asia/Tokyo");

        when(shareService.getShareByToken("token")).thenReturn(share);
        when(storeRepository.findById(26L)).thenReturn(Optional.of(store));

        controller.getSharedRoomStatus("token", null, null);

        verify(roomStatusService).getRoomStatusCalendarForStore(
                26L,
                LocalDate.of(2026, 4, 8),
                LocalDate.of(2026, 4, 14)
        );
    }

    @Test
    void publicStatisticsDefaultDate_shouldUseShareStoreTimezone() {
        RoomStatusShareService shareService = Mockito.mock(RoomStatusShareService.class);
        RoomStatusService roomStatusService = Mockito.mock(RoomStatusService.class);
        StoreRepository storeRepository = Mockito.mock(StoreRepository.class);

        RoomStatusShareController controller = new RoomStatusShareController();
        ReflectionTestUtils.setField(controller, "roomStatusShareService", shareService);
        ReflectionTestUtils.setField(controller, "roomStatusService", roomStatusService);
        ReflectionTestUtils.setField(controller, "storeRepository", storeRepository);
        ReflectionTestUtils.setField(
                controller,
                "clock",
                Clock.fixed(Instant.parse("2026-04-07T15:30:00Z"), ZoneOffset.UTC)
        );

        RoomStatusShare share = new RoomStatusShare();
        share.setUserId(7L);
        share.setStoreId(26L);

        Store store = new Store();
        store.setId(26L);
        store.setTimezone("Asia/Tokyo");

        when(shareService.getShareByToken("token")).thenReturn(share);
        when(storeRepository.findById(26L)).thenReturn(Optional.of(store));

        controller.getSharedStatistics("token", null);

        verify(roomStatusService).getRoomStatusStatisticsForStore(26L, LocalDate.of(2026, 4, 8));
    }
}
