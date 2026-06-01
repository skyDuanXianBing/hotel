package server.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.entity.SuAriSyncEvent;
import server.demo.enums.SuAriSyncEventStatus;
import server.demo.repository.StoreRepository;
import server.demo.repository.SuAriSyncEventRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuAriAutoSyncServiceScopeCoalesceTest {

    private SuAriSyncEventRepository eventRepository;
    private SuAriAutoSyncService service;

    @BeforeEach
    void setUp() {
        eventRepository = Mockito.mock(SuAriSyncEventRepository.class);
        StoreRepository storeRepository = Mockito.mock(StoreRepository.class);
        SuAriSyncService suAriSyncService = Mockito.mock(SuAriSyncService.class);
        when(storeRepository.findById(1L)).thenReturn(Optional.empty());

        Clock clock = Clock.fixed(Instant.parse("2026-02-27T15:30:00Z"), ZoneOffset.UTC);
        service = new SuAriAutoSyncService(eventRepository, storeRepository, suAriSyncService, new ObjectMapper(), clock);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "debounceSeconds", 0);
    }

    @Test
    void enqueueForStoreAndHotelScope_shouldCoalesceWhenScopeIsSame() {
        SuAriSyncEvent existing = new SuAriSyncEvent();
        ReflectionTestUtils.setField(existing, "id", 88L);
        existing.setStoreId(1L);
        existing.setHotelId("STORE1");
        existing.setStatus(SuAriSyncEventStatus.QUEUED);
        existing.setDateRanges("[{\"from\":\"2026-03-01\",\"to\":\"2026-03-02\"}]");
        existing.setRoomTypeIds("[101]");
        existing.setRatePlanIds("[201]");
        existing.setPushAvailability(true);
        existing.setPushRates(true);
        existing.setPushRestrictions(false);
        existing.setDeriveClosedFromBlockouts(false);
        existing.setCoalescedCount(0);

        when(eventRepository.findTopByStoreIdAndHotelIdAndStatusOrderByIdDesc(
                eq(1L), eq("STORE1"), eq(SuAriSyncEventStatus.QUEUED)
        )).thenReturn(Optional.of(existing));
        when(eventRepository.saveAndFlush(any(SuAriSyncEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        service.enqueueForStoreAndHotelScope(
                1L,
                "STORE1",
                "scope-test",
                List.of(new SuAriAutoSyncService.DateRange(LocalDate.of(2026, 3, 3), LocalDate.of(2026, 3, 4))),
                Set.of(101L),
                Set.of(201L),
                true,
                true,
                false,
                false
        );

        ArgumentCaptor<SuAriSyncEvent> captor = ArgumentCaptor.forClass(SuAriSyncEvent.class);
        verify(eventRepository, times(1)).saveAndFlush(captor.capture());
        SuAriSyncEvent saved = captor.getValue();
        assertEquals(88L, ReflectionTestUtils.getField(saved, "id"));
        assertEquals(1, saved.getCoalescedCount());
        assertEquals("[101]", saved.getRoomTypeIds());
        assertEquals("[201]", saved.getRatePlanIds());
    }

    @Test
    void enqueueForStoreAndHotelScope_shouldCreateNewEventWhenScopeDiffers() {
        SuAriSyncEvent existing = new SuAriSyncEvent();
        ReflectionTestUtils.setField(existing, "id", 99L);
        existing.setStoreId(1L);
        existing.setHotelId("STORE1");
        existing.setStatus(SuAriSyncEventStatus.QUEUED);
        existing.setDateRanges("[{\"from\":\"2026-03-01\",\"to\":\"2026-03-02\"}]");
        existing.setRoomTypeIds("[101]");
        existing.setRatePlanIds("[201]");
        existing.setPushAvailability(true);
        existing.setPushRates(true);
        existing.setPushRestrictions(false);
        existing.setDeriveClosedFromBlockouts(false);
        existing.setCoalescedCount(0);

        when(eventRepository.findTopByStoreIdAndHotelIdAndStatusOrderByIdDesc(
                eq(1L), eq("STORE1"), eq(SuAriSyncEventStatus.QUEUED)
        )).thenReturn(Optional.of(existing));
        when(eventRepository.saveAndFlush(any(SuAriSyncEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        service.enqueueForStoreAndHotelScope(
                1L,
                "STORE1",
                "scope-test",
                List.of(new SuAriAutoSyncService.DateRange(LocalDate.of(2026, 3, 3), LocalDate.of(2026, 3, 4))),
                Set.of(101L),
                Set.of(202L),
                true,
                true,
                false,
                false
        );

        ArgumentCaptor<SuAriSyncEvent> captor = ArgumentCaptor.forClass(SuAriSyncEvent.class);
        verify(eventRepository, times(1)).saveAndFlush(captor.capture());
        SuAriSyncEvent saved = captor.getValue();
        assertNotSame(existing, saved);
        assertEquals("[101]", saved.getRoomTypeIds());
        assertEquals("[202]", saved.getRatePlanIds());
        assertEquals(0, saved.getCoalescedCount());
    }
}
