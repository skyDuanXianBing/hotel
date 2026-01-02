package server.demo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.dto.ApiResponse;
import server.demo.entity.Store;
import server.demo.repository.StoreRepository;
import server.demo.service.OtaReservationSyncService;
import server.demo.service.SuWebhookIdempotencyService;
import server.demo.util.SuReservationNotifPayloadParser;
import server.demo.util.SuHotelIdUtil;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Su Webhook 接收端（Reservation Notification Push）。
 *
 * 文档参考：docs/预订/预订通知推送.txt
 */
@RestController
@RequestMapping("/api/v1/su/webhook")
public class SuWebhookController {

    private final ObjectMapper objectMapper;
    private final StoreRepository storeRepository;
    private final OtaReservationSyncService otaReservationSyncService;
    private final SuWebhookIdempotencyService idempotencyService;

    public SuWebhookController(
            ObjectMapper objectMapper,
            StoreRepository storeRepository,
            OtaReservationSyncService otaReservationSyncService,
            SuWebhookIdempotencyService idempotencyService
    ) {
        this.objectMapper = objectMapper;
        this.storeRepository = storeRepository;
        this.otaReservationSyncService = otaReservationSyncService;
        this.idempotencyService = idempotencyService;
    }

    @RequestMapping(value = "/reservation-notif/{hotelId}", method = {RequestMethod.GET, RequestMethod.HEAD})
    public ResponseEntity<Map<String, Object>> healthReservationNotif(@PathVariable String hotelId) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "ok",
                "hotelId", hotelId
        ));
    }

    /**
     * Su 预订通知 Push（body 仅包含 reservation_notif_id 列表，不包含 hotelid，因此使用 path 传入）
     */
    @PostMapping("/reservation-notif/{hotelId}")
    public ResponseEntity<ApiResponse<Object>> handleReservationNotif(
            @PathVariable String hotelId,
            @RequestBody String rawBody
    ) {
        String normalizedHotelId = SuHotelIdUtil.normalize(hotelId);
        if (normalizedHotelId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("hotelId 无效"));
        }

        List<String> notifIds;
        try {
            JsonNode root = objectMapper.readTree(rawBody == null ? "{}" : rawBody);
            notifIds = SuReservationNotifPayloadParser.extractNotifIds(root);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("JSON 解析失败: " + e.getMessage()));
        }

        if (notifIds.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("未包含 reservation_notif_id，忽略", Map.of("hotelId", normalizedHotelId)));
        }

        Store store = resolveStoreByHotelId(normalizedHotelId)
                .orElse(null);
        if (store == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("未找到对应门店（stores.su_hotel_id）: " + normalizedHotelId));
        }

        Set<String> toProcess = idempotencyService.markProcessingAndReturnNew(normalizedHotelId, notifIds);
        if (toProcess.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("全部 notif 已处理过（幂等命中）", Map.of(
                    "hotelId", normalizedHotelId,
                    "storeId", store.getId(),
                    "received", notifIds.size(),
                    "processed", 0
            )));
        }

        try {
            OtaReservationSyncService.ReservationSyncResult result = otaReservationSyncService.syncStoreReservations(store.getId(), toProcess);
            if (result.ackErrorMessage() == null && result.ackSuccess() == result.ackRequested()) {
                idempotencyService.markDone(normalizedHotelId, toProcess);
            } else {
                idempotencyService.clearProcessing(normalizedHotelId, toProcess);
            }

            return ResponseEntity.ok(ApiResponse.success("处理成功", Map.of(
                    "hotelId", normalizedHotelId,
                    "storeId", store.getId(),
                    "received", notifIds.size(),
                    "toProcess", toProcess.size(),
                    "sync", result
            )));
        } catch (Exception e) {
            idempotencyService.clearProcessing(normalizedHotelId, toProcess);
            return ResponseEntity.status(500).body(ApiResponse.error("处理失败: " + e.getMessage()));
        }
    }

    private Optional<Store> resolveStoreByHotelId(String hotelId) {
        Optional<Store> byConfig = storeRepository.findBySuHotelId(hotelId);
        if (byConfig.isPresent()) {
            return byConfig;
        }

        // 兼容默认规则：STORE{storeId}（如果门店还未落库 su_hotel_id，则用这个兜底）
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
}
