package server.demo.util;

import org.junit.jupiter.api.Test;
import server.demo.entity.Channel;
import server.demo.enums.PriceAdjustmentType;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IndependentSitePricePolicyTest {

    @Test
    void calculateNightPrice_shouldApplyPositiveAndNegativePercentageWithHalfUpRounding() {
        Channel channel = channel(7L, "10.00");

        assertEquals(
                new BigDecimal("110.06"),
                IndependentSitePricePolicy.calculateNightPrice(
                        7L,
                        channel,
                        new BigDecimal("100.05")
                )
        );

        channel.setPriceAdjustmentValue(new BigDecimal("-10.00"));
        assertEquals(
                new BigDecimal("90.05"),
                IndependentSitePricePolicy.calculateNightPrice(
                        7L,
                        channel,
                        new BigDecimal("100.05")
                )
        );
    }

    @Test
    void sumRoundedNights_shouldSumAlreadyRoundedNightPricesInsteadOfRoundingOnlyTotal() {
        Channel channel = channel(7L, "10.00");
        BigDecimal first = IndependentSitePricePolicy.calculateNightPrice(
                7L,
                channel,
                new BigDecimal("10.05")
        );
        BigDecimal second = IndependentSitePricePolicy.calculateNightPrice(
                7L,
                channel,
                new BigDecimal("10.05")
        );

        assertEquals(new BigDecimal("11.06"), first);
        assertEquals(new BigDecimal("22.12"), IndependentSitePricePolicy.sumRoundedNights(List.of(first, second)));
    }

    @Test
    void calculateNightPrice_shouldRejectChannelFromAnotherStore() {
        Channel channel = channel(8L, "10.00");

        assertThrows(
                IllegalArgumentException.class,
                () -> IndependentSitePricePolicy.calculateNightPrice(
                        7L,
                        channel,
                        new BigDecimal("100.00")
                )
        );
    }

    private static Channel channel(Long storeId, String adjustment) {
        Channel channel = new Channel();
        channel.setStoreId(storeId);
        channel.setPriceAdjustmentType(PriceAdjustmentType.PERCENTAGE);
        channel.setPriceAdjustmentValue(new BigDecimal(adjustment));
        return channel;
    }
}
