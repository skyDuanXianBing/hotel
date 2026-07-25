package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import server.demo.dto.FacilityDTO;
import server.demo.dto.IndependentSiteDtos;
import server.demo.entity.Channel;
import server.demo.entity.IndependentSite;
import server.demo.entity.IndependentSitePage;
import server.demo.entity.IndependentSitePublication;
import server.demo.entity.PricePlan;
import server.demo.entity.RoomType;
import server.demo.enums.IndependentSitePageFormat;
import server.demo.enums.IndependentSitePageType;
import server.demo.enums.IndependentSitePublicationType;
import server.demo.enums.PriceAdjustmentType;
import server.demo.repository.IndependentSitePageRepository;
import server.demo.repository.IndependentSitePublicationRepository;
import server.demo.repository.IndependentSiteRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypeRepository;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * format 分派：CANVAS 页保存/发布/公开读取走节点树校验器，BLOCKS 页维持旧校验器；
 * CANVAS 站点生成 CANVAS 版房型页骨架；URL 导入钉死 BLOCKS。
 */
class IndependentSiteCanvasFormatDispatchTest {

    private static final String BLOCKS_DRAFT = """
            {
              "schemaVersion":"independent_site_page_v1",
              "theme":{
                "primaryColor":"#2563EB","accentColor":"#F59E0B","surfaceColor":"#FFFFFF",
                "textColor":"#111827","typography":"MODERN","cornerStyle":"SOFT"
              },
              "sections":[{"type":"HERO","title":"Welcome","alignment":"CENTER"}]
            }
            """;

    private static final String CANVAS_DRAFT = """
            {
              "schemaVersion":"independent_site_canvas_v1",
              "root":{"id":"root","type":"element","tag":"main","class":"min-h-screen bg-white","children":[
                {"id":"t1","type":"text","text":"山景温泉民宿"},
                {"id":"s1","type":"slot","slot":"room-list"}
              ]}
            }
            """;

    private static final String CANVAS_DOUBLE_SLOT = """
            {
              "schemaVersion":"independent_site_canvas_v1",
              "root":{"id":"root","type":"element","tag":"main","children":[
                {"id":"s1","type":"slot","slot":"room-list"},
                {"id":"s2","type":"slot","slot":"room-list"}
              ]}
            }
            """;

    private static final String CANVAS_BAD_TAG = """
            {
              "schemaVersion":"independent_site_canvas_v1",
              "root":{"id":"root","type":"element","tag":"script"}
            }
            """;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void updatePage_shouldDispatchDraftValidationByFormat() throws Exception {
        // BLOCKS 页拒绝 CANVAS 草稿
        ManagementFixture blocksFixture = managementFixture(IndependentSitePageFormat.BLOCKS);
        assertThrows(
                IllegalArgumentException.class,
                () -> blocksFixture.service.updatePage(
                        1L,
                        11L,
                        111L,
                        new IndependentSiteDtos.PageUpdateRequest(
                                null, null, null, null, null,
                                objectMapper.readTree(CANVAS_DRAFT), null
                        )
                )
        );
        // BLOCKS 页接受 BLOCKS 草稿（旧管线回归）
        IndependentSiteDtos.PageDetailResponse blocksSaved = blocksFixture.service.updatePage(
                1L,
                11L,
                111L,
                new IndependentSiteDtos.PageUpdateRequest(
                        null, null, null, null, null,
                        objectMapper.readTree(BLOCKS_DRAFT), null
                )
        );
        assertEquals("BLOCKS", blocksSaved.format());
        assertEquals(
                IndependentSitePageSchemaValidator.SCHEMA_VERSION,
                blocksSaved.draftSchema().path("schemaVersion").asText()
        );

        // CANVAS 页拒绝 BLOCKS 草稿，接受 CANVAS 草稿
        ManagementFixture canvasFixture = managementFixture(IndependentSitePageFormat.CANVAS);
        assertThrows(
                IllegalArgumentException.class,
                () -> canvasFixture.service.updatePage(
                        1L,
                        11L,
                        111L,
                        new IndependentSiteDtos.PageUpdateRequest(
                                null, null, null, null, null,
                                objectMapper.readTree(BLOCKS_DRAFT), null
                        )
                )
        );
        IndependentSiteDtos.PageDetailResponse canvasSaved = canvasFixture.service.updatePage(
                1L,
                11L,
                111L,
                new IndependentSiteDtos.PageUpdateRequest(
                        null, null, null, null, null,
                        objectMapper.readTree(CANVAS_DRAFT), null
                )
        );
        assertEquals("CANVAS", canvasSaved.format());
        assertEquals(
                IndependentSiteCanvasValidator.SCHEMA_VERSION,
                canvasSaved.draftSchema().path("schemaVersion").asText()
        );
    }

