package server.demo.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class AutoMessageTemplateRendererTest {

    @Test
    void render_nullTemplate_returnsNull() {
        assertNull(AutoMessageTemplateRenderer.render(null, Map.of("guest_name", "A")));
    }

    @Test
    void render_replacesKnownTokens_andUnknownToEmpty() {
        String tpl = "Hi {{guest_name}}, welcome to {{property_name}}. {{unknown}}";
        String out = AutoMessageTemplateRenderer.render(tpl, Map.of(
                "guest_name", "Alice",
                "property_name", "HotelX"
        ));
        assertEquals("Hi Alice, welcome to HotelX. ", out);
    }

    @Test
    void render_allowsWhitespaceInsideBraces() {
        String tpl = "Hello {{  guest_name  }}";
        String out = AutoMessageTemplateRenderer.render(tpl, Map.of("guest_name", "Bob"));
        assertEquals("Hello Bob", out);
    }
}

