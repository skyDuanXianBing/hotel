package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import server.demo.entity.AutoMessage;
import server.demo.entity.Reservation;
import server.demo.entity.Store;
import server.demo.enums.ReservationStatus;
import server.demo.repository.AutoMessageRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.StoreRepository;
import server.demo.util.AutoMessageTimingUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 自动化消息触发服务（基于前端 AutoMessage 页面模型）：
 * - action: BOOKING_CONFIRM / CHECK_IN / CHECK_OUT
 * - sendTiming: IMMEDIATELY / 5_MIN ... / 24_HOUR
 *
 * 发送通道：Su Messaging（仅渠道 19/244）。
 */
@Service
public class AutoMessageTriggerService {

    private static final Logger logger = LoggerFactory.getLogger(AutoMessageTriggerService.class);
    private static final Logger autoMessageLogger = LoggerFactory.getLogger("SU_AUTO_MESSAGE");

    private final StoreRepository storeRepository;
    private final AutoMessageRepository autoMessageRepository;
    private final ReservationRepository reservationRepository;
    private final SuBusinessAutoMessageService businessAutoMessageService;

    public AutoMessageTriggerService(
            StoreRepository storeRepository,
            AutoMessageRepository autoMessageRepository,
            ReservationRepository reservationRepository,
            SuBusinessAutoMessageService businessAutoMessageService
    ) {
        this.storeRepository = storeRepository;
        this.autoMessageRepository = autoMessageRepository;
        this.reservationRepository = reservationRepository;
        this.businessAutoMessageService = businessAutoMessageService;
    }

    /**
     * 为了支持 5/10/15/30 分钟等 sendTiming，按分钟轮询处理。
     */
    @Scheduled(cron = "0 * * * * *")
    public void tick() {
        LocalDateTime now = LocalDateTime.now();
        try {
            dispatchAllStores(now);
        } catch (Exception e) {
            logger.error("[AutoMessage] tick failed. err={}", e.getMessage(), e);
        }
    }

    /**
     * 手动触发（用于预订创建/入住/退房后，尽快处理 IMMEDIATELY 的消息）。
     */
    public void dispatchStoreOnce(Long storeId) {
        if (storeId == null) {
            return;
        }
        try {
            dispatchStore(storeId, LocalDateTime.now());
        } catch (Exception e) {
            logger.warn("[AutoMessage] dispatchStoreOnce failed. storeId={}, err={}", storeId, e.getMessage(), e);
            autoMessageLogger.error("[AutoMessage] dispatchStoreOnce failed. storeId={}, err={}", storeId, e.getMessage());
        }
    }

    private void dispatchAllStores(LocalDateTime now) {
        List<Store> stores = storeRepository.findAll();
        for (Store store : stores) {
            if (store == null || store.getId() == null) {
                continue;
            }
            try {
                dispatchStore(store.getId(), now);
            } catch (Exception e) {
                logger.warn("[AutoMessage] dispatch store failed. storeId={}, err={}", store.getId(), e.getMessage(), e);
                autoMessageLogger.error("[AutoMessage] dispatch store failed. storeId={}, err={}", store.getId(), e.getMessage());
            }
        }
    }

    private void dispatchStore(Long storeId, LocalDateTime now) {
        List<AutoMessage> templates = autoMessageRepository.findByStoreIdAndEnabledTrue(storeId);
        if (templates.isEmpty()) {
            return;
        }

        int totalCandidates = 0;
        int totalTemplates = 0;

        for (AutoMessage template : templates) {
            if (template == null) {
                continue;
            }
            String action = template.getAction();
            if (action == null || action.isBlank()) {
                continue;
            }
            String sendTiming = template.getSendTiming();
            if (sendTiming == null || sendTiming.isBlank()) {
                continue;
            }

            Duration delay;
            try {
                delay = AutoMessageTimingUtil.parseSendTiming(sendTiming);
            } catch (Exception e) {
                autoMessageLogger.error("[AutoMessage] invalid sendTiming, skip. storeId={}, autoMessageId={}, sendTiming={}",
                        storeId, template.getId(), sendTiming);
                continue;
            }

            LocalDateTime earliest = businessAutoMessageService.computeEarliestEventTime(template, now);
            LocalDateTime latest = now.minus(delay);
            if (latest.isBefore(earliest)) {
                continue;
            }

            List<Reservation> candidates = findCandidates(storeId, action, earliest, latest);
            if (candidates.isEmpty()) {
                continue;
            }

            totalTemplates++;
            totalCandidates += candidates.size();

            for (Reservation reservation : candidates) {
                if (reservation == null) {
                    continue;
                }
                if (!shouldConsiderForAction(reservation, action)) {
                    continue;
                }
                businessAutoMessageService.trySendForReservation(storeId, reservation, template, now, delay);
            }
        }

        if (totalTemplates > 0) {
            autoMessageLogger.info("[AutoMessage] tick done. storeId={}, templates={}, candidates={}", storeId, totalTemplates, totalCandidates);
        }
    }

    private List<Reservation> findCandidates(Long storeId, String action, LocalDateTime start, LocalDateTime end) {
        String normalized = action.trim().toUpperCase();
        return switch (normalized) {
            case "BOOKING_CONFIRM" -> reservationRepository.findByStoreIdAndCreatedAtBetween(storeId, start, end);
            case "CHECK_IN" -> reservationRepository.findByStoreIdAndActualCheckInBetween(storeId, start, end);
            case "CHECK_OUT" -> reservationRepository.findByStoreIdAndActualCheckOutBetween(storeId, start, end);
            default -> List.of();
        };
    }

    private boolean shouldConsiderForAction(Reservation reservation, String action) {
        if (reservation.getStatus() == ReservationStatus.CANCELLED || reservation.getStatus() == ReservationStatus.NO_SHOW) {
            return false;
        }
        String normalized = action != null ? action.trim().toUpperCase() : "";
        return switch (normalized) {
            case "BOOKING_CONFIRM" -> true;
            case "CHECK_IN" -> reservation.getActualCheckIn() != null
                    && (reservation.getStatus() == ReservationStatus.CHECKED_IN || reservation.getStatus() == ReservationStatus.CHECKED_OUT);
            case "CHECK_OUT" -> reservation.getActualCheckOut() != null && reservation.getStatus() == ReservationStatus.CHECKED_OUT;
            default -> false;
        };
    }
}
