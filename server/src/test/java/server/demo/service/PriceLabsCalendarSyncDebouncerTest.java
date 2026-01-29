package server.demo.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import server.demo.util.PriceLabsIdUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;

class PriceLabsCalendarSyncDebouncerTest {

    @Test
    void requestSync_mergesRangesAndDebouncesIntoSinglePush() throws Exception {
        PriceLabsSyncService syncService = Mockito.mock(PriceLabsSyncService.class);
        CountDownLatch latch = new CountDownLatch(1);

        Mockito.doAnswer(inv -> {
            latch.countDown();
            return null;
        }).when(syncService).syncCalendarForListingIds(anyList(), any(LocalDate.class), any(LocalDate.class));

        ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1);
        try {
            PriceLabsCalendarSyncDebouncer debouncer = new PriceLabsCalendarSyncDebouncer(syncService, 50L, scheduler);

            Long storeId = 13L;
            Long roomTypeId = 10L;

            debouncer.requestSync(storeId, roomTypeId, LocalDate.of(2026, 1, 20), LocalDate.of(2026, 1, 21));
            debouncer.requestSync(storeId, roomTypeId, LocalDate.of(2026, 1, 21), LocalDate.of(2026, 1, 25));

            assertTrue(latch.await(2, TimeUnit.SECONDS));

            ArgumentCaptor<List> listingCaptor = ArgumentCaptor.forClass(List.class);
            ArgumentCaptor<LocalDate> startCaptor = ArgumentCaptor.forClass(LocalDate.class);
            ArgumentCaptor<LocalDate> endCaptor = ArgumentCaptor.forClass(LocalDate.class);

            Mockito.verify(syncService, times(1)).syncCalendarForListingIds(
                    listingCaptor.capture(),
                    startCaptor.capture(),
                    endCaptor.capture()
            );

            assertEquals(List.of(PriceLabsIdUtil.formatListingId(storeId, roomTypeId)), listingCaptor.getValue());
            assertEquals(LocalDate.of(2026, 1, 20), startCaptor.getValue());
            assertEquals(LocalDate.of(2026, 1, 25), endCaptor.getValue());
        } finally {
            scheduler.shutdownNow();
        }
    }
}

