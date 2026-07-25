package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import server.demo.dto.IndependentSiteDtos;
import server.demo.entity.IndependentSite;
import server.demo.entity.IndependentSitePage;
import server.demo.entity.IndependentSitePublication;
import server.demo.entity.RoomType;
import server.demo.entity.Store;
import server.demo.enums.IndependentSitePageFormat;
import server.demo.enums.IndependentSitePageType;
import server.demo.enums.IndependentSitePublicationType;
import server.demo.repository.IndependentSitePageRepository;
import server.demo.repository.IndependentSitePublicationRepository;
import server.demo.repository.IndependentSiteRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.repository.StoreRepository;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CANVAS 生成管线：generate / ai-edit 走节点树 prompt + 200_000 parser 上限 + canvas 白名单校验，
 * 修复循环与 BLOCKS 同构（stub AI 客户端先非法后合法）。
 */
class IndependentSiteCanvasGenerationTest {

    private static final String VALID_CANVAS = """
            {
              "schemaVersion":"independent_site_canvas_v1",
              "root":{"id":"root","type":"element","tag":"main","class":"min-h-screen bg-white text-slate-800","children":[
                {"id":"hero","type":"element","tag":"section","class":"flex min-h-[60vh] flex-col items-center justify-center gap-6 px-6 text-center","children":[
                  {"id":"hero-t","type":"text","text":"山景温泉民宿"}
                ]},
                {"id":"rooms","type":"slot","slot":"room-list","props":{"layout":"grid"}}
              ]}
            }
            """;

    private static final String INVALID_TAG_CANVAS = """
            {"schemaVersion":"independent_site_canvas_v1",
             "root":{"id":"root","type":"element","tag":"script","children":[
               {"id":"t1","type":"text","text":"hello"}
             ]}}
            """;

    private static final String ILLEGAL_IMG_CANVAS = """
            {"schemaVersion":"independent_site_canvas_v1",
             "root":{"id":"root","type":"element","tag":"main","children":[
               {"id":"img1","type":"element","tag":"img","attrs":{"src":"javascript:alert(1)","alt":"x"}}
             ]}}
            """;

    @Test
    void generate_shouldBuildCanvasPromptWithFullContextAndReturnPublishableDraft() {
        Fixture fixture = new Fixture(new ObjectMapper());
        FakeAiClient client = new FakeAiClient(List.of(VALID_CANVAS));

        IndependentSiteDtos.PageDraftResponse response = fixture.service(client)
                .generateForPage(1L, 11L, 111L, request());

        assertTrue(response.publishable());
        assertEquals("OPENAI_CONFIRMED", response.providerStatus());
        assertEquals(IndependentSiteCanvasValidator.SCHEMA_VERSION, response.schemaVersion());
        assertEquals(1, client.callCount());

        String prompt = client.messages().get(0);
        assertTrue(prompt.contains("You design hotel websites."));
        assertTrue(prompt.contains("independent_site_canvas_v1"));
        assertTrue(prompt.contains("Hotel name: Alpha Hotel"));
        assertTrue(prompt.contains("Store description: A quiet inn by the lake."));
        assertTrue(prompt.contains("Output language: zh"));
        assertTrue(prompt.contains("Visual style: MINIMAL"));
        assertTrue(prompt.contains("Page type: HOME"));
        assertTrue(prompt.contains("Page title: Home"));
        assertTrue(prompt.contains("Page path: /"));
        // 图片池：发布范围房型的照片 URL；img src 只能取自池内
        assertTrue(prompt.contains("<IMAGE_POOL>"));
        assertTrue(prompt.contains("/media/1/room/dlx-a.jpg"));
        assertTrue(prompt.contains("must come from IMAGE_POOL"));
        // 房型名列表仅供文案参考
        assertTrue(prompt.contains("<ROOM_TYPE_NAMES>"));
        assertTrue(prompt.contains("Deluxe Suite"));
        // 插槽目录与 CTA 约束
        assertTrue(prompt.contains("room-list"));
        assertTrue(prompt.contains("scroll-to-booking"));
        // 插槽目录新增 booking-flow；页面 chrome 要求（sticky header + footer）
        assertTrue(prompt.contains("booking-flow"));
        assertTrue(prompt.contains("sticky header"));
        // 站点 slug 与已发布页面导航（PAGE_LINKS）注入
        assertTrue(prompt.contains("Site slug: alpha"));
        assertTrue(prompt.contains("<PAGE_LINKS>"));
        assertTrue(prompt.contains("- Home -> /stay/alpha"));
        assertTrue(prompt.contains("- About Us -> /stay/alpha/p/about"));
        assertTrue(prompt.contains("<USER_BRIEF>"));
        assertTrue(prompt.contains("打造一个安静的禅意首页"));
    }

