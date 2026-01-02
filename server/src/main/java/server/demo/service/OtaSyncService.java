package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.Channel;
import server.demo.entity.ChannelPrice;
import server.demo.entity.Store;
import server.demo.repository.ChannelPriceRepository;
import server.demo.repository.ChannelRepository;
import server.demo.repository.StoreRepository;
import server.demo.util.SuHotelIdUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OtaSyncService {

    private static final Logger logger = LoggerFactory.getLogger(OtaSyncService.class);

    private static final int DEFAULT_SYNC_DAYS = 365;
    private static final int MAX_SYNC_DAYS = 500;

    private static final String OTA_CHANNEL_CODE_AIRBNB = "AIRBNB";
    private static final String OTA_CHANNEL_CODE_BOOKING = "BOOKING";

    private static final int SU_OTA_CODE_AIRBNB = 244;
    private static final int SU_OTA_CODE_BOOKING = 19;

    private static final List<String> DEFAULT_OTA_CHANNEL_CODES = List.of(
            OTA_CHANNEL_CODE_AIRBNB,
            OTA_CHANNEL_CODE_BOOKING
    );

    private static final Map<String, Integer> SU_OTA_CODE_BY_CHANNEL_CODE = Map.of(
            OTA_CHANNEL_CODE_AIRBNB, SU_OTA_CODE_AIRBNB,
            OTA_CHANNEL_CODE_BOOKING, SU_OTA_CODE_BOOKING
    );

    private final SuApiClient suApiClient;
    private final ChannelRepository channelRepository;
    private final ChannelPriceRepository channelPriceRepository;
    private final StoreRepository storeRepository;
    private final ChannelPriceFallbackService channelPriceFallbackService;
    private final SuAccessTokenService suAccessTokenService;

    public OtaSyncService(
            SuApiClient suApiClient,
            ChannelRepository channelRepository,
            ChannelPriceRepository channelPriceRepository,
            StoreRepository storeRepository,
            ChannelPriceFallbackService channelPriceFallbackService,
            SuAccessTokenService suAccessTokenService
    ) {
        this.suApiClient = suApiClient;
        this.channelRepository = channelRepository;
        this.channelPriceRepository = channelPriceRepository;
        this.storeRepository = storeRepository;
        this.channelPriceFallbackService = channelPriceFallbackService;
        this.suAccessTokenService = suAccessTokenService;
    }

    public List<String> getDefaultOtaChannelCodes() {
        return DEFAULT_OTA_CHANNEL_CODES;
    }

    public record OtaSyncResult(
            Long storeId,
            LocalDate startDate,
            LocalDate endDate,
            Map<String, Integer> pushedCountByChannel,
            Map<String, Integer> markedSyncedCountByChannel
    ) {}

    @Transactional
    public OtaSyncResult syncStorePricesToSu(Long storeId, LocalDate startDate, Integer days) {
        if (storeId == null) {
            throw new IllegalArgumentException("storeId is required");
        }

        LocalDate effectiveStartDate = startDate != null ? startDate : LocalDate.now();
        int effectiveDays = clampDays(days != null ? days : DEFAULT_SYNC_DAYS);
        LocalDate effectiveEndDate = effectiveStartDate.plusDays(effectiveDays - 1L);

        // 自动兜底：未使用 PriceLabs/未回推价格时，使用本地房价生成渠道价，保证生产环境可直接推送
        channelPriceFallbackService.generate(storeId, effectiveStartDate, effectiveDays, DEFAULT_OTA_CHANNEL_CODES, null);

        String hotelId = resolveHotelId(storeId);

        Map<String, Integer> pushed = new LinkedHashMap<>();
        Map<String, Integer> marked = new LinkedHashMap<>();

        for (String channelCode : DEFAULT_OTA_CHANNEL_CODES) {
            Integer suOtaCode = SU_OTA_CODE_BY_CHANNEL_CODE.get(channelCode);
            if (suOtaCode == null) {
                continue;
            }

            Optional<Channel> channelOpt = channelRepository.findByStoreIdAndCode(storeId, channelCode);
            if (channelOpt.isEmpty()) {
                logger.warn("Skip OTA sync: channel not found. storeId={}, channelCode={}", storeId, channelCode);
                pushed.put(channelCode, 0);
                marked.put(channelCode, 0);
                continue;
            }

            Channel channel = channelOpt.get();
            List<ChannelPrice> prices = channelPriceRepository.findUnsyncedByStoreIdAndChannelIdAndDateRange(
                    storeId, channel.getId(), effectiveStartDate, effectiveEndDate
            );

            prices = prices.stream()
                    .filter(cp -> cp.getChannelPrice() != null)
                    .collect(Collectors.toList());

            if (prices.isEmpty()) {
                pushed.put(channelCode, 0);
                marked.put(channelCode, 0);
                continue;
            }

            Map<String, Object> payload = buildSuInvRateControlPayload(
                    hotelId,
                    suOtaCode,
                    prices
            );

            JsonNode response = suAccessTokenService.executeWithTokenRetry(
                    token -> suApiClient.postInvRateControl(token, payload),
                    "invratecontrol"
            );
            boolean isSuccess = isSuSuccess(response);

            if (!isSuccess) {
                logger.warn("Su invratecontrol returned non-success status. storeId={}, channelCode={}, body={}",
                        storeId, channelCode, response);
                pushed.put(channelCode, prices.size());
                marked.put(channelCode, 0);
                continue;
            }

            List<Long> ids = prices.stream().map(ChannelPrice::getId).filter(Objects::nonNull).toList();
            int updated = ids.isEmpty() ? 0 : channelPriceRepository.markAsSyncedToOta(ids);

            pushed.put(channelCode, prices.size());
            marked.put(channelCode, updated);
        }

        return new OtaSyncResult(storeId, effectiveStartDate, effectiveEndDate, pushed, marked);
    }

    private String resolveHotelId(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("门店不存在"));

        String hotelId = SuHotelIdUtil.normalize(store.getSuHotelId());
        if (hotelId == null) {
            hotelId = SuHotelIdUtil.buildDefault(storeId);
            store.setSuHotelId(hotelId);
            storeRepository.save(store);
        }
        return hotelId;
    }

    private static int clampDays(int days) {
        if (days < 1) {
            return 1;
        }
        return Math.min(days, MAX_SYNC_DAYS);
    }

    private static boolean isSuSuccess(JsonNode response) {
        if (response == null) {
            return true;
        }
        JsonNode successNode = response.get("Success");
        if (successNode != null) {
            String success = successNode.asText("");
            return "Success".equalsIgnoreCase(success) || "SUCCESS".equalsIgnoreCase(success);
        }
        JsonNode statusNode = response.get("status");
        if (statusNode == null) {
            statusNode = response.get("Status");
        }
        if (statusNode == null) {
            return true;
        }
        String status = statusNode.asText("");
        return "Success".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status);
    }

    private static Map<String, Object> buildSuInvRateControlPayload(
            String hotelId,
            int suOtaCode,
            List<ChannelPrice> prices
    ) {
        Map<String, List<ChannelPrice>> byRoomAndRate = prices.stream()
                .collect(Collectors.groupingBy(cp -> buildRoomRateKey(cp.getRoomType().getId(), cp.getPricePlan().getId())));

        List<Map<String, Object>> rateControls = new ArrayList<>();

        for (Map.Entry<String, List<ChannelPrice>> entry : byRoomAndRate.entrySet()) {
            List<ChannelPrice> group = entry.getValue();
            if (group.isEmpty()) {
                continue;
            }

            ChannelPrice first = group.get(0);
            String roomId = String.valueOf(first.getRoomType().getId());
            String rateId = String.valueOf(first.getPricePlan().getId());

            List<Map<String, Object>> dateSegments = buildDateSegments(group, suOtaCode);
            if (dateSegments.isEmpty()) {
                continue;
            }

            Map<String, Object> rateControl = new LinkedHashMap<>();
            rateControl.put("roomid", roomId);
            rateControl.put("rateid", rateId);
            rateControl.put("date", dateSegments);
            rateControls.add(rateControl);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("hotelid", hotelId);
        payload.put("ratecontrol", rateControls);
        return payload;
    }

    private static List<Map<String, Object>> buildDateSegments(List<ChannelPrice> prices, int suOtaCode) {
        List<ChannelPrice> sorted = prices.stream()
                .sorted(Comparator.comparing(ChannelPrice::getPriceDate))
                .toList();

        List<PriceRange> ranges = new ArrayList<>();
        PriceRange current = null;

        for (ChannelPrice cp : sorted) {
            if (cp.getPriceDate() == null || cp.getChannelPrice() == null) {
                continue;
            }
            BigDecimal value = cp.getChannelPrice().setScale(2, RoundingMode.HALF_UP);

            if (current == null) {
                current = new PriceRange(cp.getPriceDate(), cp.getPriceDate(), value);
                continue;
            }

            boolean isNextDay = current.to.plusDays(1).equals(cp.getPriceDate());
            boolean samePrice = current.price.compareTo(value) == 0;

            if (isNextDay && samePrice) {
                current = new PriceRange(current.from, cp.getPriceDate(), current.price);
            } else {
                ranges.add(current);
                current = new PriceRange(cp.getPriceDate(), cp.getPriceDate(), value);
            }
        }

        if (current != null) {
            ranges.add(current);
        }

        return ranges.stream()
                .map(range -> toSuDateSegment(range, suOtaCode))
                .collect(Collectors.toList());
    }

    private static Map<String, Object> toSuDateSegment(PriceRange range, int suOtaCode) {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("type", "Fixed");
        rule.put("value", range.price.stripTrailingZeros().toPlainString());

        Map<String, Object> otaRule = new LinkedHashMap<>();
        otaRule.put("OTACode", List.of(suOtaCode));
        otaRule.put("rule", rule);

        Map<String, Object> segment = new LinkedHashMap<>();
        segment.put("from", range.from.toString());
        segment.put("to", range.to.toString());
        segment.put("OTARule", List.of(otaRule));
        return segment;
    }

    private static String buildRoomRateKey(Long roomTypeId, Long pricePlanId) {
        return roomTypeId + "_" + pricePlanId;
    }

    private record PriceRange(LocalDate from, LocalDate to, BigDecimal price) {}
}
