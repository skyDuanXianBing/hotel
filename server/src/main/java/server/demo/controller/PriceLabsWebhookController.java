package server.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.config.PriceLabsConfig;
import server.demo.dto.ApiResponse;
import server.demo.dto.PriceLabsWebhookRequest;
import server.demo.service.PriceLabsService;
import server.demo.service.PriceLabsSyncService;
import server.demo.util.PriceLabsIdUtil;
import server.demo.util.PriceLabsSignatureVerifier;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

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
            @RequestBody String rawBody) {

        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("=== PriceLabs Webhook Sync Request ===");
            System.out.println("Signature: " + signature);
            System.out.println("Raw Body: " + rawBody);
            System.out.println("Body length: " + (rawBody != null ? rawBody.length() : "null"));
            System.out.println("Contains 'verify': " + (rawBody != null && rawBody.contains("verify")));

            // 检查是否是PriceLabs的验证请求 (只有 {"verify": true},没有签名)
            if (rawBody != null && rawBody.trim().contains("\"verify\"")) {
                System.out.println("✓ Verification request detected - returning success");
                response.put("success", true);
                response.put("message", "Webhook endpoint verified");
                return ResponseEntity.ok(response);
            }

            if (signature == null || signature.isBlank()) {
                System.out.println("✗ No signature provided and not a verification request");
                response.put("success", false);
                response.put("message", "缺少签名");
                return ResponseEntity.status(401).body(response);
            }

            // 1. 验证签名
            PriceLabsSignatureVerifier verifier = new PriceLabsSignatureVerifier(config.getIntegrationToken());
            if (!verifier.verifySignature(rawBody, signature)) {
                response.put("success", false);
                response.put("message", "签名验证失败");
                return ResponseEntity.status(401).body(response);
            }

            // 2. 解析请求数据
            PriceLabsWebhookRequest webhookData = objectMapper.readValue(rawBody, PriceLabsWebhookRequest.class);

            // 3. 处理价格更新
            priceLabsService.handleWebhookPriceUpdate(webhookData);

            response.put("success", true);
            response.put("message", "价格更新已接收");
            response.put("processed_count", webhookData.getData() != null ? webhookData.getData().size() : 0);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "处理失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
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
            @RequestBody String rawBody) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 检查是否是PriceLabs的验证请求
            if (rawBody != null && rawBody.trim().contains("\"verify\"")) {
                response.put("success", true);
                response.put("message", "Webhook endpoint verified");
                return ResponseEntity.ok(response);
            }

            if (signature == null || signature.isBlank()) {
                response.put("success", false);
                response.put("message", "缺少签名");
                return ResponseEntity.status(401).body(response);
            }

            // 验证签名
            PriceLabsSignatureVerifier verifier = new PriceLabsSignatureVerifier(config.getIntegrationToken());
            if (!verifier.verifySignature(rawBody, signature)) {
                response.put("success", false);
                response.put("message", "签名验证失败");
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

            if (storeId == null) {
                logger.warn("[PriceLabsWebhook] calendar-trigger missing/invalid listing_id, skip sync. request={}", requestData);
                response.put("message", "日历刷新请求已接收，但缺少可解析的 listing_id，已忽略");
                return ResponseEntity.ok(response);
            }

            try {
                priceLabsSyncService.syncCalendar(storeId, startDate, endDate);
                response.put("message", "日历刷新已触发");
                response.put("storeId", storeId);
                response.put("startDate", startDate.toString());
                response.put("endDate", endDate.toString());
            } catch (Exception ex) {
                logger.error("[PriceLabsWebhook] calendar-trigger sync failed. storeId={}, err={}", storeId, ex.getMessage(), ex);
                response.put("message", "日历刷新请求已接收，但同步失败（已记录日志）");
                response.put("storeId", storeId);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("[PriceLabsWebhook] calendar-trigger handler failed", e);
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
            @RequestBody String rawBody) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 检查是否是PriceLabs的验证请求
            if (rawBody != null && rawBody.trim().contains("\"verify\"")) {
                response.put("success", true);
                response.put("message", "Webhook endpoint verified");
                return ResponseEntity.ok(response);
            }

            if (signature == null || signature.isBlank()) {
                response.put("success", false);
                response.put("message", "缺少签名");
                return ResponseEntity.status(401).body(response);
            }

            // 验证签名
            PriceLabsSignatureVerifier verifier = new PriceLabsSignatureVerifier(config.getIntegrationToken());
            if (!verifier.verifySignature(rawBody, signature)) {
                response.put("success", false);
                response.put("message", "签名验证失败");
                return ResponseEntity.status(401).body(response);
            }

            // 解析错误通知
            @SuppressWarnings("unchecked")
            Map<String, Object> errorData = objectMapper.readValue(rawBody, Map.class);

            // 记录错误日志
            System.err.println("收到 PriceLabs 错误通知: " + errorData);

            response.put("success", true);
            response.put("message", "错误通知已接收");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "处理失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
