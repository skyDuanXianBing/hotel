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
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.enums.IndependentSitePageType;
import server.demo.enums.IndependentSitePublicationType;
import server.demo.repository.ChannelRepository;
import server.demo.repository.IndependentSitePageRepository;
import server.demo.repository.IndependentSitePublicationRepository;
import server.demo.repository.IndependentSiteRepository;
import server.demo.repository.PaymentAttemptRepository;
import server.demo.repository.PricePlanRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypePricePlanRepository;
import server.demo.repository.RoomTypeRepository;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 多站点 + 多页面管理服务测试。仓库为 JDK 动态代理假实现（不用 Mockito）。
 */
class IndependentSiteManagementServiceTest {

    @Test
    void createSite_shouldCreateHomePageAndListSitesInCreationOrder() {
        Fixture fixture = new Fixture();

        IndependentSiteDtos.SiteDetailResponse alpha = fixture.service.createSite(
                1L,
                new IndependentSiteDtos.SiteCreateRequest("Alpha Hotel", "alpha", null)
        );
        IndependentSiteDtos.SiteDetailResponse beta = fixture.service.createSite(
                1L,
                new IndependentSiteDtos.SiteCreateRequest("Beta Hotel", "beta", "modern")
        );

        assertFalse(alpha.enabled());
        assertEquals("classic", alpha.themeKey());
        assertEquals("modern", beta.themeKey());
        assertEquals(1, alpha.pages().size());
        IndependentSiteDtos.PageSummaryResponse alphaHome = alpha.pages().get(0);
        assertEquals("/", alphaHome.path());
        assertEquals("HOME", alphaHome.type());
        assertEquals("CANVAS", alphaHome.format());
        assertEquals("Alpha Hotel", alphaHome.title());
        assertTrue(alphaHome.hasUnpublishedChanges());

        IndependentSitePage homePage = fixture.pagesById.get(alphaHome.id());
        assertNotNull(homePage.getDraftSchemaJson());
        assertTrue(homePage.getDraftSchemaJson().contains("independent_site_canvas_v1"));
        assertNull(homePage.getPublishedSchemaJson());
        assertEquals(1L, homePage.getDraftVersion());
        assertTrue(homePage.getEnabled());

        List<IndependentSiteDtos.SiteSummaryResponse> sites = fixture.service.listSites(1L);
        assertEquals(2, sites.size());
        assertEquals("alpha", sites.get(0).slug());
        assertTrue(sites.get(0).isDefault());
        assertEquals(1, sites.get(0).pageCount());
        assertEquals("beta", sites.get(1).slug());
        assertFalse(sites.get(1).isDefault());

        IndependentSiteServiceException conflict = assertThrows(
                IndependentSiteServiceException.class,
                () -> fixture.service.createSite(
                        1L,
                        new IndependentSiteDtos.SiteCreateRequest("Alpha Again", "ALPHA", null)
                )
        );
        assertEquals("SLUG_ALREADY_EXISTS", conflict.getCode());
        assertEquals(409, conflict.getStatus().value());
    }

    @Test
    void updateSite_shouldUpdateConfigResyncPublicationsAndRejectStripe() {
        Fixture fixture = new Fixture();
        IndependentSiteDtos.SiteDetailResponse site = fixture.createSite("alpha");

        IndependentSiteDtos.SiteDetailResponse updated = fixture.service.updateSite(
                1L,
                site.id(),
                new IndependentSiteDtos.SiteUpdateRequest(
                        "Renamed Hotel",
                        "alpha",
                        false,
                        "elegant",
                        fixture.pricePlan.getId(),
                        new BigDecimal("12.50"),
                        null,
                        true,
                        Set.of(101L),
                        null
                )
        );

        assertEquals("Renamed Hotel", updated.name());
        assertEquals("elegant", updated.themeKey());
        assertEquals(Set.of(101L), updated.publishedRoomTypeIds());
        assertTrue(updated.simulatedPaymentEnabled());
        assertEquals(new BigDecimal("12.50"), updated.priceAdjustmentValue());

        IndependentSiteServiceException stripe = assertThrows(
                IndependentSiteServiceException.class,
                () -> fixture.service.updateSite(
                        1L,
                        site.id(),
                        new IndependentSiteDtos.SiteUpdateRequest(
                                null,
                                "alpha",
                                false,
                                null,
                                fixture.pricePlan.getId(),
                                BigDecimal.ZERO,
                                "STRIPE",
                                true,
                                Set.of(101L),
                                null
                        )
                )
        );
        assertEquals("PAYMENT_PROVIDER_NOT_AVAILABLE", stripe.getCode());
        assertEquals(422, stripe.getStatus().value());
    }

    @Test
    void updateSite_shouldAcceptStripeProviderWhenConfiguredAndExposeStripeAvailable() {
        Fixture fixture = new Fixture();
        IndependentSiteDtos.SiteDetailResponse site = fixture.createSite("alpha");
        assertFalse(site.stripeAvailable());

        IndependentSiteManagementService configured = fixture.buildService(
                stripeSettingsService(true)
        );
        IndependentSiteDtos.SiteDetailResponse updated = configured.updateSite(
                1L,
                site.id(),
                new IndependentSiteDtos.SiteUpdateRequest(
                        null,
                        "alpha",
                        false,
                        null,
                        fixture.pricePlan.getId(),
                        BigDecimal.ZERO,
                        "STRIPE",
                        false,
                        Set.of(101L),
                        null
                )
        );

        assertEquals("STRIPE", updated.paymentProvider());
        assertTrue(updated.stripeAvailable());

        // 未配置 Stripe 的实例读取同一站点：站点保留 STRIPE，但 stripeAvailable=false（UI 应禁选）
        IndependentSiteDtos.SiteDetailResponse reread = fixture.service.getSite(1L, site.id());
        assertEquals("STRIPE", reread.paymentProvider());
        assertFalse(reread.stripeAvailable());
    }

