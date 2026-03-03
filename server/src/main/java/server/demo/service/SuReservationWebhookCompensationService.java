package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.SuReservationWebhookEvent;
import server.demo.enums.SuWebhookEventStatus;
import server.demo.enums.SuWebhookEventType;
import server.demo.repository.SuReservationWebhookEventRepository;
import server.demo.util.SuReservationParser;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class SuReservationWebhookCompensationService {

    private static final Logger logger = LoggerFactory.getLogger(SuReservationWebhookCompensationService.class);
    private static final Logger reservationLogger = LoggerFactory.getLogger("SU_RESERVATION");

    private static final int MAX_RETRIES = 20;
    private static final Duration BASE_BACKOFF = Duration.ofMinutes(1);
    private static final Duration MAX_BACKOFF = Duration.ofMinutes(60);

    private final SuReservationWebhookEventRepository eventRepository;
    private final ObjectMapper objectMapper;
    private final OtaReservationSyncService otaReservationSyncService;

    public SuReservationWebhookCompensationService(
            SuReservationWebhookEventRepository eventRepository,
            ObjectMapper objectMapper,
            OtaReservationSyncService otaReservationSyncService
    ) {
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
        this.otaReservationSyncService = otaReservationSyncService;
    }

    @Transactional
    public void recordPullNotifIds(Long storeId, String hotelId, Set<String> notifIds) {
        if (storeId == null || hotelId == null || hotelId.isBlank() || notifIds == null || notifIds.isEmpty()) {
            return;
        }
        String normalizedHotelId = hotelId.trim();
        for (String id : notifIds) {
            if (id == null || id.trim().isBlank()) {
                continue;
            }
            String notifId = id.trim();
            eventRepository.findByHotelIdAndReservationNotifId(normalizedHotelId, notifId)
                    .orElseGet(() -> {
                        SuReservationWebhookEvent e = new SuReservationWebhookEvent();
                        e.setStoreId(storeId);
                        e.setHotelId(normalizedHotelId);
                        e.setReservationNotifId(notifId);
                        e.setEventType(SuWebhookEventType.PULL);
                        e.setStatus(SuWebhookEventStatus.RECEIVED);
                        e.setRetryCount(0);
                        e.setNextRetryAt(null);
                        try {
                            return eventRepository.save(e);
                        } catch (DataIntegrityViolationException ignored) {
                            return e;
                        }
                    });
        }
    }

    @Transactional
    public void recordPushReservations(Long storeId, String hotelId, List<JsonNode> reservations) {
        if (storeId == null || hotelId == null || hotelId.isBlank() || reservations == null || reservations.isEmpty()) {
            return;
        }
        String normalizedHotelId = hotelId.trim();
        for (JsonNode reservation : reservations) {
            if (reservation == null || reservation.isNull()) {
                continue;
            }
            String notifId = SuReservationParser.extractReservationNotifId(reservation);
            if (notifId == null || notifId.isBlank()) {
                continue;
            }
            String trimmed = notifId.trim();
            eventRepository.findByHotelIdAndReservationNotifId(normalizedHotelId, trimmed)
                    .orElseGet(() -> {
                        SuReservationWebhookEvent e = new SuReservationWebhookEvent();
                        e.setStoreId(storeId);
                        e.setHotelId(normalizedHotelId);
                        e.setReservationNotifId(trimmed);
                        e.setEventType(SuWebhookEventType.PUSH);
                        e.setStatus(SuWebhookEventStatus.RECEIVED);
                        e.setRetryCount(0);
                        e.setNextRetryAt(null);
                        try {
                            e.setPayloadJson(objectMapper.writeValueAsString(reservation));
                        } catch (Exception ex) {
                            e.setPayloadJson(null);
                            e.setLastError("payload serialize failed: " + ex.getMessage());
                        }
                        try {
                            return eventRepository.save(e);
                        } catch (DataIntegrityViolationException ignored) {
                            return e;
                        }
                    });
        }
    }

    @Transactional
    public int processDueEventsOnce(int limit) {
        int effectiveLimit = limit > 0 ? Math.min(limit, 200) : 50;
        LocalDateTime now = LocalDateTime.now();
        List<SuReservationWebhookEvent> due = eventRepository.findDueEvents(
                List.of(SuWebhookEventStatus.RECEIVED, SuWebhookEventStatus.FAILED),
                now,
                PageRequest.of(0, effectiveLimit)
        );
        if (due.isEmpty()) {
            return 0;
        }

        int processed = 0;
        for (SuReservationWebhookEvent e : due) {
            try {
                e.setStatus(SuWebhookEventStatus.PROCESSING);
                e.setLastError(null);
                e.setNextRetryAt(null);
                eventRepository.save(e);
                boolean ok = processSingleEvent(e);
                if (ok) {
                    e.setStatus(SuWebhookEventStatus.PROCESSED);
                    e.setLastError(null);
                    e.setNextRetryAt(null);
                    eventRepository.save(e);
                }
            } catch (Exception ex) {
                markFailed(e, ex);
            } finally {
                processed++;
            }
        }
        return processed;
    }

    /**
     * Webhook notif-only 模式下：为了避免“先 ACK 导致 Su 队列被清空，从而拉不到取消/修改”的问题，
     * 这里提供同步处理入口：先 pull+upsert 成功后，再由 controller 返回 webhook ACK body。
     *
     * @return 本次成功 upsert 的 reservation_notif_id 集合（用于 webhook ACK）。
     */
    @Transactional
    public Set<String> processPullNotifIdsNow(Long storeId, String hotelId, Set<String> notifIds) {
        if (storeId == null || hotelId == null || hotelId.isBlank() || notifIds == null || notifIds.isEmpty()) {
            return Set.of();
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String raw : notifIds) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            normalized.add(raw.trim());
        }
        if (normalized.isEmpty()) {
            return Set.of();
        }

        OtaReservationSyncService.PullUpsertResult result =
                otaReservationSyncService.pullAndUpsertReservationsWithoutAck(storeId, normalized);
        Set<String> processed = result != null && result.processedNotifIds() != null ? Set.copyOf(result.processedNotifIds()) : Set.of();

        // Update event status for each notifId so scheduler/UI can see real outcome.
        for (String notifId : normalized) {
            try {
                SuReservationWebhookEvent e = eventRepository.findByHotelIdAndReservationNotifId(hotelId.trim(), notifId)
                        .orElse(null);
                if (e == null) {
                    continue;
                }
                if (processed.contains(notifId)) {
                    e.setStatus(SuWebhookEventStatus.PROCESSED);
                    e.setLastError(null);
                    e.setNextRetryAt(null);
                    eventRepository.save(e);
                } else {
                    String msg = (result != null && result.errors() != null && !result.errors().isEmpty())
                            ? ("pull-upsert did not include notifId=" + notifId + ", errors=" + result.errors())
                            : ("pull-upsert did not include notifId=" + notifId);
                    markFailed(e, new RuntimeException(msg));
                }
            } catch (Exception ex) {
                // Best-effort: don't block other ids.
                reservationLogger.error("[WebhookCompensate] update event status failed. storeId={}, hotelId={}, notifId={}, err={}",
                        storeId, hotelId, notifId, ex.getMessage(), ex);
            }
        }

        return processed;
    }
    
    /**
     * Webhook reservation-push 模式下的同步处理：直接用 webhook payload upsert（不依赖 pull）。
     *
     * @return 本次成功 upsert 的 reservation_notif_id 集合（用于 webhook ACK）。
     */
    @Transactional
    public Set<String> processPushReservationsNow(Long storeId, String hotelId, List<JsonNode> reservations) {
        if (storeId == null || hotelId == null || hotelId.isBlank() || reservations == null || reservations.isEmpty()) {
            return Set.of();
        }

        List<JsonNode> safe = reservations.stream().filter(r -> r != null && !r.isNull()).toList();
        if (safe.isEmpty()) {
            return Set.of();
        }

        OtaReservationSyncService.UpsertOnlyResult result =
                otaReservationSyncService.upsertReservationsFromWebhook(storeId, safe);
        Set<String> processed = result != null && result.processedNotifIds() != null ? Set.copyOf(result.processedNotifIds()) : Set.of();

        for (JsonNode reservation : safe) {
            String notifId = SuReservationParser.extractReservationNotifId(reservation);
            if (notifId == null || notifId.isBlank()) {
                continue;
            }
            String trimmed = notifId.trim();
            try {
                SuReservationWebhookEvent e = eventRepository.findByHotelIdAndReservationNotifId(hotelId.trim(), trimmed)
                        .orElse(null);
                if (e == null) {
                    continue;
                }
                if (processed.contains(trimmed)) {
                    e.setStatus(SuWebhookEventStatus.PROCESSED);
                    e.setLastError(null);
                    e.setNextRetryAt(null);
                    eventRepository.save(e);
                } else {
                    String msg = (result != null && result.errors() != null && !result.errors().isEmpty())
                            ? ("push-upsert did not include notifId=" + trimmed + ", errors=" + result.errors())
                            : ("push-upsert did not include notifId=" + trimmed);
                    markFailed(e, new RuntimeException(msg));
                }
            } catch (Exception ex) {
                reservationLogger.error("[WebhookCompensate] update event status failed (push). storeId={}, hotelId={}, notifId={}, err={}",
                        storeId, hotelId, trimmed, ex.getMessage(), ex);
            }
        }

        return processed;
    }

    private boolean processSingleEvent(SuReservationWebhookEvent e) {
        if (e.getStoreId() == null || e.getHotelId() == null || e.getReservationNotifId() == null) {
            throw new IllegalStateException("missing storeId/hotelId/notifId");
        }
        if (e.getEventType() == SuWebhookEventType.PULL) {
            OtaReservationSyncService.PullUpsertResult result =
                    otaReservationSyncService.pullAndUpsertReservationsWithoutAck(
                            e.getStoreId(),
                            Set.of(e.getReservationNotifId())
                    );
            boolean ok = result != null
                    && result.failedCount() == 0
                    && result.processedNotifIds() != null
                    && result.processedNotifIds().contains(e.getReservationNotifId());
            if (!ok) {
                throw new RuntimeException("pull-upsert missing notifId. notifId=" + e.getReservationNotifId()
                        + ", failedCount=" + (result != null ? result.failedCount() : null)
                        + ", processedNotifIds=" + (result != null ? result.processedNotifIds() : null)
                        + ", errors=" + (result != null ? result.errors() : null));
            }
            reservationLogger.info("[WebhookCompensate] processed pull notifId. storeId={}, hotelId={}, notifId={}, ok=true",
                    e.getStoreId(), e.getHotelId(), e.getReservationNotifId());
            return true;
        }

        String payload = e.getPayloadJson();
        if (payload == null || payload.isBlank()) {
            throw new IllegalStateException("missing payload_json for PUSH event");
        }
        try {
            JsonNode node = objectMapper.readTree(payload);
            OtaReservationSyncService.UpsertOnlyResult result =
                    otaReservationSyncService.upsertReservationsFromWebhook(e.getStoreId(), List.of(node));
            boolean ok = result != null
                    && result.failedCount() == 0
                    && result.processedNotifIds() != null
                    && result.processedNotifIds().contains(e.getReservationNotifId());
            if (!ok) {
                throw new RuntimeException("push-upsert missing notifId. notifId=" + e.getReservationNotifId()
                        + ", failedCount=" + (result != null ? result.failedCount() : null)
                        + ", processedNotifIds=" + (result != null ? result.processedNotifIds() : null)
                        + ", errors=" + (result != null ? result.errors() : null));
            }
            reservationLogger.info("[WebhookCompensate] processed push reservation. storeId={}, hotelId={}, notifId={}, ok=true",
                    e.getStoreId(), e.getHotelId(), e.getReservationNotifId());
            return true;
        } catch (Exception ex) {
            throw new RuntimeException("parse/process PUSH payload failed: " + ex.getMessage(), ex);
        }
    }

    private void markFailed(SuReservationWebhookEvent e, Exception ex) {
        int current = e.getRetryCount() != null ? e.getRetryCount() : 0;
        int next = current + 1;
        e.setRetryCount(next);

        String msg = ex != null ? ex.getMessage() : "unknown error";
        if (msg != null && msg.length() > 2000) {
            msg = msg.substring(0, 2000);
        }
        e.setLastError(msg);

        if (next >= MAX_RETRIES) {
            e.setStatus(SuWebhookEventStatus.DEAD);
            e.setNextRetryAt(null);
            eventRepository.save(e);
            reservationLogger.error("[WebhookCompensate] event dead. storeId={}, hotelId={}, notifId={}, retries={}, err={}",
                    e.getStoreId(), e.getHotelId(), e.getReservationNotifId(), next, msg);
            return;
        }

        Duration backoff = BASE_BACKOFF.multipliedBy((long) Math.pow(2, Math.max(0, next - 1)));
        if (backoff.compareTo(MAX_BACKOFF) > 0) {
            backoff = MAX_BACKOFF;
        }
        e.setStatus(SuWebhookEventStatus.FAILED);
        e.setNextRetryAt(LocalDateTime.now().plus(backoff));
        eventRepository.save(e);

        logger.warn("[WebhookCompensate] event failed, scheduled retry. storeId={}, hotelId={}, notifId={}, retries={}, nextRetryAt={}, err={}",
                e.getStoreId(), e.getHotelId(), e.getReservationNotifId(), next, e.getNextRetryAt(), msg);
    }
}
