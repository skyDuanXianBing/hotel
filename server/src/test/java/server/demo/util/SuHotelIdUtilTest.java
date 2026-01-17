package server.demo.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SuHotelIdUtilTest {

    @Test
    void generateRandom_default_shouldBeValid() {
        String id = SuHotelIdUtil.generateRandom();
        assertNotNull(id);
        assertTrue(SuHotelIdUtil.isValid(id));
        assertTrue(id.length() <= 15);
        assertEquals(id, id.toUpperCase());
    }

    @Test
    void generateRandom_customLength_shouldBeValid() {
        String id = SuHotelIdUtil.generateRandom(15);
        assertEquals(15, id.length());
        assertTrue(SuHotelIdUtil.isValid(id));
    }

    @Test
    void generateRandom_invalidLength_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> SuHotelIdUtil.generateRandom(0));
        assertThrows(IllegalArgumentException.class, () -> SuHotelIdUtil.generateRandom(16));
    }
}

