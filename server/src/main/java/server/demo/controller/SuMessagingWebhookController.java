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
import server.demo.config.SuMessagingWebhookAuthConfig;
import server.demo.entity.Store;
import server.demo.repository.StoreRepository;
import server.demo.service.SuMessagingService;
import server.demo.util.BasicAuthUtil;
import server.demo.util.SuHotelIdUtil;

import java.util.Map;
import java.util.Optional;

/**
 * Su Messaging webhook 接收端（Collection of Messages from OTA）。
 * 文档参考：docs/消息传递.txt
 */
@RestController
@RequestMapping("/api/v1/su/webhook")
public class SuMessagingWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(SuMessagingWebhookController.class);

    private final ObjectMapper objectMapper;
    private final StoreRepository storeRepository;
    private final SuMessagingService suMessagingService;
    private final SuMessagingWebhookAuthConfig authConfig;

    public SuMessagingWebhookController(
            ObjectMapper objectMapper,
            StoreRepository storeRepository,
            SuMessagingService suMessagingService,
            SuMessagingWebhookAuthConfig authConfig
    ) {
        this.objectMapper = objectMapper;
        this.storeRepository = storeRepository;
        this.suMessagingService = suMessagingService;
        this.authConfig = authConfig;
    }

    @PostMapping("/messaging")
    public ResponseEntity<Map<String, String>> handleMessagingWebhook(
            HttpServletRequest request,
            @RequestBody(required = false) String rawBody
    ) {
        if (authConfig.isAuthEnabled() && !BasicAuthUtil.isBasicAuthOk(request, authConfig.getUsername(), authConfig.getPassword())) {
            logger.warn("[SuMessagingWebhook] unauthorized request. remoteIp={}", request.getRemoteAddr());
            return ResponseEntity.status(401).body(Map.of("status", "Fail"));
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(rawBody == null ? "{}" : rawBody);
        } catch (Exception e) {
            logger.error("[SuMessagingWebhook] JSON 解析失败: {}", e.getMessage());
            // 按你的要求：尽量返回 Success，避免 Su 重试堆积
            return ResponseEntity.ok(Map.of("status", "Success"));
        }

        String hotelId = readText(root, "hotelid");
        String normalizedHotelId = SuHotelIdUtil.normalize(hotelId);
        if (normalizedHotelId == null) {
            logger.error("[SuMessagingWebhook] 缺少 hotelid，raw={}", safeTrim(rawBody));
            return ResponseEntity.ok(Map.of("status", "Success"));
        }

        Store store = resolveStoreByHotelId(normalizedHotelId).orElse(null);
        if (store == null) {
            logger.error("[SuMessagingWebhook] 未找到对应门店（stores.su_hotel_id={}），已忽略该消息。raw={}", normalizedHotelId, safeTrim(rawBody));
            return ResponseEntity.ok(Map.of("status", "Success"));
        }

        try {
            suMessagingService.handleInboundMessage(store.getId(), normalizedHotelId, root, rawBody);
        } catch (Exception e) {
            logger.error("[SuMessagingWebhook] 入库失败（但仍返回 Success 以避免重试）. storeId={}, hotelId={}, err={}",
                    store.getId(), normalizedHotelId, e.getMessage(), e);
        }

        return ResponseEntity.ok(Map.of("status", "Success"));
    }

    private Optional<Store> resolveStoreByHotelId(String hotelId) {
        Optional<Store> byConfig = storeRepository.findBySuHotelId(hotelId);
        if (byConfig.isPresent()) {
            return byConfig;
        }

        // 兼容默认规则：STORE{storeId}（若门店尚未落库 su_hotel_id，则用此兜底）
        if (hotelId != null && hotelId.startsWith("STORE")) {
            String suffix = hotelId.substring("STORE".length());
            try {
                Long storeId = Long.parseLong(suffix);
                Optional<Store> byId = storeRepository.findById(storeId);
                byId.ifPresent(store -> {
                    if (store.getSuHotelId() == null || store.getSuHotelId().isBlank()) {
                        store.setSuHotelId(hotelId);
                        storeRepository.save(store);
                    }
                });
                return byId;
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    private static String readText(JsonNode root, String field) {
        if (root == null || field == null) {
            return null;
        }
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText(null);
        return value != null && !value.isBlank() ? value.trim() : null;
    }

    private static String safeTrim(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.length() <= 1000) {
            return trimmed;
        }
        return trimmed.substring(0, 1000) + "...";
    }
}
