package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import server.demo.entity.Channel;
import server.demo.entity.ChannelMappingPriceSetting;
import server.demo.repository.ChannelMappingPriceSettingRepository;
import server.demo.repository.ChannelRepository;
import server.demo.util.SuChannelCatalog;
import server.demo.util.SuHotelIdUtil;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import server.demo.i18n.ApiMessages;
/**
 * Review 的当前门店、渠道、渠道物业及 Airbnb listing 映射校验。
 * <p>
 * Push、Pull 与外部写操作共用同一快照和断言，避免依赖历史评价字段判断当前授权范围。
 */
@Component
public class SuReviewWebhookMappingValidator {

    // 评论映射快照加载范围以目录 review 能力集为准（Su 官方 Review API：19/244/9；TRIP/AGODA 不支持）
    private static final List<Integer> SUPPORTED_CHANNEL_IDS = SuChannelCatalog.reviewSupportedSuIds();

    private final ChannelRepository channelRepository;
    private final ChannelMappingPriceSettingRepository mappingRepository;

    public SuReviewWebhookMappingValidator(
            ChannelRepository channelRepository,
            ChannelMappingPriceSettingRepository mappingRepository
    ) {
        this.channelRepository = channelRepository;
        this.mappingRepository = mappingRepository;
    }

    /**
     * 校验整个 Push payload，任何一条失败时都不得进入持久化。
     */
    public void validate(Long storeId, String routedHotelId, JsonNode root) {
        String normalizedHotelId = SuHotelIdUtil.normalize(routedHotelId);
        if (storeId == null || normalizedHotelId == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.df682f5f37dc"));
        }

        List<JsonNode> reviewNodes = SuReviewPayloadMapper.extractPushReviewNodes(root);
        if (reviewNodes.isEmpty()) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.2adc9f382269"));
        }

