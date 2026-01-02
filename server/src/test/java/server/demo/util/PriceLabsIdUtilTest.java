package server.demo.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PriceLabsIdUtilTest {

    @Test
    void formatAndParseListingId() {
        String listingId = PriceLabsIdUtil.formatListingId(2L, 13L);
        assertEquals("store_2_room_type_13", listingId);

        assertEquals(2L, PriceLabsIdUtil.parseStoreId(listingId).orElseThrow());
        assertEquals(13L, PriceLabsIdUtil.parseRoomTypeId(listingId).orElseThrow());
    }

    @Test
    void parseLegacyListingId() {
        assertEquals(13L, PriceLabsIdUtil.parseRoomTypeId("rt_13").orElseThrow());
        assertTrue(PriceLabsIdUtil.parseStoreId("rt_13").isEmpty());

        assertEquals(2L, PriceLabsIdUtil.parseStoreId("store_2_rt_13_plan_8").orElseThrow());
        assertEquals(13L, PriceLabsIdUtil.parseRoomTypeId("store_2_rt_13_plan_8").orElseThrow());
    }

    @Test
    void formatAndParseRatePlanId() {
        String ratePlanId = PriceLabsIdUtil.formatRatePlanId(8L);
        assertEquals("plan_8", ratePlanId);
        assertEquals(8L, PriceLabsIdUtil.parsePricePlanId(ratePlanId).orElseThrow());
    }

    @Test
    void parseLegacyRatePlanId() {
        assertEquals(8L, PriceLabsIdUtil.parsePricePlanId("pl_8").orElseThrow());
    }
}

