package server.demo.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class SuReservationWebhookCompensationScheduler {

    private final SuReservationWebhookCompensationService compensationService;

    public SuReservationWebhookCompensationScheduler(SuReservationWebhookCompensationService compensationService) {
        this.compensationService = compensationService;
    }

    @Scheduled(initialDelay = 10_000, fixedDelay = 60_000)
    public void run() {
        // Best-effort: process a small batch periodically.
        compensationService.processDueEventsOnce(50);
    }
}

