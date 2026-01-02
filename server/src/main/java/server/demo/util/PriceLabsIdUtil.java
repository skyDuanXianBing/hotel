package server.demo.util;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PriceLabsIdUtil {
    private static final Pattern LISTING_ID_PATTERN = Pattern.compile("^store_(\\d+)_room_type_(\\d+)$");
    private static final Pattern LEGACY_LISTING_ID_PATTERN = Pattern.compile("^rt_(\\d+)$");
    private static final Pattern LEGACY_LISTING_ID_PATTERN_2 = Pattern.compile("^store_(\\d+)_rt_(\\d+)(?:_plan_(\\d+))?$");

    private static final Pattern RATE_PLAN_ID_PATTERN = Pattern.compile("^plan_(\\d+)$");
    private static final Pattern LEGACY_RATE_PLAN_ID_PATTERN = Pattern.compile("^pl_(\\d+)$");

    private PriceLabsIdUtil() {}

    public static String formatListingId(long storeId, long roomTypeId) {
        return "store_" + storeId + "_room_type_" + roomTypeId;
    }

    public static String formatRatePlanId(long pricePlanId) {
        return "plan_" + pricePlanId;
    }

    public static Optional<Long> parseStoreId(String listingId) {
        if (listingId == null) return Optional.empty();

        Matcher matcher = LISTING_ID_PATTERN.matcher(listingId);
        if (matcher.matches()) return Optional.of(Long.parseLong(matcher.group(1)));

        Matcher legacyMatcher = LEGACY_LISTING_ID_PATTERN_2.matcher(listingId);
        if (legacyMatcher.matches()) return Optional.of(Long.parseLong(legacyMatcher.group(1)));

        return Optional.empty();
    }

    public static Optional<Long> parseRoomTypeId(String listingId) {
        if (listingId == null) return Optional.empty();

        Matcher matcher = LISTING_ID_PATTERN.matcher(listingId);
        if (matcher.matches()) return Optional.of(Long.parseLong(matcher.group(2)));

        Matcher legacyMatcher = LEGACY_LISTING_ID_PATTERN.matcher(listingId);
        if (legacyMatcher.matches()) return Optional.of(Long.parseLong(legacyMatcher.group(1)));

        Matcher legacyMatcher2 = LEGACY_LISTING_ID_PATTERN_2.matcher(listingId);
        if (legacyMatcher2.matches()) return Optional.of(Long.parseLong(legacyMatcher2.group(2)));

        return Optional.empty();
    }

    public static Optional<Long> parsePricePlanId(String ratePlanId) {
        if (ratePlanId == null) return Optional.empty();

        Matcher matcher = RATE_PLAN_ID_PATTERN.matcher(ratePlanId);
        if (matcher.matches()) return Optional.of(Long.parseLong(matcher.group(1)));

        Matcher legacyMatcher = LEGACY_RATE_PLAN_ID_PATTERN.matcher(ratePlanId);
        if (legacyMatcher.matches()) return Optional.of(Long.parseLong(legacyMatcher.group(1)));

        return Optional.empty();
    }
}

