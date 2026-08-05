package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import server.demo.entity.Reservation;
import server.demo.entity.SuMessageThread;
import server.demo.repository.SuMessageThreadRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P4 聚焦测试：订单落库时的消息线程 upsert 仅对 Su 官方支持消息的渠道建线程。
 * EXPEDIA(9) 以 bookingid 为会话键建线程；TRIP(339)/AGODA(189) 官方不支持消息，不建线程。
 */
class OtaReservationSyncServiceMessageThreadUpsertTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void tryUpsertMessageThread_expediaCreatesBookingIdKeyedThread() throws Exception {
        SuMessageThreadRepository threadRepository = mock(SuMessageThreadRepository.class);
        OtaReservationSyncService service = createService(threadRepository);

        Reservation reservation = new Reservation();
        reservation.setStoreId(10L);
        reservation.setChannelOrderNumber("EXP-123456");
        reservation.setOtaRoomId("778899");
        reservation.setGuestName("Grace");

        JsonNode reservationNode = OBJECT_MAPPER.readTree("{}");
        when(threadRepository.findByStoreIdAndChannelIdAndThreadKey(10L, 9, "EXP-123456"))
                .thenReturn(Optional.empty());
        when(threadRepository.save(any(SuMessageThread.class))).thenAnswer(inv -> inv.getArgument(0));

        service.tryUpsertMessageThreadFromReservation(10L, "HOTEL1", "EXPEDIA", reservationNode, null, reservation);

        ArgumentCaptor<SuMessageThread> captor = ArgumentCaptor.forClass(SuMessageThread.class);
        verify(threadRepository).save(captor.capture());
        SuMessageThread thread = captor.getValue();
        assertEquals(10L, thread.getStoreId());
        assertEquals("HOTEL1", thread.getSuHotelId());
        assertEquals(Integer.valueOf(9), thread.getChannelId());
        // Expedia 官方 bookingid 必填，与 Booking 同以 bookingid 为会话键
        assertEquals("EXP-123456", thread.getThreadKey());
        assertEquals("EXP-123456", thread.getBookingId());
        // listingid 走通用策略：webhook 无 channel_room_id 时回退 ota_room_id
        assertEquals("778899", thread.getListingId());
        assertEquals("Grace", thread.getGuestName());
    }

    @Test
    void tryUpsertMessageThread_tripAndAgodaNeverTouchThreadRepository() throws Exception {
        SuMessageThreadRepository threadRepository = mock(SuMessageThreadRepository.class);
        OtaReservationSyncService service = createService(threadRepository);
        JsonNode reservationNode = OBJECT_MAPPER.readTree("{}");

        Reservation tripReservation = new Reservation();
        tripReservation.setStoreId(10L);
        tripReservation.setChannelOrderNumber("TRIP-1");
        tripReservation.setOtaRoomId("778899");

        Reservation agodaReservation = new Reservation();
        agodaReservation.setStoreId(10L);
        agodaReservation.setChannelOrderNumber("AGODA-1");
        agodaReservation.setOtaRoomId("778899");

        service.tryUpsertMessageThreadFromReservation(10L, "HOTEL1", "TRIP", reservationNode, null, tripReservation);
        service.tryUpsertMessageThreadFromReservation(10L, "HOTEL1", "AGODA", reservationNode, null, agodaReservation);
        service.tryUpsertMessageThreadFromReservation(10L, "HOTEL1", "CTRIP", reservationNode, null, tripReservation);

        verify(threadRepository, never()).save(any());
        verify(threadRepository, never()).findByStoreIdAndChannelIdAndThreadKey(any(), any(), any());
    }

    @Test
    void tryUpsertMessageThread_expediaWithoutBookingIdSkips() throws Exception {
        SuMessageThreadRepository threadRepository = mock(SuMessageThreadRepository.class);
        OtaReservationSyncService service = createService(threadRepository);
        JsonNode reservationNode = OBJECT_MAPPER.readTree("{}");

        Reservation reservation = new Reservation();
        reservation.setStoreId(10L);
        reservation.setOtaRoomId("778899");

        service.tryUpsertMessageThreadFromReservation(10L, "HOTEL1", "EXPEDIA", reservationNode, null, reservation);

        verify(threadRepository, never()).save(any());
        assertNull(reservation.getChannelOrderNumber());
    }

    private static OtaReservationSyncService createService(SuMessageThreadRepository threadRepository) {
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        return new OtaReservationSyncService(
                null,
                null,
                null,
                null,
                null,
                null,
                threadRepository,
                null,
                null,
                transactionManager,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
