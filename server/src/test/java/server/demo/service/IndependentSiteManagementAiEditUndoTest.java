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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndependentSiteManagementAiEditUndoTest {

    private static final String BACKUP_SCHEMA = """
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

    private static final String CURRENT_DRAFT = """
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
                {"type":"HERO","title":"A slower welcome","body":"Discover a calm stay","alignment":"CENTER"}
              ]
            }
            """;

    @Test
    void undoAiEdit_shouldRestoreBackupIntoDraftAndClearBackup() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Fixture fixture = new Fixture(objectMapper, BACKUP_SCHEMA);

        IndependentSiteDtos.PageDetailResponse response = fixture.service().undoAiEdit(1L, 11L, 111L);

        assertEquals(BACKUP_SCHEMA, fixture.page.getDraftSchemaJson());
        assertNull(fixture.page.getDraftBackupSchemaJson());
        assertEquals(5L, fixture.page.getDraftVersion());
        assertNotNull(fixture.page.getDraftUpdatedAt());
        assertFalse(response.hasAiBackup());
        assertEquals(5L, response.draftVersion());
        JsonNode canonicalBackup = new IndependentSitePageSchemaValidator(objectMapper)
                .validate(objectMapper.readTree(BACKUP_SCHEMA));
        assertEquals(canonicalBackup, response.draftSchema());
        assertEquals(1, fixture.saves.size());
    }

    @Test
    void undoAiEdit_shouldReturn404WhenBackupIsMissing() {
        ObjectMapper objectMapper = new ObjectMapper();
        Fixture fixture = new Fixture(objectMapper, null);

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> fixture.service().undoAiEdit(1L, 11L, 111L)
        );

        assertEquals(404, exception.getStatus().value());
        assertEquals("AI_EDIT_BACKUP_NOT_FOUND", exception.getCode());
        assertEquals(CURRENT_DRAFT, fixture.page.getDraftSchemaJson());
        assertEquals(4L, fixture.page.getDraftVersion());
        assertTrue(fixture.saves.isEmpty());
    }

    @Test
    void undoAiEdit_shouldRejectCrossSiteAndCrossStorePage() {
        ObjectMapper objectMapper = new ObjectMapper();
        Fixture fixture = new Fixture(objectMapper, BACKUP_SCHEMA);

        IllegalArgumentException crossStore = assertThrows(
                IllegalArgumentException.class,
                () -> fixture.service().undoAiEdit(2L, 11L, 111L)
        );
        assertEquals("独立站不存在", crossStore.getMessage());

        IllegalArgumentException crossSite = assertThrows(
                IllegalArgumentException.class,
                () -> fixture.service().undoAiEdit(1L, 22L, 111L)
        );
        assertEquals("独立站不存在", crossSite.getMessage());

        IllegalArgumentException unknownPage = assertThrows(
                IllegalArgumentException.class,
                () -> fixture.service().undoAiEdit(1L, 11L, 999L)
        );
        assertEquals("页面不存在", unknownPage.getMessage());
        assertTrue(fixture.saves.isEmpty());
    }

    private static final class Fixture {
        private final IndependentSitePage page;
        private final IndependentSiteManagementService service;
        private final List<IndependentSitePage> saves = new ArrayList<>();

        private Fixture(ObjectMapper objectMapper, String backupSchemaJson) {
            IndependentSite site = new IndependentSite();
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
            page.setDraftSchemaJson(CURRENT_DRAFT);
            page.setDraftBackupSchemaJson(backupSchemaJson);
            page.setDraftVersion(4L);
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
                        if ("findByStoreIdAndSiteIdAndIdForUpdate".equals(method.getName())) {
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

        private IndependentSiteManagementService service() {
            return service;
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
