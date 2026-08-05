package server.demo.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Su 渠道目录：集中维护内部渠道 code 与 Su channel_id / 加密渠道码的映射。
 * <p>
 * 加密码取值与 SuApiClient 既有预置值一致（官方核验），Trip.com 主键名为 TRIP
 * （Su channel id 339，Su 内部名 ctripnew），CTRIP 保留为别名兼容。
 */
public final class SuChannelCatalog {

    /**
     * Su 渠道条目。
     *
     * @param code          系统内部渠道 code（大写）
     * @param suId          Su channel_id
     * @param encryptedCode Su encrypted_channel_code（Widget/渠道映射用）
     * @param displayName   渠道展示名
     */
    public record SuChannel(String code, int suId, String encryptedCode, String displayName) {
    }

    public static final SuChannel BOOKING = new SuChannel(
            "BOOKING", 19, "Qa9Qwq4PF32srUVea3mYzzvBFiszeXK4aaQINYhXlm8", "Booking.com");
    public static final SuChannel AIRBNB = new SuChannel(
            "AIRBNB", 244, "aM4JjiWOnUx5qS2IT8wHCbVmIWbA9tTD3PFcjnt8M-Y", "Airbnb");
    public static final SuChannel EXPEDIA = new SuChannel(
            "EXPEDIA", 9, "_4PYESNQm9vU15C3DR4xRrW2VHVrEVGPdhx4du8_uBw", "Expedia");
    public static final SuChannel TRIP = new SuChannel(
            "TRIP", 339, "mvYVz5x5ExxioyfyMo3jUUpNVZVbMyC6SUExMG9iaIY", "Trip.com");
    public static final SuChannel AGODA = new SuChannel(
            "AGODA", 189, "sAr2QsPWYcMUS-7PKJtEDGG0aZODNK5Sv4B5o2LTPA0", "Agoda");

    private static final List<SuChannel> ALL = List.of(BOOKING, AIRBNB, EXPEDIA, TRIP, AGODA);

    /**
     * 经 Su OTA Messages Collection & Reply API 收发消息的渠道。
     * <p>
     * 依据 Su 官方文档《OTA Messages Collection and Reply API》支持清单：
     * Airbnb 244 / Booking.com 19 / Expedia 9 / VRBO 253。VRBO(253) 本期未接入故不含；
     * Trip.com(339) 与 Agoda(189) 官方明确不支持消息，严禁进入消息链路。
     */
    private static final List<SuChannel> MESSAGING_SUPPORTED = List.of(BOOKING, AIRBNB, EXPEDIA);

    /**
     * 经 Su Review API 同步/回复评论的渠道。
     * <p>
     * 依据 Su 官方文档《Review Master Data》渠道清单：Booking.com 19 / Airbnb 244 / Expedia 9；
     * Trip.com(339) 与 Agoda(189) 不在列，严禁进入评论链路。
     */
    private static final List<SuChannel> REVIEW_SUPPORTED = List.of(BOOKING, AIRBNB, EXPEDIA);

    /**
     * 历史别名：BOOKING.COM→BOOKING、CTRIP→TRIP。
     */
    private static final Map<String, SuChannel> CODE_ALIASES = Map.of(
            "BOOKING.COM", BOOKING,
            "CTRIP", TRIP
    );

    private static final Map<String, SuChannel> BY_CODE = buildByCode();
    private static final Map<Integer, SuChannel> BY_SU_ID = buildBySuId();

    private SuChannelCatalog() {
    }

    /**
     * 全部渠道条目（目录顺序：BOOKING/AIRBNB/EXPEDIA/TRIP/AGODA）。
     */
    public static List<SuChannel> all() {
        return ALL;
    }

    /**
     * 按内部渠道 code 查询（大小写不敏感，兼容 BOOKING.COM/CTRIP 别名）。
     */
    public static Optional<SuChannel> byCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        String normalized = code.trim().toUpperCase();
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_CODE.get(normalized));
    }

    /**
     * 按 Su channel_id 查询。
     */
    public static Optional<SuChannel> bySuId(Integer suId) {
        if (suId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_SU_ID.get(suId));
    }

    /**
     * 渠道 code 是否在目录内（含别名）。
     */
    public static boolean isKnown(String code) {
        return byCode(code).isPresent();
    }

    /**
     * 全部内部渠道 code（主键名，不含别名）。
     */
    public static List<String> allCodes() {
        return ALL.stream().map(SuChannel::code).toList();
    }

    /**
     * 全部 Su channel_id。
     */
    public static List<Integer> allSuIds() {
        return ALL.stream().map(SuChannel::suId).toList();
    }

    /**
     * 参与连接/房价/库存同步的内部渠道 code 全集。
     */
    public static List<String> supportedOtaChannelCodes() {
        return allCodes();
    }

    /**
     * 参与订单（Reservation）同步的内部渠道 code 全集：
     * 拉取与 webhook 机制渠道无关，五个渠道全部支持。
     */
    public static List<String> supportedReservationChannelCodes() {
        return allCodes();
    }

    /**
     * 支持 Su OTA Messages API（消息收发）的渠道条目：BOOKING/AIRBNB/EXPEDIA。
     */
    public static List<SuChannel> messagingSupportedChannels() {
        return MESSAGING_SUPPORTED;
    }

    /**
     * 支持 Su OTA Messages API 的内部渠道 code。
     */
    public static List<String> messagingSupportedChannelCodes() {
        return MESSAGING_SUPPORTED.stream().map(SuChannel::code).toList();
    }

    /**
     * 支持 Su OTA Messages API 的 Su channel_id。
     */
    public static List<Integer> messagingSupportedSuIds() {
        return MESSAGING_SUPPORTED.stream().map(SuChannel::suId).toList();
    }

    /**
     * 指定 Su channel_id 是否支持 Su OTA Messages API。
     */
    public static boolean isMessagingSupportedSuId(Integer suId) {
        return bySuId(suId).filter(MESSAGING_SUPPORTED::contains).isPresent();
    }

    /**
     * 支持 Su Review API（评论同步/回复）的渠道条目：BOOKING/AIRBNB/EXPEDIA。
     */
    public static List<SuChannel> reviewSupportedChannels() {
        return REVIEW_SUPPORTED;
    }

    /**
     * 支持 Su Review API 的内部渠道 code。
     */
    public static List<String> reviewSupportedChannelCodes() {
        return REVIEW_SUPPORTED.stream().map(SuChannel::code).toList();
    }

    /**
     * 支持 Su Review API 的 Su channel_id。
     */
    public static List<Integer> reviewSupportedSuIds() {
        return REVIEW_SUPPORTED.stream().map(SuChannel::suId).toList();
    }

    /**
     * 指定 Su channel_id 是否支持 Su Review API。
     */
    public static boolean isReviewSupportedSuId(Integer suId) {
        return bySuId(suId).filter(REVIEW_SUPPORTED::contains).isPresent();
    }

    private static Map<String, SuChannel> buildByCode() {
        Map<String, SuChannel> map = new LinkedHashMap<>();
        for (SuChannel channel : ALL) {
            map.put(channel.code(), channel);
        }
        CODE_ALIASES.forEach(map::putIfAbsent);
        return Collections.unmodifiableMap(map);
    }

    private static Map<Integer, SuChannel> buildBySuId() {
        Map<Integer, SuChannel> map = new LinkedHashMap<>();
        for (SuChannel channel : ALL) {
            map.put(channel.suId(), channel);
        }
        return Collections.unmodifiableMap(map);
    }
}
