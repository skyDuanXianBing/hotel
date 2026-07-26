package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndependentSitePageSchemaValidatorTest {

    private ObjectMapper objectMapper;
    private IndependentSitePageSchemaParser parser;
    private IndependentSitePageSchemaValidator validator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        parser = new IndependentSitePageSchemaParser(objectMapper);
        validator = new IndependentSitePageSchemaValidator(objectMapper);
    }

    @Test
    void parseAndValidate_shouldAcceptFencedWhitelistedSchemaAndCanonicalizeEnums() {
        String output = """
                ```json
                {
                  "schemaVersion": "independent_site_page_v1",
                  "theme": {
                    "primaryColor": "#123abc",
                    "accentColor": "#ff8800",
                    "surfaceColor": "#ffffff",
                    "textColor": "#111827",
                    "typography": "modern",
                    "cornerStyle": "soft"
                  },
                  "sections": [
                    {
                      "type": "hero",
                      "title": "A calm city stay",
                      "body": "Thoughtful rooms near the old town.",
                      "alignment": "center"
                    },
                    {
                      "type": "amenities",
                      "title": "Amenities",
                      "items": ["Quiet rooms", "Luggage storage"],
                      "alignment": "left"
                    }
                  ]
                }
                ```
                """;

        JsonNode schema = validator.validate(parser.parse(output));

        assertEquals("independent_site_page_v1", schema.get("schemaVersion").asText());
        assertEquals("#123ABC", schema.path("theme").path("primaryColor").asText());
        assertEquals("MODERN", schema.path("theme").path("typography").asText());
        assertEquals("HERO", schema.path("sections").get(0).path("type").asText());
        assertTrue(schema.path("sections").get(0).path("items").isMissingNode());
    }

    @Test
    void validate_shouldRejectHtmlUrlsPaymentAndUnknownExecutableFields() throws Exception {
        JsonNode html = objectMapper.readTree(validJson().replace(
                "\"A calm stay\"",
                "\"<script>alert(1)</script>\""
        ));
        JsonNode url = objectMapper.readTree(validJson().replace(
                "\"A calm stay\"",
                "\"Visit https://example.test\""
        ));
        JsonNode payment = objectMapper.readTree(validJson().replace(
                "\"A calm stay\"",
                "\"Payment options\""
        ));
        JsonNode unknownField = objectMapper.readTree(validJson().replace(
                "\"alignment\":\"CENTER\"",
                "\"alignment\":\"CENTER\",\"onClick\":\"run\""
        ));

        assertThrows(IllegalArgumentException.class, () -> validator.validate(html));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(url));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(payment));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(unknownField));
    }

    @Test
    void validate_shouldRejectSchemaThatAttemptsToReplaceFixedBookingSection() throws Exception {
        JsonNode schema = objectMapper.readTree(validJson().replace("\"HERO\"", "\"CHECKOUT\""));

        assertThrows(IllegalArgumentException.class, () -> validator.validate(schema));
    }

    @Test
    void validate_shouldRejectCssRelativeRoutesAndBareDomains() throws Exception {
        JsonNode css = objectMapper.readTree(validJson().replace(
                "\"A calm stay\"",
                "\"color: red; display: none\""
        ));
        JsonNode route = objectMapper.readTree(validJson().replace(
                "\"A calm stay\"",
                "\"Continue at /rooms/select\""
        ));
        JsonNode domain = objectMapper.readTree(validJson().replace(
                "\"A calm stay\"",
                "\"Visit example.hotel for details\""
        ));

        assertThrows(IllegalArgumentException.class, () -> validator.validate(css));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(route));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(domain));
    }

    @Test
    void validate_shouldAcceptSectionIdImageUrlGalleryAndBookingExtensions() throws Exception {
        JsonNode schema = objectMapper.readTree("""
                {
                  "schemaVersion":"independent_site_page_v1",
                  "theme":{
                    "primaryColor":"#2563EB",
                    "accentColor":"#F59E0B",
                    "surfaceColor":"#FFFFFF",
                    "textColor":"#111827",
                    "typography":"MODERN",
                    "cornerStyle":"SOFT"
                  },
                  "sections":[
                    {
                      "type":"HERO",
                      "id":"hero-main",
                      "title":"A calm stay",
                      "body":"Comfort in the heart of town.",
                      "imageUrl":"https://cdn.example.test/hero.jpg",
                      "alignment":"CENTER"
                    },
                    {
                      "type":"GALLERY",
                      "title":"Gallery",
                      "images":[
                        {"url":"/media/1/site/a.jpg","alt":"Sunny lobby"},
                        {"url":"https://cdn.example.test/b.jpg"}
                      ]
                    },
                    {
                      "type":"BOOKING",
                      "title":"Reserve",
                      "alignment":"CENTER"
                    }
                  ]
                }
                """);

        JsonNode validated = validator.validate(schema);

        assertEquals("hero-main", validated.path("sections").get(0).path("id").asText());
        assertEquals(
                "https://cdn.example.test/hero.jpg",
                validated.path("sections").get(0).path("imageUrl").asText()
        );
        assertEquals(
                "Sunny lobby",
                validated.path("sections").get(1).path("images").get(0).path("alt").asText()
        );
        assertEquals("BOOKING", validated.path("sections").get(2).path("type").asText());
    }

    @Test
    void validate_shouldRejectInvalidImageUrlValues() throws Exception {
        JsonNode javascript = objectMapper.readTree(validJson().replace(
                "\"alignment\":\"CENTER\"",
                "\"alignment\":\"CENTER\",\"imageUrl\":\"javascript:alert(1)\""
        ));
        JsonNode data = objectMapper.readTree(validJson().replace(
                "\"alignment\":\"CENTER\"",
                "\"alignment\":\"CENTER\",\"imageUrl\":\"data:image/png;base64,AA\""
        ));
        JsonNode bare = objectMapper.readTree(validJson().replace(
                "\"alignment\":\"CENTER\"",
                "\"alignment\":\"CENTER\",\"imageUrl\":\"example.test/hero.jpg\""
        ));
        JsonNode onLocation = objectMapper.readTree(validJson().replace(
                "\"type\":\"HERO\"",
                "\"type\":\"LOCATION\""
        ).replace(
                "\"alignment\":\"CENTER\"",
                "\"alignment\":\"CENTER\",\"imageUrl\":\"https://cdn.example.test/a.jpg\""
        ));

        assertThrows(IllegalArgumentException.class, () -> validator.validate(javascript));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(data));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(bare));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(onLocation));
    }

    @Test
    void validate_shouldEnforceGalleryRules() throws Exception {
        JsonNode galleryWithItems = objectMapper.readTree(validJson().replace(
                "\"alignment\":\"CENTER\"",
                """
                "alignment":"CENTER"
                  },
                  {
                    "type":"GALLERY",
                    "title":"Gallery",
                    "items":["one"],
                    "images":[{"url":"https://cdn.example.test/a.jpg"}]
                """));
        JsonNode galleryWithoutImages = objectMapper.readTree(validJson().replace(
                "\"alignment\":\"CENTER\"",
                """
                "alignment":"CENTER"
                  },
                  {
                    "type":"GALLERY",
                    "title":"Gallery"
                """));
        JsonNode galleryBadUrl = objectMapper.readTree(validJson().replace(
                "\"alignment\":\"CENTER\"",
                """
                "alignment":"CENTER"
                  },
                  {
                    "type":"GALLERY",
                    "title":"Gallery",
                    "images":[{"url":"data:text/html;base64,AA"}]
                """));
        JsonNode galleryBadAlt = objectMapper.readTree(validJson().replace(
                "\"alignment\":\"CENTER\"",
                """
                "alignment":"CENTER"
                  },
                  {
                    "type":"GALLERY",
                    "title":"Gallery",
                    "images":[{"url":"https://cdn.example.test/a.jpg","alt":"see https://x.test"}]
                """));
        JsonNode aboutWithImages = objectMapper.readTree(validJson().replace(
                "\"alignment\":\"CENTER\"",
                "\"alignment\":\"CENTER\",\"images\":[{\"url\":\"https://cdn.example.test/a.jpg\"}]"
        ));

        assertThrows(IllegalArgumentException.class, () -> validator.validate(galleryWithItems));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(galleryWithoutImages));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(galleryBadUrl));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(galleryBadAlt));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(aboutWithImages));
    }

    @Test
    void validate_shouldRejectSecondBookingSectionAndBookingItems() throws Exception {
        JsonNode twoBookings = objectMapper.readTree(validJson().replace(
                "\"alignment\":\"CENTER\"",
                """
                "alignment":"CENTER"
                  },
                  {
                    "type":"BOOKING",
                    "title":"Reserve"
                  },
                  {
                    "type":"BOOKING",
                    "title":"Reserve again"
                """));
        JsonNode bookingWithItems = objectMapper.readTree(validJson().replace(
                "\"alignment\":\"CENTER\"",
                """
                "alignment":"CENTER"
                  },
                  {
                    "type":"BOOKING",
                    "title":"Reserve",
                    "items":["one"]
                """));
        JsonNode bookingWithImage = objectMapper.readTree(validJson().replace(
                "\"alignment\":\"CENTER\"",
                """
                "alignment":"CENTER"
                  },
                  {
                    "type":"BOOKING",
                    "title":"Reserve",
                    "imageUrl":"https://cdn.example.test/a.jpg"
                """));

        assertThrows(IllegalArgumentException.class, () -> validator.validate(twoBookings));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(bookingWithItems));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(bookingWithImage));
    }

    @Test
    void validate_shouldRejectMalformedSectionId() throws Exception {
        JsonNode schema = objectMapper.readTree(validJson().replace(
                "\"alignment\":\"CENTER\"",
                "\"alignment\":\"CENTER\",\"id\":\"bad id with spaces\""
        ));

        assertThrows(IllegalArgumentException.class, () -> validator.validate(schema));
    }

    private static String validJson() {
        return """
                {
                  "schemaVersion":"independent_site_page_v1",
                  "theme":{
                    "primaryColor":"#2563EB",
                    "accentColor":"#F59E0B",
                    "surfaceColor":"#FFFFFF",
                    "textColor":"#111827",
                    "typography":"MODERN",
                    "cornerStyle":"SOFT"
                  },
                  "sections":[
                    {
                      "type":"HERO",
                      "title":"A calm stay",
                      "body":"Comfort in the heart of town.",
                      "alignment":"CENTER"
                    }
                  ]
                }
                """;
    }
}
