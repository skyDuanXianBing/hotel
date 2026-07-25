package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import server.demo.dto.IndependentSiteDtos;
import server.demo.entity.Channel;
import server.demo.entity.IndependentSite;
import server.demo.entity.IndependentSitePage;
import server.demo.entity.PricePlan;
import server.demo.enums.IndependentSitePageType;
import server.demo.enums.IndependentSitePaymentProvider;
import server.demo.enums.PriceAdjustmentType;
import server.demo.repository.IndependentSitePageRepository;
import server.demo.repository.IndependentSitePublicationRepository;
import server.demo.repository.IndependentSiteRepository;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IndependentSiteManagementDraftLifecycleTest {

    @Test
    void saveThenPublish_shouldKeepPublicSchemaUnchangedUntilExplicitVersionedPublish()
            throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        IndependentSitePageSchemaValidator validator =
                new IndependentSitePageSchemaValidator(objectMapper);
        IndependentSite site = site();
        IndependentSitePage home = homePage(site, objectMapper, validator);
        IndependentSiteManagementService service = service(site, home, objectMapper, validator);
        String originalPublishedSchema = home.getPublishedSchemaJson();
        LocalDateTime originalPublishedAt = home.getPublishedAt();
        JsonNode draft = schema(objectMapper, "A newly drafted welcome");

        IndependentSiteDtos.PageDraftStateResponse saved = service.savePageDraft(
                1L,
                new IndependentSiteDtos.PageDraftSaveRequest(draft, 0L)
        );

        assertEquals(1L, saved.draftVersion());
        assertEquals(originalPublishedSchema, home.getPublishedSchemaJson());
        assertEquals(originalPublishedAt, home.getPublishedAt());
        assertNotNull(home.getDraftUpdatedAt());

        IndependentSiteServiceException stale = assertThrows(
                IndependentSiteServiceException.class,
                () -> service.publishPageDraft(
                        1L,
                        new IndependentSiteDtos.PublishPageDraftRequest(0L)
                )
        );
        assertEquals("DRAFT_VERSION_CONFLICT", stale.getCode());
        assertEquals(originalPublishedSchema, home.getPublishedSchemaJson());

        IndependentSiteDtos.ConfigResponse published = service.publishPageDraft(
                1L,
                new IndependentSiteDtos.PublishPageDraftRequest(saved.draftVersion())
        );

        assertEquals(
                "A newly drafted welcome",
                published.publishedPageSchema().path("sections").get(0).path("title").asText()
        );
        assertEquals(saved.draftVersion(), published.draftVersion());
        assertNotNull(published.publishedAt());
        assertNotNull(site.getPublishedAt());
    }

    @Test
    void configUpdateContract_shouldNotContainPageSchemaWriteField() {
        boolean containsPageSchema = Arrays.stream(
                        IndependentSiteDtos.ConfigUpdateRequest.class.getRecordComponents()
                )
                .map(component -> component.getName())
                .anyMatch(name -> name.toLowerCase(java.util.Locale.ROOT).contains("pageschema"));

        assertFalse(containsPageSchema);
    }

    private static IndependentSiteManagementService service(
            IndependentSite site,
            IndependentSitePage home,
            ObjectMapper objectMapper,
            IndependentSitePageSchemaValidator validator
    ) {
        IndependentSiteRepository siteRepository = repository(
                IndependentSiteRepository.class,
                (proxy, method, args) -> {
                    if ("findByStoreIdOrderByCreatedAtAscIdAsc".equals(method.getName())) {
                        return List.of(site);
                    }
                    if ("save".equals(method.getName())) {
                        return args[0];
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
                    if ("findByStoreIdAndSiteIdAndIdForUpdate".equals(method.getName())) {
                        return Objects.equals(args[2], home.getId())
                                ? Optional.of(home)
                                : Optional.empty();
                    }
                    if ("save".equals(method.getName())) {
                        return args[0];
                    }
                    return objectMethodOrFail(proxy, method, args);
                }
        );
        IndependentSitePublicationRepository publicationRepository = repository(
                IndependentSitePublicationRepository.class,
                (proxy, method, args) -> {
                    if ("findByStoreIdAndSiteIdAndEnabledTrueOrderByDisplayOrderAscIdAsc"
                            .equals(method.getName())) {
                        return List.of();
                    }
                    return objectMethodOrFail(proxy, method, args);
                }
        );
        return new IndependentSiteManagementService(
                siteRepository,
                publicationRepository,
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
                null,
                null,
                null,
                unconfiguredStripeSettings(),
                new IndependentSiteCanvasValidator(objectMapper)
        );
    }

    private static IndependentSite site() {
        PricePlan pricePlan = new PricePlan();
        pricePlan.setId(501L);
        pricePlan.setStoreId(1L);
        pricePlan.setName("Standard");

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
        site.setEnabled(false);
        site.setChannel(channel);
        site.setPaymentProvider(IndependentSitePaymentProvider.SIMULATED);
        site.setSimulatedPaymentEnabled(false);
        site.setPublishedAt(LocalDateTime.of(2026, 7, 19, 12, 0));
        return site;
    }

    private static IndependentSitePage homePage(
            IndependentSite site,
            ObjectMapper objectMapper,
            IndependentSitePageSchemaValidator validator
    ) throws Exception {
        IndependentSitePage home = new IndependentSitePage();
        home.setId(111L);
        home.setStoreId(site.getStoreId());
        home.setSite(site);
        home.setPath("/");
        home.setType(IndependentSitePageType.HOME);
        home.setTitle(site.getName());
        home.setPublishedSchemaJson(objectMapper.writeValueAsString(validator.defaultSchema()));
        home.setPublishedAt(LocalDateTime.of(2026, 7, 19, 12, 0));
        home.setDraftVersion(0L);
        home.setEnabled(true);
        home.setSortOrder(0);
        return home;
    }

    private static JsonNode schema(ObjectMapper objectMapper, String title) throws Exception {
        JsonNode root = objectMapper.readTree("""
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
                      "title":"placeholder",
                      "body":"Comfort in the heart of town.",
                      "alignment":"CENTER"
                    }
                  ]
                }
                """);
        ((com.fasterxml.jackson.databind.node.ObjectNode) root.path("sections").get(0))
                .put("title", title);
        return root;
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
