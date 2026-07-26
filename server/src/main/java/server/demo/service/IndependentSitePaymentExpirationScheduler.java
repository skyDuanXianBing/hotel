package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import server.demo.enums.PaymentAttemptStatus;
import server.demo.repository.PaymentAttemptRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class IndependentSitePaymentExpirationScheduler {

    private static final Logger logger =
            LoggerFactory.getLogger(IndependentSitePaymentExpirationScheduler.class);
    private static final int BATCH_SIZE = 100;

    private final PaymentAttemptRepository paymentAttemptRepository;
    private final IndependentSiteBookingService bookingService;
    private final Clock clock;

    public IndependentSitePaymentExpirationScheduler(
            PaymentAttemptRepository paymentAttemptRepository,
            IndependentSiteBookingService bookingService,
            Clock clock
    ) {
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.bookingService = bookingService;
        this.clock = clock;
    }

    @Scheduled(initialDelay = 60_000, fixedDelay = 60_000)
    public void expireDueAttempts() {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        List<String> references = paymentAttemptRepository.findExpiredPublicReferences(
                PaymentAttemptStatus.PENDING,
                now,
                PageRequest.of(0, BATCH_SIZE)
        );
        for (String reference : references) {
            try {
                bookingService.expirePaymentAttempt(reference);
            } catch (Exception e) {
                logger.warn(
                        "Expire independent-site payment attempt failed. reference={}, error={}",
                        reference,
                        e.getMessage()
                );
            }
        }
    }
}
