package server.demo.util;

import org.junit.jupiter.api.Test;
import server.demo.entity.PriceLabsSyncLog;

import static org.junit.jupiter.api.Assertions.*;

class JsonColumnUtilTest {

    @Test
    void normalizeJsonText_nullOrBlank_returnsNull() {
        assertNull(JsonColumnUtil.normalizeJsonText(null));
        assertNull(JsonColumnUtil.normalizeJsonText(""));
        assertNull(JsonColumnUtil.normalizeJsonText("   "));
    }

    @Test
    void normalizeJsonText_plainText_becomesJsonString() {
        assertEquals("\"PriceLabs 集成注册\"", JsonColumnUtil.normalizeJsonText("PriceLabs 集成注册"));
    }

    @Test
    void normalizeJsonText_validJsonObject_keptTrimmed() {
        assertEquals("{\"a\":1}", JsonColumnUtil.normalizeJsonText("  {\"a\":1}  "));
    }

    @Test
    void normalizeJsonText_validJsonArray_keptTrimmed() {
        assertEquals("[1,2,3]", JsonColumnUtil.normalizeJsonText("\n[1,2,3]\n"));
    }

    @Test
    void normalizeJsonText_invalidJson_isQuotedAsString() {
        assertEquals("\"{a:1}\"", JsonColumnUtil.normalizeJsonText("{a:1}"));
    }

    @Test
    void entitySetter_requestData_isAlwaysValidJsonText() {
        PriceLabsSyncLog log = new PriceLabsSyncLog();
        log.setRequestData("type: FULL_SYNC");
        assertEquals("\"type: FULL_SYNC\"", log.getRequestData());
    }
}