    @Test
    void generate_shouldRepairAfterCanvasValidationFailure() {
        Fixture fixture = new Fixture(new ObjectMapper());
        FakeAiClient client = new FakeAiClient(List.of(INVALID_TAG_CANVAS, VALID_CANVAS));

        IndependentSiteDtos.PageDraftResponse response = fixture.service(client)
                .generateForPage(1L, 11L, 111L, request());

        assertTrue(response.publishable());
        assertEquals(2, client.callCount());
        String repair = client.messages().get(1);
        assertTrue(repair.contains("Validation error"));
        assertTrue(repair.contains("PREVIOUS_ANSWER"));
        assertTrue(repair.contains("script"));
    }

    @Test
    void generate_shouldRejectIllegalImageUrlAndRepair() {
        Fixture fixture = new Fixture(new ObjectMapper());
        FakeAiClient client = new FakeAiClient(List.of(ILLEGAL_IMG_CANVAS, VALID_CANVAS));

        IndependentSiteDtos.PageDraftResponse response = fixture.service(client)
                .generateForPage(1L, 11L, 111L, request());

        assertTrue(response.publishable());
        assertEquals(2, client.callCount());
        assertTrue(client.messages().get(1).contains("Validation error"));
    }

    @Test
    void generate_shouldRejectAfterExhaustingAttempts() {
        Fixture fixture = new Fixture(new ObjectMapper());
        FakeAiClient client = new FakeAiClient(List.of(
                INVALID_TAG_CANVAS,
                INVALID_TAG_CANVAS,
                INVALID_TAG_CANVAS,
                VALID_CANVAS
        ));

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> fixture.service(client).generateForPage(1L, 11L, 111L, request())
        );