    @Test
    void updateSite_shouldRequirePublishedHomePageBeforeEnable() {
        Fixture fixture = new Fixture();
        IndependentSiteDtos.SiteDetailResponse site = fixture.createSite("alpha");
        IndependentSiteDtos.SiteUpdateRequest enableRequest = new IndependentSiteDtos.SiteUpdateRequest(
                null,
                "alpha",
                true,
                null,
                fixture.pricePlan.getId(),
                BigDecimal.ZERO,
                null,
                true,
                Set.of(101L),
                null
        );

        IllegalArgumentException blocked = assertThrows(
                IllegalArgumentException.class,
                () -> fixture.service.updateSite(1L, site.id(), enableRequest)
        );
        assertEquals("启用独立站前请先发布首页", blocked.getMessage());

        Long homeId = site.pages().get(0).id();
        fixture.service.publishPage(1L, site.id(), homeId, new IndependentSiteDtos.PublishPageDraftRequest(1L));

        IndependentSiteDtos.SiteDetailResponse enabled =
                fixture.service.updateSite(1L, site.id(), enableRequest);
        assertTrue(enabled.enabled());
        assertNotNull(enabled.publishedAt());
        assertNotNull(fixture.sitesById.get(site.id()).getPublishedAt());
        assertTrue(fixture.channel.getEnabled());
    }

    @Test
    void deleteSite_shouldBlockWhenPaymentsExistAndCascadeOtherwise() {
        Fixture fixture = new Fixture();
        IndependentSiteDtos.SiteDetailResponse withPayments = fixture.createSite("alpha");
        fixture.siteIdsWithPayments.add(withPayments.id());

        IndependentSiteServiceException blocked = assertThrows(
                IndependentSiteServiceException.class,
                () -> fixture.service.deleteSite(1L, withPayments.id())
        );
        assertEquals("SITE_HAS_PAYMENTS", blocked.getCode());
        assertTrue(fixture.sitesById.containsKey(withPayments.id()));

        IndependentSiteDtos.SiteDetailResponse clean = fixture.createSite("beta");
        fixture.service.deleteSite(1L, clean.id());
        assertFalse(fixture.sitesById.containsKey(clean.id()));
    }

    @Test
    void twoSites_shouldKeepPublicationScopesIsolated() {
        Fixture fixture = new Fixture();
        IndependentSiteDtos.SiteDetailResponse alpha = fixture.createSite("alpha");
        IndependentSiteDtos.SiteDetailResponse beta = fixture.createSite("beta");

        fixture.updateSite(alpha.id(), Set.of(101L));
        fixture.updateSite(beta.id(), Set.of(102L));

        assertEquals(Set.of(101L), fixture.service.getSite(1L, alpha.id()).publishedRoomTypeIds());
        assertEquals(Set.of(102L), fixture.service.getSite(1L, beta.id()).publishedRoomTypeIds());
    }

    @Test
    void updateSite_shouldRecomputeSharedChannelEnabledFromAnyEnabledSite() {
        Fixture fixture = new Fixture();
        IndependentSiteDtos.SiteDetailResponse alpha = fixture.createSite("alpha");
        IndependentSiteDtos.SiteDetailResponse beta = fixture.createSite("beta");
        fixture.publishHomePage(alpha);
        fixture.publishHomePage(beta);

        fixture.setSiteEnabled(alpha.id(), true);
        fixture.setSiteEnabled(beta.id(), true);
        assertTrue(fixture.channel.getEnabled());
        assertTrue(fixture.channel.getIsActive());

        // 关闭 beta：alpha 仍启用 → 共享渠道保持启用，alpha 的公开解析不受影响
        fixture.setSiteEnabled(beta.id(), false);
        assertTrue(fixture.channel.getEnabled());
        assertTrue(fixture.channel.getIsActive());
        assertDoesNotThrow(
                () -> fixture.quoteService().validateSiteChannel(fixture.sitesById.get(alpha.id()))
        );
        IndependentSiteServiceException betaDown = assertThrows(
                IndependentSiteServiceException.class,
                () -> fixture.quoteService().validateSiteChannel(fixture.sitesById.get(beta.id()))
        );
        assertEquals("SITE_UNAVAILABLE", betaDown.getCode());

        // 全部站点关闭 → 共享渠道停用
        fixture.setSiteEnabled(alpha.id(), false);
        assertFalse(fixture.channel.getEnabled());
        assertFalse(fixture.channel.getIsActive());
        IndependentSiteServiceException alphaDown = assertThrows(
                IndependentSiteServiceException.class,
                () -> fixture.quoteService().validateSiteChannel(fixture.sitesById.get(alpha.id()))
        );
        assertEquals("SITE_UNAVAILABLE", alphaDown.getCode());

        // 重新启用任一站点 → 共享渠道恢复启用
        fixture.setSiteEnabled(alpha.id(), true);
        assertTrue(fixture.channel.getEnabled());
        assertTrue(fixture.channel.getIsActive());
        assertDoesNotThrow(
                () -> fixture.quoteService().validateSiteChannel(fixture.sitesById.get(alpha.id()))
        );
    }

