package server.demo.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SuRoomIdParserTest {

    @Test
    void parse_shouldReturnNullForInvalid() {
        assertNull(SuRoomIdParser.parse(null));
        assertNull(SuRoomIdParser.parse(""));
        assertNull(SuRoomIdParser.parse("   "));
        assertNull(SuRoomIdParser.parse("1-"));
        assertNull(SuRoomIdParser.parse("-101"));
        assertNull(SuRoomIdParser.parse("x-101"));
        assertNull(SuRoomIdParser.parse("x"));
    }

    @Test
    void parse_shouldParseRoomTypeIdAndRoomNumber() {
        SuRoomIdParser.ParsedRoomId parsed = SuRoomIdParser.parse("1-101");
        assertNotNull(parsed);
        assertEquals(1L, parsed.roomTypeId());
        assertEquals("101", parsed.roomNumber());
        assertEquals("1-101", parsed.raw());
    }

    @Test
    void parse_shouldParseRoomTypeIdOnly() {
        SuRoomIdParser.ParsedRoomId parsed = SuRoomIdParser.parse("1");
        assertNotNull(parsed);
        assertEquals(1L, parsed.roomTypeId());
        assertNull(parsed.roomNumber());
        assertEquals("1", parsed.raw());
    }

    @Test
    void parse_shouldSplitOnlyOnFirstDash() {
        SuRoomIdParser.ParsedRoomId parsed = SuRoomIdParser.parse("12-A-101");
        assertNotNull(parsed);
        assertEquals(12L, parsed.roomTypeId());
        assertEquals("A-101", parsed.roomNumber());
    }
}

