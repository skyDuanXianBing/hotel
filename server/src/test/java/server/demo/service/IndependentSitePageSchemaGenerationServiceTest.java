package server.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import server.demo.dto.IndependentSiteDtos;
import server.demo.entity.IndependentSite;
import server.demo.entity.Store;
import server.demo.repository.IndependentSiteRepository;
import server.demo.repository.StoreRepository;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndependentSitePageSchemaGenerationServiceTest {

    private static final String VALID_SCHEMA = """
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
                {"type":"HERO","title":"Welcome","body":"Discover a calm stay","alignment":"CENTER"}
              ]
            }
            """;

    @Test
    void generate_shouldFailClosedWhenAiClientIsNotConfigured() {
        FakeAiClient client = new FakeAiClient(false, List.of(VALID_SCHEMA));
        IndependentSitePageSchemaGenerationService service = service(client);

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> service.generate(1L, 11L, request())
        );

        assertEquals("OPENAI_CHANNEL_UNAVAILABLE", exception.getCode());
        assertFalse(client.wasCalled());
    }

    @Test
    void generate_shouldReturnPublishableDraftWhenFirstOutputIsValid() {
        FakeAiClient client = new FakeAiClient(true, List.of(VALID_SCHEMA));
        IndependentSitePageSchemaGenerationService service = service(client);

        IndependentSiteDtos.PageDraftResponse response = service.generate(1L, 11L, request());

        assertTrue(response.publishable());
        assertEquals("OPENAI_CONFIRMED", response.providerStatus());
        assertEquals(IndependentSitePageSchemaValidator.SCHEMA_VERSION, response.schemaVersion());
        assertEquals(1, client.callCount());
    }

    @Test
    void generate_shouldRepairAfterValidationFailureAndReturnDraft() {
        FakeAiClient client = new FakeAiClient(
                true,
                List.of("{\"schemaVersion\":\"wrong\"}", VALID_SCHEMA)
        );
        IndependentSitePageSchemaGenerationService service = service(client);

        IndependentSiteDtos.PageDraftResponse response = service.generate(1L, 11L, request());

        assertTrue(response.publishable());
        assertEquals(2, client.callCount());
        String repairMessage = client.messages().get(1);
        assertTrue(repairMessage.contains("Validation error"));
        assertTrue(repairMessage.contains("PREVIOUS_ANSWER"));
        assertTrue(repairMessage.contains("wrong"));
    }

    @Test
    void generate_shouldRejectAfterExhaustingAttemptsWithoutPersisting() {
        FakeAiClient client = new FakeAiClient(
                true,
                List.of(
                        "{\"schemaVersion\":\"wrong\"}",
                        "{\"schemaVersion\":\"wrong\"}",
                        "{\"schemaVersion\":\"wrong\"}",
                        VALID_SCHEMA
                )
        );
        IndependentSitePageSchemaGenerationService service = service(client);

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> service.generate(1L, 11L, request())
        );

        assertEquals("INVALID_AI_SCHEMA", exception.getCode());
        assertEquals(3, client.callCount());
        assertTrue(exception.getMessage().contains("模型输出未通过独立站页面白名单校验"));
        assertTrue(exception.getMessage().contains("仅支持"));
    }

    @Test
    void generateForPage_shouldIncludePageContextInPrompt() {
        FakeAiClient client = new FakeAiClient(true, List.of(VALID_SCHEMA));
        IndependentSitePageSchemaGenerationService service = service(client);

        IndependentSiteDtos.PageDraftResponse response = service.generateForPage(1L, 11L, 111L, request());

        assertTrue(response.publishable());
        String prompt = client.messages().get(0);
        assertTrue(prompt.contains("Page type: HOME"));
        assertTrue(prompt.contains("Page title: Home"));
        assertTrue(prompt.contains("Page path: /"));
    }

    @Test
    void generateForPage_shouldRejectUnknownPageBeforeAiCall() {
        FakeAiClient client = new FakeAiClient(true, List.of(VALID_SCHEMA));
        ObjectMapper objectMapper = new ObjectMapper();
        IndependentSite site = new IndependentSite();
        site.setId(11L);
        site.setStoreId(1L);
        IndependentSiteRepository siteRepository = repository(
                IndependentSiteRepository.class,
                (proxy, method, args) -> {
                    if ("findByStoreIdAndId".equals(method.getName())) {
                        return Optional.of(site);
                    }
                    return objectMethodOrFail(proxy, method, args);
                }
        );
        server.demo.repository.IndependentSitePageRepository pageRepository = repository(
                server.demo.repository.IndependentSitePageRepository.class,
                (proxy, method, args) -> {
                    if ("findByStoreIdAndSiteIdAndId".equals(method.getName())) {
                        return Optional.empty();
                    }
                    return objectMethodOrFail(proxy, method, args);
                }
        );
        IndependentSitePageSchemaGenerationService service = new IndependentSitePageSchemaGenerationService(
                client,
                new IndependentSitePageSchemaParser(objectMapper),
                new IndependentSitePageSchemaValidator(objectMapper),
                siteRepository,
                pageRepository,
                null,
                null,
                null,
                new IndependentSiteCanvasValidator(objectMapper),
                null,
                null,
                null
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.generateForPage(1L, 11L, 999L, request())
        );

        assertEquals("页面不存在", exception.getMessage());
        assertFalse(client.wasCalled());
    }

    private static IndependentSitePageSchemaGenerationService service(FakeAiClient client) {
        ObjectMapper objectMapper = new ObjectMapper();
        IndependentSite site = new IndependentSite();
        site.setId(11L);
        site.setStoreId(1L);

        server.demo.entity.IndependentSitePage home = new server.demo.entity.IndependentSitePage();
        home.setId(111L);
        home.setStoreId(1L);
        home.setSite(site);
        home.setPath("/");
        home.setType(server.demo.enums.IndependentSitePageType.HOME);
        home.setTitle("Home");

        Store store = new Store();
        store.setId(1L);
        store.setName("Alpha Hotel");

        IndependentSiteRepository siteRepository = repository(
                IndependentSiteRepository.class,
                (proxy, method, args) -> {
                    if ("findByStoreIdAndId".equals(method.getName())) {
                        return Optional.of(site);
                    }
                    if ("save".equals(method.getName())) {
                        throw new AssertionError("AI generation must not persist a draft");
                    }
                    return objectMethodOrFail(proxy, method, args);
                }
        );
        server.demo.repository.IndependentSitePageRepository pageRepository = repository(
                server.demo.repository.IndependentSitePageRepository.class,
                (proxy, method, args) -> {
                    if ("findByStoreIdAndSiteIdAndType".equals(method.getName())) {
                        return List.of(home);
                    }
                    if ("findByStoreIdAndSiteIdAndId".equals(method.getName())) {
                        return Optional.of(home);
                    }
                    return objectMethodOrFail(proxy, method, args);
                }
        );
        StoreRepository storeRepository = repository(
                StoreRepository.class,
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return Optional.of(store);
                    }
                    return objectMethodOrFail(proxy, method, args);
                }
        );
        return new IndependentSitePageSchemaGenerationService(
                client,
                new IndependentSitePageSchemaParser(objectMapper),
                new IndependentSitePageSchemaValidator(objectMapper),
                siteRepository,
                pageRepository,
                storeRepository,
                null,
                null,
                new IndependentSiteCanvasValidator(objectMapper),
                null,
                null,
                null
        );
    }

    private static IndependentSiteDtos.PageDraftRequest request() {
        return new IndependentSiteDtos.PageDraftRequest(
                "Create a calm hotel landing page",
                "en",
                "MODERN"
        );
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
