package server.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import server.demo.constants.PriceLabsSyncDefaults;
import server.demo.dto.PriceLabsWebhookRequest;
import server.demo.util.PriceLabsWebhookListingIdsParser;
import server.demo.util.PriceLabsWebhookPayloadNormalizer;

/**
 * PriceLabs /webhook/sync 的真实业务处理（异步执行）。
 */
@Service
public class PriceLabsWebhookAsyncService {

    private static final Logger logger = LoggerFactory.getLogger(PriceLabsWebhookAsyncService.class);

    private final PriceLabsService priceLabsService;
    private final PriceLabsSyncService priceLabsSyncService;
    private final PriceLabsWebhookTaskTracker taskTracker;
    private final ObjectMapper objectMapper;

    public PriceLabsWebhookAsyncService(
            PriceLabsService priceLabsService,
            PriceLabsSyncService priceLabsSyncService,
            PriceLabsWebhookTaskTracker taskTracker,
            ObjectMapper objectMapper
    ) {
        this.priceLabsService = priceLabsService;
        this.priceLabsSyncService = priceLabsSyncService;
        this.taskTracker = taskTracker;
        this.objectMapper = objectMapper;
    }

    public void processSync(String traceId, String rawBody) {
        long startedAt = System.currentTimeMillis();
        taskTracker.markRunning(traceId);

        try {
            PriceLabsWebhookListingIdsParser.ListingIdsPayload listingPayload =
                    PriceLabsWebhookListingIdsParser.parse(objectMapper, rawBody);
            if (!listingPayload.hasPriceData() && !listingPayload.listingIds().isEmpty()) {
                int syncDays = PriceLabsSyncDefaults.DEFAULT_SYNC_DAYS;
                priceLabsSyncService.pullPricesForListingIds(listingPayload.listingIds(), syncDays);
                priceLabsSyncService.syncCalendarForListingIds(listingPayload.listingIds(), syncDays);

                String message = "listing_ids processed. listingCount=" + listingPayload.listingIds().size();
                taskTracker.markSuccess(traceId, message);
                logger.info(
                        "[PriceLabsWebhook][{}] async /sync listing_ids processed. listingCount={}, costMs={}",
                        traceId,
                        listingPayload.listingIds().size(),
                        System.currentTimeMillis() - startedAt
                );
                return;
            }

            PriceLabsWebhookPayloadNormalizer.NormalizedPayload normalized =
                    PriceLabsWebhookPayloadNormalizer.normalizeSyncPayload(objectMapper, rawBody);
            PriceLabsWebhookRequest webhookData = normalized.request();
            priceLabsService.handleWebhookPriceUpdate(webhookData);

            int processedCount = webhookData.getData() != null ? webhookData.getData().size() : 0;
            String message = "price_update processed. format=" + normalized.format() + ", processed_count=" + processedCount;
            taskTracker.markSuccess(traceId, message);
            logger.info(
                    "[PriceLabsWebhook][{}] async /sync processed ok. format={}, processed_count={}, costMs={}",
                    traceId,
                    normalized.format(),
                    processedCount,
                    System.currentTimeMillis() - startedAt
            );
        } catch (Exception e) {
            String err = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            taskTracker.markFailed(traceId, err);
            logger.error(
                    "[PriceLabsWebhook][{}] async /sync failed. costMs={}, err={}",
                    traceId,
                    System.currentTimeMillis() - startedAt,
                    err,
                    e
            );
        }
    }
}

