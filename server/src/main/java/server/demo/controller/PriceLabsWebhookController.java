package server.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.config.PriceLabsConfig;
import server.demo.constants.PriceLabsSyncDefaults;
import server.demo.dto.ApiResponse;
import server.demo.dto.PriceLabsWebhookRequest;
import server.demo.service.PriceLabsWebhookAsyncProcessor;
import server.demo.service.PriceLabsWebhookAsyncService;
import server.demo.service.PriceLabsWebhookTaskTracker;
import server.demo.service.PriceLabsService;
import server.demo.service.PriceLabsSyncService;
import server.demo.util.PriceLabsIdUtil;
import server.demo.util.PriceLabsSignatureVerifier;
import server.demo.util.PriceLabsWebhookPayloadNormalizer;
import server.demo.util.PriceLabsWebhookListingIdsParser;
import server.demo.util.PriceLabsWebhookSignatureVerifier;

import java.time.LocalDate;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * PriceLabs Webhook 控制器
 * 接收来自 PriceLabs 的推送通知
 */
@RestController
@RequestMapping("/api/v1/pricelabs/webhook")
@CrossOrigin(origins = {"http://localhost:8091", "http://127.0.0.1:8091"}, allowCredentials = "true")
public class PriceLabsWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(PriceLabsWebhookController.class);

    @Autowired
    private PriceLabsConfig config;

    @Autowired
    private PriceLabsService priceLabsService;

    @Autowired
    private PriceLabsSyncService priceLabsSyncService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PriceLabsWebhookAsyncProcessor priceLabsWebhookAsyncProcessor;

    @Autowired
    private PriceLabsWebhookAsyncService priceLabsWebhookAsyncService;

    @Autowired
    private PriceLabsWebhookTaskTracker priceLabsWebhookTaskTracker;

    /**
     * 接收 PriceLabs 价格更新推送
     * POST /api/v1/pricelabs/webhook/sync
     */
    @RequestMapping(value = "/sync", method = {RequestMethod.GET, RequestMethod.HEAD})
    public ResponseEntity<Map<String, Object>> healthSync() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "ok");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> handlePriceSync(
            @RequestHeader(value = "X-Signature", required = false) String signature,
            @RequestHeader(value = "X-PL-SIGNED-HEADERS", required = false) String plSignedHeaders,
            @RequestHeader(value = "X-PL-SIGNED-BODY", required = false) String plSignedBody,
            @RequestHeader(value = "X-SOURCE", required = false) String plSource,
            @RequestHeader(value = "X-PL-TIMESTAMP", required = false) String plTimestamp,
            @RequestHeader(value = "X-PL-REQUESTID", required = false) String plRequestId,
            @RequestBody String rawBody,
            HttpServletRequest httpRequest) {

        Map<String, Object> response = new HashMap<>();
        String traceId = java.util.UUID.randomUUID().toString().replace("-", "");
        long startedAt = System.currentTimeMillis();

        try {
            if (config.isDebug() && httpRequest != null) {
                logger.info("[PriceLabsWebhook][{}] /sync headers={}", traceId, readHeadersForLog(httpRequest));
            }

            if (config.isDebug()) {
                String snippet = rawBody == null ? "null" : rawBody.substring(0, Math.min(1200, rawBody.length()));
                logger.info("[PriceLabsWebhook][{}] /sync received. signaturePresent={}, bodyLen={}, bodySnippet={}",
                        traceId,
                        signature != null && !signature.isBlank(),
                        rawBody != null ? rawBody.length() : -1,
                        snippet);
            } else {
                logger.info("[PriceLabsWebhook][{}] /sync received. signaturePresent={}, bodyLen={}",
                        traceId,
                        signature != null && !signature.isBlank(),
                        rawBody != null ? rawBody.length() : -1);
            }

            // 检查是否是PriceLabs的验证请求 (只有 {"verify": true},没有签名)
            if (rawBody != null && rawBody.trim().contains("\"verify\"")) {
                logger.info("[PriceLabsWebhook][{}] /sync verification request detected", traceId);
                response.put("success", true);
                response.put("message", "Webhook endpoint verified");
                return ResponseEntity.ok(response);
            }

            boolean hasPlSignature = (plSignedHeaders != null && !plSignedHeaders.isBlank())
                    || (plSignedBody != null && !plSignedBody.isBlank());
            if (hasPlSignature) {
                if (plSignedHeaders == null || plSignedHeaders.isBlank()) {
                    logger.warn("[PriceLabsWebhook][{}] /sync missing X-PL-SIGNED-HEADERS", traceId);
                    response.put("success", false);
                    response.put("message", "缺少签名");
                    return ResponseEntity.status(401).body(response);
                }
                if (plSignedBody == null || plSignedBody.isBlank()) {
                    logger.warn("[PriceLabsWebhook][{}] /sync missing X-PL-SIGNED-BODY", traceId);
                    response.put("success", false);
                    response.put("message", "缺少签名");
                    return ResponseEntity.status(401).body(response);
                }

                PriceLabsWebhookSignatureVerifier verifier = new PriceLabsWebhookSignatureVerifier(config.getIntegrationToken());
                if (!verifier.verify(plSignedHeaders, plSignedBody, plSource, plTimestamp, plRequestId, rawBody)) {
                    logger.warn("[PriceLabsWebhook][{}] /sync X-PL signature verification failed", traceId);
                    response.put("success", false);
                    response.put("message", "签名验证失败");
                    if (config.isDebug()) {
                        response.put("traceId", traceId);
                    }
                    return ResponseEntity.status(401).body(response);
                }
            } else {
                boolean skipSignature = false;
                if (signature == null || signature.isBlank()) {
                    if (config.isDebug()) {
                        skipSignature = true;
                        logger.warn("[PriceLabsWebhook][{}] /sync missing signature (debug mode: skip signature verification)", traceId);
                    } else {
                        logger.warn("[PriceLabsWebhook][{}] /sync missing signature", traceId);
                        response.put("success", false);
                        response.put("message", "缺少签名");
                        return ResponseEntity.status(401).body(response);
                    }
                }

                if (!skipSignature) {
                    PriceLabsSignatureVerifier verifier = new PriceLabsSignatureVerifier(config.getIntegrationToken());
                    if (!verifier.verifySignature(rawBody, signature)) {
                        logger.warn("[PriceLabsWebhook][{}] /sync signature verification failed", traceId);
                        response.put("success", false);
                        response.put("message", "签名验证失败");
                        if (config.isDebug()) {
                            response.put("traceId", traceId);
                        }
                        return ResponseEntity.status(401).body(response);
                    }
                }
            }

            // 快速 ACK：避免同步处理耗时/异常导致 PriceLabs 判定 rejected
            priceLabsWebhookTaskTracker.markPending(traceId, "sync");
            try {
                priceLabsWebhookAsyncProcessor.submit(
                        "sync-" + traceId,
                        () -> priceLabsWebhookAsyncService.processSync(traceId, rawBody)
                );
            } catch (Exception enqueueEx) {
                priceLabsWebhookTaskTracker.markFailed(traceId, enqueueEx.getMessage());
                response.put("success", false);
                response.put("message", "enqueue failed: " + enqueueEx.getMessage());
                response.put("traceId", traceId);
                return ResponseEntity.status(500).body(response);
            }

            response.put("success", true);
            response.put("message", "accepted");
            response.put("traceId", traceId);
            response.put("status", "PENDING");
            logger.info("[PriceLabsWebhook][{}] /sync accepted for async processing. costMs={}",
                    traceId,
                    System.currentTimeMillis() - startedAt);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("[PriceLabsWebhook][{}] /sync failed. costMs={}, err={}",
                    traceId,
                    System.currentTimeMillis() - startedAt,
                    e.getMessage(),
                    e);
            response.put("success", false);
            response.put("message", "处理失败: " + e.getMessage());
            response.put("traceId", traceId);
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/sync/task/{traceId}")
    public ResponseEntity<Map<String, Object>> getSyncTaskStatus(@PathVariable("traceId") String traceId) {
        Map<String, Object> response = new HashMap<>();
        return priceLabsWebhookTaskTracker.get(traceId)
                .<ResponseEntity<Map<String, Object>>>map(state -> {
                    response.put("success", true);
                    response.put("data", state);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    response.put("success", false);
                    response.put("message", "task not found");
                    response.put("traceId", traceId);
                    return ResponseEntity.status(404).body(response);
                });
    }

    private static Map<String, String> readHeadersForLog(HttpServletRequest request) {
        Map<String, String> out = new LinkedHashMap<>();
        if (request == null) {
            return out;
        }
        Enumeration<String> names = request.getHeaderNames();
        if (names == null) {
            return out;
        }
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (name == null) {
                continue;
            }
            String value = request.getHeader(name);
            out.put(name, redactHeaderValue(name, value));
        }
        return out;
    }

    private static String redactHeaderValue(String name, String value) {
        if (value == null) {
            return null;
        }
        String n = name == null ? "" : name.toLowerCase();
        Set<String> sensitive = Set.of(
                "authorization",
                "cookie",
                "set-cookie",
                "x-signature",
                "x-pl-signed-headers",
                "x-pl-signed-body",
                "x-integration-token",
                "x-api-key"
        );
        if (!sensitive.contains(n)) {
            return value.length() > 200 ? value.substring(0, 200) + "..." : value;
        }
        String v = value.trim();
        if (v.length() <= 8) {
            return "****";
        }
        return v.substring(0, 4) + "..." + v.substring(v.length() - 4) + "(len=" + v.length() + ")";
    }

    /**
     * 接收日历刷新请求
     * POST /api/v1/pricelabs/webhook/calendar-trigger
     */
    @RequestMapping(value = "/calendar-trigger", method = {RequestMethod.GET, RequestMethod.HEAD})
    public ResponseEntity<Map<String, Object>> healthCalendarTrigger() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "ok");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/calendar-trigger")
    public ResponseEntity<Map<String, Object>> handleCalendarTrigger(
            @RequestHeader(value = "X-Signature", required = false) String signature,
            @RequestHeader(value = "X-PL-SIGNED-HEADERS", required = false) String plSignedHeaders,
            @RequestHeader(value = "X-PL-SIGNED-BODY", required = false) String plSignedBody,
            @RequestHeader(value = "X-SOURCE", required = false) String plSource,
            @RequestHeader(value = "X-PL-TIMESTAMP", required = false) String plTimestamp,
            @RequestHeader(value = "X-PL-REQUESTID", required = false) String plRequestId,
            @RequestBody String rawBody,
            HttpServletRequest httpRequest) {

        Map<String, Object> response = new HashMap<>();
        String traceId = java.util.UUID.randomUUID().toString().replace("-", "");
        long startedAt = System.currentTimeMillis();

        try {
            if (config.isDebug() && httpRequest != null) {
                logger.info("[PriceLabsWebhook][{}] /calendar-trigger headers={}", traceId, readHeadersForLog(httpRequest));
            }

            boolean signaturePresent = (plSignedHeaders != null && !plSignedHeaders.isBlank())
                    || (plSignedBody != null && !plSignedBody.isBlank());
            if (config.isDebug()) {
                String snippet = rawBody == null ? "null" : rawBody.substring(0, Math.min(1200, rawBody.length()));
                logger.info("[PriceLabsWebhook][{}] /calendar-trigger received. signaturePresent={}, bodyLen={}, bodySnippet={}",
                        traceId,
                        signaturePresent,
                        rawBody != null ? rawBody.length() : -1,
                        snippet);
            } else {
                logger.info("[PriceLabsWebhook][{}] /calendar-trigger received. signaturePresent={}, bodyLen={}",
                        traceId,
                        signaturePresent,
                        rawBody != null ? rawBody.length() : -1);
            }
            // 检查是否是PriceLabs的验证请求
            if (rawBody != null && rawBody.trim().contains("\"verify\"")) {
                response.put("success", true);
                response.put("message", "Webhook endpoint verified");
                return ResponseEntity.ok(response);
            }

            if (plSignedHeaders == null || plSignedHeaders.isBlank()
                    || plSignedBody == null || plSignedBody.isBlank()
                    || plSource == null || plSource.isBlank()
                    || plTimestamp == null || plTimestamp.isBlank()
                    || plRequestId == null || plRequestId.isBlank()) {
                logger.warn("[PriceLabsWebhook][{}] /calendar-trigger missing X-PL signature headers", traceId);
                response.put("success", false);
                response.put("message", "缺少签名");
                return ResponseEntity.status(401).body(response);
            }

            PriceLabsWebhookSignatureVerifier verifier = new PriceLabsWebhookSignatureVerifier(config.getIntegrationToken());
            if (!verifier.verify(plSignedHeaders, plSignedBody, plSource, plTimestamp, plRequestId, rawBody)) {
                logger.warn("[PriceLabsWebhook][{}] /calendar-trigger X-PL signature verification failed", traceId);
                response.put("success", false);
                response.put("message", "签名验证失败");
                if (config.isDebug()) {
                    response.put("traceId", traceId);
                }
                return ResponseEntity.status(401).body(response);
            }

            // 解析请求
            @SuppressWarnings("unchecked")
            Map<String, Object> requestData = objectMapper.readValue(rawBody, Map.class);

            String listingId = readString(requestData, "listing_id", "listingId");
            if (listingId == null) {
                Object data = requestData.get("data");
                if (data instanceof Map<?, ?> map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> inner = (Map<String, Object>) map;
                    listingId = readString(inner, "listing_id", "listingId");
                }
            }

            Long storeId = listingId != null ? PriceLabsIdUtil.parseStoreId(listingId).orElse(null) : null;
            LocalDate startDate = parseDate(readString(requestData, "start_date", "startDate", "start"));
            LocalDate endDate = parseDate(readString(requestData, "end_date", "endDate", "end"));

            if (startDate == null) {
                startDate = LocalDate.now();
            }
            if (endDate == null) {
                endDate = startDate.plusYears(1).minusDays(1);
            }

            response.put("success", true);

            if (listingId == null) {
                logger.warn("[PriceLabsWebhook] calendar-trigger missing/invalid listing_id, skip sync. request={}", requestData);
                response.put("message", "日历刷新请求已接收，但缺少可解析的 listing_id，已忽略");
                if (config.isDebug()) {
                    response.put("traceId", traceId);
                }
                return ResponseEntity.ok(response);
            }

            try {
                if (listingId != null) {
                    priceLabsSyncService.syncCalendarForListingIds(java.util.List.of(listingId), startDate, endDate);
                } else {
                    priceLabsSyncService.syncCalendar(storeId, startDate, endDate);
                }
                response.put("message", "日历刷新已触发");
                response.put("listingId", listingId);
                if (storeId != null) {
                    response.put("storeId", storeId);
                }
                response.put("startDate", startDate.toString());
                response.put("endDate", endDate.toString());
                if (config.isDebug()) {
                    response.put("traceId", traceId);
                }
                logger.info("[PriceLabsWebhook][{}] /calendar-trigger sync ok. storeId={}, listingId={}, range={}~{}, costMs={}",
                        traceId,
                        storeId,
                        listingId,
                        startDate,
                        endDate,
                        System.currentTimeMillis() - startedAt);
            } catch (Exception ex) {
                logger.error("[PriceLabsWebhook] calendar-trigger sync failed. storeId={}, listingId={}, err={}", storeId, listingId, ex.getMessage(), ex);
                response.put("success", false);
                response.put("message", "日历刷新同步失败: " + ex.getMessage());
                response.put("listingId", listingId);
                if (storeId != null) {
                    response.put("storeId", storeId);
                }
                if (config.isDebug()) {
                    response.put("traceId", traceId);
                    return ResponseEntity.status(500).body(response);
                }
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("[PriceLabsWebhook] calendar-trigger handler failed", e);
            if (config.isDebug()) {
                response.put("success", false);
                response.put("traceId", traceId);
                response.put("message", "日历刷新处理失败: " + e.getMessage());
                return ResponseEntity.status(500).body(response);
            }
            response.put("success", true);
            response.put("message", "日历刷新请求已接收，但处理失败（已记录日志）");
            return ResponseEntity.ok(response);
        }
    }

    private static String readString(Map<String, Object> obj, String... keys) {
        if (obj == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (key == null) {
                continue;
            }
            Object v = obj.get(key);
            if (v instanceof String s) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) {
                    return trimmed;
                }
            }
        }
        return null;
    }

    private static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        if (s.length() >= 10) {
            s = s.substring(0, 10);
        }
        try {
            return LocalDate.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 接收错误通知
     * POST /api/v1/pricelabs/webhook/hook
     */
    @RequestMapping(value = "/hook", method = {RequestMethod.GET, RequestMethod.HEAD})
    public ResponseEntity<Map<String, Object>> healthHook() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "ok");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/hook")
    public ResponseEntity<Map<String, Object>> handleHook(
            @RequestHeader(value = "X-Signature", required = false) String signature,
            @RequestHeader(value = "X-PL-SIGNED-HEADERS", required = false) String plSignedHeaders,
            @RequestHeader(value = "X-PL-SIGNED-BODY", required = false) String plSignedBody,
            @RequestHeader(value = "X-SOURCE", required = false) String plSource,
            @RequestHeader(value = "X-PL-TIMESTAMP", required = false) String plTimestamp,
            @RequestHeader(value = "X-PL-REQUESTID", required = false) String plRequestId,
            @RequestBody String rawBody,
            HttpServletRequest httpRequest) {

        Map<String, Object> response = new HashMap<>();
        String traceId = java.util.UUID.randomUUID().toString().replace("-", "");

        try {
            if (config.isDebug() && httpRequest != null) {
                logger.info("[PriceLabsWebhook][{}] /hook headers={}", traceId, readHeadersForLog(httpRequest));
            }

            boolean signaturePresent = (plSignedHeaders != null && !plSignedHeaders.isBlank())
                    || (plSignedBody != null && !plSignedBody.isBlank());
            if (config.isDebug()) {
                String snippet = rawBody == null ? "null" : rawBody.substring(0, Math.min(1200, rawBody.length()));
                logger.info("[PriceLabsWebhook][{}] /hook received. signaturePresent={}, bodyLen={}, bodySnippet={}",
                        traceId,
                        signaturePresent,
                        rawBody != null ? rawBody.length() : -1,
                        snippet);
            } else {
                logger.info("[PriceLabsWebhook][{}] /hook received. signaturePresent={}, bodyLen={}",
                        traceId,
                        signaturePresent,
                        rawBody != null ? rawBody.length() : -1);
            }
            // 检查是否是PriceLabs的验证请求
            if (rawBody != null && rawBody.trim().contains("\"verify\"")) {
                response.put("success", true);
                response.put("message", "Webhook endpoint verified");
                return ResponseEntity.ok(response);
            }

            if (plSignedHeaders == null || plSignedHeaders.isBlank()
                    || plSignedBody == null || plSignedBody.isBlank()
                    || plSource == null || plSource.isBlank()
                    || plTimestamp == null || plTimestamp.isBlank()
                    || plRequestId == null || plRequestId.isBlank()) {
                logger.warn("[PriceLabsWebhook][{}] /hook missing X-PL signature headers", traceId);
                response.put("success", false);
                response.put("message", "缺少签名");
                return ResponseEntity.status(401).body(response);
            }

            PriceLabsWebhookSignatureVerifier verifier = new PriceLabsWebhookSignatureVerifier(config.getIntegrationToken());
            if (!verifier.verify(plSignedHeaders, plSignedBody, plSource, plTimestamp, plRequestId, rawBody)) {
                logger.warn("[PriceLabsWebhook][{}] /hook X-PL signature verification failed", traceId);
                response.put("success", false);
                response.put("message", "签名验证失败");
                if (config.isDebug()) {
                    response.put("traceId", traceId);
                }
                return ResponseEntity.status(401).body(response);
            }

            // 解析错误通知
            @SuppressWarnings("unchecked")
            Map<String, Object> errorData = objectMapper.readValue(rawBody, Map.class);

            // 记录错误日志
            logger.warn("[PriceLabsWebhook][{}] received PriceLabs error hook: {}", traceId, errorData);

            response.put("success", true);
            response.put("message", "错误通知已接收");
            if (config.isDebug()) {
                response.put("traceId", traceId);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "处理失败: " + e.getMessage());
            if (config.isDebug()) {
                response.put("traceId", traceId);
            }
            return ResponseEntity.status(500).body(response);
        }
    }
}
