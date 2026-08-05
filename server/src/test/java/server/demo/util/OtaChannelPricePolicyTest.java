package server.demo.util;

import org.junit.jupiter.api.Test;
import server.demo.entity.Channel;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OtaChannelPricePolicyTest {

    @Test
    void usesSuMappingMultiplier_shouldCoverAllFiveSuChannels() {
        assertTrue(OtaChannelPricePolicy.usesSuMappingMultiplier("AIRBNB"));
        assertTrue(OtaChannelPricePolicy.usesSuMappingMultiplier("BOOKING"));
        assertTrue(OtaChannelPricePolicy.usesSuMappingMultiplier("BOOKING.COM"));
        assertTrue(OtaChannelPricePolicy.usesSuMappingMultiplier("EXPEDIA"));
        assertTrue(OtaChannelPricePolicy.usesSuMappingMultiplier("TRIP"));
        assertTrue(OtaChannelPricePolicy.usesSuMappingMultiplier("AGODA"));
    }

    @Test
    void usesSuMappingMultiplier_shouldBeCaseInsensitive() {
        assertTrue(OtaChannelPricePolicy.usesSuMappingMultiplier("expedia"));
        assertTrue(OtaChannelPricePolicy.usesSuMappingMultiplier(" trip "));
        assertTrue(OtaChannelPricePolicy.usesSuMappingMultiplier("agoda"));
    }

    @Test
    void usesSuMappingMultiplier_shouldRejectNonSuChannels() {
        assertFalse(OtaChannelPricePolicy.usesSuMappingMultiplier((String) null));
        assertFalse(OtaChannelPricePolicy.usesSuMappingMultiplier(""));
        assertFalse(OtaChannelPricePolicy.usesSuMappingMultiplier("DIRECT"));
        assertFalse(OtaChannelPricePolicy.usesSuMappingMultiplier("BOOKING_ENGINE"));
        assertFalse(OtaChannelPricePolicy.usesSuMappingMultiplier("TRAVELOKA"));
    }

    @Test
    void resolveLocalFixedPrice_shouldKeepBasePriceForNewSuChannels() {
        Channel expedia = new Channel();
        expedia.setCode("EXPEDIA");
        Channel trip = new Channel();
        trip.setCode("TRIP");
        Channel agoda = new Channel();
        agoda.setCode("AGODA");

        BigDecimal base = new BigDecimal("100.00");
        // Su 映射倍率模式下本地固定价 = 基础价（倍率在 Su 侧映射上生效）
        assertEquals(base, OtaChannelPricePolicy.resolveLocalFixedPrice(expedia, base));
        assertEquals(base, OtaChannelPricePolicy.resolveLocalFixedPrice(trip, base));
        assertEquals(base, OtaChannelPricePolicy.resolveLocalFixedPrice(agoda, base));
    }
}