    @Test
    void pageCrud_shouldEnforcePathRulesTypeRulesAndDraftVersioning() {
        Fixture fixture = new Fixture();
        IndependentSiteDtos.SiteDetailResponse site = fixture.createSite("alpha");

        IndependentSiteDtos.PageDetailResponse page = fixture.service.createPage(
                1L,
                site.id(),
                new IndependentSiteDtos.PageCreateRequest("/About-Us", "About Us", null, null, null)
        );
        assertEquals("/about-us", page.path());
        assertEquals("CUSTOM", page.type());
        assertEquals("CANVAS", page.format());
        assertTrue(page.enabled());
        assertEquals(1L, page.draftVersion());
        assertNotNull(page.draftSchema());
        assertEquals("independent_site_canvas_v1", page.draftSchema().path("schemaVersion").asText());
        assertNull(page.publishedSchema());

        IndependentSiteServiceException duplicate = assertThrows(
                IndependentSiteServiceException.class,
                () -> fixture.service.createPage(
                        1L,
                        site.id(),
                        new IndependentSiteDtos.PageCreateRequest("/about-us", "Again", null, null, null)
                )
        );
        assertEquals("PAGE_PATH_ALREADY_EXISTS", duplicate.getCode());

        assertThrows(
                IllegalArgumentException.class,
                () -> fixture.service.createPage(
                        1L,
                        site.id(),
                        new IndependentSiteDtos.PageCreateRequest("no-slash", "X", null, null, null)
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> fixture.service.createPage(
                        1L,
                        site.id(),
                        new IndependentSiteDtos.PageCreateRequest("/trail/", "X", null, null, null)
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> fixture.service.createPage(
                        1L,
                        site.id(),
                        new IndependentSiteDtos.PageCreateRequest("/second-home", "X", "HOME", null, null)
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> fixture.service.createPage(
                        1L,
                        site.id(),
                        new IndependentSiteDtos.PageCreateRequest("/", "Root", "CUSTOM", null, null)
                )
        );

        IndependentSiteServiceException stale = assertThrows(
                IndependentSiteServiceException.class,
                () -> fixture.service.updatePage(
                        1L,
                        site.id(),
                        page.id(),
                        new IndependentSiteDtos.PageUpdateRequest(
                                null, null, null, null, null, fixture.validCanvasSchema("Draft v2"), 99L
                        )
                )
        );
        assertEquals("DRAFT_VERSION_CONFLICT", stale.getCode());

        // CANVAS 页面拒绝 BLOCKS 草稿（format 分派校验）
        assertThrows(
                IllegalArgumentException.class,
                () -> fixture.service.updatePage(
                        1L,
                        site.id(),
                        page.id(),
                        new IndependentSiteDtos.PageUpdateRequest(
                                null, null, null, null, null, fixture.validSchema("Blocks draft"), 1L
                        )
                )
        );

        IndependentSiteDtos.PageDetailResponse saved = fixture.service.updatePage(
                1L,
                site.id(),
                page.id(),
                new IndependentSiteDtos.PageUpdateRequest(
                        "About Us v2", "seo text", null, null, 3, fixture.validCanvasSchema("Draft v2"), 1L
                )
        );
        assertEquals(2L, saved.draftVersion());
        assertEquals("About Us v2", saved.title());
        assertEquals("seo text", saved.seoDescription());
        assertEquals(3, saved.sortOrder());
        assertNull(saved.publishedSchema());

        Long homeId = site.pages().get(0).id();
        assertThrows(
                IllegalArgumentException.class,
                () -> fixture.service.updatePage(
                        1L,
                        site.id(),
                        homeId,
                        new IndependentSiteDtos.PageUpdateRequest(
                                null, null, "/moved", null, null, null, null
                        )
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> fixture.service.deletePage(1L, site.id(), homeId)
        );

        fixture.service.deletePage(1L, site.id(), page.id());
        assertFalse(fixture.pagesById.containsKey(page.id()));
    }

    @Test
    void publishPage_shouldCopyDraftToPublishedAndOnlySyncSiteForHome() {
        Fixture fixture = new Fixture();
        IndependentSiteDtos.SiteDetailResponse site = fixture.createSite("alpha");
        IndependentSiteDtos.PageDetailResponse custom = fixture.service.createPage(
                1L,
                site.id(),
                new IndependentSiteDtos.PageCreateRequest("/about-us", "About Us", null, null, null)
        );

        IndependentSiteDtos.PageDetailResponse publishedCustom = fixture.service.publishPage(
                1L,
                site.id(),
                custom.id(),
                new IndependentSiteDtos.PublishPageDraftRequest(1L)
        );
        assertNotNull(publishedCustom.publishedSchema());
        assertNotNull(publishedCustom.publishedAt());
        assertNull(fixture.sitesById.get(site.id()).getPublishedAt());

        Long homeId = site.pages().get(0).id();
        fixture.service.publishPage(1L, site.id(), homeId, new IndependentSiteDtos.PublishPageDraftRequest(1L));
        assertNotNull(fixture.sitesById.get(site.id()).getPublishedAt());

        IndependentSiteServiceException stale = assertThrows(
                IndependentSiteServiceException.class,
                () -> fixture.service.publishPage(
                        1L,
                        site.id(),
                        homeId,
                        new IndependentSiteDtos.PublishPageDraftRequest(99L)
                )
        );
        assertEquals("DRAFT_VERSION_CONFLICT", stale.getCode());
    }

    @Test
    void legacyCurrentEndpoints_shouldDelegateToDefaultSiteHomePage() {
        Fixture fixture = new Fixture();
        IndependentSiteDtos.SiteDetailResponse alpha = fixture.createSite("alpha");
        IndependentSiteDtos.SiteDetailResponse beta = fixture.createSite("beta");
        // 旧端点服务旧 BLOCKS 编辑器：模拟存量 BLOCKS 站点
        fixture.forceBlocksHome(alpha.id());
        fixture.forceBlocksHome(beta.id());

        assertNotNull(fixture.service.getCurrent(1L).draftPageSchema());
        assertEquals(alpha.id(), fixture.service.getCurrent(1L).id());
        assertNotNull(fixture.service.getCurrent(1L).publishedPageSchema());

        IndependentSiteDtos.PageDraftStateResponse saved = fixture.service.savePageDraft(
                1L,
                new IndependentSiteDtos.PageDraftSaveRequest(fixture.validSchema("Legacy draft"), 1L)
        );
        assertEquals(alpha.id(), saved.siteId());
        assertEquals(2L, saved.draftVersion());

        Long alphaHomeId = alpha.pages().get(0).id();
        Long betaHomeId = beta.pages().get(0).id();
        assertEquals(2L, fixture.pagesById.get(alphaHomeId).getDraftVersion());
        assertEquals(1L, fixture.pagesById.get(betaHomeId).getDraftVersion());

        IndependentSiteDtos.ConfigResponse published = fixture.service.publishPageDraft(
                1L,
                new IndependentSiteDtos.PublishPageDraftRequest(saved.draftVersion())
        );
        assertEquals(alpha.id(), published.id());
        assertEquals(
                "Legacy draft",
                published.publishedPageSchema().path("sections").get(0).path("title").asText()
        );
        assertNotNull(fixture.sitesById.get(alpha.id()).getPublishedAt());
        assertNull(fixture.sitesById.get(beta.id()).getPublishedAt());
    }

    @Test
    void legacyUpdateCurrent_shouldCreateDefaultSiteWithHomePageWhenAbsent() {
        Fixture fixture = new Fixture();

        IndependentSiteDtos.ConfigResponse response = fixture.service.updateCurrent(
                1L,
                new IndependentSiteDtos.ConfigUpdateRequest(
                        "alpha",
                        false,
                        fixture.pricePlan.getId(),
                        BigDecimal.ZERO,
                        false,
                        Set.of(),
                        null
                )
        );

        assertNotNull(response.id());
        assertEquals("alpha", response.slug());
        assertEquals(1, fixture.sitesById.size());
        IndependentSitePage home = fixture.pagesById.values().stream().findFirst().orElseThrow();
        assertEquals(IndependentSitePageType.HOME, home.getType());
        assertNotNull(home.getDraftSchemaJson());
        assertNull(home.getPublishedSchemaJson());
    }

    @Test
    void generateRoomTypePages_shouldCreateRoomDetailPagesPassingValidator() throws Exception {
        Fixture fixture = new Fixture();
        fixture.roomTypes.add(detailedRoomType(103L, "Deluxe Suite", "DLX-103"));
        IndependentSiteDtos.SiteDetailResponse site = fixture.createSite("alpha");
        // 存量 BLOCKS 站点：房型页沿用旧区块骨架
        fixture.forceBlocksHome(site.id());
        fixture.updateSite(site.id(), Set.of(103L));

        IndependentSiteDtos.GenerateRoomPagesResponse response =
                fixture.service.generateRoomTypePages(1L, site.id());

        assertEquals(1, response.generated());
        assertEquals(0, response.refreshed());
        assertTrue(response.skipped().isEmpty());
        IndependentSiteDtos.PageSummaryResponse summary = response.pages().stream()
                .filter(page -> "ROOM_DETAIL".equals(page.type()))
                .findFirst()
                .orElseThrow();
        assertEquals("/rooms/dlx-103", summary.path());
        assertEquals("Deluxe Suite", summary.title());
        assertEquals(103L, summary.roomTypeId());
        assertTrue(summary.enabled());

        IndependentSitePage page = fixture.pagesById.get(summary.id());
        assertEquals(10, page.getSortOrder());
        assertNull(page.getPublishedSchemaJson());
        assertNull(page.getPublishedAt());
        assertEquals(1L, page.getDraftVersion());

        JsonNode validated = fixture.validator.validate(
                fixture.objectMapper.readTree(page.getDraftSchemaJson())
        );
        assertEquals(IndependentSitePageSchemaValidator.SCHEMA_VERSION, validated.path("schemaVersion").asText());
        JsonNode sections = validated.path("sections");
        assertEquals(5, sections.size());

        JsonNode hero = sections.get(0);
        assertEquals("HERO", hero.path("type").asText());
        assertEquals("Deluxe Suite", hero.path("title").asText());
        assertEquals("A bright suite with garden view.", hero.path("body").asText());
        assertEquals("/media/1/room/dlx-a.jpg", hero.path("imageUrl").asText());

        JsonNode gallery = sections.get(1);
        assertEquals("GALLERY", gallery.path("type").asText());
        assertEquals(3, gallery.path("images").size());
        assertEquals("/media/1/room/dlx-a.jpg", gallery.path("images").get(0).path("url").asText());
        assertEquals("Deluxe Suite", gallery.path("images").get(0).path("alt").asText());

        JsonNode about = sections.get(2);
        assertEquals("ABOUT", about.path("type").asText());
        assertEquals("房型介绍", about.path("title").asText());
        assertTrue(about.path("body").asText().contains("面积 25.5 ㎡"));
        assertTrue(about.path("body").asText().contains("2 位成人"));
        assertTrue(about.path("body").asText().contains("1 名儿童"));

        JsonNode amenities = sections.get(3);
        assertEquals("AMENITIES", amenities.path("type").asText());
        assertEquals("设施", amenities.path("title").asText());
        assertEquals(2, amenities.path("items").size());
        assertEquals("WiFi", amenities.path("items").get(0).asText());

        JsonNode booking = sections.get(4);
        assertEquals("BOOKING", booking.path("type").asText());
        assertEquals("立即预订", booking.path("title").asText());
    }

    @Test
    void generateRoomTypePages_shouldRefreshOnlyDraftAndKeepPublishedSchema() {
        Fixture fixture = new Fixture();
        fixture.roomTypes.add(detailedRoomType(103L, "Deluxe Suite", "DLX-103"));
        IndependentSiteDtos.SiteDetailResponse site = fixture.createSite("alpha");
        fixture.updateSite(site.id(), Set.of(103L));
        fixture.service.generateRoomTypePages(1L, site.id());
        Long pageId = roomPageId(fixture, site.id(), 103L);

        fixture.service.publishPage(1L, site.id(), pageId, new IndependentSiteDtos.PublishPageDraftRequest(1L));
        IndependentSitePage page = fixture.pagesById.get(pageId);
        String publishedSchema = page.getPublishedSchemaJson();
        assertNotNull(publishedSchema);

        roomTypeById(fixture, 103L).setDescription("Renovated with mountain view.");
        IndependentSiteDtos.GenerateRoomPagesResponse response =
                fixture.service.generateRoomTypePages(1L, site.id());

        assertEquals(0, response.generated());
        assertEquals(1, response.refreshed());
        assertTrue(response.skipped().isEmpty());
        assertEquals(publishedSchema, page.getPublishedSchemaJson());
        assertNotNull(page.getPublishedAt());
        assertEquals(2L, page.getDraftVersion());
        assertTrue(page.getEnabled());
        assertEquals("/rooms/dlx-103", page.getPath());
        assertTrue(page.getDraftSchemaJson().contains("Renovated with mountain view."));
    }

    @Test
    void generateRoomTypePages_shouldAppendRoomTypeIdWhenPathConflicts() {
        Fixture fixture = new Fixture();
        fixture.roomTypes.add(detailedRoomType(103L, "Deluxe Suite", "DLX-103"));
        IndependentSiteDtos.SiteDetailResponse site = fixture.createSite("alpha");
        fixture.service.createPage(
                1L,
                site.id(),
                new IndependentSiteDtos.PageCreateRequest("/rooms/dlx-103", "Custom", null, null, null)
        );
        fixture.updateSite(site.id(), Set.of(103L));

        IndependentSiteDtos.GenerateRoomPagesResponse response =
                fixture.service.generateRoomTypePages(1L, site.id());

        assertEquals(1, response.generated());
        IndependentSiteDtos.PageSummaryResponse summary = response.pages().stream()
                .filter(page -> "ROOM_DETAIL".equals(page.type()))
                .findFirst()
                .orElseThrow();
        assertEquals("/rooms/dlx-103-103", summary.path());
    }

    @Test
    void generateRoomTypePages_shouldSkipRoomTypeFailingValidation() {
        Fixture fixture = new Fixture();
        RoomType invalid = detailedRoomType(103L, "Deluxe Suite", "DLX-103");
        invalid.setDescription("Best deal at https://spam.example.com right now");
        fixture.roomTypes.add(invalid);
        IndependentSiteDtos.SiteDetailResponse site = fixture.createSite("alpha");
        fixture.updateSite(site.id(), Set.of(103L));

        IndependentSiteDtos.GenerateRoomPagesResponse response =
                fixture.service.generateRoomTypePages(1L, site.id());

        assertEquals(0, response.generated());
        assertEquals(0, response.refreshed());
        assertEquals(1, response.skipped().size());
        assertEquals(103L, response.skipped().get(0).roomTypeId());
        assertNotNull(response.skipped().get(0).reason());
        assertTrue(response.pages().stream().noneMatch(page -> "ROOM_DETAIL".equals(page.type())));
    }

    @Test
    void updateSite_shouldDisableRoomDetailPagesRemovedFromPublicationScope() {
        Fixture fixture = new Fixture();
        fixture.roomTypes.add(detailedRoomType(103L, "Deluxe Suite", "DLX-103"));
        fixture.roomTypes.add(detailedRoomType(104L, "Garden Twin", "TWN-104"));
        IndependentSiteDtos.SiteDetailResponse site = fixture.createSite("alpha");
        fixture.updateSite(site.id(), Set.of(103L, 104L));
        fixture.service.generateRoomTypePages(1L, site.id());
        IndependentSitePage page103 = fixture.pagesById.get(roomPageId(fixture, site.id(), 103L));
        IndependentSitePage page104 = fixture.pagesById.get(roomPageId(fixture, site.id(), 104L));
        assertTrue(page103.getEnabled());
        assertTrue(page104.getEnabled());

        fixture.updateSite(site.id(), Set.of(103L));
        assertTrue(page103.getEnabled());
        assertFalse(page104.getEnabled());
        String draft104 = page104.getDraftSchemaJson();
        assertNotNull(draft104);

        fixture.updateSite(site.id(), Set.of(103L, 104L));
        assertFalse(page104.getEnabled());

        IndependentSiteDtos.GenerateRoomPagesResponse response =
                fixture.service.generateRoomTypePages(1L, site.id());
        assertEquals(0, response.generated());
        assertEquals(2, response.refreshed());
        assertTrue(page104.getEnabled());
        assertEquals(2L, page104.getDraftVersion());
        assertEquals(draft104, page104.getDraftSchemaJson());
    }

    @Test
    void generateRoomTypePages_shouldIncludeRoomTypesOfPublishedRooms() {
        Fixture fixture = new Fixture();
        RoomType roomType = detailedRoomType(103L, "Deluxe Suite", "DLX-103");
        fixture.roomTypes.add(roomType);
        Room room = new Room();
        room.setId(9001L);
        room.setStoreId(1L);
        room.setRoomNumber("101");
        room.setRoomType(roomType);
        fixture.rooms.add(room);
        IndependentSiteDtos.SiteDetailResponse site = fixture.createSite("alpha");
        fixture.updateSite(site.id(), Set.of(), Set.of(9001L));

        IndependentSiteDtos.GenerateRoomPagesResponse response =
                fixture.service.generateRoomTypePages(1L, site.id());

        assertEquals(1, response.generated());
        IndependentSiteDtos.PageSummaryResponse summary = response.pages().stream()
                .filter(page -> "ROOM_DETAIL".equals(page.type()))
                .findFirst()
                .orElseThrow();
        assertEquals(103L, summary.roomTypeId());
        assertEquals("/rooms/dlx-103", summary.path());

        fixture.updateSite(site.id(), Set.of(), Set.of());
        assertFalse(fixture.pagesById.get(summary.id()).getEnabled());
    }

    private static Long roomPageId(Fixture fixture, Long siteId, Long roomTypeId) {
        return fixture.pagesById.values().stream()
                .filter(page -> page.getSite() != null && Objects.equals(page.getSite().getId(), siteId))
                .filter(page -> page.getType() == IndependentSitePageType.ROOM_DETAIL)
                .filter(page -> Objects.equals(page.getRoomTypeId(), roomTypeId))
                .map(IndependentSitePage::getId)
                .findFirst()
                .orElseThrow();
    }

    private static RoomType roomTypeById(Fixture fixture, Long roomTypeId) {
        return fixture.roomTypes.stream()
                .filter(roomType -> Objects.equals(roomType.getId(), roomTypeId))
                .findFirst()
                .orElseThrow();
    }

    private static RoomType detailedRoomType(Long id, String name, String code) {
        RoomType roomType = new RoomType();
        roomType.setId(id);
        roomType.setStoreId(1L);
        roomType.setName(name);
        roomType.setCode(code);
        roomType.setDescription("A bright suite with garden view.");
        roomType.setMaxGuests(2);
        roomType.setMaxChildOccupancy(1);
        roomType.setSizeMeasurement(new BigDecimal("25.50"));
        roomType.setSizeMeasurementUnit("㎡");
        roomType.setDesktopPhotoUrls(List.of(
                "/media/1/room/dlx-a.jpg",
                "/media/1/room/dlx-b.jpg",
                "/media/1/room/dlx-c.jpg"
        ));
        roomType.setFacilities(List.of(
                new FacilityDTO("basic", "WiFi"),
                new FacilityDTO("basic", "TV")
        ));
        return roomType;
    }

    // ------------------------------------------------------------------
    // Fixture
    // ------------------------------------------------------------------

    private static final class Fixture {

        private final ObjectMapper objectMapper = new ObjectMapper();
        private final IndependentSitePageSchemaValidator validator =
                new IndependentSitePageSchemaValidator(objectMapper);
        private final IndependentSiteCanvasValidator canvasValidator =
                new IndependentSiteCanvasValidator(objectMapper);
        private final Map<Long, IndependentSite> sitesById = new LinkedHashMap<>();
        private final Map<Long, IndependentSitePage> pagesById = new LinkedHashMap<>();
        private final Map<String, List<IndependentSitePublication>> publicationsBySite =
                new LinkedHashMap<>();
        private final Set<Long> siteIdsWithPayments = new HashSet<>();
        private final Channel channel = channel();
        private final PricePlan pricePlan;
        private final List<RoomType> roomTypes = new ArrayList<>(List.of(roomType(101L), roomType(102L)));
        private final List<Room> rooms = new ArrayList<>();
        private long siteSequence = 100L;
        private long pageSequence = 500L;

        private final IndependentSiteManagementService service = buildService(
                stripeSettingsService(false)
        );

        private IndependentSiteManagementService buildService(
                IndependentSiteStripeSettingsService stripeSettingsService
        ) {
            return new IndependentSiteManagementService(
                    repository(IndependentSiteRepository.class, this::handleSiteRepository),
                    repository(IndependentSitePublicationRepository.class, this::handlePublicationRepository),
                    repository(IndependentSitePageRepository.class, this::handlePageRepository),
                    repository(PaymentAttemptRepository.class, this::handlePaymentAttemptRepository),
                    repository(ChannelRepository.class, this::handleChannelRepository),
                    repository(PricePlanRepository.class, this::handlePricePlanRepository),
                    repository(RoomTypeRepository.class, this::handleRoomTypeRepository),
                    repository(RoomRepository.class, this::handleRoomRepository),
                    repository(RoomTypePricePlanRepository.class, this::handleMappingRepository),
                    new NoopChannelBootstrapService(),
                    validator,
                    objectMapper,
                    null,
                    null,
                    null,
                    stripeSettingsService,
                    canvasValidator
            );
        }

        private Fixture() {
            this.pricePlan = channel.getDefaultPricePlan();
        }

        private IndependentSiteDtos.SiteDetailResponse createSite(String slug) {
            return service.createSite(
                    1L,
                    new IndependentSiteDtos.SiteCreateRequest(slug + " hotel", slug, null)
            );
        }

        private void publishHomePage(IndependentSiteDtos.SiteDetailResponse site) {
            Long homeId = site.pages().get(0).id();
            service.publishPage(
                    1L,
                    site.id(),
                    homeId,
                    new IndependentSiteDtos.PublishPageDraftRequest(1L)
            );
        }

        private void setSiteEnabled(Long siteId, boolean enabled) {
            service.updateSite(
                    1L,
                    siteId,
                    new IndependentSiteDtos.SiteUpdateRequest(
                            null,
                            sitesById.get(siteId).getSlug(),
                            enabled,
                            null,
                            pricePlan.getId(),
                            BigDecimal.ZERO,
                            null,
                            false,
                            Set.of(101L),
                            null
                    )
            );
        }

        /** 仅挂页面仓库的最小 QuoteService：validateSiteChannel 只依赖站点/渠道与已发布首页查询。 */
        private IndependentSiteQuoteService quoteService() {
            return new IndependentSiteQuoteService(
                    null,
                    null,
                    repository(IndependentSitePageRepository.class, this::handlePageRepository),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    validator,
                    objectMapper,
                    Clock.systemUTC(),
                    canvasValidator
            );
        }

        /** 把指定站点的首页强制改回 BLOCKS（模拟存量站点），供旧管线回归测试使用。 */
        private void forceBlocksHome(Long siteId) {
            IndependentSitePage home = pagesById.values().stream()
                    .filter(page -> page.getSite() != null && Objects.equals(page.getSite().getId(), siteId))
                    .filter(page -> page.getType() == IndependentSitePageType.HOME)
                    .findFirst()
                    .orElseThrow();
            home.setFormat(server.demo.enums.IndependentSitePageFormat.BLOCKS);
            try {
                home.setDraftSchemaJson(objectMapper.writeValueAsString(validator.defaultSchema()));
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }

        private void updateSite(Long siteId, Set<Long> roomTypeIds) {
            updateSite(siteId, roomTypeIds, null);
        }

        private void updateSite(Long siteId, Set<Long> roomTypeIds, Set<Long> roomIds) {
            service.updateSite(
                    1L,
                    siteId,
                    new IndependentSiteDtos.SiteUpdateRequest(
                            null,
                            sitesById.get(siteId).getSlug(),
                            false,
                            null,
                            pricePlan.getId(),
                            BigDecimal.ZERO,
                            null,
                            false,
                            roomTypeIds,
                            roomIds
                    )
            );
        }

        private com.fasterxml.jackson.databind.JsonNode validSchema(String title) {
            try {
                return objectMapper.readTree("""
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
                              "title":"%s",
                              "body":"Comfort in the heart of town.",
                              "alignment":"CENTER"
                            }
                          ]
                        }
                        """.formatted(title));
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }

        private com.fasterxml.jackson.databind.JsonNode validCanvasSchema(String text) {
            try {
                return objectMapper.readTree("""
                        {
                          "schemaVersion":"independent_site_canvas_v1",
                          "root":{
                            "id":"root",
                            "type":"element",
                            "tag":"main",
                            "class":"min-h-screen bg-white",
                            "children":[
                              {"id":"n-t1","type":"text","text":"%s"}
                            ]
                          }
                        }
                        """.formatted(text));
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }

        private Object handleSiteRepository(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "findByStoreIdOrderByCreatedAtAscIdAsc" -> sitesById.values().stream()
                        .filter(site -> Objects.equals(site.getStoreId(), args[0]))
                        .toList();
                case "findByStoreIdAndId",
                     "findByStoreIdAndIdWithChannel",
                     "findByStoreIdAndIdWithChannelForUpdate" -> Optional.ofNullable(
                        sitesById.values().stream()
                                .filter(site -> Objects.equals(site.getStoreId(), args[0]))
                                .filter(site -> Objects.equals(site.getId(), args[1]))
                                .findFirst()
                                .orElse(null)
                );
                case "existsBySlug" -> sitesById.values().stream()
                        .anyMatch(site -> Objects.equals(site.getSlug(), args[0]));
                case "existsBySlugAndIdNot" -> sitesById.values().stream()
                        .anyMatch(site -> Objects.equals(site.getSlug(), args[0])
                                && !Objects.equals(site.getId(), args[1]));
                case "save" -> saveSite((IndependentSite) args[0]);
                case "delete" -> deleteSite((IndependentSite) args[0]);
                default -> objectMethodOrFail(proxy, method, args);
            };
        }

        private Object handlePageRepository(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "findByStoreIdAndSiteIdOrderBySortOrderAscIdAsc" -> pagesOf(args[0], args[1])
                        .sorted(Comparator.comparing(IndependentSitePage::getSortOrder)
                                .thenComparing(IndependentSitePage::getId))
                        .toList();
                case "findByStoreIdAndSiteIdAndId", "findByStoreIdAndSiteIdAndIdForUpdate" ->
                        pagesOf(args[0], args[1])
                                .filter(page -> Objects.equals(page.getId(), args[2]))
                                .findFirst();
                case "findByStoreIdAndSiteIdAndPath" -> pagesOf(args[0], args[1])
                        .filter(page -> Objects.equals(page.getPath(), args[2]))
                        .findFirst();
                case "findByStoreIdAndSiteIdAndType" -> pagesOf(args[0], args[1])
                        .filter(page -> page.getType() == args[2])
                        .toList();
                case "findBySiteIdAndTypeAndPublishedAtIsNotNullAndEnabledTrue" ->
                        pagesById.values().stream()
                                .filter(page -> page.getSite() != null
                                        && Objects.equals(page.getSite().getId(), args[0]))
                                .filter(page -> page.getType() == args[1])
                                .filter(page -> page.getPublishedAt() != null)
                                .filter(page -> Boolean.TRUE.equals(page.getEnabled()))
                                .findFirst();
                case "countByStoreIdAndSiteId" -> pagesOf(args[0], args[1]).count();
                case "save" -> savePage((IndependentSitePage) args[0]);
                case "delete" -> deletePage((IndependentSitePage) args[0]);
                default -> objectMethodOrFail(proxy, method, args);
            };
        }

        private java.util.stream.Stream<IndependentSitePage> pagesOf(Object storeId, Object siteId) {
            return pagesById.values().stream()
                    .filter(page -> Objects.equals(page.getStoreId(), storeId))
                    .filter(page -> page.getSite() != null
                            && Objects.equals(page.getSite().getId(), siteId));
        }

        private Object handlePublicationRepository(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "findByStoreIdAndSiteIdAndEnabledTrueOrderByDisplayOrderAscIdAsc" ->
                        publicationsOf(args[0], args[1]).stream()
                                .filter(row -> Boolean.TRUE.equals(row.getEnabled()))
                                .toList();
                case "deleteByStoreIdAndSiteIdInBulk" -> {
                    List<IndependentSitePublication> rows = publicationsOf(args[0], args[1]);
                    int removed = rows.size();
                    rows.clear();
                    yield removed;
                }
                case "saveAll" -> {
                    @SuppressWarnings("unchecked")
                    Iterable<IndependentSitePublication> rows =
                            (Iterable<IndependentSitePublication>) args[0];
                    for (IndependentSitePublication row : rows) {
                        row.setStoreId(row.getSite().getStoreId());
                        publicationsOf(
                                row.getSite().getStoreId(),
                                row.getSite().getId()
                        ).add(row);
                    }
                    yield args[0];
                }
                case "countByStoreIdAndSiteId" -> (long) publicationsOf(args[0], args[1]).size();
                default -> objectMethodOrFail(proxy, method, args);
            };
        }

        private List<IndependentSitePublication> publicationsOf(Object storeId, Object siteId) {
            return publicationsBySite.computeIfAbsent(
                    storeId + ":" + siteId,
                    key -> new ArrayList<>()
            );
        }

        private Object handlePaymentAttemptRepository(Object proxy, Method method, Object[] args) {
            if ("existsBySite_Id".equals(method.getName())) {
                return siteIdsWithPayments.contains(args[0]);
            }
            return objectMethodOrFail(proxy, method, args);
        }

        private Object handleChannelRepository(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "findByStoreIdAndCode" -> Optional.ofNullable(
                        Objects.equals(channel.getStoreId(), args[0])
                                && Objects.equals(channel.getCode(), args[1])
                                ? channel
                                : null
                );
                case "save" -> args[0];
                default -> objectMethodOrFail(proxy, method, args);
            };
        }

        private Object handlePricePlanRepository(Object proxy, Method method, Object[] args) {
            if ("findByStoreIdAndId".equals(method.getName())) {
                return Optional.ofNullable(
                        Objects.equals(pricePlan.getStoreId(), args[0])
                                && Objects.equals(pricePlan.getId(), args[1])
                                ? pricePlan
                                : null
                );
            }
            return objectMethodOrFail(proxy, method, args);
        }

        private Object handleRoomTypeRepository(Object proxy, Method method, Object[] args) {
            if ("findByStoreIdAndIdIn".equals(method.getName())) {
                @SuppressWarnings("unchecked")
                List<Long> ids = (List<Long>) args[1];
                return roomTypes.stream()
                        .filter(roomType -> Objects.equals(roomType.getStoreId(), args[0]))
                        .filter(roomType -> ids.contains(roomType.getId()))
                        .toList();
            }
            return objectMethodOrFail(proxy, method, args);
        }

        private Object handleRoomRepository(Object proxy, Method method, Object[] args) {
            if ("findByStoreIdAndIdIn".equals(method.getName())) {
                @SuppressWarnings("unchecked")
                java.util.Collection<Long> ids = (java.util.Collection<Long>) args[1];
                return rooms.stream()
                        .filter(room -> Objects.equals(room.getStoreId(), args[0]))
                        .filter(room -> ids.contains(room.getId()))
                        .toList();
            }
            return objectMethodOrFail(proxy, method, args);
        }

        private Object handleMappingRepository(Object proxy, Method method, Object[] args) {
            if ("existsByStoreIdAndRoomTypeIdAndPricePlanId".equals(method.getName())) {
                return true;
            }
            return objectMethodOrFail(proxy, method, args);
        }

        private IndependentSite saveSite(IndependentSite site) {
            if (site.getId() == null) {
                site.setId(++siteSequence);
            }
            sitesById.put(site.getId(), site);
            return site;
        }

        private Void deleteSite(IndependentSite site) {
            sitesById.remove(site.getId());
            return null;
        }

        private IndependentSitePage savePage(IndependentSitePage page) {
            if (page.getId() == null) {
                page.setId(++pageSequence);
            }
            pagesById.put(page.getId(), page);
            return page;
        }

        private Void deletePage(IndependentSitePage page) {
            pagesById.remove(page.getId());
            return null;
        }

        private static Channel channel() {
            PricePlan plan = new PricePlan();
            plan.setId(501L);
            plan.setStoreId(1L);
            plan.setName("Standard");

            Channel channel = new Channel();
            channel.setId(301L);
            channel.setStoreId(1L);
            channel.setCode(IndependentSiteManagementService.BOOKING_ENGINE_CHANNEL_CODE);
            channel.setEnabled(false);
            channel.setIsActive(true);
            channel.setPriceAdjustmentType(server.demo.enums.PriceAdjustmentType.PERCENTAGE);
            channel.setPriceAdjustmentValue(BigDecimal.ZERO.setScale(2));
            channel.setDefaultPricePlan(plan);
            return channel;
        }

        private static RoomType roomType(Long id) {
            RoomType roomType = new RoomType();
            roomType.setId(id);
            roomType.setStoreId(1L);
            roomType.setName("Room " + id);
            roomType.setCode("RT-" + id);
            return roomType;
        }
    }

    private static final class NoopChannelBootstrapService extends ChannelBootstrapService {
        private NoopChannelBootstrapService() {
            super(null);
        }

        @Override
        public int ensureDefaultChannelsForStore(Long storeId) {
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T repository(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    /**
     * 真实门店 Stripe 设置服务（AES-GCM 密钥 + 代理仓库）：
     * configured=true 时门店 1 预置三密钥齐全的配置行，覆盖"门槛=门店三密钥齐全"的完整解密链路。
     */
    private static IndependentSiteStripeSettingsService stripeSettingsService(boolean configured) {
        String encryptionKey = java.util.Base64.getEncoder().encodeToString(new byte[32]);
        server.demo.util.AesGcmCrypto crypto = server.demo.util.AesGcmCrypto.fromBase64Key(encryptionKey);
        server.demo.entity.IndependentSiteStripeSettings row = null;
        if (configured) {
            row = new server.demo.entity.IndependentSiteStripeSettings();
            row.setId(1L);
            row.setStoreId(1L);
            row.setPublishableKey("pk_test_123");
            row.setSecretKeyEncrypted(crypto.encrypt("sk_test_123"));
            row.setWebhookSecretEncrypted(crypto.encrypt("whsec_test_123"));
        }
        final server.demo.entity.IndependentSiteStripeSettings stored = row;
        server.demo.repository.IndependentSiteStripeSettingsRepository settingsRepository = repository(
                server.demo.repository.IndependentSiteStripeSettingsRepository.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByStoreId" -> Optional.ofNullable(
                            stored != null && Objects.equals(stored.getStoreId(), args[0]) ? stored : null
                    );
                    default -> objectMethodOrFail(proxy, method, args);
                }
        );
        return new IndependentSiteStripeSettingsService(settingsRepository, encryptionKey);
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
