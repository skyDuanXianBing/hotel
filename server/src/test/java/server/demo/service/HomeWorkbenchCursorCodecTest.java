package server.demo.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HomeWorkbenchCursorCodecTest {
    private final HomeWorkbenchCursorCodec codec = new HomeWorkbenchCursorCodec();

    @Test
    void roundTripsVersionedSortKey() {
        HomeWorkbenchCursorCodec.SortKey expected = new HomeWorkbenchCursorCodec.SortKey(
                1, 0, LocalDateTime.of(2026, 7, 12, 9, 30), 2, 9182L);

        String cursor = codec.encode("store=26|user=3|review", expected);

        assertEquals(expected, codec.decode(cursor, "store=26|user=3|review"));
    }

    @Test
    void rejectsMalformedAndCrossContextCursor() {
        HomeWorkbenchCursorCodec.SortKey key = new HomeWorkbenchCursorCodec.SortKey(
                0, 1, null, 5, 99L);
        String cursor = codec.encode("store=26", key);

        assertThrows(IllegalArgumentException.class, () -> codec.decode("not-base64", "store=26"));
        assertThrows(IllegalArgumentException.class, () -> codec.decode(cursor, "store=27"));
    }

    @Test
    void rejectsOversizedCursorBeforeDecode() {
        assertThrows(IllegalArgumentException.class,
                () -> codec.decode("A".repeat(513), "store=26"));
    }

    @Test
    void rejectsForgedRanksNullContradictionNegativeIdAndIllegalTime() {
        HomeWorkbenchCursorCodec.SortKey key = new HomeWorkbenchCursorCodec.SortKey(
                1, 0, LocalDateTime.of(2026, 7, 12, 10, 0), 4, 31L);
        String valid = codec.encode("store=26", key);

        assertInvalid(forge(valid, 2, "9"));
        assertInvalid(forge(valid, 3, "2"));
        assertInvalid(forge(valid, 3, "1"));
        assertInvalid(forge(valid, 5, "6"));
        assertInvalid(forge(valid, 6, "-1"));
        assertInvalid(forge(valid, 4, "not-a-time"));
    }

    private void assertInvalid(String cursor) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> codec.decode(cursor, "store=26")
        );
        assertEquals("无效的工作台游标", exception.getMessage());
    }

    private static String forge(String cursor, int field, String value) {
        String payload = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
        String[] fields = payload.split("\\|", -1);
        fields[field] = value;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(String.join("|", fields).getBytes(StandardCharsets.UTF_8));
    }
}