        assertEquals("INVALID_AI_SCHEMA", exception.getCode());
        assertEquals(422, exception.getStatus().value());
        assertEquals(3, client.callCount());
    }

    @Test
    void aiEdit_shouldUseCanvasPromptAndKeepBackupSemantics() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Fixture fixture = new Fixture(objectMapper);
        String originalDraft = fixture.page.getDraftSchemaJson();
        FakeAiClient client = new FakeAiClient(List.of(VALID_CANVAS));

        IndependentSiteDtos.PageDetailResponse response = fixture.service(client)
                .aiEdit(1L, 11L, 111L, "把 hero 改成深色背景");

        assertEquals(1, client.callCount());
        String prompt = client.messages().get(0);
        assertTrue(prompt.contains("CURRENT_SCHEMA"));
        assertTrue(prompt.contains("USER_INSTRUCTION"));
        assertTrue(prompt.contains("把 hero 改成深色背景"));
        assertTrue(prompt.contains("Keep every node id unchanged"));
        assertTrue(prompt.contains("byte-identical"));
        // ai-edit 同样注入 slug / PAGE_LINKS，且要求编辑不得丢插槽与导航
        assertTrue(prompt.contains("Site slug: alpha"));
        assertTrue(prompt.contains("<PAGE_LINKS>"));
        assertTrue(prompt.contains("/stay/alpha/p/about"));
        assertTrue(prompt.contains("booking-flow"));
        assertTrue(prompt.contains("never remove the room-list or booking-flow slots"));

        // 备份/版本行为与 BLOCKS 完全一致：旧草稿原文进备份列，draftVersion+1
        assertEquals(originalDraft, fixture.page.getDraftBackupSchemaJson());
        assertEquals(4L, fixture.page.getDraftVersion());
        assertNotNull(fixture.page.getDraftUpdatedAt());
        assertTrue(response.hasAiBackup());
        assertEquals("CANVAS", response.format());
        assertEquals(
                IndependentSiteCanvasValidator.SCHEMA_VERSION,
                response.draftSchema().path("schemaVersion").asText()
        );
        JsonNode canonical = new IndependentSiteCanvasValidator(objectMapper)
                .validate(objectMapper.readTree(VALID_CANVAS));
        assertEquals(canonical, response.draftSchema());
    }

    private static IndependentSiteDtos.PageDraftRequest request() {
        return new IndependentSiteDtos.PageDraftRequest("打造一个安静的禅意首页", "zh", "MINIMAL");
    }

    private static final class Fixture {
        private final IndependentSite site;
        private final IndependentSitePage page;
        private final IndependentSiteRepository siteRepository;
        private final IndependentSitePageRepository pageRepository;
        private final StoreRepository storeRepository;
        private final IndependentSitePublicationRepository publicationRepository;
        private final RoomTypeRepository roomTypeRepository;
        private final RoomRepository roomRepository;
        private final ObjectMapper objectMapper;
        private final List<IndependentSitePage> saves = new ArrayList<>();

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
            page.setFormat(IndependentSitePageFormat.CANVAS);
            page.setTitle("Home");
            page.setDraftSchemaJson(
                    new IndependentSiteCanvasValidator(objectMapper).defaultCanvasSchema("Alpha Hotel").toString()
            );
            page.setDraftVersion(3L);
            page.setEnabled(true);
            page.setSortOrder(0);
            page.setPublishedAt(LocalDateTime.of(2026, 7, 20, 11, 0));

            // 第二个已发布且启用的页面：注入 PAGE_LINKS 的子页条目
            IndependentSitePage aboutPage = new IndependentSitePage();
            aboutPage.setId(112L);
            aboutPage.setStoreId(1L);
            aboutPage.setSite(site);
            aboutPage.setPath("/about");
            aboutPage.setType(IndependentSitePageType.CUSTOM);
            aboutPage.setFormat(IndependentSitePageFormat.CANVAS);
            aboutPage.setTitle("About Us");
            aboutPage.setEnabled(true);
            aboutPage.setSortOrder(1);
            aboutPage.setPublishedAt(LocalDateTime.of(2026, 7, 20, 12, 0));

            Store store = new Store();
            store.setId(1L);
            store.setName("Alpha Hotel");
            store.setDescription("A quiet inn by the lake.");

            RoomType roomType = new RoomType();
            roomType.setId(101L);
            roomType.setStoreId(1L);
            roomType.setName("Deluxe Suite");
            roomType.setCode("DLX-101");
            roomType.setMaxGuests(2);
            roomType.setSizeMeasurement(new BigDecimal("25.50"));
            roomType.setSizeMeasurementUnit("㎡");
            roomType.setDesktopPhotoUrls(List.of(
                    "/media/1/room/dlx-a.jpg",
                    "/media/1/room/dlx-b.jpg"
            ));

            IndependentSitePublication publication = new IndependentSitePublication();
            publication.setSite(site);
            publication.setTargetType(IndependentSitePublicationType.ROOM_TYPE);
            publication.setTargetId(101L);
            publication.setEnabled(true);

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
                        if ("findByStoreIdAndSiteIdAndPublishedAtIsNotNullAndEnabledTrueOrderBySortOrderAscIdAsc"
                                .equals(method.getName())) {
                            return List.of(page, aboutPage);
                        }
                        if ("save".equals(method.getName())) {
                            saves.add((IndependentSitePage) args[0]);
                            return args[0];
                        }
                        return objectMethodOrFail(proxy, method, args);
                    }
            );
            storeRepository = repository(
                    StoreRepository.class,
                    (proxy, method, args) -> {
                        if ("findById".equals(method.getName())) {
                            return Optional.of(store);
                        }
                        return objectMethodOrFail(proxy, method, args);
                    }
            );
            publicationRepository = repository(
                    IndependentSitePublicationRepository.class,
                    (proxy, method, args) -> {
                        if ("findByStoreIdAndSiteIdAndEnabledTrueOrderByDisplayOrderAscIdAsc"
                                .equals(method.getName())) {
                            return List.of(publication);
                        }
                        return objectMethodOrFail(proxy, method, args);
                    }
            );
            roomTypeRepository = repository(
                    RoomTypeRepository.class,
                    (proxy, method, args) -> {
                        if ("findByStoreIdAndIdIn".equals(method.getName())) {
                            return List.of(roomType);
                        }
                        return objectMethodOrFail(proxy, method, args);
                    }
            );
            roomRepository = repository(
                    RoomRepository.class,
                    (proxy, method, args) -> {
                        if ("findByStoreIdAndIdIn".equals(method.getName())) {
                            return List.of();
                        }
                        return objectMethodOrFail(proxy, method, args);
                    }
            );
        }

        private IndependentSitePageSchemaGenerationService service(FakeAiClient client) {
            return new IndependentSitePageSchemaGenerationService(
                    client,
                    new IndependentSitePageSchemaParser(objectMapper),
                    new IndependentSitePageSchemaValidator(objectMapper),
                    siteRepository,
                    pageRepository,
                    storeRepository,
                    new IndependentSiteManagementRateLimiter(
                            Clock.fixed(Instant.parse("2026-07-24T00:00:00Z"), ZoneOffset.UTC)
                    ),
                    objectMapper,
                    new IndependentSiteCanvasValidator(objectMapper),
                    publicationRepository,
                    roomRepository,
                    roomTypeRepository
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

        private final Queue<String> outputs;
        private final List<String> messages = new ArrayList<>();

        private FakeAiClient(List<String> outputs) {
            this.outputs = new ConcurrentLinkedQueue<>(outputs);
        }

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

        int callCount() {
            return messages.size();
        }

        List<String> messages() {
            return messages;
        }
    }
}
