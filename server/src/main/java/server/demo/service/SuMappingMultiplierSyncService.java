package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import server.demo.dto.ChannelMappingMultiplierSyncSummaryDTO;
import server.demo.entity.Channel;
import server.demo.entity.OtaIntegration;
import server.demo.entity.Store;
import server.demo.enums.PriceAdjustmentType;
import server.demo.repository.OtaIntegrationRepository;
import server.demo.repository.StoreRepository;
import server.demo.util.OtaChannelPricePolicy;
import server.demo.util.SuChannelCatalog;
import server.demo.util.SuHotelIdUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import server.demo.i18n.ApiMessages;
@Service
public class SuMappingMultiplierSyncService {

    private static final String STATUS_ACTIVE = "Active";
    private static final String STATUS_SKIPPED = "SKIPPED";
    private static final String STATUS_FAILED = "FAILED";
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal DEFAULT_SURCHARGE = BigDecimal.ZERO;
    private static final BigDecimal DEFAULT_MULTIPLIER = BigDecimal.ONE;

    private final OtaIntegrationRepository otaIntegrationRepository;
    private final StoreRepository storeRepository;
    private final SuApiClient suApiClient;
    private final SuAccessTokenService suAccessTokenService;

    public SuMappingMultiplierSyncService(
            OtaIntegrationRepository otaIntegrationRepository,
            StoreRepository storeRepository,
            SuApiClient suApiClient,
            SuAccessTokenService suAccessTokenService
    ) {
        this.otaIntegrationRepository = otaIntegrationRepository;
        this.storeRepository = storeRepository;
        this.suApiClient = suApiClient;
        this.suAccessTokenService = suAccessTokenService;
    }

    public ChannelMappingMultiplierSyncSummaryDTO syncForChannel(Long storeId, Channel channel) {
        if (storeId == null) {
            return ChannelMappingMultiplierSyncSummaryDTO.skipped(null, ApiMessages.get("api.t.2ea59979ee53"));
        }
        if (channel == null || channel.getCode() == null) {
            return ChannelMappingMultiplierSyncSummaryDTO.skipped(null, ApiMessages.get("api.t.70a48c7a87e9"));
        }

        String channelCode = normalizeChannelCode(channel.getCode());
        String suChannelId = resolveSuChannelId(channelCode);
        if (suChannelId == null) {
            return ChannelMappingMultiplierSyncSummaryDTO.skipped(channelCode, ApiMessages.get("api.t.2160c520542e"));
        }

        PriceModifier modifier = buildPriceModifier(channel);
        ChannelMappingMultiplierSyncSummaryDTO summary = new ChannelMappingMultiplierSyncSummaryDTO();
        summary.setChannelCode(channelCode);
        summary.setSuChannelId(suChannelId);
        summary.setRequestedMultiplier(modifier.multiplier());
        summary.setRequestedSurcharge(modifier.surcharge());

        if (OtaChannelPricePolicy.CHANNEL_CODE_AIRBNB.equals(channelCode)) {
            summary.setStatus(STATUS_SKIPPED);
            summary.setMessage(ApiMessages.get("api.t.c8a775331584"));
            return summary;
        }

        if (modifier.multiplier().compareTo(BigDecimal.ZERO) < 0) {
            summary.setStatus(STATUS_FAILED);
            summary.setMessage(ApiMessages.get("api.t.00c6964fa6ae"));
            return summary;
        }

        Optional<OtaIntegration> integrationOpt = findOtaIntegration(storeId, channelCode);
        if (integrationOpt.isEmpty()) {
            summary.setStatus(STATUS_FAILED);
            summary.setMessage(ApiMessages.get("api.t.2f152166d528"));
            return summary;
        }

        OtaIntegration integration = integrationOpt.get();
        String hotelId;
        try {
            hotelId = resolveReadOnlySuHotelId(storeId, integration);
        } catch (Exception e) {
            summary.setStatus(STATUS_FAILED);
            summary.setMessage(ApiMessages.get("api.t.ec44d7b6a88e") + e.getMessage());
            return summary;
        }
        summary.setHotelId(hotelId);

        JsonNode mappings;
        try {
            mappings = suAccessTokenService.executeWithTokenRetry(
                    token -> suApiClient.getMappings(token, hotelId, suChannelId),
                    "mappings"
            );
        } catch (Exception e) {
            summary.setStatus(STATUS_FAILED);
            summary.setMessage(ApiMessages.get("api.t.f8b1cd03d09a") + e.getMessage());
            return summary;
        }

        List<MappingTarget> targets = extractTargets(channelCode, suChannelId, mappings);
        if (targets.isEmpty()) {
            summary.setStatus(STATUS_FAILED);
            summary.setMessage(ApiMessages.get("api.t.f2895d650cde"));
            return summary;
        }

        List<ChannelMappingMultiplierSyncSummaryDTO.Item> items = new ArrayList<>();
        for (MappingTarget target : targets) {
            ChannelMappingMultiplierSyncSummaryDTO.MappingRef ref = target.toRef();
            String missing = validateTarget(channelCode, target);
            if (missing != null) {
                items.add(ChannelMappingMultiplierSyncSummaryDTO.Item.failure(ref, missing));
                continue;
            }

            try {
                JsonNode response = postMappingUpdate(channelCode, suChannelId, hotelId, target, modifier);
                if (suApiClient.isSuSuccess(response)) {
                    items.add(ChannelMappingMultiplierSyncSummaryDTO.Item.success(ref, ApiMessages.get("api.t.21f991d48579")));
                    continue;
                }
                String message = suApiClient.extractSuErrorMessage(response);
                if (message == null || message.isBlank()) {
                    message = "{api.t.e6a6240b1105}";
                }
                items.add(ChannelMappingMultiplierSyncSummaryDTO.Item.failure(ref, message));
            } catch (Exception e) {
                items.add(ChannelMappingMultiplierSyncSummaryDTO.Item.failure(ref, e.getMessage()));
            }
        }

        summary.setItems(items);
        summary.refreshCounts();
        summary.setMessage(buildSummaryMessage(summary));
        return summary;
    }

