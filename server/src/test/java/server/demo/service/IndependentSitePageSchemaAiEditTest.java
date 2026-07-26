package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import server.demo.dto.IndependentSiteDtos;
import server.demo.entity.IndependentSite;
import server.demo.entity.IndependentSitePage;
import server.demo.enums.IndependentSitePageType;
import server.demo.repository.IndependentSitePageRepository;
import server.demo.repository.IndependentSiteRepository;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndependentSitePageSchemaAiEditTest {

    private static final String ORIGINAL_DRAFT = """
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
                {"type":"HERO","title":"Welcome","body":"Discover a calm stay","alignment":"CENTER"},
                {"type":"ABOUT","title":"About us","body":"A quiet inn","alignment":"LEFT"}
              ]
            }
            """;

    private static final String EDITED_SCHEMA = """
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
                {"type":"HERO","title":"A slower welcome","body":"Discover a calm stay","alignment":"CENTER"},
                {"type":"ABOUT","title":"About us","body":"A quiet inn","alignment":"LEFT"}
              ]
            }
            """;

    private static final String INVALID_SCHEMA = "{\"schemaVersion\":\"wrong\"}";

    @Test
    void aiEdit_shouldChangeOnlyInstructedSectionAndBackupPreviousDraft() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Fixture fixture = new Fixture(objectMapper);
        FakeAiClient client = new FakeAiClient(true, List.of(EDITED_SCHEMA));
        IndependentSitePageSchemaGenerationService service = fixture.service(
                client,
                defaultLimiter()
        );

        IndependentSiteDtos.PageDetailResponse response = service.aiEdit(
                1L,
                11L,
                111L,
                "Make the hero title warmer"
        );

        JsonNode sections = response.draftSchema().path("sections");
        assertEquals("A slower welcome", sections.get(0).path("title").asText());
        JsonNode canonicalOriginal = new IndependentSitePageSchemaValidator(objectMapper)
                .validate(objectMapper.readTree(ORIGINAL_DRAFT));
        assertEquals(canonicalOriginal.path("sections").get(1), sections.get(1));
        assertEquals(4L, response.draftVersion());
        assertEquals(4L, fixture.page.getDraftVersion());
        assertEquals(ORIGINAL_DRAFT, fixture.page.getDraftBackupSchemaJson());
        assertTrue(response.hasAiBackup());
        assertNotNull(fixture.page.getDraftUpdatedAt());
        assertEquals(1, fixture.saves.size());
        assertEquals(1, client.callCount());
        String prompt = client.messages().get(0);
        assertTrue(prompt.contains("CURRENT_SCHEMA"));
        assertTrue(prompt.contains("Discover a calm stay"));
        assertTrue(prompt.contains("Make the hero title warmer"));
    }

    @Test
    void aiEdit_shouldRepairAfterValidationFailureAndEchoDraftAndInstruction() {
        ObjectMapper objectMapper = new ObjectMapper();
        Fixture fixture = new Fixture(objectMapper);
        FakeAiClient client = new FakeAiClient(true, List.of(INVALID_SCHEMA, EDITED_SCHEMA));
        IndependentSitePageSchemaGenerationService service = fixture.service(
                client,
                defaultLimiter()
        );

        IndependentSiteDtos.PageDetailResponse response = service.aiEdit(
                1L,
                11L,
                111L,
                "Make the hero title warmer"
        );

        assertTrue(response.hasAiBackup());
        assertEquals(2, client.callCount());
        String repairMessage = client.messages().get(1);
        assertTrue(repairMessage.contains("Validation error"));
        assertTrue(repairMessage.contains("PREVIOUS_ANSWER"));
        assertTrue(repairMessage.contains("Discover a calm stay"));
        assertTrue(repairMessage.contains("Make the hero title warmer"));
    }

    @Test
    void aiEdit_shouldLeaveDraftAndBackupUntouchedWhenAttemptsExhausted() {
        ObjectMapper objectMapper = new ObjectMapper();
        Fixture fixture = new Fixture(objectMapper);
        FakeAiClient client = new FakeAiClient(
                true,
                List.of(INVALID_SCHEMA, INVALID_SCHEMA, INVALID_SCHEMA, EDITED_SCHEMA)
        );
        IndependentSitePageSchemaGenerationService service = fixture.service(
                client,
                defaultLimiter()
        );

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> service.aiEdit(1L, 11L, 111L, "Make the hero title warmer")
        );

        assertEquals("INVALID_AI_SCHEMA", exception.getCode());
        assertEquals(3, client.callCount());
        assertEquals(ORIGINAL_DRAFT, fixture.page.getDraftSchemaJson());
        assertNull(fixture.page.getDraftBackupSchemaJson());
        assertEquals(3L, fixture.page.getDraftVersion());
        assertTrue(fixture.saves.isEmpty());
    }

    @Test
    void aiEdit_shouldFailClosedWhenAiClientIsNotConfigured() {
        ObjectMapper objectMapper = new ObjectMapper();
        Fixture fixture = new Fixture(objectMapper);
        FakeAiClient client = new FakeAiClient(false, List.of(EDITED_SCHEMA));
        IndependentSitePageSchemaGenerationService service = fixture.service(
                client,
                defaultLimiter()
        );

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> service.aiEdit(1L, 11L, 111L, "Make the hero title warmer")
        );

        assertEquals(503, exception.getStatus().value());
        assertEquals("OPENAI_CHANNEL_UNAVAILABLE", exception.getCode());
        assertFalse(client.wasCalled());
        assertEquals(ORIGINAL_DRAFT, fixture.page.getDraftSchemaJson());
        assertNull(fixture.page.getDraftBackupSchemaJson());
        assertTrue(fixture.saves.isEmpty());
    }

    @Test
    void aiEdit_shouldRejectAboveRateLimitBeforeCallingAi() {
        ObjectMapper objectMapper = new ObjectMapper();
        Fixture fixture = new Fixture(objectMapper);
        FakeAiClient client = new FakeAiClient(
                true,
                List.of(EDITED_SCHEMA, EDITED_SCHEMA, EDITED_SCHEMA)
        );
        IndependentSiteManagementRateLimiter limiter = new IndependentSiteManagementRateLimiter(
                Clock.fixed(Instant.parse("2026-07-24T00:00:00Z"), ZoneOffset.UTC),
                2,
                Duration.ofHours(1)
        );
        IndependentSitePageSchemaGenerationService service = fixture.service(client, limiter);

        service.aiEdit(1L, 11L, 111L, "First edit");
        service.aiEdit(1L, 11L, 111L, "Second edit");
        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> service.aiEdit(1L, 11L, 111L, "Third edit")
        );

        assertEquals(429, exception.getStatus().value());
        assertEquals("AI_EDIT_RATE_LIMITED", exception.getCode());
        assertEquals(2, client.callCount());
        assertEquals(2, fixture.saves.size());
    }

    @Test
    void aiEdit_shouldRejectCrossSiteAndCrossStorePageBeforeAiCall() {
        ObjectMapper objectMapper = new ObjectMapper();
        Fixture fixture = new Fixture(objectMapper);
        FakeAiClient client = new FakeAiClient(true, List.of(EDITED_SCHEMA));
        IndependentSitePageSchemaGenerationService service = fixture.service(
                client,
                defaultLimiter()
        );

        IllegalArgumentException crossStore = assertThrows(
                IllegalArgumentException.class,
                () -> service.aiEdit(2L, 11L, 111L, "edit")
        );
        assertEquals("独立站不存在", crossStore.getMessage());

        IllegalArgumentException crossSite = assertThrows(
                IllegalArgumentException.class,
                () -> service.aiEdit(1L, 22L, 111L, "edit")
        );
        assertEquals("独立站不存在", crossSite.getMessage());

        IllegalArgumentException unknownPage = assertThrows(
                IllegalArgumentException.class,
                () -> service.aiEdit(1L, 11L, 999L, "edit")
        );
        assertEquals("页面不存在", unknownPage.getMessage());
        assertFalse(client.wasCalled());
        assertTrue(fixture.saves.isEmpty());
    }

    @Test
    void aiEdit_shouldRejectInvalidInstructionBeforeAiCall() {
        ObjectMapper objectMapper = new ObjectMapper();
        Fixture fixture = new Fixture(objectMapper);
        FakeAiClient client = new FakeAiClient(true, List.of(EDITED_SCHEMA));
        IndependentSitePageSchemaGenerationService service = fixture.service(
                client,
                defaultLimiter()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.aiEdit(1L, 11L, 111L, "   ")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> service.aiEdit(1L, 11L, 111L, "x".repeat(2001))
        );
        assertFalse(client.wasCalled());
        assertTrue(fixture.saves.isEmpty());
    }

    @Test
    void aiEdit_shouldFallBackToPublishedSchemaWhenDraftIsBlank() {
        ObjectMapper objectMapper = new ObjectMapper();
        Fixture fixture = new Fixture(objectMapper);
        fixture.page.setDraftSchemaJson(null);
        fixture.page.setPublishedSchemaJson(ORIGINAL_DRAFT);
        FakeAiClient client = new FakeAiClient(true, List.of(EDITED_SCHEMA));
        IndependentSitePageSchemaGenerationService service = fixture.service(
                client,
                defaultLimiter()
        );

        IndependentSiteDtos.PageDetailResponse response = service.aiEdit(
                1L,
                11L,
                111L,
                "Make the hero title warmer"
        );

        assertTrue(client.messages().get(0).contains("Discover a calm stay"));
        assertEquals(ORIGINAL_DRAFT, fixture.page.getDraftBackupSchemaJson());
        assertTrue(response.hasAiBackup());
        assertEquals(
                "A slower welcome",
                response.draftSchema().path("sections").get(0).path("title").asText()
        );
    }

    private static IndependentSiteManagementRateLimiter defaultLimiter() {
        return new IndependentSiteManagementRateLimiter(
                Clock.fixed(Instant.parse("2026-07-24T00:00:00Z"), ZoneOffset.UTC)
        );
    }

    private static final class Fixture {
        private final IndependentSite site;
        private final IndependentSitePage page;
        private final IndependentSiteRepository siteRepository;
        private final IndependentSitePageRepository pageRepository;
        private final List<IndependentSitePage> saves = new ArrayList<>();
        private final ObjectMapper objectMapper;

        private Fixture(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            site = new IndependentSite();
            site.setId(11L);
            site.setStoreId(1L);
            site.setSlug("alpha");
            site.setName("Alpha Hotel");

            page = new IndependentSitePage();
            page.setId(111L);
            page.setStoreId(1L);
            page.setSite(site);
            page.setPath("/");
            page.setType(IndependentSitePageType.HOME);
            page.setTitle("Home");
            page.setDraftSchemaJson(ORIGINAL_DRAFT);
            page.setDraftVersion(3L);
            page.setEnabled(true);
            page.setSortOrder(0);

            siteRepository = repository(
                    IndependentSiteRepository.class,
                    (proxy, method, args) -> {
                        if ("findByStoreIdAndId".equals(method.getName())) {
                            return Objects.equals(args[0], 1L) && Objects.equals(args[1], 11L)
                                    ? Optional.of(site)
                                    : Optional.empty();
                        }
                        return objectMethodOrFail(proxy, method, args);
                    }
            );
            pageRepository = repository(
                    IndependentSitePageRepository.class,
                    (proxy, method, args) -> {
                        if ("findByStoreIdAndSiteIdAndId".equals(method.getName())
                                || "findByStoreIdAndSiteIdAndIdForUpdate".equals(method.getName())) {
                            return Objects.equals(args[0], 1L)
                                    && Objects.equals(args[1], 11L)
                                    && Objects.equals(args[2], 111L)
                                    ? Optional.of(page)
                                    : Optional.empty();
                        }
                        if ("save".equals(method.getName())) {
                            saves.add((IndependentSitePage) args[0]);
                            return args[0];
                        }
                        return objectMethodOrFail(proxy, method, args);
                    }
            );
        }

        private IndependentSitePageSchemaGenerationService service(
                FakeAiClient client,
                IndependentSiteManagementRateLimiter limiter
        ) {
            return new IndependentSitePageSchemaGenerationService(
                    client,
                    new IndependentSitePageSchemaParser(objectMapper),
                    new IndependentSitePageSchemaValidator(objectMapper),
                    siteRepository,
                    pageRepository,
                    null,
                    limiter,
                    objectMapper,
                    new IndependentSiteCanvasValidator(objectMapper),
                    null,
                    null,
                    null
            );
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T repository(
            Class<T> type,
            java.lang.reflect.InvocationHandler handler
    ) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private static Object objectMethodOrFail(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
            case "toString" -> proxy.getClass().getInterfaces()[0].getSimpleName() + "Proxy";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> throw new AssertionError("Unexpected repository method: " + method);
        };
    }

    private static final class FakeAiClient implements IndependentSitePageSchemaAiClient {

        private final boolean configured;
        private final Queue<String> outputs;
        private final List<String> messages = new ArrayList<>();

        private FakeAiClient(boolean configured, List<String> outputs) {
            this.configured = configured;
            this.outputs = new ConcurrentLinkedQueue<>(outputs);
        }

        @Override
        public boolean isConfigured() {
            return configured;
        }

        @Override
        public String complete(String userMessage) {
            messages.add(userMessage);
            String output = outputs.poll();
            if (output == null) {
                throw new AssertionError("Unexpected extra AI call");
            }
            return output;
        }

        boolean wasCalled() {
            return !messages.isEmpty();
        }

        int callCount() {
            return messages.size();
        }

        List<String> messages() {
            return messages;
        }
    }
}