    @Test
    void publishPage_shouldDispatchValidationByFormat() throws Exception {
        // CANVAS 页草稿违反 canvas 规则（双 room-list）→ 发布被拒
        ManagementFixture fixture = managementFixture(IndependentSitePageFormat.CANVAS);
        fixture.page.setDraftSchemaJson(CANVAS_DOUBLE_SLOT);
        assertThrows(
                IllegalArgumentException.class,
                () -> fixture.service.publishPage(
                        1L,
                        11L,
                        111L,
                        new IndependentSiteDtos.PublishPageDraftRequest(3L)
                )
        );

        // 合法 CANVAS 草稿可发布
        fixture.page.setDraftSchemaJson(CANVAS_DRAFT);
        IndependentSiteDtos.PageDetailResponse published = fixture.service.publishPage(
                1L,
                11L,
                111L,
                new IndependentSiteDtos.PublishPageDraftRequest(3L)
        );
        assertEquals("CANVAS", published.format());
        assertNotNull(published.publishedSchema());
        assertEquals(
                IndependentSiteCanvasValidator.SCHEMA_VERSION,
                published.publishedSchema().path("schemaVersion").asText()
        );
        assertNotNull(fixture.page.getPublishedAt());
    }

    @Test
    void generateRoomTypePages_shouldBuildCanvasSkeletonForCanvasSite() throws Exception {
        RoomTypeFixture fixture = new RoomTypeFixture(objectMapper);

        IndependentSiteDtos.GenerateRoomPagesResponse response =
                fixture.service.generateRoomTypePages(1L, 11L);

        assertEquals(1, response.generated());
        assertTrue(response.skipped().isEmpty());
        IndependentSitePage created = fixture.savedPages.stream()
                .filter(page -> page.getType() == IndependentSitePageType.ROOM_DETAIL)
                .findFirst()
                .orElseThrow();
        assertEquals(IndependentSitePageFormat.CANVAS, created.getFormat());
        assertEquals("/rooms/dlx-101", created.getPath());

        JsonNode schema = new IndependentSiteCanvasValidator(objectMapper)
                .validate(objectMapper.readTree(created.getDraftSchemaJson()));
        assertEquals(IndependentSiteCanvasValidator.SCHEMA_VERSION, schema.path("schemaVersion").asText());
        JsonNode rootChildren = schema.path("root").path("children");

        JsonNode hero = rootChildren.get(0);
        assertEquals("sec-hero", hero.path("id").asText());
        List<JsonNode> heroChildren = new ArrayList<>();
        hero.path("children").forEach(heroChildren::add);
        assertEquals("Deluxe Suite", heroChildren.get(0).path("children").get(0).path("text").asText());
        JsonNode heroImg = heroChildren.stream()
                .filter(node -> "img".equals(node.path("tag").asText()))
                .findFirst()
                .orElseThrow();
        assertEquals("/media/1/room/dlx-a.jpg", heroImg.path("attrs").path("src").asText());

        JsonNode info = rootChildren.get(1);
        assertEquals("sec-info", info.path("id").asText());
        List<String> facts = new ArrayList<>();
        info.path("children").get(1).path("children")
                .forEach(li -> facts.add(li.path("children").get(0).path("text").asText()));
        assertTrue(facts.stream().anyMatch(fact -> fact.contains("面积 25.5 ㎡")));
        assertTrue(facts.stream().anyMatch(fact -> fact.contains("2 位成人")));
        assertTrue(facts.stream().anyMatch(fact -> fact.equals("WiFi")));

        JsonNode cta = rootChildren.get(2);
        JsonNode button = cta.path("children").get(0);
        assertEquals("button", button.path("tag").asText());
        assertEquals("scroll-to-booking", button.path("action").asText());
        assertEquals("立即预订", button.path("children").get(0).path("text").asText());
    }