        CurrentMappingSnapshot snapshot = loadCurrentMappings(storeId, normalizedHotelId);
        for (JsonNode reviewNode : reviewNodes) {
            validateReviewNode(normalizedHotelId, reviewNode, snapshot);
        }
    }

    /**
     * 为 Pull 批次加载一次当前映射，后续逐条断言，避免按评价数量重复查询数据库。
     */
    public CurrentMappingSnapshot loadCurrentMappings(Long storeId, String rawHotelId) {
        String hotelId = SuHotelIdUtil.normalize(rawHotelId);
        if (storeId == null || hotelId == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.68b3dc8f8aa2"));
        }

        Map<Long, Channel> channelsById = new HashMap<>();
        List<Channel> channels = channelRepository.findByStoreId(storeId);
        if (channels != null) {
            for (Channel channel : channels) {
                if (channel != null && channel.getId() != null) {
                    channelsById.put(channel.getId(), channel);
                }
            }
        }

        Map<MappingKey, Set<Long>> matchedChannelIdsByKey = new HashMap<>();
        for (Integer channelId : SUPPORTED_CHANNEL_IDS) {
            String canonicalChannelCode = ReviewChannelCodePolicy.canonicalCode(channelId);
            List<ChannelMappingPriceSetting> settings =
                    mappingRepository.findByStoreIdAndSuPropertyIdAndSuChannelId(
                            storeId,
                            hotelId,
                            String.valueOf(channelId)
                    );
            if (settings == null) {
                continue;
            }
            for (ChannelMappingPriceSetting setting : settings) {
                if (!matchesBaseMapping(setting, storeId, hotelId, channelId)) {
                    continue;
                }
                Channel channel = channelsById.get(setting.getChannelId());
                if (channel == null
                        || !storeId.equals(channel.getStoreId())
                        || !ReviewChannelCodePolicy.matchesStoreCode(
                                storeId,
                                channel.getCode(),
                                canonicalChannelCode
                        )) {
                    continue;
                }

                String channelPropertyId = normalize(setting.getChannelHotelId());
                String listingId = normalize(setting.getListingId());
                if (channelPropertyId == null
                        || (channelId == ReviewChannelCodePolicy.CHANNEL_AIRBNB
                                && listingId == null)) {
                    continue;
                }
                MappingKey key = mappingKey(channelId, channelPropertyId, listingId);
                matchedChannelIdsByKey
                        .computeIfAbsent(key, ignored -> new LinkedHashSet<>())
                        .add(channel.getId());
            }
        }
        return new CurrentMappingSnapshot(storeId, hotelId, matchedChannelIdsByKey);
    }

    /**
     * 写操作使用的即时断言。调用方应在创建审计或调用 Su 前执行。
     */
    public void assertCurrentMapping(
            Long storeId,
            String hotelId,
            Integer channelId,
            String channelPropertyId,
            String listingId
    ) {
        loadCurrentMappings(storeId, hotelId)
                .assertMapped(channelId, channelPropertyId, listingId);
    }

    private void validateReviewNode(
            String routedHotelId,
            JsonNode reviewNode,
            CurrentMappingSnapshot snapshot
    ) {
        if (reviewNode == null || !reviewNode.isObject()) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.89deb1065ca0"));
        }

        String payloadHotelId = normalize(firstText(reviewNode, "hotel_id", "hotelid", "hotelId"));
        if (!routedHotelId.equals(payloadHotelId)) {
            throw new MappingRejectedException(ApiMessages.get("api.t.057a7acb4c38"));
        }

        Integer channelId = readInteger(reviewNode, "channel_id");
        if (channelId == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.bd9464382b90"));
        }
        String channelPropertyId = normalize(firstText(
                reviewNode,
                "channel_property_id",
                "channel_hotel_id"
        ));
        if (channelPropertyId == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.8cf4bef954c0"));
        }
        String listingId = normalize(firstText(reviewNode, "listing_id"));
        if (channelId == ReviewChannelCodePolicy.CHANNEL_AIRBNB && listingId == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.f8369b63e5c8"));
        }
        snapshot.assertMapped(channelId, channelPropertyId, listingId);
    }

    private static boolean matchesBaseMapping(
            ChannelMappingPriceSetting setting,
            Long storeId,
            String hotelId,
            Integer channelId
    ) {
        return setting != null
                && storeId.equals(setting.getStoreId())
                && hotelId.equals(normalize(setting.getSuPropertyId()))
                && String.valueOf(channelId).equals(normalize(setting.getSuChannelId()))
                && setting.getChannelId() != null;
    }

    private static MappingKey mappingKey(
            Integer channelId,
            String channelPropertyId,
            String listingId
    ) {
        return new MappingKey(
                channelId,
                normalize(channelPropertyId),
                channelId != null && channelId == ReviewChannelCodePolicy.CHANNEL_AIRBNB
                        ? normalize(listingId)
                        : null
        );
    }

    private static Integer readInteger(JsonNode node, String field) {
        String text = firstText(node, field);
        if (text == null) {
            return null;
        }
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.bd6fb603b8ee") + field + ApiMessages.get("api.t.be5283e17ea4"));
        }
    }

    private static String firstText(JsonNode node, String... fields) {
        if (node == null || fields == null) {
            return null;
        }
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value == null || value.isNull() || value.isContainerNode()) {
                continue;
            }
            String text = normalize(value.asText(null));
            if (text != null) {
                return text;
            }
        }
        return null;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private record MappingKey(
            Integer channelId,
            String channelPropertyId,
            String listingId
    ) {
    }

    public static final class CurrentMappingSnapshot {

        private final Long storeId;
        private final String hotelId;
        private final Map<MappingKey, Set<Long>> matchedChannelIdsByKey;

        private CurrentMappingSnapshot(
                Long storeId,
                String hotelId,
                Map<MappingKey, Set<Long>> matchedChannelIdsByKey
        ) {
            this.storeId = storeId;
            this.hotelId = hotelId;
            Map<MappingKey, Set<Long>> immutableMappings = new HashMap<>();
            matchedChannelIdsByKey.forEach(
                    (key, channelIds) -> immutableMappings.put(key, Set.copyOf(channelIds))
            );
            this.matchedChannelIdsByKey = Map.copyOf(immutableMappings);
        }

        public void assertMapped(
                Integer channelId,
                String channelPropertyId,
                String listingId
        ) {
            if (ReviewChannelCodePolicy.canonicalCode(channelId) == null) {
                throw new MappingRejectedException(ApiMessages.get("api.t.94492171aede"));
            }
            String normalizedPropertyId = normalize(channelPropertyId);
            if (normalizedPropertyId == null) {
                throw new MappingRejectedException(ApiMessages.get("api.t.67e5c41f7e4c"));
            }
            String normalizedListingId = normalize(listingId);
            if (channelId == ReviewChannelCodePolicy.CHANNEL_AIRBNB
                    && normalizedListingId == null) {
                throw new MappingRejectedException(ApiMessages.get("api.t.83e5855a35be"));
            }

            Set<Long> matchedChannelIds = matchedChannelIdsByKey.getOrDefault(
                    mappingKey(channelId, normalizedPropertyId, normalizedListingId),
                    Set.of()
            );
            if (matchedChannelIds.isEmpty()) {
                throw new MappingRejectedException(ApiMessages.get("api.t.ce378998af44"));
            }
            if (matchedChannelIds.size() > 1) {
                throw new MappingRejectedException(ApiMessages.get("api.t.79a4ff22aa54"));
            }
        }

        public Long storeId() {
            return storeId;
        }

        public String hotelId() {
            return hotelId;
        }
    }

    public static class MappingRejectedException extends RuntimeException {

        public MappingRejectedException(String message) {
            super(message);
        }
    }
}
