package server.demo.util;

import server.demo.entity.Channel;
import server.demo.enums.PriceAdjustmentType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Objects;

import server.demo.i18n.ApiMessages;
public final class IndependentSitePricePolicy {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private IndependentSitePricePolicy() {
    }

    public static BigDecimal calculateNightPrice(Long storeId, Channel channel, BigDecimal basePrice) {
        if (storeId == null || channel == null || !Objects.equals(storeId, channel.getStoreId())) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.f3fd577e458f"));
        }
        if (channel.getPriceAdjustmentType() != PriceAdjustmentType.PERCENTAGE) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.c7a99fe68d2e"));
        }
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.99ac6fb7e5e5"));
        }
        BigDecimal adjustment = channel.getPriceAdjustmentValue() == null
                ? BigDecimal.ZERO
                : channel.getPriceAdjustmentValue();
        BigDecimal multiplier = BigDecimal.ONE.add(adjustment.divide(ONE_HUNDRED));
        BigDecimal result = basePrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
        if (result.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.fb87985bb81e"));
        }
        return result;
    }

    public static BigDecimal sumRoundedNights(Collection<BigDecimal> nightlyPrices) {
        BigDecimal total = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (nightlyPrices == null) {
            return total;
        }
        for (BigDecimal nightlyPrice : nightlyPrices) {
            if (nightlyPrice == null) {
                throw new IllegalArgumentException(ApiMessages.get("api.t.e42d915eef7f"));
            }
            total = total.add(nightlyPrice.setScale(2, RoundingMode.HALF_UP));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}
