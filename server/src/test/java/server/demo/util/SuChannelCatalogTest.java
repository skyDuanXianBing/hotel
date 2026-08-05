package server.demo.util;

import org.junit.jupiter.api.Test;
import server.demo.util.SuChannelCatalog.SuChannel;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SuChannelCatalogTest {

    @Test
    void byCode_shouldResolveAllFiveChannels() {
        assertEquals(19, SuChannelCatalog.byCode("BOOKING").orElseThrow().suId());
        assertEquals(244, SuChannelCatalog.byCode("AIRBNB").orElseThrow().suId());
        assertEquals(9, SuChannelCatalog.byCode("EXPEDIA").orElseThrow().suId());
        assertEquals(339, SuChannelCatalog.byCode("TRIP").orElseThrow().suId());
        assertEquals(189, SuChannelCatalog.byCode("AGODA").orElseThrow().suId());
    }

    @Test
    void byCode_shouldBeCaseInsensitiveAndTrimmed() {
        assertEquals(339, SuChannelCatalog.byCode("trip").orElseThrow().suId());
        assertEquals(9, SuChannelCatalog.byCode(" Expedia ").orElseThrow().suId());
        assertEquals(189, SuChannelCatalog.byCode("aGoDa").orElseThrow().suId());
    }

    @Test
    void byCode_shouldResolveAliases() {
        SuChannel booking = SuChannelCatalog.byCode("BOOKING.COM").orElseThrow();
        assertEquals("BOOKING", booking.code());
        assertEquals(19, booking.suId());

        SuChannel trip = SuChannelCatalog.byCode("CTRIP").orElseThrow();
        assertEquals("TRIP", trip.code());
        assertEquals(339, trip.suId());
        assertEquals("Trip.com", trip.displayName());
    }

    @Test
    void byCode_shouldReturnEmptyForUnknownOrBlank() {
        assertTrue(SuChannelCatalog.byCode(null).isEmpty());
        assertTrue(SuChannelCatalog.byCode("").isEmpty());
        assertTrue(SuChannelCatalog.byCode("   ").isEmpty());
        assertTrue(SuChannelCatalog.byCode("VRBO").isEmpty());
        assertTrue(SuChannelCatalog.byCode("TRAVELOKA").isEmpty());
    }

    @Test
    void bySuId_shouldResolveAllFiveChannels() {
        assertEquals("BOOKING", SuChannelCatalog.bySuId(19).orElseThrow().code());
        assertEquals("AIRBNB", SuChannelCatalog.bySuId(244).orElseThrow().code());
        assertEquals("EXPEDIA", SuChannelCatalog.bySuId(9).orElseThrow().code());
        assertEquals("TRIP", SuChannelCatalog.bySuId(339).orElseThrow().code());
        assertEquals("AGODA", SuChannelCatalog.bySuId(189).orElseThrow().code());
    }

    @Test
    void bySuId_shouldReturnEmptyForUnknownOrNull() {
        assertTrue(SuChannelCatalog.bySuId(null).isEmpty());
        assertTrue(SuChannelCatalog.bySuId(999).isEmpty());
        assertTrue(SuChannelCatalog.bySuId(150).isEmpty());
    }

    @Test
    void allCodesAndSuIds_shouldContainExactlyFiveChannelsInCatalogOrder() {
        assertEquals(List.of("BOOKING", "AIRBNB", "EXPEDIA", "TRIP", "AGODA"), SuChannelCatalog.allCodes());
        assertEquals(List.of(19, 244, 9, 339, 189), SuChannelCatalog.allSuIds());
    }

    @Test
    void supportedOtaChannelCodes_shouldEqualAllCodes() {
        assertEquals(SuChannelCatalog.allCodes(), SuChannelCatalog.supportedOtaChannelCodes());
    }

    @Test
    void supportedReservationChannelCodes_shouldEqualAllCodes() {
        assertEquals(SuChannelCatalog.allCodes(), SuChannelCatalog.supportedReservationChannelCodes());
    }

    @Test
    void messagingSupportedChannels_shouldContainBookingAirbnbExpediaOnly() {
        // 依据 Su 官方《OTA Messages Collection and Reply API》：支持 19/244/9/253(VRBO)；
        // VRBO 未接入，TRIP(339)/AGODA(189) 官方不支持消息，严禁放行
        assertEquals(
                List.of(SuChannelCatalog.BOOKING, SuChannelCatalog.AIRBNB, SuChannelCatalog.EXPEDIA),
                SuChannelCatalog.messagingSupportedChannels()
        );
        assertEquals(List.of("BOOKING", "AIRBNB", "EXPEDIA"), SuChannelCatalog.messagingSupportedChannelCodes());
        assertEquals(List.of(19, 244, 9), SuChannelCatalog.messagingSupportedSuIds());

        assertTrue(SuChannelCatalog.isMessagingSupportedSuId(19));
        assertTrue(SuChannelCatalog.isMessagingSupportedSuId(244));
        assertTrue(SuChannelCatalog.isMessagingSupportedSuId(9));
        assertFalse(SuChannelCatalog.isMessagingSupportedSuId(339));
        assertFalse(SuChannelCatalog.isMessagingSupportedSuId(189));
        assertFalse(SuChannelCatalog.isMessagingSupportedSuId(253));
        assertFalse(SuChannelCatalog.isMessagingSupportedSuId(null));
        assertFalse(SuChannelCatalog.isMessagingSupportedSuId(999));
    }

    @Test
    void reviewSupportedChannels_shouldContainBookingAirbnbExpediaOnly() {
        // 依据 Su 官方《Review Master Data》渠道清单：19/244/9；TRIP/AGODA 不在列
        assertEquals(
                List.of(SuChannelCatalog.BOOKING, SuChannelCatalog.AIRBNB, SuChannelCatalog.EXPEDIA),
                SuChannelCatalog.reviewSupportedChannels()
        );
        assertEquals(List.of("BOOKING", "AIRBNB", "EXPEDIA"), SuChannelCatalog.reviewSupportedChannelCodes());
        assertEquals(List.of(19, 244, 9), SuChannelCatalog.reviewSupportedSuIds());

        assertTrue(SuChannelCatalog.isReviewSupportedSuId(19));
        assertTrue(SuChannelCatalog.isReviewSupportedSuId(244));
        assertTrue(SuChannelCatalog.isReviewSupportedSuId(9));
        assertFalse(SuChannelCatalog.isReviewSupportedSuId(339));
        assertFalse(SuChannelCatalog.isReviewSupportedSuId(189));
        assertFalse(SuChannelCatalog.isReviewSupportedSuId(253));
        assertFalse(SuChannelCatalog.isReviewSupportedSuId(null));
        assertFalse(SuChannelCatalog.isReviewSupportedSuId(999));
    }

    @Test
    void isKnown_shouldMatchByCode() {
        assertTrue(SuChannelCatalog.isKnown("TRIP"));
        assertTrue(SuChannelCatalog.isKnown("ctrip"));
        assertTrue(SuChannelCatalog.isKnown("BOOKING.COM"));
        assertFalse(SuChannelCatalog.isKnown("UNKNOWN"));
        assertFalse(SuChannelCatalog.isKnown(null));
    }

    @Test
    void encryptedCodes_shouldMatchSuApiClientPresetValues() {
        // 加密码必须与 SuApiClient 既有预置值（官方核验）一致，CTRIP 别名与 TRIP 同值
        assertEquals("mvYVz5x5ExxioyfyMo3jUUpNVZVbMyC6SUExMG9iaIY", SuChannelCatalog.TRIP.encryptedCode());
        assertEquals("Qa9Qwq4PF32srUVea3mYzzvBFiszeXK4aaQINYhXlm8", SuChannelCatalog.BOOKING.encryptedCode());
        assertEquals("aM4JjiWOnUx5qS2IT8wHCbVmIWbA9tTD3PFcjnt8M-Y", SuChannelCatalog.AIRBNB.encryptedCode());
        assertEquals("_4PYESNQm9vU15C3DR4xRrW2VHVrEVGPdhx4du8_uBw", SuChannelCatalog.EXPEDIA.encryptedCode());
        assertEquals("sAr2QsPWYcMUS-7PKJtEDGG0aZODNK5Sv4B5o2LTPA0", SuChannelCatalog.AGODA.encryptedCode());
    }
}
