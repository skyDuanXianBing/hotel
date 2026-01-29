package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import server.demo.constants.PriceLabsSyncDefaults;
import server.demo.util.PriceLabsIdUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Debounce & merge PriceLabs /calendar pushes after PMS availability changes (e.g. close-out).
 *
 * Goal: keep PL status near-real-time while avoiding request storms during bulk operations.
 */
@Service
public class PriceLabsCalendarSyncDebouncer implements DisposableBean {
    private static final Logger logger = LoggerFactory.getLogger(PriceLabsCalendarSyncDebouncer.class);

    private final PriceLabsSyncService priceLabsSyncService;
    private final long debounceMs;
    private final ScheduledExecutorService scheduler;

    private final ConcurrentHashMap<Key, Pending> pendingByKey = new ConcurrentHashMap<>();

    @Autowired
    public PriceLabsCalendarSyncDebouncer(PriceLabsSyncService priceLabsSyncService) {
        this(priceLabsSyncService, PriceLabsSyncDefaults.CALENDAR_PUSH_DEBOUNCE_MS, newSingleThreadScheduler());
    }

    PriceLabsCalendarSyncDebouncer(PriceLabsSyncService priceLabsSyncService, long debounceMs, ScheduledExecutorService scheduler) {
        this.priceLabsSyncService = Objects.requireNonNull(priceLabsSyncService, "priceLabsSyncService");
        this.debounceMs = Math.max(debounceMs, 0L);
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    public void requestSyncAfterCommit(Long storeId, Long roomTypeId, LocalDate startDate, LocalDate endDate) {
        if (storeId == null || roomTypeId == null) {
            return;
        }
        if (startDate == null || endDate == null) {
            return;
        }

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            requestSync(storeId, roomTypeId, startDate, endDate);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                requestSync(storeId, roomTypeId, startDate, endDate);
            }
        });
    }

    public void requestSync(Long storeId, Long roomTypeId, LocalDate startDate, LocalDate endDate) {
        if (storeId == null || roomTypeId == null) {
            return;
        }
        if (startDate == null || endDate == null) {
            return;
        }
        LocalDate from = startDate.isBefore(endDate) ? startDate : endDate;
        LocalDate to = endDate.isAfter(startDate) ? endDate : startDate;

        Key key = new Key(storeId, roomTypeId);
        pendingByKey.compute(key, (k, existing) -> {
            Pending p = existing != null ? existing : new Pending(from, to);
            p.merge(from, to);
            p.reschedule(() -> flush(k, p), debounceMs, scheduler);
            return p;
        });
    }

    private void flush(Key key, Pending pending) {
        try {
            Range r = pending.snapshot();
            String listingId = PriceLabsIdUtil.formatListingId(key.storeId, key.roomTypeId);
            long started = System.currentTimeMillis();
            priceLabsSyncService.syncCalendarForListingIds(List.of(listingId), r.start, r.end);
            long costMs = System.currentTimeMillis() - started;
            logger.info("[PriceLabsCalendarDebounce] pushed. storeId={}, roomTypeId={}, range={}~{}, costMs={}",
                    key.storeId, key.roomTypeId, r.start, r.end, costMs);
        } catch (Exception e) {
            Range r = pending.snapshot();
            logger.warn("[PriceLabsCalendarDebounce] push failed. storeId={}, roomTypeId={}, range={}~{}, error={}",
                    key.storeId, key.roomTypeId, r.start, r.end, e.getMessage());
        } finally {
            pendingByKey.remove(key);
        }
    }

    @Override
    public void destroy() {
        try {
            scheduler.shutdownNow();
        } catch (Exception ignored) {
            // ignore
        }
    }

    private static ScheduledExecutorService newSingleThreadScheduler() {
        ThreadFactory tf = r -> {
            Thread t = new Thread(r);
            t.setName("pricelabs-calendar-debouncer");
            t.setDaemon(true);
            return t;
        };
        return Executors.newSingleThreadScheduledExecutor(tf);
    }

    private record Key(Long storeId, Long roomTypeId) {}

    private static final class Pending {
        private LocalDate start;
        private LocalDate end;
        private ScheduledFuture<?> future;

        private Pending(LocalDate start, LocalDate end) {
            this.start = start;
            this.end = end;
        }

        synchronized void merge(LocalDate from, LocalDate to) {
            if (start == null || from.isBefore(start)) start = from;
            if (end == null || to.isAfter(end)) end = to;
        }

        synchronized Range snapshot() {
            return new Range(start, end);
        }

        synchronized void reschedule(Runnable task, long delayMs, ScheduledExecutorService scheduler) {
            if (future != null && !future.isDone()) {
                future.cancel(false);
            }
            if (delayMs <= 0L) {
                future = scheduler.schedule(task, 0L, TimeUnit.MILLISECONDS);
                return;
            }
            future = scheduler.schedule(task, delayMs, TimeUnit.MILLISECONDS);
        }
    }

    private record Range(LocalDate start, LocalDate end) {}
}
