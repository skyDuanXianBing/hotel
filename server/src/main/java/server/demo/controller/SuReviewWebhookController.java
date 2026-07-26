package server.demo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.demo.config.SuReviewWebhookAuthConfig;
import server.demo.entity.Store;
import server.demo.service.SuReviewHotelOwnershipValidator;
import server.demo.service.SuReviewService;
import server.demo.service.SuReviewWebhookMappingValidator;
import server.demo.service.SuReviewWebhookMappingValidator.MappingRejectedException;
import server.demo.util.SuHotelIdUtil;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/su/webhook")
public class SuReviewWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(SuReviewWebhookController.class);

    private final ObjectMapper objectMapper;
    private final SuReviewService reviewService;
    private final SuReviewWebhookAuthConfig authConfig;
    private final SuReviewHotelOwnershipValidator hotelOwnershipValidator;
    private final SuReviewWebhookMappingValidator mappingValidator;

    public SuReviewWebhookController(
            ObjectMapper objectMapper,
            SuReviewService reviewService,
            SuReviewWebhookAuthConfig authConfig,
            SuReviewHotelOwnershipValidator hotelOwnershipValidator,
            SuReviewWebhookMappingValidator mappingValidator
    ) {
        this.objectMapper = objectMapper;
        this.reviewService = reviewService;
        this.authConfig = authConfig;
        this.hotelOwnershipValidator = hotelOwnershipValidator;
        this.mappingValidator = mappingValidator;
    }

    @PostMapping("/reviews")
    public ResponseEntity<Map<String, Object>> handle(
            HttpServletRequest request,
            @RequestBody(required = false) String rawBody
    ) {
        if (!authConfig.isConfigured()) {
            logger.error(
                    "[SuReviewWebhook][SECURITY] disabled because Review Authorization is not configured. remoteIp={}",
                    remoteIp(request)
            );
            return ResponseEntity.status(503).body(failBody("Review webhook is not configured"));
        }
        String presentedAuthorization = request != null ? request.getHeader("Authorization") : null;
        if (!authConfig.matches(presentedAuthorization)) {
            logger.warn("[SuReviewWebhook] unauthorized. remoteIp={}", remoteIp(request));
            return ResponseEntity.status(401).body(failBody("Unauthorized"));
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(rawBody == null ? "{}" : rawBody);
        } catch (Exception e) {
            logger.warn("[SuReviewWebhook] invalid JSON. remoteIp={}, err={}", remoteIp(request), e.getMessage());
            return ResponseEntity.badRequest().body(failBody("Invalid JSON"));
        }

        String normalizedHotelId = SuHotelIdUtil.normalize(readHotelId(root));
        if (normalizedHotelId == null) {
            return ResponseEntity.badRequest().body(failBody("Missing hotel_id"));
        }
        Optional<Store> store = resolveStore(normalizedHotelId);
        if (store.isEmpty()) {
            logger.warn(
                    "[SuReviewWebhook] store route failed. remoteIp={}, hotelId={}",
                    remoteIp(request),
                    normalizedHotelId
            );
            return ResponseEntity.status(403).body(failBody("Review webhook mapping rejected"));
        }

        try {
            mappingValidator.validate(store.get().getId(), normalizedHotelId, root);
        } catch (MappingRejectedException e) {
            logger.warn(
                    "[SuReviewWebhook][SECURITY] mapping rejected. remoteIp={}, storeId={}, hotelId={}, reason={}",
                    remoteIp(request),
                    store.get().getId(),
                    normalizedHotelId,
                    e.getMessage()
            );
            return ResponseEntity.status(403).body(failBody("Review webhook mapping rejected"));
        } catch (IllegalArgumentException e) {
            logger.warn(
                    "[SuReviewWebhook] invalid mapping payload. remoteIp={}, storeId={}, hotelId={}, err={}",
                    remoteIp(request),
                    store.get().getId(),
                    normalizedHotelId,
                    e.getMessage()
            );
            return ResponseEntity.badRequest().body(failBody(e.getMessage()));
        }

        try {
            SuReviewService.WebhookResult result = reviewService.handleWebhook(
                    store.get().getId(),
                    normalizedHotelId,
                    root
            );
            logger.info(
                    "[SuReviewWebhook] persisted. storeId={}, hotelId={}, processed={}, created={}, updated={}, duplicates={}",
                    store.get().getId(),
                    normalizedHotelId,
                    result.processed(),
                    result.created(),
                    result.updated(),
                    result.duplicates()
            );
            return ResponseEntity.ok(Map.of(
                    "status", "Success",
                    "processed", result.processed(),
                    "duplicates", result.duplicates()
            ));
        } catch (IllegalArgumentException e) {
            logger.warn(
                    "[SuReviewWebhook] rejected. storeId={}, hotelId={}, err={}",
                    store.get().getId(),
                    normalizedHotelId,
                    e.getMessage()
            );
            return ResponseEntity.badRequest().body(failBody(e.getMessage()));
        } catch (Exception e) {
            // Review webhook only ACKs after the normalized review is durably persisted.
            logger.error(
                    "[SuReviewWebhook] persistence failed; returning non-2xx for Su retry. storeId={}, hotelId={}, err={}",
                    store.get().getId(),
                    normalizedHotelId,
                    e.getMessage(),
                    e
            );
            return ResponseEntity.status(500).body(failBody("Review persistence failed"));
        }
    }

    private Optional<Store> resolveStore(String hotelId) {
        return hotelOwnershipValidator.resolveUniqueOwner(hotelId);
    }

    private static String readHotelId(JsonNode root) {
        if (root == null || root.isNull()) {
            return null;
        }
        if (root.isArray()) {
            for (JsonNode node : root) {
                String value = readDirectHotelId(node);
                if (value != null) {
                    return value;
                }
            }
            return null;
        }
        String direct = readDirectHotelId(root);
        if (direct != null) {
            return direct;
        }
        JsonNode reviews = root.get("reviews");
        if (reviews == null) {
            reviews = root.get("Reviews");
        }
        if (reviews != null && reviews.isArray()) {
            for (JsonNode node : reviews) {
                String value = readDirectHotelId(node);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    private static String readDirectHotelId(JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }
        for (String field : List.of("hotel_id", "hotelid", "hotelId")) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull()) {
                String text = value.asText(null);
                if (text != null && !text.trim().isBlank()) {
                    return text.trim();
                }
            }
        }
        return null;
    }

    private static Map<String, Object> failBody(String message) {
        String safeMessage = message == null || message.isBlank() ? "Fail" : message;
        return Map.of(
                "Status", "Fail",
                "Errors", Map.of("ShortText", safeMessage)
        );
    }

    private static String remoteIp(HttpServletRequest request) {
        return request != null ? request.getRemoteAddr() : null;
    }
}
