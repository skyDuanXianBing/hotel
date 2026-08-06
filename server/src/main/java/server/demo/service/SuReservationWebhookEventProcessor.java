package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.SuReservationWebhookEvent;
import server.demo.enums.SuWebhookEventStatus;
import server.demo.enums.SuWebhookEventType;
import server.demo.repository.SuReservationWebhookEventRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * 补偿事件的单事件处理器。每个事件在独立的 REQUIRES_NEW 事务中处理：
 * 单事件失败/回滚不会毒化调度批次中的其他事件，也不会让批次状态静默回滚。
 */
@Service
public class SuReservationWebhookEventProcessor {

    private static final Logger logger = LoggerFactory.getLogger(SuReservationWebhookEventProcessor.class);
    private static final Logger reservationLogger = LoggerFactory.getLogger("SU_RESERVATION");

    private static final int MAX_RETRIES = 20;
    private static final Duration BASE_BACKOFF = Duration.ofMinutes(1);
    private static final Duration MAX_BACKOFF = Duration.ofMinutes(60);

    private final SuReservationWebhookEventRepository eventRepository;
    private final ObjectMapper objectMapper;
    private final OtaReservationSyncService otaReservationSyncService;

    public SuReservationWebhookEventProcessor(
            SuReservationWebhookEventRepository eventRepository,
            ObjectMapper objectMapper,
            OtaReservationSyncService otaReservationSyncService
    ) {
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
        this.otaReservationSyncService = otaReservationSyncService;
    }

    /**
     * 在独立事务中处理单个事件：标记 PROCESSING → 执行 upsert → 标记 PROCESSED。
     * 异常（含 commit 阶段 rollback-only）向调用方抛出，由调用方走 {@link #markFailedInNewTransaction} 留痕。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processDueEventInNewTransaction(Long eventId) {
        SuReservationWebhookEvent e = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("webhook event not found. id=" + eventId));
        e.setStatus(SuWebhookEventStatus.PROCESSING);
        e.setLastError(null);
        e.setNextRetryAt(null);
        eventRepository.save(e);
        processSingleEvent(e);
        e.setStatus(SuWebhookEventStatus.PROCESSED);
        e.setLastError(null);
        e.setNextRetryAt(null);
        eventRepository.save(e);
    }

    /**
     * 在独立事务中记录失败，保证 retry_count/last_error 一定落库（不受处理事务回滚影响）。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailedInNewTransaction(Long eventId, Exception ex) {
        SuReservationWebhookEvent e = eventRepository.findById(eventId).orElse(null);
        if (e == null) {
            logger.warn("[WebhookCompensate] markFailed skipped, event missing. id={}, err={}", eventId,
                    ex != null ? ex.getMessage() : null);
            return;
        }

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

    private void processSingleEvent(SuReservationWebhookEvent e) {
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
            return;
        }

        String payload = e.getPayloadJson();
        if (payload == null || payload.isBlank()) {
            throw new IllegalStateException("missing payload_json for PUSH event");
        }
        try {
            JsonNode node = objectMapper.readTree(payload);
            OtaReservationSyncService.UpsertOnlyResult result =
                    otaReservationSyncService.upsertReservationsFromWebhook(e.getStoreId(), java.util.List.of(node));
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
        } catch (Exception ex) {
            throw new RuntimeException("parse/process PUSH payload failed: " + ex.getMessage(), ex);
        }
    }
}
