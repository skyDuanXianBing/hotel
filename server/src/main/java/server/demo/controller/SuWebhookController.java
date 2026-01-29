package server.demo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.config.SuMessagingWebhookAuthConfig;
import server.demo.entity.Store;
import server.demo.repository.StoreRepository;
import server.demo.service.OtaReservationSyncService;
import server.demo.service.SuWebhookIdempotencyService;
import server.demo.util.BasicAuthUtil;
import server.demo.util.SuHotelIdUtil;
import server.demo.util.SuReservationNotifPayloadParser;
import server.demo.util.SuReservationParser;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Su Webhook 接收端（Reservation Notification Push）。
 */
@RestController
@RequestMapping("/api/v1/su/webhook")
public class SuWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(SuWebhookController.class);
    private static final Logger reservationLogger = LoggerFactory.getLogger("SU_RESERVATION");

    private final ObjectMapper objectMapper;
    private final StoreRepository storeRepository;
    private final OtaReservationSyncService otaReservationSyncService;
    private final SuWebhookIdempotencyService idempotencyService;
    private final SuMessagingWebhookAuthConfig authConfig;

    public SuWebhookController(
            ObjectMapper objectMapper,
            StoreRepository storeRepository,
            OtaReservationSyncService otaReservationSyncService,
            SuWebhookIdempotencyService idempotencyService,
            SuMessagingWebhookAuthConfig authConfig
    ) {
        this.objectMapper = objectMapper;
        this.storeRepository = storeRepository;
        this.otaReservationSyncService = otaReservationSyncService;
        this.idempotencyService = idempotencyService;
        this.authConfig = authConfig;
    }

    @RequestMapping(value = "/reservation-notif", method = {RequestMethod.GET, RequestMethod.HEAD})
    public ResponseEntity<Map<String, Object>> healthReservationNotif() {
        return ResponseEntity.ok(Map.of("success", true, "message", "ok"));
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
     * 推荐：账号级 webhook（从 body.hotelid 路由到门店）
     * <p>
     * Su push body 至少包含 reservation_notif.reservation_notif_id 列表；部分场景也会携带 hotelid。
     */
    @PostMapping("/reservation-notif")
    public ResponseEntity<Map<String, Object>> handleReservationNotif(
            HttpServletRequest request,
            @RequestBody(required = false) String rawBody
    ) {
        if (authConfig.isAuthEnabled() && !BasicAuthUtil.isBasicAuthOk(request, authConfig.getUsername(), authConfig.getPassword())) {
            String remoteIp = request != null ? request.getRemoteAddr() : null;
            logger.warn("[SuReservationWebhook] unauthorized request. remoteIp={}", remoteIp);
            reservationLogger.warn("[ReservationWebhook] unauthorized. remoteIp={}", remoteIp);
            return ResponseEntity.status(401).body(failBody("Unauthorized"));
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(rawBody == null ? "{}" : rawBody);
        } catch (Exception e) {
            reservationLogger.error("[ReservationWebhook] invalid json. remoteIp={}, err={}, raw={}",
                    request != null ? request.getRemoteAddr() : null, e.getMessage(), safeTrim(rawBody));
            return ResponseEntity.ok(failBody("Invalid JSON"));
        }

        String hotelId = readHotelId(root);
        List<String> notifIds = SuReservationNotifPayloadParser.extractNotifIds(root);
        if (hotelId == null) {
            // PUSH API Method sometimes sends `hotel_id` only inside each reservation.
            hotelId = readHotelIdFromReservations(root);
        }

        // Compatibility: Some Su flows deliver reservation details directly (reservations array) without reservation_notif_id.
        // In that case, treat it as a reservation push (no ack via Reservation_notif API needed here) and return success
        // once the reservations are upserted locally.
        if ((notifIds == null || notifIds.isEmpty()) && isReservationPushPayload(root)) {
            if (hotelId == null) {
                reservationLogger.error("[ReservationWebhook] missing hotelid (reservation-push). remoteIp={}, raw={}",
                        request != null ? request.getRemoteAddr() : null, safeTrim(rawBody));
                return ResponseEntity.ok(failBody("Missing hotelid/hotel_id"));
            }
            return handleReservationPushInternal(request, hotelId, root, rawBody);
        }

        if (hotelId == null) {
            reservationLogger.error("[ReservationWebhook] missing hotelid (account-level). remoteIp={}, notifIds={}, raw={} ",
                    request != null ? request.getRemoteAddr() : null, notifIds.size(), safeTrim(rawBody));
            return ResponseEntity.ok(failBody("Missing hotelid/hotel_id"));
        }

        if (notifIds.isEmpty()) {
            reservationLogger.error("[ReservationWebhook] missing reservation_notif_id. remoteIp={}, hotelId={}, raw={}",
                    request != null ? request.getRemoteAddr() : null, hotelId, safeTrim(rawBody));
            // Without reservation_notif_id we cannot pull+ack; ask Su to retry with correct payload.
            return ResponseEntity.ok(failBody("Missing reservation_notif_id"));
        }

        return handleReservationNotifInternal(request, hotelId, root, rawBody);
    }

    /**
     * 兼容：门店级 webhook（hotelId 通过 path 传入）
     * <p>
     * Su push body 仅包含 reservation_notif_id 列表时，可用该方式路由到门店。
     */
    @PostMapping("/reservation-notif/{hotelId}")
    public ResponseEntity<Map<String, Object>> handleReservationNotif(
            HttpServletRequest request,
            @PathVariable String hotelId,
            @RequestBody(required = false) String rawBody
    ) {
        if (authConfig.isAuthEnabled() && !BasicAuthUtil.isBasicAuthOk(request, authConfig.getUsername(), authConfig.getPassword())) {
            String remoteIp = request != null ? request.getRemoteAddr() : null;
            logger.warn("[SuReservationWebhook] unauthorized request. remoteIp={}", remoteIp);
            reservationLogger.warn("[ReservationWebhook] unauthorized. remoteIp={}, hotelId={}", remoteIp, hotelId);
            return ResponseEntity.status(401).body(Map.of("status", "Fail"));
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(rawBody == null ? "{}" : rawBody);
        } catch (Exception e) {
            reservationLogger.error("[ReservationWebhook] invalid json. remoteIp={}, hotelId={}, err={}, raw={}",
                    request != null ? request.getRemoteAddr() : null, hotelId, e.getMessage(), safeTrim(rawBody));
            return ResponseEntity.ok(failBody("Invalid JSON"));
        }

        List<String> notifIds = SuReservationNotifPayloadParser.extractNotifIds(root);
        if (notifIds.isEmpty()) {
            if (isReservationPushPayload(root)) {
                return handleReservationPushInternal(request, hotelId, root, rawBody);
            }
            reservationLogger.error("[ReservationWebhook] missing reservation_notif_id. remoteIp={}, hotelId={}, raw={}",
                    request != null ? request.getRemoteAddr() : null, hotelId, safeTrim(rawBody));
            return ResponseEntity.ok(failBody("Missing reservation_notif_id"));
        }

        return handleReservationNotifInternal(request, hotelId, root, rawBody);
    }

    private ResponseEntity<Map<String, Object>> handleReservationNotifInternal(
            HttpServletRequest request,
            String hotelId,
            JsonNode root,
            String rawBody
    ) {
        List<String> notifIds = SuReservationNotifPayloadParser.extractNotifIds(root);
        if (notifIds.isEmpty()) {
            return ResponseEntity.ok(failBody("Missing reservation_notif_id"));
        }

        String normalizedHotelId = SuHotelIdUtil.normalize(hotelId);
        if (normalizedHotelId == null) {
            reservationLogger.error("[ReservationWebhook] invalid hotelid. remoteIp={}, hotelId={}, notifIds={}, raw={}",
                    request != null ? request.getRemoteAddr() : null, hotelId, notifIds.size(), safeTrim(rawBody));
            return ResponseEntity.ok(failBody("Invalid hotelid"));
        }

        Store store = resolveStoreByHotelId(normalizedHotelId).orElse(null);
        if (store == null) {
            reservationLogger.error("[ReservationWebhook] store not found. remoteIp={}, hotelId={}, notifIds={}, raw={}",
                    request != null ? request.getRemoteAddr() : null, normalizedHotelId, notifIds.size(), safeTrim(rawBody));
            return ResponseEntity.ok(failBody("Store not found for hotelid=" + normalizedHotelId));
        }

        reservationLogger.info("[ReservationWebhook] received. remoteIp={}, storeId={}, hotelId={}, notifIds={}",
                request != null ? request.getRemoteAddr() : null, store.getId(), normalizedHotelId, notifIds.size());

        Set<String> toProcess = idempotencyService.markProcessingAndReturnNew(normalizedHotelId, notifIds);
        if (toProcess.isEmpty()) {
            reservationLogger.info("[ReservationWebhook] idempotent hit. storeId={}, hotelId={}, notifIds={}",
                    store.getId(), normalizedHotelId, notifIds.size());
            return ResponseEntity.ok(successBody());
        }

        try {
            OtaReservationSyncService.ReservationSyncResult result = otaReservationSyncService.syncStoreReservations(store.getId(), toProcess);
            boolean ackOk = result.ackErrorMessage() == null && result.ackSuccess() == result.ackRequested();
            if (ackOk) {
                idempotencyService.markDone(normalizedHotelId, toProcess);
            } else {
                idempotencyService.clearProcessing(normalizedHotelId, toProcess);
            }

            reservationLogger.info("[ReservationWebhook] processed. storeId={}, hotelId={}, toProcess={}, pulled={}, created={}, updated={}, failed={}, ackRequested={}, ackSuccess={}, ackErr={}",
                    store.getId(),
                    normalizedHotelId,
                    toProcess.size(),
                    result.pulledReservations(),
                    result.createdCount(),
                    result.updatedCount(),
                    result.failedCount(),
                    result.ackRequested(),
                    result.ackSuccess(),
                    result.ackErrorMessage());

            if (!ackOk) {
                reservationLogger.error("[ReservationWebhook] sync/ack failed; ask Su to retry. storeId={}, hotelId={}, err={}",
                        store.getId(), normalizedHotelId, result.ackErrorMessage());
                return ResponseEntity.ok(failBody(result.ackErrorMessage() != null ? result.ackErrorMessage() : "Ack failed"));
            }

            return ResponseEntity.ok(successBody());
        } catch (Exception e) {
            idempotencyService.clearProcessing(normalizedHotelId, toProcess);
            reservationLogger.error("[ReservationWebhook] exception but webhook acked. storeId={}, hotelId={}, toProcess={}, err={}",
                    store.getId(), normalizedHotelId, toProcess.size(), e.getMessage(), e);
            return ResponseEntity.ok(failBody(e.getMessage() != null ? e.getMessage() : "Exception"));
        }
    }

    private ResponseEntity<Map<String, Object>> handleReservationPushInternal(
            HttpServletRequest request,
            String hotelId,
            JsonNode root,
            String rawBody
    ) {
        String normalizedHotelId = SuHotelIdUtil.normalize(hotelId);
        if (normalizedHotelId == null) {
            reservationLogger.error("[ReservationWebhook] invalid hotelid (reservation-push). remoteIp={}, hotelId={}, raw={}",
                    request != null ? request.getRemoteAddr() : null, hotelId, safeTrim(rawBody));
            return ResponseEntity.ok(failBody("Invalid hotelid"));
        }

        Store store = resolveStoreByHotelId(normalizedHotelId).orElse(null);
        if (store == null) {
            reservationLogger.error("[ReservationWebhook] store not found (reservation-push). remoteIp={}, hotelId={}, raw={}",
                    request != null ? request.getRemoteAddr() : null, normalizedHotelId, safeTrim(rawBody));
            return ResponseEntity.ok(failBody("Store not found for hotelid=" + normalizedHotelId));
        }

        List<JsonNode> reservations = SuReservationParser.extractReservationNodes(root);
        if (reservations.isEmpty()) {
            reservationLogger.error("[ReservationWebhook] empty reservations (reservation-push). remoteIp={}, storeId={}, hotelId={}, raw={}",
                    request != null ? request.getRemoteAddr() : null, store.getId(), normalizedHotelId, safeTrim(rawBody));
            return ResponseEntity.ok(failBody("Missing reservations"));
        }

        try {
            OtaReservationSyncService.UpsertOnlyResult result = otaReservationSyncService.upsertReservationsFromWebhook(store.getId(), reservations);
            boolean ok = result.createdCount() + result.updatedCount() > 0;
            reservationLogger.info("[ReservationWebhook] reservation-push processed. remoteIp={}, storeId={}, hotelId={}, reservations={}, created={}, updated={}, failed={}",
                    request != null ? request.getRemoteAddr() : null,
                    store.getId(),
                    normalizedHotelId,
                    reservations.size(),
                    result.createdCount(),
                    result.updatedCount(),
                    result.failedCount());
            if (!ok) {
                String err = result.errors() != null && !result.errors().isEmpty() ? String.join("; ", result.errors()) : "Upsert failed";
                return ResponseEntity.ok(failBody(err));
            }
            return ResponseEntity.ok(successBody());
        } catch (Exception e) {
            reservationLogger.error("[ReservationWebhook] reservation-push exception. remoteIp={}, storeId={}, hotelId={}, err={}",
                    request != null ? request.getRemoteAddr() : null, store.getId(), normalizedHotelId, e.getMessage(), e);
            return ResponseEntity.ok(failBody(e.getMessage() != null ? e.getMessage() : "Exception"));
        }
    }

    private static boolean isReservationPushPayload(JsonNode root) {
        if (root == null || root.isNull()) {
            return false;
        }
        JsonNode reservations = root.get("reservations");
        return reservations != null && reservations.isArray() && reservations.size() > 0;
    }

    private Optional<Store> resolveStoreByHotelId(String hotelId) {
        Optional<Store> byConfig = storeRepository.findBySuHotelId(hotelId);
        if (byConfig.isPresent()) {
            return byConfig;
        }

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

    private static Map<String, Object> successBody() {
        return Map.of("Status", "Success");
    }

    private static Map<String, Object> failBody(String shortText) {
        if (shortText == null || shortText.isBlank()) {
            shortText = "Fail";
        }
        return Map.of(
                "Status",
                "Fail",
                "Errors",
                Map.of("ShortText", shortText)
        );
    }

    private static String readHotelId(JsonNode root) {
        String v = readText(root, "hotelid");
        if (v == null) v = readText(root, "hotel_id");
        if (v == null) v = readText(root, "hotelId");
        if (v == null) v = readText(root, "HotelId");
        if (v == null) v = readText(root, "HotelID");
        return v;
    }

    private static String readHotelIdFromReservations(JsonNode root) {
        if (root == null || root.isNull()) {
            return null;
        }
        JsonNode reservations = root.get("reservations");
        if (reservations == null || !reservations.isArray() || reservations.size() == 0) {
            return null;
        }
        for (JsonNode item : reservations) {
            if (item == null || item.isNull()) {
                continue;
            }
            JsonNode reservation = item.get("reservation");
            JsonNode node = (reservation != null && reservation.isObject()) ? reservation : item;

            JsonNode hotelNode = node.get("hotelid");
            if (hotelNode == null) hotelNode = node.get("hotel_id");
            if (hotelNode == null) hotelNode = node.get("hotelId");
            if (hotelNode == null) hotelNode = node.get("HotelId");
            if (hotelNode == null || hotelNode.isNull()) {
                continue;
            }
            String s = hotelNode.asText(null);
            if (s != null && !s.trim().isBlank()) {
                return s.trim();
            }
        }
        return null;
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
