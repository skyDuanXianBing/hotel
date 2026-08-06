package server.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.PageRequest;
import server.demo.entity.SuReservationWebhookEvent;
import server.demo.enums.SuWebhookEventStatus;
import server.demo.enums.SuWebhookEventType;
import server.demo.repository.SuReservationWebhookEventRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 补偿事件单事件处理器：独立事务边界内的事件处理与失败留痕。
 */
class SuReservationWebhookEventProcessorTest {

    private SuReservationWebhookEventRepository eventRepository;
    private OtaReservationSyncService otaReservationSyncService;
    private SuReservationWebhookEventProcessor processor;

    @BeforeEach
    void setUp() {
        eventRepository = Mockito.mock(SuReservationWebhookEventRepository.class);
        otaReservationSyncService = Mockito.mock(OtaReservationSyncService.class);
        processor = new SuReservationWebhookEventProcessor(eventRepository, new ObjectMapper(), otaReservationSyncService);
    }

    private static SuReservationWebhookEvent newEvent(Long id, SuWebhookEventType type) {
        SuReservationWebhookEvent e = new SuReservationWebhookEvent();
        ReflectionTestUtils.setField(e, "id", id);
        e.setStoreId(7L);
        e.setHotelId("H1");
        e.setReservationNotifId("N1");
        e.setEventType(type);
        e.setStatus(SuWebhookEventStatus.RECEIVED);
        e.setRetryCount(0);
        return e;
    }

    @Test
    void processDueEvent_pullSuccess_marksProcessed() {
        SuReservationWebhookEvent e = newEvent(11L, SuWebhookEventType.PULL);
        when(eventRepository.findById(11L)).thenReturn(Optional.of(e));
        when(otaReservationSyncService.pullAndUpsertReservationsWithoutAck(eq(7L), eq(Set.of("N1"))))
                .thenReturn(new OtaReservationSyncService.PullUpsertResult(
                        7L, "H1", 1, 1, 0, 1, 0, 0, Set.of("N1"), List.of()));

        processor.processDueEventInNewTransaction(11L);

        assertEquals(SuWebhookEventStatus.PROCESSED, e.getStatus());
        verify(eventRepository, times(2)).save(e);
    }

    @Test
    void processDueEvent_pullMissingNotifId_throwsAndDoesNotMarkProcessed() {
        SuReservationWebhookEvent e = newEvent(12L, SuWebhookEventType.PULL);
        when(eventRepository.findById(12L)).thenReturn(Optional.of(e));
        when(otaReservationSyncService.pullAndUpsertReservationsWithoutAck(eq(7L), eq(Set.of("N1"))))
                .thenReturn(new OtaReservationSyncService.PullUpsertResult(
                        7L, "H1", 0, 0, 0, 0, 0, 1, Set.of(), List.of("pull failed")));

        assertThrows(RuntimeException.class, () -> processor.processDueEventInNewTransaction(12L));

        assertEquals(SuWebhookEventStatus.PROCESSING, e.getStatus());
    }

    @Test
    void processDueEvent_pushSuccess_marksProcessed() throws Exception {
        SuReservationWebhookEvent e = newEvent(13L, SuWebhookEventType.PUSH);
        e.setPayloadJson(new ObjectMapper().readTree("{\"reservation_notif_id\":\"N1\"}").toString());
        when(eventRepository.findById(13L)).thenReturn(Optional.of(e));
        when(otaReservationSyncService.upsertReservationsFromWebhook(eq(7L), anyList()))
                .thenReturn(new OtaReservationSyncService.UpsertOnlyResult(
                        1, 0, 1, 0, 0, Set.of("N1"), Set.of(100L), List.of()));

        processor.processDueEventInNewTransaction(13L);

        assertEquals(SuWebhookEventStatus.PROCESSED, e.getStatus());
    }

    @Test
    void markFailed_firstRetry_marksFailedWithBackoff() {
        SuReservationWebhookEvent e = newEvent(14L, SuWebhookEventType.PULL);
        when(eventRepository.findById(14L)).thenReturn(Optional.of(e));

        processor.markFailedInNewTransaction(14L, new RuntimeException("boom"));

        assertEquals(1, e.getRetryCount());
        assertEquals(SuWebhookEventStatus.FAILED, e.getStatus());
        assertEquals("boom", e.getLastError());
        verify(eventRepository).save(e);
    }

    @Test
    void markFailed_maxRetries_marksDead() {
        SuReservationWebhookEvent e = newEvent(15L, SuWebhookEventType.PULL);
        e.setRetryCount(19);
        when(eventRepository.findById(15L)).thenReturn(Optional.of(e));

        processor.markFailedInNewTransaction(15L, new RuntimeException("boom"));

        assertEquals(20, e.getRetryCount());
        assertEquals(SuWebhookEventStatus.DEAD, e.getStatus());
    }

    @Test
    void markFailed_missingEvent_doesNotThrow() {
        when(eventRepository.findById(16L)).thenReturn(Optional.empty());

        processor.markFailedInNewTransaction(16L, new RuntimeException("boom"));

        verify(eventRepository, never()).save(any());
    }
}
