package server.demo.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PriceLabsCountryUtilTest {

    @Test
    void normalizeToAlpha2() {
        assertEquals("CN", PriceLabsCountryUtil.normalizeToAlpha2("CN"));
        assertEquals("CN", PriceLabsCountryUtil.normalizeToAlpha2("CHN"));
        assertEquals("CN", PriceLabsCountryUtil.normalizeToAlpha2("China"));
        assertEquals("CN", PriceLabsCountryUtil.normalizeToAlpha2("中国"));
        assertEquals("JP", PriceLabsCountryUtil.normalizeToAlpha2("JPN"));
        assertEquals("US", PriceLabsCountryUtil.normalizeToAlpha2("USA"));
        assertNull(PriceLabsCountryUtil.normalizeToAlpha2("UNKNOWN"));
        assertNull(PriceLabsCountryUtil.normalizeToAlpha2(" "));
        assertNull(PriceLabsCountryUtil.normalizeToAlpha2(null));
    }

    @Test
    void normalizeToAlpha3() {
        assertEquals("CHN", PriceLabsCountryUtil.normalizeToAlpha3("CN"));
        assertEquals("CHN", PriceLabsCountryUtil.normalizeToAlpha3("CHN"));
        assertEquals("CHN", PriceLabsCountryUtil.normalizeToAlpha3("China"));
        assertEquals("CHN", PriceLabsCountryUtil.normalizeToAlpha3("中国"));
        assertEquals("JPN", PriceLabsCountryUtil.normalizeToAlpha3("JP"));
        assertEquals("JPN", PriceLabsCountryUtil.normalizeToAlpha3("JPN"));
        assertEquals("USA", PriceLabsCountryUtil.normalizeToAlpha3("US"));
        assertEquals("USA", PriceLabsCountryUtil.normalizeToAlpha3("USA"));
        assertNull(PriceLabsCountryUtil.normalizeToAlpha3("UNKNOWN"));
        assertNull(PriceLabsCountryUtil.normalizeToAlpha3(" "));
        assertNull(PriceLabsCountryUtil.normalizeToAlpha3(null));
    }
}
