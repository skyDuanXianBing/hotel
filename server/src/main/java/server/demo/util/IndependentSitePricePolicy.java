package server.demo.util;

import server.demo.entity.Channel;
import server.demo.enums.PriceAdjustmentType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Objects;

public final class IndependentSitePricePolicy {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private IndependentSitePricePolicy() {
    }

    public static BigDecimal calculateNightPrice(Long storeId, Channel channel, BigDecimal basePrice) {
        if (storeId == null || channel == null || !Objects.equals(storeId, channel.getStoreId())) {
            throw new IllegalArgumentException("独立站渠道不属于当前门店");
        }
        if (channel.getPriceAdjustmentType() != PriceAdjustmentType.PERCENTAGE) {
            throw new IllegalArgumentException("独立站渠道仅支持 PERCENTAGE 价格调整");
        }
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("基础房价必须大于 0");
        }
        BigDecimal adjustment = channel.getPriceAdjustmentValue() == null
                ? BigDecimal.ZERO
                : channel.getPriceAdjustmentValue();
        BigDecimal multiplier = BigDecimal.ONE.add(adjustment.divide(ONE_HUNDRED));
        BigDecimal result = basePrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
        if (result.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("独立站渠道调整后的房价必须大于 0");
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
                throw new IllegalArgumentException("逐晚价格不能为空");
            }
            total = total.add(nightlyPrice.setScale(2, RoundingMode.HALF_UP));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}
