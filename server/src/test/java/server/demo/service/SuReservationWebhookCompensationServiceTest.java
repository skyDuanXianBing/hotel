package server.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.entity.SuReservationWebhookEvent;
import server.demo.enums.SuWebhookEventStatus;
import server.demo.enums.SuWebhookEventType;
import server.demo.repository.SuReservationWebhookEventRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 补偿调度编排：逐事件走独立事务，单事件异常只影响该事件的失败留痕，不毒化整批。
 */
class SuReservationWebhookCompensationServiceTest {

    private SuReservationWebhookEventRepository eventRepository;
    private OtaReservationSyncService otaReservationSyncService;
    private SuReservationWebhookEventProcessor eventProcessor;
    private SuReservationWebhookCompensationService service;

    @BeforeEach
    void setUp() {
        eventRepository = Mockito.mock(SuReservationWebhookEventRepository.class);
        otaReservationSyncService = Mockito.mock(OtaReservationSyncService.class);
        eventProcessor = Mockito.mock(SuReservationWebhookEventProcessor.class);
        service = new SuReservationWebhookCompensationService(
                eventRepository, new ObjectMapper(), otaReservationSyncService, eventProcessor);
    }

    private static SuReservationWebhookEvent newEvent(Long id) {
        SuReservationWebhookEvent e = new SuReservationWebhookEvent();
        ReflectionTestUtils.setField(e, "id", id);
        e.setStoreId(7L);
        e.setHotelId("H1");
        e.setReservationNotifId("N" + id);
        e.setEventType(SuWebhookEventType.PULL);
        e.setStatus(SuWebhookEventStatus.RECEIVED);
        e.setRetryCount(0);
        return e;
    }

    @Test
    void processDueEventsOnce_noDueEvents_returnsZero() {
        when(eventRepository.findDueEvents(anyList(), any(), any(Pageable.class))).thenReturn(List.of());

        assertEquals(0, service.processDueEventsOnce(50));
    }

    @Test
    void processDueEventsOnce_processesEachEventInOwnTransaction() {
        List<SuReservationWebhookEvent> due = List.of(newEvent(1L), newEvent(2L));
        when(eventRepository.findDueEvents(anyList(), any(), any(Pageable.class))).thenReturn(due);

        int processed = service.processDueEventsOnce(50);

        assertEquals(2, processed);
        verify(eventProcessor).processDueEventInNewTransaction(1L);
        verify(eventProcessor).processDueEventInNewTransaction(2L);
        verify(eventProcessor, times(0)).markFailedInNewTransaction(any(), any());
    }

    @Test
    void processDueEventsOnce_eventFailure_marksFailedAndContinuesBatch() {
        List<SuReservationWebhookEvent> due = List.of(newEvent(1L), newEvent(2L));
        when(eventRepository.findDueEvents(anyList(), any(), any(Pageable.class))).thenReturn(due);
        RuntimeException boom = new RuntimeException("boom");
        Mockito.doThrow(boom).when(eventProcessor).processDueEventInNewTransaction(1L);

        int processed = service.processDueEventsOnce(50);

        assertEquals(2, processed);
        verify(eventProcessor).markFailedInNewTransaction(1L, boom);
        verify(eventProcessor).processDueEventInNewTransaction(2L);
    }
}
