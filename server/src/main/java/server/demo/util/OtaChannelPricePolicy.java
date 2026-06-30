package server.demo.util;

import server.demo.entity.Channel;
import server.demo.entity.ChannelPrice;
import server.demo.enums.PriceAdjustmentType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

public final class OtaChannelPricePolicy {

    public static final String CHANNEL_CODE_AIRBNB = "AIRBNB";
    public static final String CHANNEL_CODE_BOOKING = "BOOKING";
    public static final String CHANNEL_CODE_BOOKING_COM = "BOOKING.COM";

    private static final Set<String> SU_MAPPING_MULTIPLIER_CHANNELS = Set.of(
            CHANNEL_CODE_AIRBNB,
            CHANNEL_CODE_BOOKING,
            CHANNEL_CODE_BOOKING_COM
    );

    private OtaChannelPricePolicy() {
    }

    public static boolean usesSuMappingMultiplier(Channel channel) {
        if (channel == null) {
            return false;
        }
        return usesSuMappingMultiplier(channel.getCode());
    }

    public static boolean usesSuMappingMultiplier(String channelCode) {
        String normalized = normalizeCode(channelCode);
        return normalized != null && SU_MAPPING_MULTIPLIER_CHANNELS.contains(normalized);
    }

    public static BigDecimal resolveLocalFixedPrice(Channel channel, BigDecimal basePrice) {
        if (basePrice == null) {
            return null;
        }
        if (usesSuMappingMultiplier(channel)) {
            return basePrice;
        }
        return channel != null ? channel.calculateChannelPrice(basePrice) : basePrice;
    }

    public static BigDecimal resolveOtaFixedPrice(ChannelPrice channelPrice) {
        if (channelPrice == null) {
            return null;
        }
        if (channelPrice.getBasePrice() != null) {
            return channelPrice.getBasePrice();
        }
        BigDecimal fixedPrice = channelPrice.getChannelPrice();
        if (fixedPrice == null) {
            return null;
        }

        Channel channel = channelPrice.getChannel();
        if (!usesSuMappingMultiplier(channel)) {
            return fixedPrice;
        }

        return reverseLocalAdjustment(channel, fixedPrice);
    }

    private static BigDecimal reverseLocalAdjustment(Channel channel, BigDecimal adjustedPrice) {
        if (channel == null || adjustedPrice == null) {
            return adjustedPrice;
        }

        PriceAdjustmentType type = channel.getPriceAdjustmentType();
        if (type == null) {
            type = PriceAdjustmentType.PERCENTAGE;
        }

        if (type == PriceAdjustmentType.FIXED) {
            BigDecimal adjustment = channel.getPriceAdjustmentValue();
            if (adjustedPrice.compareTo(BigDecimal.ZERO) <= 0) {
                return null;
            }
            if (adjustment == null) {
                return adjustedPrice;
            }
            BigDecimal reversed = adjustedPrice.subtract(adjustment).setScale(2, RoundingMode.HALF_UP);
            return reversed.compareTo(BigDecimal.ZERO) > 0 ? reversed : adjustedPrice;
        }

        BigDecimal percentage = channel.getPriceAdjustmentValue();
        if (type == PriceAdjustmentType.COMMISSION && channel.getCommissionRate() != null) {
            percentage = BigDecimal.valueOf(channel.getCommissionRate());
        }
        if (percentage == null) {
            return adjustedPrice;
        }

        BigDecimal multiplier = BigDecimal.ONE.add(
                percentage.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
        );
        if (multiplier.compareTo(BigDecimal.ZERO) <= 0) {
            return adjustedPrice;
        }

        return adjustedPrice.divide(multiplier, 2, RoundingMode.HALF_UP);
    }

    private static String normalizeCode(String channelCode) {
        if (channelCode == null) {
            return null;
        }
        String normalized = channelCode.trim().toUpperCase();
        return normalized.isBlank() ? null : normalized;
    }
}
