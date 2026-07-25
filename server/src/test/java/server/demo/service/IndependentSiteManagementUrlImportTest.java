package server.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import server.demo.dto.IndependentSiteDtos;
import server.demo.entity.IndependentSite;
import server.demo.entity.IndependentSitePage;
import server.demo.enums.IndependentSitePageType;
import server.demo.repository.IndependentSitePageRepository;
import server.demo.repository.IndependentSiteRepository;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndependentSiteManagementUrlImportTest {

    private static final String OLD_DRAFT = """
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
                {"type":"HERO","title":"Old draft hero","body":"Old draft body text here","alignment":"CENTER"}
              ]
            }
            """;

    private static final String PUBLISHED_SCHEMA = """
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
                {"type":"HERO","title":"Published hero","alignment":"LEFT"}
              ]
            }
            """;

    private static final String INVALID_SCHEMA = "{\"schemaVersion\":\"wrong\"}";

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void importNewPage_shouldCreateCustomPageWithGeneratedDraft() throws Exception {
        Fixture fixture = new Fixture(10);
        startHotelPageServer(fixture.hits);
        String url = baseUrl() + "/hotel";
        fixture.aiClient.outputs.add(validImportedSchema(url));

        IndependentSiteDtos.PageDetailResponse response = fixture.service.importPageFromUrl(
                1L,
                11L,
                new IndependentSiteDtos.ImportPageFromUrlRequest(
                        url,
                        "NEW_PAGE",
                        null,
                        "/seaside",
                        "Seaside Import"
                )
        );

        assertEquals(1, fixture.saves.size());
        IndependentSitePage saved = fixture.saves.get(0);
        assertEquals(IndependentSitePageType.CUSTOM, saved.getType());
        assertEquals("/seaside", saved.getPath());
        assertEquals("Seaside Import", saved.getTitle());
        assertTrue(saved.getEnabled());
        assertEquals(1L, saved.getDraftVersion());
        assertNull(saved.getPublishedSchemaJson());
        assertNull(saved.getDraftBackupSchemaJson());
        assertNotNull(saved.getDraftUpdatedAt());

        assertFalse(response.hasAiBackup());
        assertEquals("Seaside Resort & Spa", response.draftSchema().path("sections").path(0).path("title").asText());
        assertEquals("GALLERY", response.draftSchema().path("sections").path(2).path("type").asText());
        assertNull(response.publishedSchema());

        String prompt = fixture.aiClient.messages.get(0);
        assertTrue(prompt.contains("EXTRACTED_CONTENT"));
        assertTrue(prompt.contains("Seaside Resort &amp; Spa") || prompt.contains("Seaside Resort & Spa"));
        assertTrue(prompt.contains("/images/hero-view.jpg"));
        assertTrue(prompt.contains("GALLERY"));
        assertTrue(prompt.contains("BOOKING"));
        assertTrue(prompt.contains("imageUrl"));
        assertTrue(prompt.contains("Page path: /seaside"));
        assertEquals(1, fixture.hits.get());
    }

    @Test
    void importOverwriteDraft_shouldBackupOldDraftAndReplaceDraft() throws Exception {
        Fixture fixture = new Fixture(10);
        startHotelPageServer(fixture.hits);
        String url = baseUrl() + "/hotel";
        fixture.aiClient.outputs.add(validImportedSchema(url));

        IndependentSiteDtos.PageDetailResponse response = fixture.service.importPageFromUrl(
                1L,
                11L,
                new IndependentSiteDtos.ImportPageFromUrlRequest(url, "OVERWRITE_DRAFT", 111L, null, null)
        );

        assertEquals(1, fixture.saves.size());
        IndependentSitePage page = fixture.existingPage;
        assertEquals(OLD_DRAFT, page.getDraftBackupSchemaJson());
        assertEquals(PUBLISHED_SCHEMA, page.getPublishedSchemaJson());
        assertEquals(5L, page.getDraftVersion());
        assertTrue(page.getDraftSchemaJson().contains("Seaside Resort & Spa"));
        assertTrue(response.hasAiBackup());
        assertEquals(5L, response.draftVersion());
        assertEquals("Published hero", response.publishedSchema().path("sections").path(0).path("title").asText());
    }

    @Test
    void importNewPage_shouldNotCreatePageWhenAiFails() throws Exception {
        Fixture fixture = new Fixture(10);
        startHotelPageServer(fixture.hits);
        String url = baseUrl() + "/hotel";
        fixture.aiClient.outputs.add(INVALID_SCHEMA);
        fixture.aiClient.outputs.add(INVALID_SCHEMA);
        fixture.aiClient.outputs.add(INVALID_SCHEMA);

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> fixture.service.importPageFromUrl(
                        1L,
                        11L,
                        new IndependentSiteDtos.ImportPageFromUrlRequest(
                                url,
                                "NEW_PAGE",
                                null,
                                "/seaside",
                                "Seaside Import"
                        )
                )
        );

        assertEquals("INVALID_AI_SCHEMA", exception.getCode());
        assertEquals(3, fixture.aiClient.callCount());
        assertTrue(fixture.saves.isEmpty());
    }

    @Test
    void importOverwriteDraft_shouldNotTouchDraftOrBackupWhenAiFails() throws Exception {
        Fixture fixture = new Fixture(10);
        startHotelPageServer(fixture.hits);
        String url = baseUrl() + "/hotel";
        fixture.aiClient.outputs.add(INVALID_SCHEMA);
        fixture.aiClient.outputs.add(INVALID_SCHEMA);
        fixture.aiClient.outputs.add(INVALID_SCHEMA);

        assertThrows(
                IndependentSiteServiceException.class,
                () -> fixture.service.importPageFromUrl(
                        1L,
                        11L,
                        new IndependentSiteDtos.ImportPageFromUrlRequest(url, "OVERWRITE_DRAFT", 111L, null, null)
                )
        );

        assertEquals(OLD_DRAFT, fixture.existingPage.getDraftSchemaJson());
        assertNull(fixture.existingPage.getDraftBackupSchemaJson());
        assertEquals(4L, fixture.existingPage.getDraftVersion());
        assertTrue(fixture.saves.isEmpty());
    }

    @Test
    void importNewPage_shouldRejectDuplicatePathBeforeFetching() throws Exception {
        Fixture fixture = new Fixture(10);
        fixture.pathConflict = true;
        startHotelPageServer(fixture.hits);

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> fixture.service.importPageFromUrl(
                        1L,
                        11L,
                        new IndependentSiteDtos.ImportPageFromUrlRequest(
                                "http://example.com/hotel",
                                "NEW_PAGE",
                                null,
                                "/seaside",
                                "Seaside Import"
                        )
                )
        );

        assertEquals(409, exception.getStatus().value());
        assertEquals("PAGE_PATH_ALREADY_EXISTS", exception.getCode());
        assertEquals(0, fixture.hits.get());
        assertFalse(fixture.aiClient.wasCalled());
        assertTrue(fixture.saves.isEmpty());
    }

    @Test
    void importOverwriteDraft_shouldRequirePageId() throws Exception {
        Fixture fixture = new Fixture(10);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fixture.service.importPageFromUrl(
                        1L,
                        11L,
                        new IndependentSiteDtos.ImportPageFromUrlRequest(
                                "http://example.com/hotel",
                                "OVERWRITE_DRAFT",
                                null,
                                null,
                                null
                        )
                )
        );

        assertEquals("覆盖草稿模式必须提供 pageId", exception.getMessage());
        assertFalse(fixture.aiClient.wasCalled());
        assertTrue(fixture.saves.isEmpty());
    }

    @Test
    void importPageFromUrl_shouldRejectUnknownSiteAndPage() throws Exception {
        Fixture fixture = new Fixture(10);
        IndependentSiteDtos.ImportPageFromUrlRequest request =
                new IndependentSiteDtos.ImportPageFromUrlRequest(
                        "http://example.com/hotel",
                        "NEW_PAGE",
                        null,
                        "/seaside",
                        "Seaside Import"
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> fixture.service.importPageFromUrl(2L, 11L, request)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> fixture.service.importPageFromUrl(1L, 22L, request)
        );
        IllegalArgumentException unknownPage = assertThrows(
                IllegalArgumentException.class,
                () -> fixture.service.importPageFromUrl(
                        1L,
                        11L,
                        new IndependentSiteDtos.ImportPageFromUrlRequest(
                                "http://example.com/hotel",
                                "OVERWRITE_DRAFT",
                                999L,
                                null,
                                null
                        )
                )
        );
        assertEquals("页面不存在", unknownPage.getMessage());
        assertFalse(fixture.aiClient.wasCalled());
    }

    @Test
    void importPageFromUrl_shouldRejectInternalUrlsBeforeFetching() throws Exception {
        Fixture fixture = new Fixture(10, InetAddress::getAllByName);

        assertNotAllowed(fixture, "http://127.0.0.1:1/");
        assertNotAllowed(fixture, "http://169.254.169.254/");
        assertNotAllowed(fixture, "http://localhost:1/");
        assertNotAllowed(fixture, "http://10.0.0.8/");
        assertNotAllowed(fixture, "http://192.168.1.10/");
        assertFalse(fixture.aiClient.wasCalled());
        assertTrue(fixture.saves.isEmpty());
    }

    @Test
    void importPageFromUrl_shouldRateLimitImportsPerStore() throws Exception {
        Fixture fixture = new Fixture(1);
        startHotelPageServer(fixture.hits);
        String url = baseUrl() + "/hotel";
        fixture.aiClient.outputs.add(validImportedSchema(url));

        fixture.service.importPageFromUrl(
                1L,
                11L,
                new IndependentSiteDtos.ImportPageFromUrlRequest(url, "NEW_PAGE", null, "/seaside", "Seaside Import")
        );

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> fixture.service.importPageFromUrl(
                        1L,
                        11L,
                        new IndependentSiteDtos.ImportPageFromUrlRequest(url, "NEW_PAGE", null, "/garden", "Garden Import")
                )
        );

        assertEquals(429, exception.getStatus().value());
        assertEquals("URL_IMPORT_RATE_LIMITED", exception.getCode());
        assertEquals(1, fixture.saves.size());
        assertEquals(1, fixture.aiClient.callCount());
    }

    private static void assertNotAllowed(Fixture fixture, String url) {
        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> fixture.service.importPageFromUrl(
                        1L,
                        11L,
                        new IndependentSiteDtos.ImportPageFromUrlRequest(
                                url,
                                "NEW_PAGE",
                                null,
                                "/seaside",
                                "Seaside Import"
                        )
                )
        );
        assertEquals(400, exception.getStatus().value());
        assertEquals("URL_NOT_ALLOWED", exception.getCode());
    }

    private void startHotelPageServer(AtomicInteger hits) throws IOException {
        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                <title>Seaside Resort &amp; Spa</title>
                <meta name="description" content="A quiet resort by the sea.">
                </head>
                <body>
                <h1>Seaside Resort &amp; Spa</h1>
                <p>Nestled between pine forests and a private cove, our resort offers quiet rooms and slow mornings for every guest.</p>
                <img src="/images/hero-view.jpg" width="1200" height="800">
                <ul>
                  <li>设施与服务</li>
                  <li>免费无线网络覆盖全馆</li>
                </ul>
                </body>
                </html>
                """;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/hotel", exchange -> {
            hits.incrementAndGet();
            byte[] body = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static String validImportedSchema(String pageUrl) {
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
                      "title":"Seaside Resort & Spa",
                      "imageUrl":"%s/images/hero-view.jpg",
                      "alignment":"CENTER"
                    },
                    {
                      "type":"ABOUT",
                      "title":"About us",
                      "body":"Nestled between pine forests and a private cove, our resort offers quiet rooms and slow mornings."
                    },
                    {
                      "type":"GALLERY",
                      "title":"Views",
                      "images":[{"url":"%s/images/hero-view.jpg","alt":"Ocean view"}]
                    },
                    {
                      "type":"AMENITIES",
                      "title":"设施",
                      "items":["免费无线网络覆盖全馆"]
                    },
                    {
                      "type":"BOOKING",
                      "title":"立即预订"
                    }
                  ]
                }
                """.formatted(pageUrl, pageUrl);
    }

    private static final class Fixture {
        private final IndependentSiteManagementService service;
        private final FakeAiClient aiClient = new FakeAiClient();
        private final List<IndependentSitePage> saves = new ArrayList<>();
        private final AtomicInteger hits = new AtomicInteger();
        private final IndependentSitePage existingPage;
        private boolean pathConflict;

        private Fixture(int urlImportLimit) throws Exception {
            this(urlImportLimit, null);
        }

        private Fixture(
                int urlImportLimit,
                IndependentSiteUrlFetchService.AddressResolver addressResolver
        ) throws Exception {
            ObjectMapper objectMapper = new ObjectMapper();
            IndependentSitePageSchemaValidator validator =
                    new IndependentSitePageSchemaValidator(objectMapper);

            IndependentSite site = new IndependentSite();
            site.setId(11L);
            site.setStoreId(1L);
            site.setSlug("alpha");
            site.setName("Alpha Hotel");

            existingPage = new IndependentSitePage();
            existingPage.setId(111L);
            existingPage.setStoreId(1L);
            existingPage.setSite(site);
            existingPage.setPath("/about");
            existingPage.setType(IndependentSitePageType.CUSTOM);
            existingPage.setTitle("About");
            existingPage.setDraftSchemaJson(OLD_DRAFT);
            existingPage.setPublishedSchemaJson(PUBLISHED_SCHEMA);
            existingPage.setDraftVersion(4L);
            existingPage.setEnabled(true);
            existingPage.setSortOrder(0);

            IndependentSiteRepository siteRepository = repository(
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
            IndependentSitePageRepository pageRepository = repository(
                    IndependentSitePageRepository.class,
                    (proxy, method, args) -> {
                        if ("findByStoreIdAndSiteIdAndPath".equals(method.getName())) {
                            return pathConflict && Objects.equals(args[0], 1L)
                                    && Objects.equals(args[1], 11L)
                                    && Objects.equals(args[2], "/seaside")
                                    ? Optional.of(existingPage)
                                    : Optional.empty();
                        }
                        if ("findByStoreIdAndSiteIdAndIdForUpdate".equals(method.getName())) {
                            return Objects.equals(args[0], 1L)
                                    && Objects.equals(args[1], 11L)
                                    && Objects.equals(args[2], 111L)
                                    ? Optional.of(existingPage)
                                    : Optional.empty();
                        }
                        if ("save".equals(method.getName())) {
                            saves.add((IndependentSitePage) args[0]);
                            return args[0];
                        }
                        return objectMethodOrFail(proxy, method, args);
                    }
            );

            IndependentSiteManagementRateLimiter rateLimiter = new IndependentSiteManagementRateLimiter(
                    Clock.fixed(Instant.parse("2026-07-24T00:00:00Z"), ZoneOffset.UTC),
                    30,
                    Duration.ofHours(1),
                    urlImportLimit,
                    Duration.ofHours(1)
            );

            IndependentSiteUrlFetchService.AddressResolver resolver = addressResolver != null
                    ? addressResolver
                    : ignored -> new InetAddress[]{InetAddress.getByName("93.184.216.34")};
            IndependentSiteUrlFetchService fetchService = new IndependentSiteUrlFetchService(
                    HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build(),
                    resolver
            );

            IndependentSitePageSchemaGenerationService generationService =
                    new IndependentSitePageSchemaGenerationService(
                            aiClient,
                            new IndependentSitePageSchemaParser(objectMapper),
                            validator,
                            null,
                            null,
                            null,
                            null,
                            objectMapper,
                            new IndependentSiteCanvasValidator(objectMapper),
                            null,
                            null,
                            null
                    );

            service = new IndependentSiteManagementService(
                    siteRepository,
                    null,
                    pageRepository,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    validator,
                    objectMapper,
                    rateLimiter,
                    fetchService,
                    generationService,
                    unconfiguredStripeSettings(),
                    new IndependentSiteCanvasValidator(objectMapper)
            );
        }
    }

    /** 门店 Stripe 密钥未配置的替身：STRIPE 门槛恒不满足。 */
    private static IndependentSiteStripeSettingsService unconfiguredStripeSettings() {
        return new IndependentSiteStripeSettingsService(null, "") {
            @Override
            public boolean isFullyConfigured(Long storeId) {
                return false;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static <T> T repository(Class<T> type, java.lang.reflect.InvocationHandler handler) {
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

        private final Queue<String> outputs = new ConcurrentLinkedQueue<>();
        private final List<String> messages = new ArrayList<>();

        @Override
        public boolean isConfigured() {
            return true;
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
    }
}
