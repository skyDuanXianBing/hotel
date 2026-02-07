package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.Store;
import server.demo.entity.SuAriSyncEvent;
import server.demo.enums.SuAriSyncEventStatus;
import server.demo.repository.StoreRepository;
import server.demo.repository.SuAriSyncEventRepository;
import server.demo.util.SuHotelIdUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SuAriAutoSyncService {

    private static final Logger logger = LoggerFactory.getLogger(SuAriAutoSyncService.class);

    private final SuAriSyncEventRepository eventRepository;
    private final StoreRepository storeRepository;
    private final SuAriSyncService suAriSyncService;

    @Value("${su.ari.autosync.enabled:true}")
    private boolean enabled;

    @Value("${su.ari.autosync.days:365}")
    private int days;

    @Value("${su.ari.autosync.debounceSeconds:60}")
    private int debounceSeconds;

    @Value("${su.ari.autosync.maxRetryDelayMinutes:120}")
    private int maxRetryDelayMinutes;

    public SuAriAutoSyncService(
            SuAriSyncEventRepository eventRepository,
            StoreRepository storeRepository,
            SuAriSyncService suAriSyncService
    ) {
        this.eventRepository = eventRepository;
        this.storeRepository = storeRepository;
        this.suAriSyncService = suAriSyncService;
    }

    public void enqueueForStore(Long storeId, String source) {
        if (!enabled) {
            return;
        }
        if (storeId == null) {
            return;
        }
        String hotelId = resolveOrInitSuHotelId(storeId);
        if (hotelId == null || hotelId.isBlank()) {
            logger.warn("[SuAriAutoSync] skip enqueue: hotelId empty. storeId={}", storeId);
            return;
        }
        enqueueForStoreAndHotel(storeId, hotelId, source);
    }

    @Transactional
    protected void enqueueForStoreAndHotel(Long storeId, String hotelId, String source) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime notBefore = now.plusSeconds(Math.max(0, debounceSeconds));

        Optional<SuAriSyncEvent> existingOpt = eventRepository.findTopByStoreIdAndHotelIdAndStatusOrderByIdDesc(
                storeId,
                hotelId,
                SuAriSyncEventStatus.QUEUED
        );

        if (existingOpt.isPresent()) {
            SuAriSyncEvent existing = existingOpt.get();
            existing.setCoalescedCount((existing.getCoalescedCount() != null ? existing.getCoalescedCount() : 0) + 1);
            existing.setNotBeforeAt(notBefore);
            existing.setSource(source);
            eventRepository.save(existing);
            return;
        }

        SuAriSyncEvent e = new SuAriSyncEvent();
        e.setStoreId(storeId);
        e.setHotelId(hotelId);
        e.setStatus(SuAriSyncEventStatus.QUEUED);
        e.setSource(source);
        e.setNotBeforeAt(notBefore);
        eventRepository.save(e);
    }

    public int processDueEvents(int limit) {
        if (!enabled) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        List<SuAriSyncEvent> due = eventRepository.findDueEvents(SuAriSyncEventStatus.QUEUED, now);
        if (due.isEmpty()) {
            return 0;
        }

        int processed = 0;
        for (SuAriSyncEvent e : due) {
            if (processed >= Math.max(1, limit)) {
                break;
            }
            if (e == null || e.getId() == null) {
                continue;
            }

            SuAriSyncEvent claimed = claimForProcessing(e.getId());
            if (claimed == null) {
                continue;
            }

            try {
                SuAriSyncService.SuAriSyncSummary summary = suAriSyncService.syncBaseAriForNextDays(
                        claimed.getStoreId(),
                        claimed.getHotelId(),
                        days
                );

                boolean ok = summary != null && summary.availabilityPushed() && summary.ratesPushed();
                if (ok) {
                    markSucceeded(claimed.getId());
                } else {
                    String err = summary != null ? summary.error() : "unknown";
                    scheduleRetry(claimed.getId(), err);
                }
            } catch (Exception ex) {
                scheduleRetry(claimed.getId(), ex.getMessage());
            }
            processed++;
        }

        return processed;
    }

    @Transactional
    protected SuAriSyncEvent claimForProcessing(Long id) {
        SuAriSyncEvent e = eventRepository.findById(id).orElse(null);
        if (e == null) {
            return null;
        }
        if (e.getStatus() != SuAriSyncEventStatus.QUEUED) {
            return null;
        }
        e.setStatus(SuAriSyncEventStatus.PROCESSING);
        e.setLastRunAt(LocalDateTime.now());
        eventRepository.save(e);
        return e;
    }

    @Transactional
    protected void markSucceeded(Long id) {
        SuAriSyncEvent e = eventRepository.findById(id).orElse(null);
        if (e == null) {
            return;
        }
        e.setStatus(SuAriSyncEventStatus.SUCCEEDED);
        e.setLastError(null);
        e.setNextRetryAt(null);
        e.setNotBeforeAt(null);
        e.setLastSuccessAt(LocalDateTime.now());
        eventRepository.save(e);
        logger.info("[SuAriAutoSync] succeeded. eventId={}, storeId={}, hotelId={}", e.getId(), e.getStoreId(), e.getHotelId());
    }

    @Transactional
    protected void scheduleRetry(Long id, String error) {
        SuAriSyncEvent e = eventRepository.findById(id).orElse(null);
        if (e == null) {
            return;
        }

        int retry = (e.getRetryCount() != null ? e.getRetryCount() : 0) + 1;
        e.setRetryCount(retry);
        e.setStatus(SuAriSyncEventStatus.QUEUED);
        e.setLastError(error != null ? error : "unknown");

        Duration delay = computeBackoff(retry);
        e.setNextRetryAt(LocalDateTime.now().plus(delay));
        e.setNotBeforeAt(null);
        eventRepository.save(e);

        logger.warn("[SuAriAutoSync] failed, scheduled retry. eventId={}, retry={}, delaySec={}, err={}",
                e.getId(), retry, delay.toSeconds(), e.getLastError());
    }

    private Duration computeBackoff(int retry) {
        long baseSeconds = 30L;
        long seconds = baseSeconds * (1L << Math.min(10, Math.max(0, retry - 1)));
        long maxSeconds = Math.max(60L, (long) maxRetryDelayMinutes * 60L);
        if (seconds > maxSeconds) {
            seconds = maxSeconds;
        }
        return Duration.ofSeconds(seconds);
    }

    private String resolveOrInitSuHotelId(Long storeId) {
        Store store = storeRepository.findById(storeId).orElse(null);
        if (store == null) {
            return null;
        }
        String hotelId = SuHotelIdUtil.normalize(store.getSuHotelId());
        if (hotelId != null) {
            return hotelId;
        }
        String generated = SuHotelIdUtil.buildDefault(storeId);
        store.setSuHotelId(generated);
        storeRepository.save(store);
        return generated;
    }
}