    @Test
    void importPageFromUrl_shouldRejectCanvasPageOverwriteBeforeFetch() {
        ManagementFixture fixture = managementFixture(IndependentSitePageFormat.CANVAS);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fixture.service.importPageFromUrl(
                        1L,
                        11L,
                        new IndependentSiteDtos.ImportPageFromUrlRequest(
                                "https://example.com/page",
                                "OVERWRITE_DRAFT",
                                111L,
                                null,
                                null
                        )
                )
        );

        assertEquals("URL 导入暂不支持 CANVAS 页面", exception.getMessage());
    }

    @Test
    void getPublicPage_shouldDispatchValidationByFormatAndFailClosed() throws Exception {
        QuoteFixture fixture = new QuoteFixture(objectMapper, CANVAS_DRAFT);

        IndependentSiteDtos.PublicPageResponse response =
                fixture.service.getPublicPage("alpha", "/about");

        assertEquals("CANVAS", response.format());
        assertEquals(
                IndependentSiteCanvasValidator.SCHEMA_VERSION,
                response.schema().path("schemaVersion").asText()
        );

        // 已发布内容被篡改（非法 tag）→ fail-closed 为站点不可用
        QuoteFixture tampered = new QuoteFixture(objectMapper, CANVAS_BAD_TAG);
        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> tampered.service.getPublicPage("alpha", "/about")
        );
        assertEquals(404, exception.getStatus().value());
        assertEquals("SITE_UNAVAILABLE", exception.getCode());
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private ManagementFixture managementFixture(IndependentSitePageFormat format) {
        return new ManagementFixture(objectMapper, format);
    }

    private static final class ManagementFixture {
        private final IndependentSitePage page;
        private final IndependentSiteManagementService service;

        private ManagementFixture(ObjectMapper objectMapper, IndependentSitePageFormat format) {
            IndependentSite site = new IndependentSite();
            site.setId(11L);
            site.setStoreId(1L);
            site.setSlug("alpha");
            site.setName("Alpha Hotel");

            page = new IndependentSitePage();
            page.setId(111L);
            page.setStoreId(1L);
            page.setSite(site);
            page.setPath("/about");
            page.setType(IndependentSitePageType.CUSTOM);
            page.setFormat(format);
            page.setTitle("About");
            page.setDraftSchemaJson(
                    format == IndependentSitePageFormat.CANVAS ? CANVAS_DRAFT : BLOCKS_DRAFT
            );
            page.setDraftVersion(3L);
            page.setEnabled(true);
            page.setSortOrder(0);

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
                        if ("findByStoreIdAndSiteIdAndId".equals(method.getName())
                                || "findByStoreIdAndSiteIdAndIdForUpdate".equals(method.getName())) {
                            return Objects.equals(args[2], 111L)
                                    ? Optional.of(page)
                                    : Optional.empty();
                        }
                        if ("findByStoreIdAndSiteIdAndPath".equals(method.getName())) {
                            return Optional.empty();
                        }
                        if ("save".equals(method.getName())) {
                            return args[0];
                        }
                        return objectMethodOrFail(proxy, method, args);
                    }
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
                    new IndependentSitePageSchemaValidator(objectMapper),
                    objectMapper,
                    null,
                    null,
                    null,
                    unconfiguredStripeSettings(),
                    new IndependentSiteCanvasValidator(objectMapper)
            );
        }
    }

    private static final class RoomTypeFixture {
        private final IndependentSiteManagementService service;
        private final List<IndependentSitePage> savedPages = new ArrayList<>();

        private RoomTypeFixture(ObjectMapper objectMapper) {
            IndependentSite site = new IndependentSite();
            site.setId(11L);
            site.setStoreId(1L);
            site.setSlug("alpha");
            site.setName("Alpha Hotel");

            IndependentSitePage home = new IndependentSitePage();
            home.setId(110L);
            home.setStoreId(1L);
            home.setSite(site);
            home.setPath("/");
            home.setType(IndependentSitePageType.HOME);
            home.setFormat(IndependentSitePageFormat.CANVAS);
            home.setTitle("Home");
            home.setEnabled(true);
            home.setSortOrder(0);

            RoomType roomType = new RoomType();
            roomType.setId(101L);
            roomType.setStoreId(1L);
            roomType.setName("Deluxe Suite");
            roomType.setCode("DLX-101");
            roomType.setDescription("A bright suite with garden view.");
            roomType.setMaxGuests(2);
            roomType.setMaxChildOccupancy(1);
            roomType.setSizeMeasurement(new BigDecimal("25.50"));
            roomType.setSizeMeasurementUnit("㎡");
            roomType.setDesktopPhotoUrls(List.of("/media/1/room/dlx-a.jpg"));
            roomType.setFacilities(List.of(new FacilityDTO("basic", "WiFi")));

            IndependentSitePublication publication = new IndependentSitePublication();
            publication.setSite(site);
            publication.setTargetType(IndependentSitePublicationType.ROOM_TYPE);
            publication.setTargetId(101L);
            publication.setEnabled(true);

            IndependentSiteRepository siteRepository = repository(
                    IndependentSiteRepository.class,
                    (proxy, method, args) -> {
                        if ("findByStoreIdAndId".equals(method.getName())) {
                            return Optional.of(site);
                        }
                        return objectMethodOrFail(proxy, method, args);
                    }
            );
            IndependentSitePageRepository pageRepository = repository(
                    IndependentSitePageRepository.class,
                    (proxy, method, args) -> {
                        if ("findByStoreIdAndSiteIdAndType".equals(method.getName())) {
                            return args[2] == IndependentSitePageType.HOME ? List.of(home) : List.of();
                        }
                        if ("findByStoreIdAndSiteIdOrderBySortOrderAscIdAsc".equals(method.getName())) {
                            List<IndependentSitePage> all = new ArrayList<>(List.of(home));
                            all.addAll(savedPages);
                            return all;
                        }
                        if ("save".equals(method.getName())) {
                            IndependentSitePage saved = (IndependentSitePage) args[0];
                            if (saved.getId() == null) {
                                saved.setId(500L + savedPages.size());
                            }
                            savedPages.removeIf(existing -> Objects.equals(existing.getId(), saved.getId()));
                            savedPages.add(saved);
                            return saved;
                        }
                        return objectMethodOrFail(proxy, method, args);
                    }
            );
            IndependentSitePublicationRepository publicationRepository = repository(
                    IndependentSitePublicationRepository.class,
                    (proxy, method, args) -> {
                        if ("findByStoreIdAndSiteIdAndEnabledTrueOrderByDisplayOrderAscIdAsc"
                                .equals(method.getName())) {
                            return List.of(publication);
                        }
                        return objectMethodOrFail(proxy, method, args);
                    }
            );
            RoomTypeRepository roomTypeRepository = repository(
                    RoomTypeRepository.class,
                    (proxy, method, args) -> {
                        if ("findByStoreIdAndIdIn".equals(method.getName())) {
                            return List.of(roomType);
                        }
                        return objectMethodOrFail(proxy, method, args);
                    }
            );
            RoomRepository roomRepository = repository(
                    RoomRepository.class,
                    (proxy, method, args) -> {
                        if ("findByStoreIdAndIdIn".equals(method.getName())) {
                            return List.of();
                        }
                        return objectMethodOrFail(proxy, method, args);
                    }
            );
            service = new IndependentSiteManagementService(
                    siteRepository,
                    publicationRepository,
                    pageRepository,
                    null,
                    null,
                    null,
                    roomTypeRepository,
                    roomRepository,
                    null,
                    null,
                    new IndependentSitePageSchemaValidator(objectMapper),
                    objectMapper,
                    null,
                    null,
                    null,
                    unconfiguredStripeSettings(),
                    new IndependentSiteCanvasValidator(objectMapper)
            );
        }
    }

    private static final class QuoteFixture {
        private final IndependentSiteQuoteService service;

        private QuoteFixture(ObjectMapper objectMapper, String publishedSchemaJson) {
            PricePlan pricePlan = new PricePlan();
            pricePlan.setId(501L);
            pricePlan.setStoreId(1L);

            Channel channel = new Channel();
            channel.setId(301L);
            channel.setStoreId(1L);
            channel.setCode(IndependentSiteManagementService.BOOKING_ENGINE_CHANNEL_CODE);
            channel.setEnabled(true);
            channel.setIsActive(true);
            channel.setPriceAdjustmentType(PriceAdjustmentType.PERCENTAGE);
            channel.setPriceAdjustmentValue(BigDecimal.ZERO.setScale(2));
            channel.setDefaultPricePlan(pricePlan);

            IndependentSite site = new IndependentSite();
            site.setId(11L);
            site.setStoreId(1L);
            site.setSlug("alpha");
            site.setName("Alpha Hotel");
            site.setEnabled(true);
            site.setChannel(channel);
            site.setPublishedAt(LocalDateTime.of(2026, 7, 20, 12, 0));

            IndependentSitePage home = new IndependentSitePage();
            home.setId(110L);
            home.setStoreId(1L);
            home.setSite(site);
            home.setPath("/");
            home.setType(IndependentSitePageType.HOME);
            home.setFormat(IndependentSitePageFormat.CANVAS);
            home.setPublishedAt(LocalDateTime.of(2026, 7, 20, 12, 0));
            home.setEnabled(true);

            IndependentSitePage page = new IndependentSitePage();
            page.setId(111L);
            page.setStoreId(1L);
            page.setSite(site);
            page.setPath("/about");
            page.setType(IndependentSitePageType.CUSTOM);
            page.setFormat(IndependentSitePageFormat.CANVAS);
            page.setTitle("About");
            page.setPublishedSchemaJson(publishedSchemaJson);
            page.setPublishedAt(LocalDateTime.of(2026, 7, 20, 12, 0));
            page.setEnabled(true);

            IndependentSiteRepository siteRepository = repository(
                    IndependentSiteRepository.class,
                    (proxy, method, args) -> {
                        if ("findEnabledBySlugWithChannel".equals(method.getName())) {
                            return "alpha".equals(args[0]) ? Optional.of(site) : Optional.empty();
                        }
                        return objectMethodOrFail(proxy, method, args);
                    }
            );
            IndependentSitePageRepository pageRepository = repository(
                    IndependentSitePageRepository.class,
                    (proxy, method, args) -> {
                        if ("findBySiteIdAndTypeAndPublishedAtIsNotNullAndEnabledTrue"
                                .equals(method.getName())) {
                            return Optional.of(home);
                        }
                        if ("findByStoreIdAndSiteIdAndPath".equals(method.getName())) {
                            return "/about".equals(args[2]) ? Optional.of(page) : Optional.empty();
                        }
                        return objectMethodOrFail(proxy, method, args);
                    }
            );
            service = new IndependentSiteQuoteService(
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
                    new IndependentSitePageSchemaValidator(objectMapper),
                    objectMapper,
                    Clock.systemUTC(),
                    new IndependentSiteCanvasValidator(objectMapper)
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

    /** 门店 Stripe 密钥未配置的替身：STRIPE 门槛恒不满足。 */
    private static IndependentSiteStripeSettingsService unconfiguredStripeSettings() {
        return new IndependentSiteStripeSettingsService(null, "") {
            @Override
            public boolean isFullyConfigured(Long storeId) {
                return false;
            }
        };
    }

    private static Object objectMethodOrFail(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
            case "toString" -> proxy.getClass().getInterfaces()[0].getSimpleName() + "Proxy";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> throw new AssertionError("Unexpected repository method: " + method);
        };
    }
}
