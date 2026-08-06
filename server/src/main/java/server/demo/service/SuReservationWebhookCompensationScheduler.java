package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class SuReservationWebhookCompensationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(SuReservationWebhookCompensationScheduler.class);

    private final SuReservationWebhookCompensationService compensationService;

    public SuReservationWebhookCompensationScheduler(SuReservationWebhookCompensationService compensationService) {
        this.compensationService = compensationService;
    }

    @Scheduled(initialDelay = 10_000, fixedDelay = 60_000)
    public void run() {
        try {
            // Best-effort: process a small batch periodically.
            compensationService.processDueEventsOnce(50);
        } catch (Exception e) {
            // 调度异常必须留痕：历史上这里曾发生过整批静默回滚、一行日志都没有的事故。
            logger.error("[WebhookCompensate] scheduler run failed: {}", e.getMessage(), e);
        }
    }
}