    private Optional<OtaIntegration> findOtaIntegration(Long storeId, String channelCode) {
        Optional<OtaIntegration> direct = otaIntegrationRepository.findByStoreIdAndCode(storeId, channelCode);
        if (direct.isPresent()) {
            return direct;
        }
        if (OtaChannelPricePolicy.CHANNEL_CODE_BOOKING_COM.equals(channelCode)) {
            return otaIntegrationRepository.findByStoreIdAndCode(
                    storeId,
                    OtaChannelPricePolicy.CHANNEL_CODE_BOOKING
            );
        }
        return Optional.empty();
    }

    private String resolveReadOnlySuHotelId(Long storeId, OtaIntegration integration) {
        String hotelId = integration.getSuPropertyId();
        if (hotelId != null && !hotelId.isBlank()) {
            return hotelId.trim();
        }

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException(ApiMessages.get("api.t.051df0941ec3")));
        String storeHotelId = SuHotelIdUtil.normalize(store.getSuHotelId());
        if (storeHotelId == null) {
            storeHotelId = SuHotelIdUtil.buildDefault(storeId);
        }
        return storeHotelId;
    }

    private PriceModifier buildPriceModifier(Channel channel) {
        PriceAdjustmentType type = channel.getPriceAdjustmentType();
        if (type == null) {
            type = PriceAdjustmentType.PERCENTAGE;
        }
        BigDecimal adjustmentValue = channel.getPriceAdjustmentValue();
        if (adjustmentValue == null) {
            adjustmentValue = BigDecimal.ZERO;
        }

        if (type == PriceAdjustmentType.FIXED) {
            return new PriceModifier(DEFAULT_MULTIPLIER, normalizeMoney(adjustmentValue));
        }

        BigDecimal multiplier = BigDecimal.ONE.add(
                adjustmentValue.divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP)
        );
        return new PriceModifier(normalizeMultiplier(multiplier), DEFAULT_SURCHARGE);
    }

    private List<MappingTarget> extractTargets(String channelCode, String suChannelId, JsonNode root) {
        List<MappingTarget> targets = new ArrayList<>();
        JsonNode channelArray = findChannelArray(root, suChannelId);
        if (channelArray == null || !channelArray.isArray()) {
            return targets;
        }

        for (JsonNode mappingNode : channelArray) {
            if (mappingNode == null || !mappingNode.isObject()) {
                continue;
            }
            String channelHotelId = readText(mappingNode, "ChannelHotelID", "ChannelHotelId", "channelhotelid");
            String mappingStatus = readText(mappingNode, "Status", "status");
            List<String> roomIds = readStringArray(mappingNode.get("RoomIDs"));
            JsonNode ratePlans = mappingNode.get("Rateplans");

            if (ratePlans == null || !ratePlans.isArray() || ratePlans.size() == 0) {
                targets.add(new MappingTarget(channelCode, channelHotelId, first(roomIds), null, null, null, null, null, mappingStatus));
                continue;
            }

            for (JsonNode ratePlanNode : ratePlans) {
                if (ratePlanNode == null || !ratePlanNode.isObject()) {
                    continue;
                }
                JsonNode pricingNode = ratePlanNode.get("Pricing");
                String roomId = firstNonBlank(
                        readText(ratePlanNode, "PMSRoomID", "PMSRoomId", "RoomID", "RoomId", "roomid"),
                        first(roomIds)
                );
                String rateId = readText(ratePlanNode, "PMSRateID", "PMSRateId", "RatePlanID", "RateID", "rateid");
                String channelRoomId = readText(
                        ratePlanNode,
                        "ChannelRoomID",
                        "ChannelRoomId",
                        "ChannelRoomTypeID",
                        "ChannelRoomTypeId",
                        "channelroomid"
                );
                String channelRateId = readText(
                        ratePlanNode,
                        "ChannelRateID",
                        "ChannelRateId",
                        "ChannelPricePlanID",
                        "ChannelPricePlanId",
                        "channelrateid"
                );
                String listingId = readText(ratePlanNode, "ListingID", "ListingId", "listingid");
                if (listingId == null && OtaChannelPricePolicy.CHANNEL_CODE_AIRBNB.equals(channelCode)) {
                    listingId = channelRoomId;
                }
                String applicableGuest = firstNonBlank(
                        readText(pricingNode, "ApplicableNoOfGuest", "applicablenoofguest", "Occupancy", "occupancy"),
                        readText(ratePlanNode, "ApplicableNoOfGuest", "applicablenoofguest", "Occupancy", "occupancy", "fixed_occupany")
                );
                String ratePlanStatus = firstNonBlank(
                        readText(ratePlanNode, "MappingStatus", "mappingStatus"),
                        mappingStatus
                );
                targets.add(new MappingTarget(
                        channelCode,
                        channelHotelId,
                        roomId,
                        rateId,
                        channelRoomId,
                        channelRateId,
                        listingId,
                        applicableGuest,
                        ratePlanStatus
                ));
            }
        }

        return targets;
    }

    private String validateTarget(String channelCode, MappingTarget target) {
        if (!isActive(target.status())) {
            return ApiMessages.get("api.t.aa40d7023589");
        }
        if (isBlank(target.channelHotelId())) {
            return ApiMessages.get("api.t.a578691015a6");
        }
        if (isBlank(target.roomId())) {
            return ApiMessages.get("api.t.5c67452b9080");
        }
        if (isBlank(target.rateId())) {
            return ApiMessages.get("api.t.99adecbccbaf");
        }
        if (isBlank(target.applicableNoOfGuest())) {
            return ApiMessages.get("api.t.3488af0a1ec5");
        }
        if (OtaChannelPricePolicy.CHANNEL_CODE_AIRBNB.equals(channelCode)) {
            if (isBlank(target.listingId())) {
                return ApiMessages.get("api.t.c2c122bf0cbd");
            }
            return null;
        }
        if (isBlank(target.channelRoomId())) {
            return ApiMessages.get("api.t.601ccdb857a2");
        }
        if (isBlank(target.channelRateId())) {
            return ApiMessages.get("api.t.76be9aae3ce9");
        }
        return null;
    }

    private JsonNode postMappingUpdate(
            String channelCode,
            String suChannelId,
            String hotelId,
            MappingTarget target,
            PriceModifier modifier
    ) {
        if (OtaChannelPricePolicy.CHANNEL_CODE_AIRBNB.equals(channelCode)) {
            throw new IllegalStateException("Airbnb legacy channel-level sync is unsupported; use mapping-level settings");
        }

        Map<String, Object> payload = buildBookingRatePlanMapPayload(hotelId, suChannelId, target, modifier);
        return suAccessTokenService.executeWithTokenRetry(
                token -> suApiClient.postBookingRatePlanMap(token, payload),
                "OTA_RatePlanMap"
        );
    }

    private Map<String, Object> buildBookingRatePlanMapPayload(
            String hotelId,
            String suChannelId,
            MappingTarget target,
            PriceModifier modifier
    ) {
        Map<String, Object> pricing = new LinkedHashMap<>();
        pricing.put("applicablenoofguest", parseIntegerOrText(target.applicableNoOfGuest()));
        pricing.put("multiplier", modifier.multiplier());
        pricing.put("surcharge", modifier.surcharge());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("hotelid", hotelId);
        payload.put("action", "setup");
        // OTA_RatePlanMap 为通用端点：channelid 必须带当前渠道解析出的 Su channel id，避免误推到 Booking
        payload.put("channelid", Integer.parseInt(suChannelId));
        payload.put("status", STATUS_ACTIVE);
        payload.put("channelhotelid", target.channelHotelId());
        payload.put("roomid", target.roomId());
        payload.put("rateid", target.rateId());
        payload.put("channelroomid", target.channelRoomId());
        payload.put("channelrateid", target.channelRateId());
        payload.put("pricing", List.of(pricing));
        return payload;
    }

    private JsonNode findChannelArray(JsonNode root, String suChannelId) {
        if (root == null || root.isNull()) {
            return null;
        }
        JsonNode explicit = root.get(suChannelId);
        if (explicit != null && explicit.isArray()) {
            return explicit;
        }
        if (!root.isObject()) {
            return null;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            JsonNode value = entry.getValue();
            if (value != null && value.isArray()) {
                return value;
            }
        }
        return null;
    }

    private static List<String> readStringArray(JsonNode arrayNode) {
        List<String> values = new ArrayList<>();
        if (arrayNode == null || !arrayNode.isArray()) {
            return values;
        }
        for (JsonNode node : arrayNode) {
            if (node == null || node.isNull()) {
                continue;
            }
            String text = node.asText("");
            if (!text.isBlank()) {
                values.add(text.trim());
            }
        }
        return values;
    }

    private static String readText(JsonNode node, String... fields) {
        if (node == null || node.isNull() || fields == null) {
            return null;
        }
        for (String field : fields) {
            if (field == null || field.isBlank()) {
                continue;
            }
            JsonNode valueNode = node.get(field);
            if (valueNode == null || valueNode.isNull()) {
                continue;
            }
            String value = valueNode.asText("");
            if (!value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String first(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    private static String firstNonBlank(String first, String second) {
        if (!isBlank(first)) {
            return first.trim();
        }
        if (!isBlank(second)) {
            return second.trim();
        }
        return null;
    }

    private static boolean isActive(String status) {
        return status == null || status.isBlank() || STATUS_ACTIVE.equalsIgnoreCase(status.trim());
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }

    private static Object parseIntegerOrText(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return value.trim();
        }
    }

    private static String normalizeChannelCode(String channelCode) {
        String normalized = channelCode.trim().toUpperCase();
        if (OtaChannelPricePolicy.CHANNEL_CODE_BOOKING_COM.equals(normalized)) {
            return OtaChannelPricePolicy.CHANNEL_CODE_BOOKING;
        }
        return normalized;
    }

    static String resolveSuChannelId(String channelCode) {
        return SuChannelCatalog.byCode(channelCode)
                .map(channel -> String.valueOf(channel.suId()))
                .orElse(null);
    }

    private static BigDecimal normalizeMultiplier(BigDecimal value) {
        return value.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    private static BigDecimal normalizeMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    private static String buildSummaryMessage(ChannelMappingMultiplierSyncSummaryDTO summary) {
        if (STATUS_SKIPPED.equalsIgnoreCase(summary.getStatus())) {
            return summary.getMessage();
        }
        if (summary.getFailureCount() == 0) {
            return ApiMessages.get("api.t.5f11b61b4028");
        }
        if (summary.getSuccessCount() == 0) {
            return ApiMessages.get("api.t.a51fc31e4bbe");
        }
        return ApiMessages.get("api.t.2ac4d961fbe4");
    }

    private record PriceModifier(BigDecimal multiplier, BigDecimal surcharge) {}

    private record MappingTarget(
            String channelCode,
            String channelHotelId,
            String roomId,
            String rateId,
            String channelRoomId,
            String channelRateId,
            String listingId,
            String applicableNoOfGuest,
            String status
    ) {
        ChannelMappingMultiplierSyncSummaryDTO.MappingRef toRef() {
            return new ChannelMappingMultiplierSyncSummaryDTO.MappingRef(
                    channelHotelId,
                    roomId,
                    rateId,
                    channelRoomId,
                    channelRateId,
                    listingId,
                    applicableNoOfGuest
            );
        }
    }
}
