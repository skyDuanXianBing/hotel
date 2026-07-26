package server.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import server.demo.annotation.RequirePermission;
import server.demo.annotation.StoreScoped;
import server.demo.context.StoreContextHolder;
import server.demo.controller.advice.IndependentSiteApiExceptionHandler;
import server.demo.dto.IndependentSiteDtos;
import server.demo.entity.StoreUser;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.exception.StoreAccessDeniedException;
import server.demo.interceptor.StoreContextInterceptor;
import server.demo.repository.StoreUserRepository;
import server.demo.service.ChannelPriceWarmupService;
import server.demo.service.IndependentSiteBookingService;
import server.demo.service.IndependentSiteServiceException;
import server.demo.service.IndependentSiteStripeWebhookService;
import server.demo.service.PermissionService;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IndependentSiteControllerSecurityBoundaryTest {

    @Test
    void publicController_shouldNotExposePaymentSimulationMutation() {
        boolean exposesSimulation = Arrays.stream(PublicIndependentSiteController.class.getDeclaredMethods())
                .map(method -> method.getAnnotation(PostMapping.class))
                .filter(java.util.Objects::nonNull)
                .flatMap(mapping -> Arrays.stream(mapping.value()))
                .anyMatch(path -> path.contains("/simulate"));

        assertFalse(exposesSimulation);
    }

    @Test
    void publicController_guestHoldAndConfirm_shouldNotRequireStoreContext() throws Exception {
        // 公开游客下单/确认支付是产品设计允许的能力；它们不得依赖门店上下文或权限注解，
        // 且不得暴露管理端 /simulate 路径。
        assertNull(PublicIndependentSiteController.class.getAnnotation(StoreScoped.class));

        Method createHold = PublicIndependentSiteController.class.getDeclaredMethod(
                "createHold",
                String.class,
                IndependentSiteDtos.HoldRequest.class,
                jakarta.servlet.http.HttpServletRequest.class
        );
        Method confirmPayment = PublicIndependentSiteController.class.getDeclaredMethod(
                "confirmPayment",
                String.class,
                String.class
        );
        assertNull(createHold.getAnnotation(StoreScoped.class));
        assertNull(createHold.getAnnotation(RequirePermission.class));
        assertNull(confirmPayment.getAnnotation(StoreScoped.class));
        assertNull(confirmPayment.getAnnotation(RequirePermission.class));

        boolean exposesManagementSimulation = Arrays.stream(
                        PublicIndependentSiteController.class.getDeclaredMethods()
                )
                .map(method -> method.getAnnotation(PostMapping.class))
                .filter(java.util.Objects::nonNull)
                .flatMap(mapping -> Arrays.stream(mapping.value()))
                .anyMatch(path -> path.contains("/simulate"));
        assertFalse(exposesManagementSimulation);
    }

    @Test
    void managementPreviewHold_shouldBeStoreScopedAndPermissionProtected() throws Exception {
        assertNotNull(IndependentSiteController.class.getAnnotation(StoreScoped.class));
        Method method = previewHoldMethod();
        PostMapping mapping = method.getAnnotation(PostMapping.class);
        RequirePermission permission = method.getAnnotation(RequirePermission.class);

        assertNotNull(mapping);
        assertTrue(Arrays.asList(mapping.value()).contains("/{slug}/preview-holds"));
        assertNotNull(permission);
        assertEquals(PermissionModule.CHANNEL, permission.module());
        assertEquals(PermissionAction.MANAGE_CHANNELS, permission.action());
        StoreScoped storeScoped = method.getAnnotation(StoreScoped.class);
        assertNotNull(storeScoped);
        assertFalse(storeScoped.warmupChannelPrices());
        assertEquals(2, method.getParameterCount());
    }

    @Test
    void managementPreviewHold_shouldRejectMissingStoreContext() {
        StoreContextHolder.clear();
        IndependentSiteController controller = new IndependentSiteController(null, null, null, null, null);

        try {
            assertThrows(
                    StoreAccessDeniedException.class,
                    () -> controller.createPreviewHold("alpha-hotel", holdRequest())
            );
        } finally {
            StoreContextHolder.clear();
        }
    }

    @Test
    void managementPreviewHold_shouldRejectPermissionBeforeWarmupOrBookingWrite() throws Exception {
        RecordingPermissionService permissionService = new RecordingPermissionService(false);
        RecordingBookingService bookingService = new RecordingBookingService();
        RecordingWarmupService warmupService = new RecordingWarmupService();
        MockMvc mockMvc = managementMockMvc(permissionService, bookingService, warmupService);

        mockMvc.perform(previewHoldRequest())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        assertEquals(1, permissionService.calls);
        assertEquals(0, warmupService.calls);
        assertEquals(0, bookingService.previewHoldCalls);
        assertEquals(0, bookingService.confirmationCalls);
        assertNull(StoreContextHolder.getContext());
    }

    @Test
    void managementPreviewHold_shouldPreserveStoreContextForAuthorizedPreview() throws Exception {
        RecordingPermissionService permissionService = new RecordingPermissionService(true);
        RecordingBookingService bookingService = new RecordingBookingService();
        RecordingWarmupService warmupService = new RecordingWarmupService();
        MockMvc mockMvc = managementMockMvc(permissionService, bookingService, warmupService);

        mockMvc.perform(previewHoldRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertEquals(1, permissionService.calls);
        assertEquals(0, warmupService.calls);
        assertEquals(1, bookingService.previewHoldCalls);
        assertEquals(0, bookingService.confirmationCalls);
        assertEquals(1L, bookingService.previewStoreId);
        assertEquals("alpha-hotel", bookingService.previewSlug);
        assertNull(StoreContextHolder.getContext());
    }

    @Test
    void managementConfirmation_shouldBeStoreScopedPermissionProtectedAndBodyless()
            throws Exception {
        assertNotNull(IndependentSiteController.class.getAnnotation(StoreScoped.class));
        Method method = IndependentSiteController.class.getDeclaredMethod(
                "confirmSimulatedPayment",
                String.class
        );
        PostMapping mapping = method.getAnnotation(PostMapping.class);
        RequirePermission permission = method.getAnnotation(RequirePermission.class);

        assertNotNull(mapping);
        assertTrue(Arrays.asList(mapping.value())
                .contains("/current/payments/{paymentAttemptId}/simulate"));
        assertNotNull(permission);
        assertEquals(PermissionModule.CHANNEL, permission.module());
        assertEquals(PermissionAction.MANAGE_CHANNELS, permission.action());
        StoreScoped storeScoped = method.getAnnotation(StoreScoped.class);
        assertNotNull(storeScoped);
        assertFalse(storeScoped.warmupChannelPrices());
        assertEquals(1, method.getParameterCount());
    }

    @Test
    void managementConfirmation_shouldRejectPermissionBeforeWarmupOrConfirmWrite() throws Exception {
        RecordingPermissionService permissionService = new RecordingPermissionService(false);
        RecordingBookingService bookingService = new RecordingBookingService();
        RecordingWarmupService warmupService = new RecordingWarmupService();
        MockMvc mockMvc = managementMockMvc(permissionService, bookingService, warmupService);

        mockMvc.perform(confirmationRequest())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        assertEquals(1, permissionService.calls);
        assertEquals(0, warmupService.calls);
        assertEquals(0, bookingService.previewHoldCalls);
        assertEquals(0, bookingService.confirmationCalls);
        assertNull(StoreContextHolder.getContext());
    }

    @Test
    void managementConfirmation_shouldPreserveStoreContextForAuthorizedConfirmation()
            throws Exception {
        RecordingPermissionService permissionService = new RecordingPermissionService(true);
        RecordingBookingService bookingService = new RecordingBookingService();
        RecordingWarmupService warmupService = new RecordingWarmupService();
        MockMvc mockMvc = managementMockMvc(permissionService, bookingService, warmupService);

        mockMvc.perform(confirmationRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertEquals(1, permissionService.calls);
        assertEquals(0, warmupService.calls);
        assertEquals(0, bookingService.previewHoldCalls);
        assertEquals(1, bookingService.confirmationCalls);
        assertEquals(1L, bookingService.confirmationStoreId);
        assertEquals("payment-attempt-001", bookingService.paymentAttemptId);
        assertNull(StoreContextHolder.getContext());
    }

    @Test
    void managementSiteAndPageCrud_shouldBeStoreScopedAndPermissionProtected() throws Exception {
        assertNotNull(IndependentSiteController.class.getAnnotation(StoreScoped.class));

        assertPermission(
                IndependentSiteController.class.getDeclaredMethod("listSites"),
                PermissionAction.VIEW_CHANNELS
        );
        assertPermission(
                IndependentSiteController.class.getDeclaredMethod(
                        "createSite",
                        IndependentSiteDtos.SiteCreateRequest.class
                ),
                PermissionAction.MANAGE_CHANNELS
        );
        assertPermission(
                IndependentSiteController.class.getDeclaredMethod("getSite", Long.class),
                PermissionAction.VIEW_CHANNELS
        );
        assertPermission(
                IndependentSiteController.class.getDeclaredMethod(
                        "updateSite",
                        Long.class,
                        IndependentSiteDtos.SiteUpdateRequest.class
                ),
                PermissionAction.MANAGE_CHANNELS
        );
        assertPermission(
                IndependentSiteController.class.getDeclaredMethod("deleteSite", Long.class),
                PermissionAction.MANAGE_CHANNELS
        );
        assertPermission(
                IndependentSiteController.class.getDeclaredMethod("listPages", Long.class),
                PermissionAction.VIEW_CHANNELS
        );
        assertPermission(
                IndependentSiteController.class.getDeclaredMethod(
                        "createPage",
                        Long.class,
                        IndependentSiteDtos.PageCreateRequest.class
                ),
                PermissionAction.MANAGE_CHANNELS
        );
        assertPermission(
                IndependentSiteController.class.getDeclaredMethod("getPage", Long.class, Long.class),
                PermissionAction.VIEW_CHANNELS
        );
        assertPermission(
                IndependentSiteController.class.getDeclaredMethod(
                        "updatePage",
                        Long.class,
                        Long.class,
                        IndependentSiteDtos.PageUpdateRequest.class
                ),
                PermissionAction.MANAGE_CHANNELS
        );
        assertPermission(
                IndependentSiteController.class.getDeclaredMethod("deletePage", Long.class, Long.class),
                PermissionAction.MANAGE_CHANNELS
        );
        assertPermission(
                IndependentSiteController.class.getDeclaredMethod(
                        "publishPage",
                        Long.class,
                        Long.class,
                        IndependentSiteDtos.PublishPageDraftRequest.class
                ),
                PermissionAction.MANAGE_CHANNELS
        );
        assertPermission(
                IndependentSiteController.class.getDeclaredMethod(
                        "generatePageDraftForPage",
                        Long.class,
                        Long.class,
                        IndependentSiteDtos.PageDraftRequest.class
                ),
                PermissionAction.MANAGE_CHANNELS
        );

        Method generateRoomPages = IndependentSiteController.class.getDeclaredMethod(
                "generateRoomPages",
                Long.class
        );
        assertPermission(generateRoomPages, PermissionAction.MANAGE_CHANNELS);
        PostMapping generateRoomPagesMapping = generateRoomPages.getAnnotation(PostMapping.class);
        assertNotNull(generateRoomPagesMapping);
        assertTrue(Arrays.asList(generateRoomPagesMapping.value())
                .contains("/{id}/pages/generate-room-pages"));
    }

    @Test
    void aiEditEndpoints_shouldBeStoreScopedAndPermissionProtected() throws Exception {
        assertNotNull(IndependentSiteController.class.getAnnotation(StoreScoped.class));

        Method aiEdit = IndependentSiteController.class.getDeclaredMethod(
                "aiEditPage",
                Long.class,
                Long.class,
                IndependentSiteDtos.AiEditPageRequest.class
        );
        assertPermission(aiEdit, PermissionAction.MANAGE_CHANNELS);
        PostMapping aiEditMapping = aiEdit.getAnnotation(PostMapping.class);
        assertNotNull(aiEditMapping);
        assertTrue(Arrays.asList(aiEditMapping.value()).contains("/{id}/pages/{pageId}/ai-edit"));

        Method undo = IndependentSiteController.class.getDeclaredMethod(
                "undoAiEditPage",
                Long.class,
                Long.class
        );
        assertPermission(undo, PermissionAction.MANAGE_CHANNELS);
        PostMapping undoMapping = undo.getAnnotation(PostMapping.class);
        assertNotNull(undoMapping);
        assertTrue(Arrays.asList(undoMapping.value()).contains("/{id}/pages/{pageId}/ai-edit/undo"));
    }

    @Test
    void importPageFromUrlEndpoint_shouldBeStoreScopedAndPermissionProtected() throws Exception {
        assertNotNull(IndependentSiteController.class.getAnnotation(StoreScoped.class));

        Method importUrl = IndependentSiteController.class.getDeclaredMethod(
                "importPageFromUrl",
                Long.class,
                IndependentSiteDtos.ImportPageFromUrlRequest.class
        );
        assertPermission(importUrl, PermissionAction.MANAGE_CHANNELS);
        PostMapping importUrlMapping = importUrl.getAnnotation(PostMapping.class);
        assertNotNull(importUrlMapping);
        assertTrue(Arrays.asList(importUrlMapping.value()).contains("/{id}/pages/import-url"));
    }

    @Test
    void publicPagesEndpoint_shouldNotRequireStoreContext() throws Exception {
        assertNull(PublicIndependentSiteController.class.getAnnotation(StoreScoped.class));
        Method method = PublicIndependentSiteController.class.getDeclaredMethod(
                "getPage",
                String.class,
                jakarta.servlet.http.HttpServletRequest.class
        );
        assertNull(method.getAnnotation(StoreScoped.class));
        assertNull(method.getAnnotation(RequirePermission.class));
    }

    @Test
    void stripeIntentEndpoint_shouldNotRequireStoreContextOrPermissions() throws Exception {
        // 公开 Stripe 收卡入口：与下单/确认同级，绝不可挂门店/权限注解
        assertNull(PublicIndependentSiteController.class.getAnnotation(StoreScoped.class));
        Method method = PublicIndependentSiteController.class.getDeclaredMethod(
                "createStripeIntent",
                String.class,
                String.class,
                jakarta.servlet.http.HttpServletRequest.class
        );
        assertNull(method.getAnnotation(StoreScoped.class));
        assertNull(method.getAnnotation(RequirePermission.class));
        PostMapping mapping = method.getAnnotation(PostMapping.class);
        assertNotNull(mapping);
        assertTrue(Arrays.asList(mapping.value()).contains("/{slug}/payments/{paymentAttemptId}/intent"));
    }

    @Test
    void stripeSettingsEndpoints_shouldBeStoreScopedAndPermissionProtected() throws Exception {
        // 门店 Stripe 设置是管理端能力：必须挂在类级 @StoreScoped 之下且要求 CHANNEL 权限
        assertNotNull(IndependentSiteController.class.getAnnotation(StoreScoped.class));

        Method getMethod = IndependentSiteController.class.getDeclaredMethod("getStripeSettings");
        assertPermission(getMethod, PermissionAction.VIEW_CHANNELS);
        org.springframework.web.bind.annotation.GetMapping getMapping =
                getMethod.getAnnotation(org.springframework.web.bind.annotation.GetMapping.class);
        assertNotNull(getMapping);
        assertTrue(Arrays.asList(getMapping.value()).contains("/stripe-settings"));

        Method putMethod = IndependentSiteController.class.getDeclaredMethod(
                "updateStripeSettings",
                IndependentSiteDtos.StripeSettingsUpdateRequest.class
        );
        assertPermission(putMethod, PermissionAction.MANAGE_CHANNELS);
        org.springframework.web.bind.annotation.PutMapping putMapping =
                putMethod.getAnnotation(org.springframework.web.bind.annotation.PutMapping.class);
        assertNotNull(putMapping);
        assertTrue(Arrays.asList(putMapping.value()).contains("/stripe-settings"));
    }

    @Test
    void stripeSettingsGet_shouldRequireStoreContext() {
        StoreContextHolder.clear();
        IndependentSiteController controller = new IndependentSiteController(null, null, null, null, null);

        try {
            assertThrows(StoreAccessDeniedException.class, controller::getStripeSettings);
        } finally {
            StoreContextHolder.clear();
        }
    }

    @Test
    void stripeSettingsResponse_shouldNeverExposeSecretPlaintext() throws Exception {
        // 管理端 GET /stripe-settings：sk/whsec 明文绝不出现在响应（仅 configured 布尔与尾 4 位）
        java.util.Map<Long, server.demo.entity.IndependentSiteStripeSettings> rows = new java.util.HashMap<>();
        server.demo.util.AesGcmCrypto crypto = server.demo.util.AesGcmCrypto.fromBase64Key(ENCRYPTION_KEY);
        server.demo.entity.IndependentSiteStripeSettings row = new server.demo.entity.IndependentSiteStripeSettings();
        row.setId(1L);
        row.setStoreId(1L);
        row.setPublishableKey("pk_test_boundary_visible");
        row.setSecretKeyEncrypted(crypto.encrypt("sk_test_boundary_secret_abcd"));
        row.setWebhookSecretEncrypted(crypto.encrypt("whsec_boundary_secret_wxyz"));
        rows.put(1L, row);
        server.demo.repository.IndependentSiteStripeSettingsRepository settingsRepository =
                (server.demo.repository.IndependentSiteStripeSettingsRepository) Proxy.newProxyInstance(
                        server.demo.repository.IndependentSiteStripeSettingsRepository.class.getClassLoader(),
                        new Class<?>[]{server.demo.repository.IndependentSiteStripeSettingsRepository.class},
                        (proxy, method, args) -> switch (method.getName()) {
                            case "findByStoreId" -> Optional.ofNullable(rows.get(args[0]));
                            case "toString" -> "SettingsRepositoryProxy";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> throw new AssertionError("Unexpected repository method: " + method);
                        }
                );
        server.demo.service.IndependentSiteStripeSettingsService settingsService =
                new server.demo.service.IndependentSiteStripeSettingsService(settingsRepository, ENCRYPTION_KEY);
        IndependentSiteController controller = new IndependentSiteController(
                null,
                null,
                null,
                settingsService,
                null
        );
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new IndependentSiteApiExceptionHandler())
                .addInterceptors(new StoreContextInterceptor(
                        storeUserRepository(),
                        new RecordingWarmupService()
                ))
                .build();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/independent-sites/stripe-settings")
                        .requestAttr("userId", 7L)
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.configured").value(true))
                .andExpect(jsonPath("$.data.publishableKey").value("pk_test_boundary_visible"))
                .andExpect(jsonPath("$.data.secretKeyConfigured").value(true))
                .andExpect(jsonPath("$.data.secretKeyLast4").value("abcd"))
                .andExpect(jsonPath("$.data.webhookSecretConfigured").value(true))
                .andExpect(jsonPath("$.data.webhookSecretLast4").value("wxyz"))
                .andExpect(jsonPath("$.data.secretKey").doesNotExist())
                .andExpect(jsonPath("$.data.webhookSecret").doesNotExist())
                .andExpect(result -> assertFalse(
                        result.getResponse().getContentAsString().contains("sk_test_boundary_secret_abcd")
                ))
                .andExpect(result -> assertFalse(
                        result.getResponse().getContentAsString().contains("whsec_boundary_secret_wxyz")
                ));
    }

    private static final String ENCRYPTION_KEY =
            java.util.Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    void stripeWebhookController_shouldBePublicAndRegisteredInExceptionHandler() throws Exception {
        // webhook 验签是唯一鉴权：绝不可挂门店/权限注解，且必须登记到独立站异常 handler
        assertNull(PublicIndependentSiteStripeWebhookController.class.getAnnotation(StoreScoped.class));
        assertNull(PublicIndependentSiteStripeWebhookController.class.getAnnotation(RequirePermission.class));
        RequestMapping classMapping =
                PublicIndependentSiteStripeWebhookController.class.getAnnotation(RequestMapping.class);
        assertNotNull(classMapping);
        assertTrue(Arrays.asList(classMapping.value())
                .contains("/api/public/independent-sites/stripe"));

        Method method = PublicIndependentSiteStripeWebhookController.class.getDeclaredMethod(
                "handleWebhook",
                jakarta.servlet.http.HttpServletRequest.class
        );
        assertNull(method.getAnnotation(StoreScoped.class));
        assertNull(method.getAnnotation(RequirePermission.class));
        PostMapping mapping = method.getAnnotation(PostMapping.class);
        assertNotNull(mapping);
        assertTrue(Arrays.asList(mapping.value()).contains("/webhook"));

        RestControllerAdvice advice =
                IndependentSiteApiExceptionHandler.class.getAnnotation(RestControllerAdvice.class);
        assertNotNull(advice);
        assertTrue(Arrays.asList(advice.assignableTypes())
                .contains(PublicIndependentSiteStripeWebhookController.class));
    }

    @Test
    void stripeWebhookEndpoint_shouldPassRawBodyAndSignatureToService() throws Exception {
        RecordingStripeWebhookService webhookService = new RecordingStripeWebhookService();
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new PublicIndependentSiteStripeWebhookController(webhookService))
                .setControllerAdvice(new IndependentSiteApiExceptionHandler())
                .build();
        String payload = "{\"id\":\"evt_1\",\"type\":\"payment_intent.succeeded\"}";

        mockMvc.perform(post("/api/public/independent-sites/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "t=1,v1=abc")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertEquals(1, webhookService.calls);
        assertEquals(payload, webhookService.payload);
        assertEquals("t=1,v1=abc", webhookService.signature);
    }

    @Test
    void stripeWebhookEndpoint_shouldMapServiceExceptionToStatusAndCode() throws Exception {
        RecordingStripeWebhookService webhookService = new RecordingStripeWebhookService();
        webhookService.failure = new IndependentSiteServiceException(
                HttpStatus.BAD_REQUEST,
                "STRIPE_SIGNATURE_INVALID",
                "Stripe webhook 验签失败"
        );
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new PublicIndependentSiteStripeWebhookController(webhookService))
                .setControllerAdvice(new IndependentSiteApiExceptionHandler())
                .build();

        mockMvc.perform(post("/api/public/independent-sites/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "t=1,v1=bad")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.code").value("STRIPE_SIGNATURE_INVALID"));
    }

    private static void assertPermission(Method method, PermissionAction expected) {
        RequirePermission permission = method.getAnnotation(RequirePermission.class);
        assertNotNull(permission, method.getName() + " 缺少 @RequirePermission");
        assertEquals(PermissionModule.CHANNEL, permission.module());
        assertEquals(expected, permission.action());
    }

    private static Method previewHoldMethod() throws NoSuchMethodException {
        return IndependentSiteController.class.getDeclaredMethod(
                "createPreviewHold",
                String.class,
                IndependentSiteDtos.HoldRequest.class
        );
    }

    private static IndependentSiteDtos.HoldRequest holdRequest() {
        return new IndependentSiteDtos.HoldRequest(
                "idem-key-security",
                101L,
                LocalDate.of(2026, 7, 22),
                LocalDate.of(2026, 7, 23),
                1,
                1,
                0,
                new IndependentSiteDtos.Guest("Guest One", null, null, null)
        );
    }

    private static MockHttpServletRequestBuilder previewHoldRequest() throws Exception {
        return post("/api/v1/independent-sites/alpha-hotel/preview-holds")
                .requestAttr("userId", 7L)
                .header("X-Store-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().findAndRegisterModules().writeValueAsString(holdRequest()));
    }

    private static MockHttpServletRequestBuilder confirmationRequest() {
        return post(
                "/api/v1/independent-sites/current/payments/payment-attempt-001/simulate"
        )
                .requestAttr("userId", 7L)
                .header("X-Store-Id", "1");
    }

    private static MockMvc managementMockMvc(
            PermissionService permissionService,
            IndependentSiteBookingService bookingService,
            ChannelPriceWarmupService warmupService
    ) {
        StoreContextInterceptor storeContextInterceptor = new StoreContextInterceptor(
                storeUserRepository(),
                warmupService
        );
        IndependentSiteController controller = new IndependentSiteController(
                null,
                null,
                bookingService,
                null,
                permissionService
        );
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new IndependentSiteApiExceptionHandler())
                .addInterceptors(storeContextInterceptor)
                .build();
    }

    private static StoreUserRepository storeUserRepository() {
        StoreUser membership = new StoreUser();
        membership.setRole("member");
        return (StoreUserRepository) Proxy.newProxyInstance(
                StoreUserRepository.class.getClassLoader(),
                new Class<?>[]{StoreUserRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByStoreIdAndUserId" -> Optional.of(membership);
                    case "toString" -> "StoreUserRepositoryProxy";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new AssertionError("Unexpected repository method: " + method);
                }
        );
    }

    private static final class RecordingPermissionService extends PermissionService {
        private final boolean granted;
        private int calls;

        private RecordingPermissionService(boolean granted) {
            this.granted = granted;
        }

        @Override
        public boolean hasPermission(
                Long storeId,
                Long userId,
                PermissionModule module,
                PermissionAction action
        ) {
            calls++;
            assertEquals(1L, storeId);
            assertEquals(7L, userId);
            assertEquals(PermissionModule.CHANNEL, module);
            assertEquals(PermissionAction.MANAGE_CHANNELS, action);
            return granted;
        }
    }

    private static final class RecordingWarmupService extends ChannelPriceWarmupService {
        private int calls;

        private RecordingWarmupService() {
            super(null, null, null, null, Clock.systemUTC());
        }

        @Override
        public void warmupIfNeeded(Long storeId) {
            calls++;
        }
    }

    private static final class RecordingStripeWebhookService extends IndependentSiteStripeWebhookService {
        private int calls;
        private String payload;
        private String signature;
        private IndependentSiteServiceException failure;

        private RecordingStripeWebhookService() {
            super(null, null, null, null, null);
        }

        @Override
        public void handle(String payload, String signatureHeader) {
            calls++;
            this.payload = payload;
            this.signature = signatureHeader;
            if (failure != null) {
                throw failure;
            }
        }
    }

    private static final class RecordingBookingService extends IndependentSiteBookingService {
        private int previewHoldCalls;
        private int confirmationCalls;
        private Long previewStoreId;
        private String previewSlug;
        private Long confirmationStoreId;
        private String paymentAttemptId;

        private RecordingBookingService() {
            super(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    new ObjectMapper(),
                    Clock.systemUTC(),
                    null,
                    null
            );
        }

        @Override
        public IndependentSiteDtos.PaymentAttemptResponse createPreviewHold(
                Long storeId,
                String rawSlug,
                IndependentSiteDtos.HoldRequest request
        ) {
            previewHoldCalls++;
            previewStoreId = storeId;
            previewSlug = rawSlug;
            return null;
        }

        @Override
        public IndependentSiteDtos.PaymentAttemptResponse confirmSimulatedPayment(
                Long storeId,
                String publicReference
        ) {
            confirmationCalls++;
            confirmationStoreId = storeId;
            paymentAttemptId = publicReference;
            return null;
        }
    }
}
