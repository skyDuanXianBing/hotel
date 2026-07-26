package server.demo.entity;

import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ChannelReviewHashColumnMappingTest {

    @Test
    void hashColumns_shouldMatchV056CharDefinitions() throws Exception {
        assertColumnMapping(
                ChannelReview.class.getDeclaredField("sourceEventHash"),
                "source_event_hash",
                true
        );
        assertColumnMapping(
                ChannelReviewAction.class.getDeclaredField("requestHash"),
                "request_hash",
                false
        );
    }

    private void assertColumnMapping(Field field, String expectedName, boolean expectedNullable) {
        Column column = field.getAnnotation(Column.class);

        assertNotNull(column);
        assertEquals(expectedName, column.name());
        assertEquals(64, column.length());
        assertEquals(expectedNullable, column.nullable());
        assertEquals("CHAR(64)", column.columnDefinition());
    }
}
