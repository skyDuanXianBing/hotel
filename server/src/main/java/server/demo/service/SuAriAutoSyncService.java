package server.demo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

@Service
public class SuAriAutoSyncService {

    private static final Logger logger = LoggerFactory.getLogger(SuAriAutoSyncService.class);

    private final SuAriSyncEventRepository eventRepository;
    private final StoreRepository storeRepository;
    private final SuAriSyncService suAriSyncService;
    private final ObjectMapper objectMapper;

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
            SuAriSyncService suAriSyncService,
            ObjectMapper objectMapper
    ) {
        this.eventRepository = eventRepository;
        this.storeRepository = storeRepository;
        this.suAriSyncService = suAriSyncService;
        this.objectMapper = objectMapper;
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

    public void enqueueForStoreScope(
            Long storeId,
            String source,
            LocalDate startDate,
            LocalDate endDate,
            Set<Long> roomTypeIds,
            Set<Long> ratePlanIds,
            boolean pushAvailability,
            boolean pushRates,
            boolean pushRestrictions,
            boolean deriveClosedFromBlockouts
    ) {
        List<DateRange> ranges = startDate != null && endDate != null ? List.of(new DateRange(startDate, endDate)) : List.of();
        enqueueForStoreDateRanges(
                storeId,
                source,
                ranges,
                roomTypeIds,
                ratePlanIds,
                pushAvailability,
                pushRates,
                pushRestrictions,
                deriveClosedFromBlockouts
        );
    }

    public void enqueueForStoreDateRanges(
            Long storeId,
            String source,
            List<DateRange> dateRanges,
            Set<Long> roomTypeIds,
            Set<Long> ratePlanIds,
            boolean pushAvailability,
            boolean pushRates,
            boolean pushRestrictions,
            boolean deriveClosedFromBlockouts
    ) {
        if (!enabled) {
            return;
        }
        if (storeId == null) {
            return;
        }
        String hotelId = resolveOrInitSuHotelId(storeId);
        if (hotelId == null || hotelId.isBlank()) {
            logger.warn("[SuAriAutoSync] skip enqueue(scope): hotelId empty. storeId={}", storeId);
            return;
        }
        enqueueForStoreAndHotelScope(
                storeId,
                hotelId,
                source,
                dateRanges,
                roomTypeIds,
                ratePlanIds,
                pushAvailability,
                pushRates,
                pushRestrictions,
                deriveClosedFromBlockouts
        );
    }

    @Transactional
    protected void enqueueForStoreAndHotel(Long storeId, String hotelId, String source) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(Math.max(1, days) - 1L);
        enqueueForStoreAndHotelScope(
                storeId,
                hotelId,
                source,
                List.of(new DateRange(startDate, endDate)),
                null,
                null,
                true,
                true,
                true,
                false
        );
    }

    @Transactional
    protected void enqueueForStoreAndHotelScope(
            Long storeId,
            String hotelId,
            String source,
            List<DateRange> dateRanges,
            Set<Long> roomTypeIds,
            Set<Long> ratePlanIds,
            boolean pushAvailability,
            boolean pushRates,
            boolean pushRestrictions,
            boolean deriveClosedFromBlockouts
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime notBefore = now.plusSeconds(Math.max(0, debounceSeconds));

        List<DateRange> newRanges = mergeAndNormalizeRanges(dateRanges);

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

            // merge scope
            existing.setDateRanges(writeDateRanges(mergeAndNormalizeRanges(mergeRanges(parseDateRanges(existing.getDateRanges()), newRanges))));
            existing.setRoomTypeIds(writeIdSet(mergeIdSet(parseIdSet(existing.getRoomTypeIds()), roomTypeIds)));
            existing.setRatePlanIds(writeIdSet(mergeIdSet(parseIdSet(existing.getRatePlanIds()), ratePlanIds)));
            existing.setPushAvailability(Boolean.TRUE.equals(existing.getPushAvailability()) || pushAvailability);
            existing.setPushRates(Boolean.TRUE.equals(existing.getPushRates()) || pushRates);
            existing.setPushRestrictions(Boolean.TRUE.equals(existing.getPushRestrictions()) || pushRestrictions);
            existing.setDeriveClosedFromBlockouts(Boolean.TRUE.equals(existing.getDeriveClosedFromBlockouts()) || deriveClosedFromBlockouts);
            eventRepository.save(existing);
            return;
        }

        SuAriSyncEvent e = new SuAriSyncEvent();
        e.setStoreId(storeId);
        e.setHotelId(hotelId);
        e.setStatus(SuAriSyncEventStatus.QUEUED);
        e.setSource(source);
        e.setNotBeforeAt(notBefore);
        e.setDateRanges(writeDateRanges(newRanges));
        e.setRoomTypeIds(writeIdSet(normalizeIdSet(roomTypeIds)));
        e.setRatePlanIds(writeIdSet(normalizeIdSet(ratePlanIds)));
        e.setPushAvailability(pushAvailability);
        e.setPushRates(pushRates);
        e.setPushRestrictions(pushRestrictions);
        e.setDeriveClosedFromBlockouts(deriveClosedFromBlockouts);
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
                List<DateRange> ranges = parseDateRanges(claimed.getDateRanges());
                if (ranges.isEmpty()) {
                    LocalDate startDate = LocalDate.now();
                    LocalDate endDate = startDate.plusDays(Math.max(1, days) - 1L);
                    ranges = List.of(new DateRange(startDate, endDate));
                }

                Set<Long> roomTypeIds = parseIdSet(claimed.getRoomTypeIds());
                Set<Long> ratePlanIds = parseIdSet(claimed.getRatePlanIds());

                boolean pushAvailability = !Boolean.FALSE.equals(claimed.getPushAvailability());
                boolean pushRates = !Boolean.FALSE.equals(claimed.getPushRates());
                boolean pushRestrictions = !Boolean.FALSE.equals(claimed.getPushRestrictions());
                boolean deriveClosedFromBlockouts = Boolean.TRUE.equals(claimed.getDeriveClosedFromBlockouts());

                boolean availabilityOk = true;
                boolean ratesOk = true;
                String error = null;
                for (DateRange r : mergeAndNormalizeRanges(ranges)) {
                    SuAriSyncService.SuAriSyncSummary summary = suAriSyncService.syncAriForDateRange(
                            claimed.getStoreId(),
                            claimed.getHotelId(),
                            r.from(),
                            r.to(),
                            roomTypeIds,
                            ratePlanIds,
                            pushAvailability,
                            pushRates,
                            pushRestrictions,
                            deriveClosedFromBlockouts
                    );

                    if (pushAvailability) {
                        availabilityOk = availabilityOk && summary != null && summary.availabilityPushed();
                    }
                    if (pushRates || pushRestrictions) {
                        ratesOk = ratesOk && summary != null && summary.ratesPushed();
                    }
                    if ((summary == null || !summary.availabilityPushed() || !summary.ratesPushed()) && summary != null && summary.error() != null) {
                        error = error != null ? error + "; " + summary.error() : summary.error();
                    }
                }

                boolean ok = (!pushAvailability || availabilityOk) && (!(pushRates || pushRestrictions) || ratesOk);
                if (ok) {
                    markSucceeded(claimed.getId());
                } else {
                    scheduleRetry(claimed.getId(), error != null ? error : "unknown");
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

    public record DateRange(LocalDate from, LocalDate to) {}

    private static boolean isValidRange(DateRange r) {
        return r != null && r.from() != null && r.to() != null && !r.to().isBefore(r.from());
    }

    private List<DateRange> parseDateRanges(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<DateRangeJson> parsed = objectMapper.readValue(json, new TypeReference<List<DateRangeJson>>() {});
            if (parsed == null || parsed.isEmpty()) {
                return List.of();
            }
            List<DateRange> ranges = new ArrayList<>();
            for (DateRangeJson it : parsed) {
                if (it == null || it.from == null || it.to == null) {
                    continue;
                }
                LocalDate from = LocalDate.parse(it.from);
                LocalDate to = LocalDate.parse(it.to);
                DateRange r = new DateRange(from, to);
                if (isValidRange(r)) {
                    ranges.add(r);
                }
            }
            return ranges;
        } catch (Exception e) {
            logger.warn("[SuAriAutoSync] failed to parse date_ranges, fallback to empty. err={}", e.getMessage());
            return List.of();
        }
    }

    private String writeDateRanges(List<DateRange> ranges) {
        List<DateRange> normalized = mergeAndNormalizeRanges(ranges);
        List<DateRangeJson> toWrite = new ArrayList<>();
        for (DateRange r : normalized) {
            if (!isValidRange(r)) {
                continue;
            }
            DateRangeJson dto = new DateRangeJson();
            dto.from = r.from.toString();
            dto.to = r.to.toString();
            toWrite.add(dto);
        }
        try {
            return objectMapper.writeValueAsString(toWrite);
        } catch (Exception e) {
            throw new RuntimeException("failed to write date_ranges json", e);
        }
    }

    private Set<Long> parseIdSet(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            List<Long> ids = objectMapper.readValue(json, new TypeReference<List<Long>>() {});
            if (ids == null || ids.isEmpty()) {
                return null;
            }
            Set<Long> set = new TreeSet<>();
            for (Long id : ids) {
                if (id != null) {
                    set.add(id);
                }
            }
            return set.isEmpty() ? null : set;
        } catch (Exception e) {
            logger.warn("[SuAriAutoSync] failed to parse id set, treat as ALL. err={}", e.getMessage());
            return null;
        }
    }

    private String writeIdSet(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(new ArrayList<>(new TreeSet<>(ids)));
        } catch (Exception e) {
            throw new RuntimeException("failed to write id set json", e);
        }
    }

    private static Set<Long> normalizeIdSet(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        Set<Long> out = new TreeSet<>();
        for (Long id : ids) {
            if (id != null) {
                out.add(id);
            }
        }
        return out.isEmpty() ? null : out;
    }

    private static Set<Long> mergeIdSet(Set<Long> a, Set<Long> b) {
        // NULL means "ALL", so it dominates.
        if (a == null || a.isEmpty()) {
            return null;
        }
        if (b == null || b.isEmpty()) {
            return null;
        }
        Set<Long> out = new TreeSet<>(a);
        out.addAll(b);
        return out;
    }

    private static List<DateRange> mergeRanges(List<DateRange> existing, List<DateRange> incoming) {
        List<DateRange> all = new ArrayList<>();
        if (existing != null) {
            for (DateRange r : existing) {
                if (isValidRange(r)) {
                    all.add(r);
                }
            }
        }
        if (incoming != null) {
            for (DateRange r : incoming) {
                if (isValidRange(r)) {
                    all.add(r);
                }
            }
        }
        return all;
    }

    private static List<DateRange> mergeAndNormalizeRanges(List<DateRange> ranges) {
        if (ranges == null || ranges.isEmpty()) {
            return List.of();
        }
        List<DateRange> sorted = ranges.stream()
                .filter(SuAriAutoSyncService::isValidRange)
                .sorted(Comparator.comparing(DateRange::from).thenComparing(DateRange::to))
                .toList();
        if (sorted.isEmpty()) {
            return List.of();
        }

        List<DateRange> merged = new ArrayList<>();
        DateRange cur = sorted.get(0);
        for (int i = 1; i < sorted.size(); i++) {
            DateRange next = sorted.get(i);
            if (next.from().isAfter(cur.to().plusDays(1))) {
                merged.add(cur);
                cur = next;
                continue;
            }
            LocalDate newTo = next.to().isAfter(cur.to()) ? next.to() : cur.to();
            cur = new DateRange(cur.from(), newTo);
        }
        merged.add(cur);
        return merged;
    }

    private static final class DateRangeJson {
        public String from;
        public String to;
    }
}

