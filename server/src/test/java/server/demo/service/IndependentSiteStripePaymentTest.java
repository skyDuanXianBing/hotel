package server.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.Stripe;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import server.demo.config.StripeConfig;
import server.demo.dto.IndependentSiteDtos;
import server.demo.entity.Channel;
import server.demo.entity.IndependentSite;
import server.demo.entity.IndependentSitePage;
import server.demo.entity.IndependentSiteStripeSettings;
import server.demo.entity.Payment;
import server.demo.entity.PaymentAttempt;
import server.demo.entity.PricePlan;
import server.demo.entity.Reservation;
import server.demo.entity.ReservationDailyPrice;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.entity.RoomTypePricePlan;
import server.demo.entity.Store;
import server.demo.entity.User;
import server.demo.enums.IndependentSitePageType;
import server.demo.enums.IndependentSitePaymentProvider;
import server.demo.enums.PaymentAttemptStatus;
import server.demo.enums.PriceAdjustmentType;
import server.demo.enums.ReservationStatus;
import server.demo.enums.RoomStatus;
import server.demo.repository.IndependentSitePageRepository;
import server.demo.repository.IndependentSiteRepository;
import server.demo.repository.IndependentSiteStripeSettingsRepository;
import server.demo.repository.PaymentAttemptRepository;
import server.demo.repository.PaymentRepository;
import server.demo.repository.ReservationDailyPriceRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomBlockoutRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.StoreRepository;
import server.demo.repository.UserRepository;
import server.demo.util.AesGcmCrypto;
import server.demo.util.StripeCurrencyAmounts;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Stripe 真实支付（门店级密钥）测试。
 * 密钥按门店存于 independent_site_stripe_settings（AES-GCM 加密），由
 * IndependentSiteStripeSettingsService 解密解析；HTTP 层用 JDK HttpServer mock Stripe API，
 * 客户端为真实 stripe-java SDK（StripeConfig.apiBase 指向 mock，断言 Authorization 头为门店 sk）；
 * webhook 验签用真实 Webhook.constructEvent + 门店 whsec，签名由测试自算 HMAC-SHA256 拼出。
 * 仓库为 JDK 动态代理假实现（不用 Mockito）。
 */
class IndependentSiteStripePaymentTest {

    private static final String ENCRYPTION_KEY = Base64.getEncoder().encodeToString(new byte[32]);

    private static final String SECRET_KEY = "sk_test_alpha_store";
    private static final String PUBLISHABLE_KEY = "pk_test_alpha_store";
    private static final String WEBHOOK_SECRET = "whsec_test_secret_alpha_0123456789";

    private static final String BETA_SECRET_KEY = "sk_test_beta_store";
    private static final String BETA_PUBLISHABLE_KEY = "pk_test_beta_store";
    private static final String BETA_WEBHOOK_SECRET = "whsec_test_secret_beta_0123456789";

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    // ------------------------------------------------------------------
    // 金额换算
    // ------------------------------------------------------------------

    @Test
    void stripeCurrencyAmounts_shouldConvertZeroAndTwoDecimalCurrencies() {
        assertEquals(11000L, StripeCurrencyAmounts.toMinorUnits(new BigDecimal("110.00"), "CNY"));
        assertEquals(5L, StripeCurrencyAmounts.toMinorUnits(new BigDecimal("0.05"), "USD"));
        assertEquals(999L, StripeCurrencyAmounts.toMinorUnits(new BigDecimal("9.99"), "eur"));
        // 零小数货币 ×1（大小写不敏感）
        assertEquals(11000L, StripeCurrencyAmounts.toMinorUnits(new BigDecimal("11000"), "JPY"));
        assertEquals(550L, StripeCurrencyAmounts.toMinorUnits(new BigDecimal("550.00"), "jpy"));
        assertEquals(3000L, StripeCurrencyAmounts.toMinorUnits(new BigDecimal("3000"), "KRW"));
        assertEquals(1200L, StripeCurrencyAmounts.toMinorUnits(new BigDecimal("1200"), "VND"));
        assertThrows(
                IllegalArgumentException.class,
                () -> StripeCurrencyAmounts.toMinorUnits(null, "CNY")
        );
    }

    // ------------------------------------------------------------------
    // createHold 按站点 provider
    // ------------------------------------------------------------------

    @Test
    void createPublicHold_stripeSite_shouldPersistStripeProviderWithoutSimulatedSwitch() {
        Fixture fixture = new Fixture("http://localhost:1");
        fixture.site.setPaymentProvider(IndependentSitePaymentProvider.STRIPE);
        fixture.site.setSimulatedPaymentEnabled(false);

        IndependentSiteDtos.PaymentAttemptResponse hold = fixture.createPublicHold("idem-stripe-hold-001");

        assertEquals(PaymentAttemptStatus.PENDING, hold.status());
        assertFalse(hold.simulated());
        assertEquals("STRIPE", hold.provider());
        PaymentAttempt attempt = fixture.attemptsByPublicReference.get(hold.paymentAttemptId());
        assertEquals(IndependentSitePaymentProvider.STRIPE, attempt.getProvider());
        assertEquals("STRIPE", fixture.reservations.get(0).getPaymentMethod());
        assertEquals(ReservationStatus.REQUESTED, fixture.reservations.get(0).getStatus());
        assertEquals(
                List.of(IndependentSiteReservationLifecycleService.Event.HOLD_CREATED),
                fixture.lifecycleService.events
        );
    }

    @Test
    void createPublicHold_stripeSiteWithoutStoreSettings_shouldRejectBeforeWrites() {
        Fixture fixture = new Fixture("http://localhost:1");
        fixture.site.setPaymentProvider(IndependentSitePaymentProvider.STRIPE);
        // 门店未配置 Stripe 密钥：422 且不落任何脏数据
        IndependentSiteBookingService unconfigured = fixture.buildBookingService(
                fixture.unconfiguredSettingsService()
        );

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> unconfigured.createPublicHold(
                        fixture.site.getSlug(),
                        fixture.holdRequest("idem-stripe-hold-002", "Guest Public")
                )
        );

        assertEquals("PAYMENT_PROVIDER_NOT_AVAILABLE", exception.getCode());
        assertEquals(422, exception.getStatus().value());
        assertEquals(0, fixture.quoteService.calculateCalls);
        assertTrue(fixture.reservations.isEmpty());
        assertTrue(fixture.attemptsByIdempotency.isEmpty());
    }

    @Test
    void createPublicHold_simulatedSiteWithSwitchOff_shouldStillReject() {
        Fixture fixture = new Fixture("http://localhost:1");
        fixture.site.setSimulatedPaymentEnabled(false);

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> fixture.createPublicHold("idem-sim-hold-003")
        );

        assertEquals("SIMULATED_PAYMENT_DISABLED", exception.getCode());
        assertTrue(fixture.reservations.isEmpty());
        assertTrue(fixture.attemptsByIdempotency.isEmpty());
    }

    // ------------------------------------------------------------------
    // intent 创建 / 复用（门店密钥）
    // ------------------------------------------------------------------

    @Test
    void createStripeIntent_shouldPostIntentWithMinorUnitsMetadataAndIdempotencyKey() throws Exception {
        AtomicReference<String> capturedMethodPath = new AtomicReference<>();
        AtomicReference<String> capturedAuth = new AtomicReference<>();
        AtomicReference<String> capturedIdempotency = new AtomicReference<>();
        AtomicReference<String> capturedBody = new AtomicReference<>();
        String apiBase = startStripeMock(exchange -> {
            capturedMethodPath.set(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
            capturedAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            capturedIdempotency.set(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
            capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respondJson(exchange, 200, paymentIntentJson(
                    "pi_test_123", "requires_payment_method", "pi_test_123_secret_abc"
            ));
        });

        Fixture fixture = new Fixture(apiBase);
        fixture.site.setPaymentProvider(IndependentSitePaymentProvider.STRIPE);
        IndependentSiteDtos.PaymentAttemptResponse hold = fixture.createPublicHold("idem-stripe-int-001");

        IndependentSiteDtos.StripeIntentResponse response =
                fixture.service.createStripeIntent(fixture.site.getSlug(), hold.paymentAttemptId());

        assertEquals("pi_test_123_secret_abc", response.clientSecret());
        assertEquals(PUBLISHABLE_KEY, response.publishableKey());
        assertEquals("PENDING", response.status());
        assertEquals("POST /v1/payment_intents", capturedMethodPath.get());
        assertEquals("Bearer " + SECRET_KEY, capturedAuth.get());
        assertEquals(hold.paymentAttemptId(), capturedIdempotency.get());
        Set<String> formPairs = formPairs(capturedBody.get());
        assertTrue(formPairs.contains("amount=11000"), capturedBody.get());
        assertTrue(formPairs.contains("currency=cny"), capturedBody.get());
        assertTrue(
                formPairs.contains("metadata[publicReference]=" + hold.paymentAttemptId()),
                capturedBody.get()
        );
        assertTrue(formPairs.contains("automatic_payment_methods[enabled]=true"), capturedBody.get());
        assertEquals(
                "pi_test_123",
                fixture.attemptsByPublicReference.get(hold.paymentAttemptId()).getProviderTransactionId()
        );
    }

    @Test
    void createStripeIntent_betaStore_shouldUseItsOwnStoreKeys() throws Exception {
        AtomicReference<String> capturedAuth = new AtomicReference<>();
        String apiBase = startStripeMock(exchange -> {
            capturedAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respondJson(exchange, 200, paymentIntentJson("pi_beta_1", "requires_payment_method", "sec_beta"));
        });

        Fixture fixture = new Fixture(apiBase, StoreProfile.beta());
        fixture.site.setPaymentProvider(IndependentSitePaymentProvider.STRIPE);
        IndependentSiteDtos.PaymentAttemptResponse hold = fixture.createPublicHold("idem-stripe-beta-001");

        IndependentSiteDtos.StripeIntentResponse response =
                fixture.service.createStripeIntent(fixture.site.getSlug(), hold.paymentAttemptId());

        // 多租户关键断言：打 Stripe 用的是门店 B 的 sk，回给前端的是门店 B 的 pk
        assertEquals("Bearer " + BETA_SECRET_KEY, capturedAuth.get());
        assertEquals(BETA_PUBLISHABLE_KEY, response.publishableKey());
        assertEquals("sec_beta", response.clientSecret());
    }

    @Test
    void createStripeIntent_shouldUseZeroDecimalAmountForJpy() throws Exception {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        String apiBase = startStripeMock(exchange -> {
            capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respondJson(exchange, 200, paymentIntentJson("pi_jpy_1", "requires_payment_method", "sec_jpy"));
        });

        Fixture fixture = new Fixture(apiBase);
        fixture.site.setPaymentProvider(IndependentSitePaymentProvider.STRIPE);
        IndependentSiteDtos.PaymentAttemptResponse hold = fixture.createPublicHold("idem-stripe-jpy-001");
        PaymentAttempt attempt = fixture.attemptsByPublicReference.get(hold.paymentAttemptId());
        attempt.setCurrencyCode("JPY");
        attempt.setAmount(new BigDecimal("11500"));

        IndependentSiteDtos.StripeIntentResponse response =
                fixture.service.createStripeIntent(fixture.site.getSlug(), hold.paymentAttemptId());

        assertEquals("sec_jpy", response.clientSecret());
        Set<String> formPairs = formPairs(capturedBody.get());
        assertTrue(formPairs.contains("amount=11500"), capturedBody.get());
        assertTrue(formPairs.contains("currency=jpy"), capturedBody.get());
    }

    @Test
    void createStripeIntent_shouldReuseExistingUsableIntent() throws Exception {
        AtomicInteger postCount = new AtomicInteger();
        AtomicReference<String> capturedMethodPath = new AtomicReference<>();
        String apiBase = startStripeMock(exchange -> {
            capturedMethodPath.set(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
            if ("POST".equals(exchange.getRequestMethod())) {
                postCount.incrementAndGet();
            }
            respondJson(exchange, 200, paymentIntentJson(
                    "pi_existing", "requires_payment_method", "pi_existing_secret_y"
            ));
        });

        Fixture fixture = new Fixture(apiBase);
        fixture.site.setPaymentProvider(IndependentSitePaymentProvider.STRIPE);
        IndependentSiteDtos.PaymentAttemptResponse hold = fixture.createPublicHold("idem-stripe-reuse-001");
        fixture.attemptsByPublicReference.get(hold.paymentAttemptId())
                .setProviderTransactionId("pi_existing");

        IndependentSiteDtos.StripeIntentResponse response =
                fixture.service.createStripeIntent(fixture.site.getSlug(), hold.paymentAttemptId());

        assertEquals("pi_existing_secret_y", response.clientSecret());
        assertEquals("PENDING", response.status());
        assertEquals("GET /v1/payment_intents/pi_existing", capturedMethodPath.get());
        assertEquals(0, postCount.get());
    }

    @Test
    void createStripeIntent_shouldSyncConfirmWhenExistingIntentAlreadySucceeded() throws Exception {
        String apiBase = startStripeMock(exchange ->
                respondJson(exchange, 200, paymentIntentJson("pi_done", "succeeded", null))
        );

        Fixture fixture = new Fixture(apiBase);
        fixture.site.setPaymentProvider(IndependentSitePaymentProvider.STRIPE);
        IndependentSiteDtos.PaymentAttemptResponse hold = fixture.createPublicHold("idem-stripe-sync-001");
        fixture.attemptsByPublicReference.get(hold.paymentAttemptId())
                .setProviderTransactionId("pi_done");

        IndependentSiteDtos.StripeIntentResponse response =
                fixture.service.createStripeIntent(fixture.site.getSlug(), hold.paymentAttemptId());

        assertNull(response.clientSecret());
        assertEquals("SUCCEEDED", response.status());
        PaymentAttempt attempt = fixture.attemptsByPublicReference.get(hold.paymentAttemptId());
        assertEquals(PaymentAttemptStatus.SUCCEEDED, attempt.getStatus());
        assertEquals("pi_done", attempt.getProviderTransactionId());
        assertEquals(ReservationStatus.CONFIRMED, fixture.reservations.get(0).getStatus());
        assertEquals(1, fixture.payments.size());
        assertEquals("STRIPE", fixture.payments.get(0).getPaymentMethod());
        assertEquals(
                List.of(
                        IndependentSiteReservationLifecycleService.Event.HOLD_CREATED,
                        IndependentSiteReservationLifecycleService.Event.PAYMENT_SUCCEEDED
                ),
                fixture.lifecycleService.events
        );
    }

    @Test
    void createStripeIntent_shouldRecreateWhenExistingIntentCanceled() throws Exception {
        String apiBase = startStripeMock(exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                respondJson(exchange, 200, paymentIntentJson("pi_new", "requires_payment_method", "sec_new"));
            } else {
                respondJson(exchange, 200, paymentIntentJson("pi_old", "canceled", null));
            }
        });

        Fixture fixture = new Fixture(apiBase);
        fixture.site.setPaymentProvider(IndependentSitePaymentProvider.STRIPE);
        IndependentSiteDtos.PaymentAttemptResponse hold = fixture.createPublicHold("idem-stripe-cancel-001");
        fixture.attemptsByPublicReference.get(hold.paymentAttemptId())
                .setProviderTransactionId("pi_old");

        IndependentSiteDtos.StripeIntentResponse response =
                fixture.service.createStripeIntent(fixture.site.getSlug(), hold.paymentAttemptId());

        assertEquals("sec_new", response.clientSecret());
        assertEquals(
                "pi_new",
                fixture.attemptsByPublicReference.get(hold.paymentAttemptId()).getProviderTransactionId()
        );
    }

    @Test
    void createStripeIntent_shouldRejectNonStripeAttempt() {
        Fixture fixture = new Fixture("http://localhost:1");
        IndependentSiteDtos.PaymentAttemptResponse hold = fixture.createPublicHold("idem-sim-int-001");
        assertEquals("SIMULATED", hold.provider());

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> fixture.service.createStripeIntent(fixture.site.getSlug(), hold.paymentAttemptId())
        );

        assertEquals("PAYMENT_PROVIDER_NOT_AVAILABLE", exception.getCode());
        assertEquals(422, exception.getStatus().value());
    }

    @Test
    void createStripeIntent_shouldReturnStatusWithoutClientSecretWhenNotPending() {
        Fixture fixture = new Fixture("http://localhost:1");
        fixture.site.setPaymentProvider(IndependentSitePaymentProvider.STRIPE);
        IndependentSiteDtos.PaymentAttemptResponse hold = fixture.createPublicHold("idem-stripe-np-001");
        fixture.attemptsByPublicReference.get(hold.paymentAttemptId())
                .setStatus(PaymentAttemptStatus.SUCCEEDED);

        IndependentSiteDtos.StripeIntentResponse response =
                fixture.service.createStripeIntent(fixture.site.getSlug(), hold.paymentAttemptId());

        assertEquals("SUCCEEDED", response.status());
        assertNull(response.clientSecret());
        assertEquals(PUBLISHABLE_KEY, response.publishableKey());
    }

    @Test
    void createStripeIntent_shouldDowngradePastDueHoldToExpired() {
        Fixture fixture = new Fixture("http://localhost:1");
        fixture.site.setPaymentProvider(IndependentSitePaymentProvider.STRIPE);
        IndependentSiteDtos.PaymentAttemptResponse hold = fixture.createPublicHold("idem-stripe-exp-001");
        fixture.attemptsByPublicReference.get(hold.paymentAttemptId())
                .setExpiresAt(LocalDateTime.of(2026, 7, 19, 23, 59));

        IndependentSiteDtos.StripeIntentResponse response =
                fixture.service.createStripeIntent(fixture.site.getSlug(), hold.paymentAttemptId());

        assertEquals("EXPIRED", response.status());
        assertNull(response.clientSecret());
        assertEquals(PaymentAttemptStatus.EXPIRED,
                fixture.attemptsByPublicReference.get(hold.paymentAttemptId()).getStatus());
        assertEquals(ReservationStatus.CANCELLED, fixture.reservations.get(0).getStatus());
        assertTrue(fixture.payments.isEmpty());
        assertEquals(
                IndependentSiteReservationLifecycleService.Event.PAYMENT_RELEASED,
                fixture.lifecycleService.events.get(1)
        );
    }

    @Test
    void createStripeIntent_shouldRejectWhenStoreNotConfigured() {
        Fixture fixture = new Fixture("http://localhost:1");
        fixture.site.setPaymentProvider(IndependentSitePaymentProvider.STRIPE);
        IndependentSiteDtos.PaymentAttemptResponse hold = fixture.createPublicHold("idem-stripe-nc-001");
        // 同一支付尝试，换一个读不到门店密钥的实例：422
        IndependentSiteBookingService unconfigured = fixture.buildBookingService(
                fixture.unconfiguredSettingsService()
        );

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> unconfigured.createStripeIntent(fixture.site.getSlug(), hold.paymentAttemptId())
        );

        assertEquals("PAYMENT_PROVIDER_NOT_AVAILABLE", exception.getCode());
        assertEquals(422, exception.getStatus().value());
    }

    @Test
    void createStripeIntent_shouldMapStripeApiFailureTo503() throws Exception {
        String apiBase = startStripeMock(exchange ->
                respondJson(exchange, 500, "{\"error\":{\"message\":\"boom\"}}")
        );

        Fixture fixture = new Fixture(apiBase);
        fixture.site.setPaymentProvider(IndependentSitePaymentProvider.STRIPE);
        IndependentSiteDtos.PaymentAttemptResponse hold = fixture.createPublicHold("idem-stripe-500-001");

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> fixture.service.createStripeIntent(fixture.site.getSlug(), hold.paymentAttemptId())
        );

        assertEquals("STRIPE_API_FAILED", exception.getCode());
        assertEquals(503, exception.getStatus().value());
    }

    // ------------------------------------------------------------------
    // webhook 验签与幂等确认（门店 whsec 路由）
    // ------------------------------------------------------------------

    @Test
    void webhookSucceeded_shouldConfirmThroughSharedPipeline() {
        Fixture fixture = new Fixture("http://localhost:1");
        fixture.site.setPaymentProvider(IndependentSitePaymentProvider.STRIPE);
        IndependentSiteDtos.PaymentAttemptResponse hold = fixture.createPublicHold("idem-wh-001");
        String payload = eventJson(
                "evt_succeeded_1",
                "payment_intent.succeeded",
                "pi_wh_123",
                "succeeded",
                hold.paymentAttemptId(),
                null
        );

        fixture.webhookService.handle(payload, signatureHeader(payload, WEBHOOK_SECRET));

        PaymentAttempt attempt = fixture.attemptsByPublicReference.get(hold.paymentAttemptId());
        assertEquals(PaymentAttemptStatus.SUCCEEDED, attempt.getStatus());
        assertEquals("pi_wh_123", attempt.getProviderTransactionId());
        assertNull(attempt.getFailureReason());
        Reservation reservation = fixture.reservations.get(0);
        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        assertEquals(new BigDecimal("110.00"), reservation.getPaidAmount());
        assertEquals(1, fixture.payments.size());
        Payment payment = fixture.payments.get(0);
        assertEquals("STRIPE", payment.getPaymentMethod());
        assertEquals(new BigDecimal("110.00"), payment.getAmount());
        assertEquals("独立站Stripe支付 " + hold.paymentAttemptId(), payment.getRemark());
        assertEquals("独立站Stripe支付", payment.getCreatedBy());
        assertEquals(LocalDate.of(2026, 7, 20), payment.getDate());
        assertEquals(
                List.of(
                        IndependentSiteReservationLifecycleService.Event.HOLD_CREATED,
                        IndependentSiteReservationLifecycleService.Event.PAYMENT_SUCCEEDED
                ),
                fixture.lifecycleService.events
        );
    }

    @Test
    void webhookSucceeded_shouldBeIdempotentOnRedelivery() {
        Fixture fixture = new Fixture("http://localhost:1");
        fixture.site.setPaymentProvider(IndependentSitePaymentProvider.STRIPE);
        IndependentSiteDtos.PaymentAttemptResponse hold = fixture.createPublicHold("idem-wh-002");
        String payload = eventJson(
                "evt_succeeded_2",
                "payment_intent.succeeded",
                "pi_wh_456",
                "succeeded",
                hold.paymentAttemptId(),
                null
        );

        fixture.webhookService.handle(payload, signatureHeader(payload, WEBHOOK_SECRET));
        fixture.webhookService.handle(payload, signatureHeader(payload, WEBHOOK_SECRET));

        assertEquals(PaymentAttemptStatus.SUCCEEDED,
                fixture.attemptsByPublicReference.get(hold.paymentAttemptId()).getStatus());
        assertEquals(1, fixture.payments.size());
        assertEquals(1, fixture.reservations.size());
        assertEquals(2, fixture.lifecycleService.events.size());
    }

    @Test
    void webhookSucceeded_shouldNotReviveAlreadyExpiredAttempt() {
        Fixture fixture = new Fixture("http://localhost:1");
        fixture.site.setPaymentProvider(IndependentSitePaymentProvider.STRIPE);
        IndependentSiteDtos.PaymentAttemptResponse hold = fixture.createPublicHold("idem-wh-003");
        PaymentAttempt attempt = fixture.attemptsByPublicReference.get(hold.paymentAttemptId());
        attempt.setExpiresAt(LocalDateTime.of(2026, 7, 19, 23, 59));
        fixture.service.expirePaymentAttempt(hold.paymentAttemptId());
        assertEquals(PaymentAttemptStatus.EXPIRED, attempt.getStatus());

        String payload = eventJson(
                "evt_succeeded_3",
                "payment_intent.succeeded",
                "pi_late",
                "succeeded",
                hold.paymentAttemptId(),
                null
        );
        fixture.webhookService.handle(payload, signatureHeader(payload, WEBHOOK_SECRET));

        assertEquals(PaymentAttemptStatus.EXPIRED, attempt.getStatus());
        assertEquals(ReservationStatus.CANCELLED, fixture.reservations.get(0).getStatus());
        assertTrue(fixture.payments.isEmpty());
        assertEquals(2, fixture.lifecycleService.events.size());
    }

    @Test
    void webhookSucceeded_shouldDowngradePastDueAttemptToExpired() {
        Fixture fixture = new Fixture("http://localhost:1");
        fixture.site.setPaymentProvider(IndependentSitePaymentProvider.STRIPE);
        IndependentSiteDtos.PaymentAttemptResponse hold = fixture.createPublicHold("idem-wh-004");
        fixture.attemptsByPublicReference.get(hold.paymentAttemptId())
                .setExpiresAt(LocalDateTime.of(2026, 7, 19, 23, 59));

        String payload = eventJson(
                "evt_succeeded_4",
                "payment_intent.succeeded",
                "pi_late_2",
                "succeeded",
                hold.paymentAttemptId(),
                null
        );
        fixture.webhookService.handle(payload, signatureHeader(payload, WEBHOOK_SECRET));

        PaymentAttempt attempt = fixture.attemptsByPublicReference.get(hold.paymentAttemptId());
        assertEquals(PaymentAttemptStatus.EXPIRED, attempt.getStatus());
        assertEquals(ReservationStatus.CANCELLED, fixture.reservations.get(0).getStatus());
        assertTrue(fixture.payments.isEmpty());
        assertEquals(
                IndependentSiteReservationLifecycleService.Event.PAYMENT_RELEASED,
                fixture.lifecycleService.events.get(1)
        );
    }

    @Test
    void webhookPaymentFailed_shouldFailAttemptAndCancelReservationsOnlyWhenIntentCanceled() throws Exception {
        AtomicReference<String> capturedMethodPath = new AtomicReference<>();
        String apiBase = startStripeMock(exchange -> {
            capturedMethodPath.set(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
            respondJson(exchange, 200, paymentIntentJson("pi_failed_1", "canceled", null));
        });

        Fixture fixture = new Fixture(apiBase);
        fixture.site.setPaymentProvider(IndependentSitePaymentProvider.STRIPE);
        IndependentSiteDtos.PaymentAttemptResponse hold = fixture.createPublicHold("idem-wh-005");
        // 事件内快照仍是可重试状态，retrieve 到 canceled 终态才允许释放保留
        String payload = eventJson(
                "evt_failed_1",
                "payment_intent.payment_failed",
                "pi_failed_1",
                "requires_payment_method",
                hold.paymentAttemptId(),
                "Your card was declined."
        );

        fixture.webhookService.handle(payload, signatureHeader(payload, WEBHOOK_SECRET));

        assertEquals("GET /v1/payment_intents/pi_failed_1", capturedMethodPath.get());
        PaymentAttempt attempt = fixture.attemptsByPublicReference.get(hold.paymentAttemptId());
        assertEquals(PaymentAttemptStatus.FAILED, attempt.getStatus());
        assertEquals("Stripe 支付失败：Your card was declined.", attempt.getFailureReason());
        assertEquals(ReservationStatus.CANCELLED, fixture.reservations.get(0).getStatus());
        assertTrue(fixture.payments.isEmpty());
        assertEquals(
                IndependentSiteReservationLifecycleService.Event.PAYMENT_RELEASED,
                fixture.lifecycleService.events.get(1)
        );

        // 重复投递安全：仍 FAILED，不重复释放
        fixture.webhookService.handle(payload, signatureHeader(payload, WEBHOOK_SECRET));
        assertEquals(PaymentAttemptStatus.FAILED, attempt.getStatus());
        assertEquals(2, fixture.lifecycleService.events.size());
    }

    @Test
    void webhookPaymentFailed_shouldKeepPendingWhenIntentStillRetryable() throws Exception {
        AtomicReference<String> capturedMethodPath = new AtomicReference<>();
        String apiBase = startStripeMock(exchange -> {
            capturedMethodPath.set(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
            respondJson(exchange, 200, paymentIntentJson("pi_retry_1", "requires_payment_method", "sec_retry"));
        });

        Fixture fixture = new Fixture(apiBase);
        fixture.site.setPaymentProvider(IndependentSitePaymentProvider.STRIPE);
        IndependentSiteDtos.PaymentAttemptResponse hold = fixture.createPublicHold("idem-wh-005b");
        String payload = eventJson(
                "evt_failed_retry",
                "payment_intent.payment_failed",
                "pi_retry_1",
                "requires_payment_method",
                hold.paymentAttemptId(),
                "Your card was declined."
        );

        fixture.webhookService.handle(payload, signatureHeader(payload, WEBHOOK_SECRET));

        assertEquals("GET /v1/payment_intents/pi_retry_1", capturedMethodPath.get());
        PaymentAttempt attempt = fixture.attemptsByPublicReference.get(hold.paymentAttemptId());
        assertEquals(PaymentAttemptStatus.PENDING, attempt.getStatus());
        assertNull(attempt.getFailureReason());
        assertNull(attempt.getCompletedAt());
        assertEquals(ReservationStatus.REQUESTED, fixture.reservations.get(0).getStatus());
        assertTrue(fixture.payments.isEmpty());
        assertEquals(1, fixture.lifecycleService.events.size());
    }

    @Test
    void webhookPaymentFailed_shouldKeepPendingWhenRetrieveFails() {
        // localhost:1 不可达：retrieve 抛 StripeException，保守保持 PENDING，交给过期调度器兜底
        Fixture fixture = new Fixture("http://localhost:1");
        fixture.site.setPaymentProvider(IndependentSitePaymentProvider.STRIPE);
        IndependentSiteDtos.PaymentAttemptResponse hold = fixture.createPublicHold("idem-wh-005c");
        String payload = eventJson(
                "evt_failed_unreachable",
                "payment_intent.payment_failed",
                "pi_unreachable",
                "requires_payment_method",
                hold.paymentAttemptId(),
                "Your card was declined."
        );

        fixture.webhookService.handle(payload, signatureHeader(payload, WEBHOOK_SECRET));

        PaymentAttempt attempt = fixture.attemptsByPublicReference.get(hold.paymentAttemptId());
        assertEquals(PaymentAttemptStatus.PENDING, attempt.getStatus());
        assertNull(attempt.getFailureReason());
        assertEquals(ReservationStatus.REQUESTED, fixture.reservations.get(0).getStatus());
        assertTrue(fixture.payments.isEmpty());
        assertEquals(1, fixture.lifecycleService.events.size());
    }

    @Test
    void webhookTamperedPayload_shouldRejectSignatureWithoutSideEffects() {
        Fixture fixture = new Fixture("http://localhost:1");
        fixture.site.setPaymentProvider(IndependentSitePaymentProvider.STRIPE);
        IndependentSiteDtos.PaymentAttemptResponse hold = fixture.createPublicHold("idem-wh-006");
        String payload = eventJson(
                "evt_tampered",
                "payment_intent.succeeded",
                "pi_tampered",
                "succeeded",
                hold.paymentAttemptId(),
                null
        );
        String signature = signatureHeader(payload, WEBHOOK_SECRET);
        String tampered = payload.replace("11000", "10");

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> fixture.webhookService.handle(tampered, signature)
        );

        assertEquals("STRIPE_SIGNATURE_INVALID", exception.getCode());
        assertEquals(400, exception.getStatus().value());
        assertEquals(PaymentAttemptStatus.PENDING,
                fixture.attemptsByPublicReference.get(hold.paymentAttemptId()).getStatus());
        assertEquals(ReservationStatus.REQUESTED, fixture.reservations.get(0).getStatus());
        assertTrue(fixture.payments.isEmpty());
        assertEquals(1, fixture.lifecycleService.events.size());
    }

    @Test
    void webhookWrongSecret_shouldRejectSignatureWithoutSideEffects() {
        Fixture fixture = new Fixture("http://localhost:1");
        fixture.site.setPaymentProvider(IndependentSitePaymentProvider.STRIPE);
        IndependentSiteDtos.PaymentAttemptResponse hold = fixture.createPublicHold("idem-wh-007");
        String payload = eventJson(
                "evt_wrong_secret",
                "payment_intent.succeeded",
                "pi_wrong",
                "succeeded",
                hold.paymentAttemptId(),
                null
        );

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> fixture.webhookService.handle(payload, signatureHeader(payload, "whsec_other"))
        );

        assertEquals("STRIPE_SIGNATURE_INVALID", exception.getCode());
        assertEquals(400, exception.getStatus().value());
        assertEquals(PaymentAttemptStatus.PENDING,
                fixture.attemptsByPublicReference.get(hold.paymentAttemptId()).getStatus());
        assertTrue(fixture.payments.isEmpty());
        assertEquals(1, fixture.lifecycleService.events.size());
    }

    @Test
    void webhookStoreWithoutWebhookSecret_shouldReturn400WithoutSideEffects() {
        Fixture fixture = new Fixture("http://localhost:1");
        fixture.site.setPaymentProvider(IndependentSitePaymentProvider.STRIPE);
        IndependentSiteDtos.PaymentAttemptResponse hold = fixture.createPublicHold("idem-wh-nc-001");
        String payload = eventJson(
                "evt_no_whsec",
                "payment_intent.succeeded",
                "pi_no_whsec",
                "succeeded",
                hold.paymentAttemptId(),
                null
        );
        // attempt 找得到，但门店设置缺失（无 whsec 可验签）→ 400，绝不按信任处理
        IndependentSiteStripeWebhookService unconfigured = new IndependentSiteStripeWebhookService(
                fixture.unconfiguredSettingsService(),
                fixture.service,
                repository(PaymentAttemptRepository.class, fixture::handlePaymentAttemptRepository),
                fixture.stripeConfig,
                new ObjectMapper().findAndRegisterModules()
        );

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> unconfigured.handle(payload, signatureHeader(payload, WEBHOOK_SECRET))
        );

        assertEquals("STRIPE_WEBHOOK_NOT_CONFIGURED", exception.getCode());
        assertEquals(400, exception.getStatus().value());
        assertEquals(PaymentAttemptStatus.PENDING,
                fixture.attemptsByPublicReference.get(hold.paymentAttemptId()).getStatus());
        assertTrue(fixture.payments.isEmpty());
    }

    @Test
    void webhookMissingOrUnknownReference_shouldReturn400WithoutSideEffects() {
        Fixture fixture = new Fixture("http://localhost:1");
        fixture.site.setPaymentProvider(IndependentSitePaymentProvider.STRIPE);
        IndependentSiteDtos.PaymentAttemptResponse hold = fixture.createPublicHold("idem-wh-008");

        // 无法解析的 payload
        IndependentSiteServiceException invalidPayload = assertThrows(
                IndependentSiteServiceException.class,
                () -> fixture.webhookService.handle("not-json", "t=1,v1=abc")
        );
        assertEquals("STRIPE_PAYLOAD_INVALID", invalidPayload.getCode());
        assertEquals(400, invalidPayload.getStatus().value());

        // 无 metadata.publicReference：即使签名可算，也找不到路由目标
        String noReference = eventJson(
                "evt_no_ref", "payment_intent.succeeded", "pi_no_ref", "succeeded", null, null
        );
        IndependentSiteServiceException missing = assertThrows(
                IndependentSiteServiceException.class,
                () -> fixture.webhookService.handle(noReference, signatureHeader(noReference, WEBHOOK_SECRET))
        );
        assertEquals("STRIPE_REFERENCE_MISSING", missing.getCode());
        assertEquals(400, missing.getStatus().value());

        // publicReference 不属于任何支付尝试
        String unknownReference = eventJson(
                "evt_unknown_ref", "payment_intent.succeeded", "pi_unknown", "succeeded",
                "00000000-0000-0000-0000-000000000000", null
        );
        IndependentSiteServiceException unknown = assertThrows(
                IndependentSiteServiceException.class,
                () -> fixture.webhookService.handle(
                        unknownReference,
                        signatureHeader(unknownReference, WEBHOOK_SECRET)
                )
        );
        assertEquals("STRIPE_REFERENCE_UNKNOWN", unknown.getCode());
        assertEquals(400, unknown.getStatus().value());

        // 全程无任何状态副作用
        assertEquals(PaymentAttemptStatus.PENDING,
                fixture.attemptsByPublicReference.get(hold.paymentAttemptId()).getStatus());
        assertEquals(ReservationStatus.REQUESTED, fixture.reservations.get(0).getStatus());
        assertTrue(fixture.payments.isEmpty());
        assertEquals(1, fixture.lifecycleService.events.size());
    }

    @Test
    void webhookUnknownEventType_shouldBeIgnoredAfterSignatureVerified() {
        Fixture fixture = new Fixture("http://localhost:1");
        fixture.site.setPaymentProvider(IndependentSitePaymentProvider.STRIPE);
        IndependentSiteDtos.PaymentAttemptResponse hold = fixture.createPublicHold("idem-wh-009");

        // 未订阅处理的事件类型：走完路由与验签后忽略，正常返回
        String unknownType = eventJson(
                "evt_other", "payment_intent.created", "pi_other", "requires_payment_method",
                hold.paymentAttemptId(), null
        );
        fixture.webhookService.handle(unknownType, signatureHeader(unknownType, WEBHOOK_SECRET));

        assertEquals(PaymentAttemptStatus.PENDING,
                fixture.attemptsByPublicReference.get(hold.paymentAttemptId()).getStatus());
        assertEquals(ReservationStatus.REQUESTED, fixture.reservations.get(0).getStatus());
        assertTrue(fixture.payments.isEmpty());
        assertEquals(1, fixture.lifecycleService.events.size());
    }

    @Test
    void webhook_shouldRouteByPublicReferenceToCorrectStoreWebhookSecret() {
        // 双门店不同 whsec：同一 webhook 端点按 metadata.publicReference 路由到正确门店验签
        Fixture alpha = new Fixture("http://localhost:1", StoreProfile.alpha());
        alpha.site.setPaymentProvider(IndependentSitePaymentProvider.STRIPE);
        IndependentSiteDtos.PaymentAttemptResponse alphaHold = alpha.createPublicHold("idem-wh-alpha-001");

        Fixture beta = new Fixture("http://localhost:1", StoreProfile.beta());
        beta.site.setPaymentProvider(IndependentSitePaymentProvider.STRIPE);
        IndependentSiteDtos.PaymentAttemptResponse betaHold = beta.createPublicHold("idem-wh-beta-001");

        IndependentSiteStripeWebhookService composite = compositeWebhookService(alpha, beta);

        // 错签（用门店 B 的 whsec 签门店 A 的事件）→ 400，且两边都无副作用
        String alphaPayload = eventJson(
                "evt_route_alpha",
                "payment_intent.succeeded",
                "pi_route_alpha",
                "succeeded",
                alphaHold.paymentAttemptId(),
                null
        );
        IndependentSiteServiceException wrongStoreSignature = assertThrows(
                IndependentSiteServiceException.class,
                () -> composite.handle(alphaPayload, signatureHeader(alphaPayload, BETA_WEBHOOK_SECRET))
        );
        assertEquals("STRIPE_SIGNATURE_INVALID", wrongStoreSignature.getCode());
        assertEquals(PaymentAttemptStatus.PENDING,
                alpha.attemptsByPublicReference.get(alphaHold.paymentAttemptId()).getStatus());
        assertEquals(PaymentAttemptStatus.PENDING,
                beta.attemptsByPublicReference.get(betaHold.paymentAttemptId()).getStatus());
        assertTrue(alpha.payments.isEmpty());
        assertTrue(beta.payments.isEmpty());

        // 正确签名 → 只有门店 A 的支付尝试被确认，门店 B 不受影响
        composite.handle(alphaPayload, signatureHeader(alphaPayload, WEBHOOK_SECRET));
        assertEquals(PaymentAttemptStatus.SUCCEEDED,
                alpha.attemptsByPublicReference.get(alphaHold.paymentAttemptId()).getStatus());
        assertEquals(ReservationStatus.CONFIRMED, alpha.reservations.get(0).getStatus());
        assertEquals(1, alpha.payments.size());
        assertEquals(PaymentAttemptStatus.PENDING,
                beta.attemptsByPublicReference.get(betaHold.paymentAttemptId()).getStatus());
        assertEquals(ReservationStatus.REQUESTED, beta.reservations.get(0).getStatus());
        assertTrue(beta.payments.isEmpty());

        // 反向：门店 B 的事件用门店 B 的 whsec 验签通过；用门店 A 的拒绝
        String betaPayload = eventJson(
                "evt_route_beta",
                "payment_intent.succeeded",
                "pi_route_beta",
                "succeeded",
                betaHold.paymentAttemptId(),
                null
        );
        assertThrows(
                IndependentSiteServiceException.class,
                () -> composite.handle(betaPayload, signatureHeader(betaPayload, WEBHOOK_SECRET))
        );
        composite.handle(betaPayload, signatureHeader(betaPayload, BETA_WEBHOOK_SECRET));
        assertEquals(PaymentAttemptStatus.SUCCEEDED,
                beta.attemptsByPublicReference.get(betaHold.paymentAttemptId()).getStatus());
        assertEquals(1, beta.payments.size());
    }

    /** 跨两个门店 fixture 的组合 webhook 服务：设置/尝试按 storeId、publicReference 路由。 */
    private static IndependentSiteStripeWebhookService compositeWebhookService(Fixture alpha, Fixture beta) {
        IndependentSiteStripeSettingsService compositeSettings =
                new IndependentSiteStripeSettingsService(null, "") {
                    @Override
                    public Optional<ResolvedStripeKeys> resolveForStore(Long storeId) {
                        if (Objects.equals(storeId, alpha.store.getId())) {
                            return alpha.stripeSettingsService.resolveForStore(storeId);
                        }
                        if (Objects.equals(storeId, beta.store.getId())) {
                            return beta.stripeSettingsService.resolveForStore(storeId);
                        }
                        return Optional.empty();
                    }
                };
        IndependentSiteBookingService compositeBooking = new IndependentSiteBookingService(
                null, null, null, null, null, null, null, null, null,
                null, null, null, new ObjectMapper(), Clock.systemUTC(), null, null
        ) {
            @Override
            public void confirmStripePayment(String publicReference, String paymentIntentId) {
                route(publicReference).confirmStripePayment(publicReference, paymentIntentId);
            }

            @Override
            public void failStripePayment(String publicReference, String failureReason) {
                route(publicReference).failStripePayment(publicReference, failureReason);
            }

            private IndependentSiteBookingService route(String publicReference) {
                if (alpha.attemptsByPublicReference.containsKey(publicReference)) {
                    return alpha.service;
                }
                if (beta.attemptsByPublicReference.containsKey(publicReference)) {
                    return beta.service;
                }
                throw new AssertionError("Unexpected reference: " + publicReference);
            }
        };
        PaymentAttemptRepository compositeAttempts = repository(
                PaymentAttemptRepository.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByPublicReference" -> {
                        PaymentAttempt attempt = alpha.attemptsByPublicReference.get(args[0]);
                        yield Optional.ofNullable(
                                attempt != null ? attempt : beta.attemptsByPublicReference.get(args[0])
                        );
                    }
                    default -> objectMethodOrFail(proxy, method, args);
                }
        );
        return new IndependentSiteStripeWebhookService(
                compositeSettings,
                compositeBooking,
                compositeAttempts,
                new StripeConfig("http://localhost:1"),
                new ObjectMapper().findAndRegisterModules()
        );
    }

    // ------------------------------------------------------------------
    // 测试基础设施
    // ------------------------------------------------------------------

    private String startStripeMock(HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", handler);
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static void respondJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static String paymentIntentJson(String id, String status, String clientSecret) {
        return "{\"id\":\"" + id + "\",\"object\":\"payment_intent\","
                + "\"amount\":11000,\"currency\":\"cny\",\"status\":\"" + status + "\","
                + "\"client_secret\":" + (clientSecret == null ? "null" : "\"" + clientSecret + "\"")
                + ",\"livemode\":false}";
    }

    private static Set<String> formPairs(String formBody) {
        return new HashSet<>(Arrays.asList(
                URLDecoder.decode(formBody, StandardCharsets.UTF_8).split("&")
        ));
    }

    private static String eventJson(
            String eventId,
            String type,
            String paymentIntentId,
            String intentStatus,
            String publicReference,
            String lastErrorMessage
    ) {
        String metadata = publicReference == null
                ? "{}"
                : "{\"publicReference\":\"" + publicReference + "\"}";
        String lastError = lastErrorMessage == null
                ? "null"
                : "{\"message\":\"" + lastErrorMessage + "\"}";
        return "{\"id\":\"" + eventId + "\",\"object\":\"event\",\"type\":\"" + type + "\","
                + "\"api_version\":\"" + Stripe.API_VERSION + "\",\"livemode\":false,"
                + "\"created\":1784900000,\"pending_webhooks\":1,"
                + "\"request\":{\"id\":null,\"idempotency_key\":null},"
                + "\"data\":{\"object\":{\"id\":\"" + paymentIntentId + "\","
                + "\"object\":\"payment_intent\",\"amount\":11000,\"currency\":\"cny\","
                + "\"status\":\"" + intentStatus + "\",\"metadata\":" + metadata + ","
                + "\"last_payment_error\":" + lastError + "}}}";
    }

    /** 与 Stripe 相同的签名算法：HMAC-SHA256("{t}.{payload}", secret)，拼 t=...,v1=... 头。 */
    private static String signatureHeader(String payload, String secret) {
        long timestamp = Instant.now().getEpochSecond();
        String signedPayload = timestamp + "." + payload;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String hex = HexFormat.of().formatHex(mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));
            return "t=" + timestamp + ",v1=" + hex;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** 门店画像：一店一站一套 Stripe 密钥；ids 全局唯一，便于双门店 fixture 共存断言。 */
    private record StoreProfile(
            long storeId,
            long siteId,
            long channelId,
            long pricePlanId,
            long roomTypeId,
            long roomId,
            long userId,
            String slug,
            String storeName,
            String publishableKey,
            String secretKey,
            String webhookSecret
    ) {
        private static StoreProfile alpha() {
            return new StoreProfile(
                    1L, 11L, 301L, 501L, 101L, 201L, 901L,
                    "alpha-hotel", "Alpha Hotel",
                    PUBLISHABLE_KEY, SECRET_KEY, WEBHOOK_SECRET
            );
        }

        private static StoreProfile beta() {
            return new StoreProfile(
                    2L, 12L, 302L, 502L, 102L, 202L, 902L,
                    "beta-hotel", "Beta Hotel",
                    BETA_PUBLISHABLE_KEY, BETA_SECRET_KEY, BETA_WEBHOOK_SECRET
            );
        }
    }

    private static final class Fixture {

        private static final Clock CLOCK = Clock.fixed(
                Instant.parse("2026-07-20T00:00:00Z"),
                ZoneOffset.UTC
        );

        private final StoreProfile profile;
        private final IndependentSite site;
        private final Store store;
        private final User owner;
        private final Room room;
        private final List<Reservation> reservations = new ArrayList<>();
        private final List<ReservationDailyPrice> dailyPrices = new ArrayList<>();
        private final List<Payment> payments = new ArrayList<>();
        private final Map<String, PaymentAttempt> attemptsByIdempotency = new LinkedHashMap<>();
        private final Map<String, PaymentAttempt> attemptsByPublicReference = new LinkedHashMap<>();
        private final RecordingQuoteService quoteService;
        private final RecordingInventoryLockService inventoryLockService =
                new RecordingInventoryLockService();
        private final RecordingLifecycleService lifecycleService = new RecordingLifecycleService();
        private final StripeConfig stripeConfig;
        private final IndependentSiteStripeSettingsService stripeSettingsService;
        private final IndependentSiteBookingService service;
        private final IndependentSiteStripeWebhookService webhookService;
        private long reservationSequence = 1000L;
        private long attemptSequence = 2000L;
        private long paymentSequence = 3000L;

        private Fixture(String stripeApiBase) {
            this(stripeApiBase, StoreProfile.alpha());
        }

        private Fixture(String stripeApiBase, StoreProfile profile) {
            this.profile = profile;
            this.site = site(profile);
            this.store = store(profile);
            this.owner = owner(profile);
            this.room = room(profile);
            this.quoteService = new RecordingQuoteService(site, quote());
            // 门店密钥：AES-GCM 加密后入"库"，与生产同格式
            AesGcmCrypto crypto = AesGcmCrypto.fromBase64Key(ENCRYPTION_KEY);
            IndependentSiteStripeSettings settings = new IndependentSiteStripeSettings();
            settings.setId(profile.storeId());
            settings.setStoreId(profile.storeId());
            settings.setPublishableKey(profile.publishableKey());
            settings.setSecretKeyEncrypted(crypto.encrypt(profile.secretKey()));
            settings.setWebhookSecretEncrypted(crypto.encrypt(profile.webhookSecret()));
            this.stripeSettingsService = new IndependentSiteStripeSettingsService(
                    repository(
                            IndependentSiteStripeSettingsRepository.class,
                            (proxy, method, args) -> switch (method.getName()) {
                                case "findByStoreId" -> Optional.ofNullable(
                                        Objects.equals(settings.getStoreId(), args[0]) ? settings : null
                                );
                                default -> objectMethodOrFail(proxy, method, args);
                            }
                    ),
                    ENCRYPTION_KEY
            );
            this.stripeConfig = new StripeConfig(stripeApiBase);
            this.service = buildBookingService(stripeSettingsService);
            this.webhookService = new IndependentSiteStripeWebhookService(
                    stripeSettingsService,
                    service,
                    repository(PaymentAttemptRepository.class, this::handlePaymentAttemptRepository),
                    stripeConfig,
                    new ObjectMapper().findAndRegisterModules()
            );
        }

        /** 读不到任何门店密钥的设置服务（门店未配置 Stripe 的等价形态）。 */
        private IndependentSiteStripeSettingsService unconfiguredSettingsService() {
            return new IndependentSiteStripeSettingsService(
                    repository(
                            IndependentSiteStripeSettingsRepository.class,
                            (proxy, method, args) -> switch (method.getName()) {
                                case "findByStoreId" -> Optional.empty();
                                default -> objectMethodOrFail(proxy, method, args);
                            }
                    ),
                    ENCRYPTION_KEY
            );
        }

        private IndependentSiteBookingService buildBookingService(
                IndependentSiteStripeSettingsService settingsService
        ) {
            return new IndependentSiteBookingService(
                    repository(IndependentSiteRepository.class, this::handleSiteRepository),
                    repository(PaymentAttemptRepository.class, this::handlePaymentAttemptRepository),
                    repository(ReservationRepository.class, this::handleReservationRepository),
                    repository(ReservationDailyPriceRepository.class, this::handleDailyPriceRepository),
                    repository(PaymentRepository.class, this::handlePaymentRepository),
                    repository(RoomRepository.class, this::handleRoomRepository),
                    repository(RoomBlockoutRepository.class, this::handleBlockoutRepository),
                    repository(StoreRepository.class, this::handleStoreRepository),
                    repository(UserRepository.class, this::handleUserRepository),
                    quoteService,
                    inventoryLockService,
                    lifecycleService,
                    new ObjectMapper().findAndRegisterModules(),
                    CLOCK,
                    stripeConfig,
                    settingsService
            );
        }

        private IndependentSiteDtos.PaymentAttemptResponse createPublicHold(String idempotencyKey) {
            return service.createPublicHold(
                    site.getSlug(),
                    holdRequest(idempotencyKey, "Guest Public")
            );
        }

        private Object handleSiteRepository(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "findEnabledByStoreIdAndSlugForUpdate" -> Optional.ofNullable(
                        Objects.equals(site.getStoreId(), args[0])
                                && Objects.equals(site.getSlug(), args[1])
                                ? site
                                : null
                );
                case "findEnabledBySlugForUpdate" ->
                        Optional.ofNullable(Objects.equals(site.getSlug(), args[0]) ? site : null);
                case "findByStoreIdAndIdWithChannelForUpdate" ->
                        Optional.ofNullable(Objects.equals(site.getStoreId(), args[0])
                                && Objects.equals(site.getId(), args[1])
                                ? site
                                : null);
                default -> objectMethodOrFail(proxy, method, args);
            };
        }

        private Object handlePaymentAttemptRepository(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "findByStoreIdAndIdempotencyKeyWithSite" ->
                        Optional.ofNullable(attemptsByIdempotency.get(args[1]));
                case "findByStoreIdAndPublicReferenceForUpdate", "findByPublicReferenceForUpdate",
                     "findByPublicReference" ->
                        Optional.ofNullable(attemptsByPublicReference.get(args[args.length - 1]));
                case "save" -> saveAttempt((PaymentAttempt) args[0]);
                default -> objectMethodOrFail(proxy, method, args);
            };
        }

        private Object handleReservationRepository(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "save" -> saveReservation((Reservation) args[0]);
                case "saveAll" -> args[0];
                case "findByStoreIdAndGroupOrderNoOrderByIdAsc",
                     "findByStoreIdAndGroupOrderNoForUpdate" ->
                        reservations.stream()
                                .filter(value -> Objects.equals(value.getStoreId(), args[0]))
                                .filter(value -> Objects.equals(value.getGroupOrderNo(), args[1]))
                                .toList();
                case "findByStoreIdAndRoomIdAndDateRange" -> reservations.stream()
                        .filter(value -> value.getStatus() == ReservationStatus.REQUESTED
                                || value.getStatus() == ReservationStatus.CONFIRMED
                                || value.getStatus() == ReservationStatus.CHECKED_IN)
                        .filter(value -> value.getRoom() != null
                                && Objects.equals(value.getRoom().getId(), args[1]))
                        .filter(value -> value.getCheckInDate().isBefore((LocalDate) args[3])
                                && value.getCheckOutDate().isAfter((LocalDate) args[2]))
                        .toList();
                default -> objectMethodOrFail(proxy, method, args);
            };
        }

        private Object handleDailyPriceRepository(Object proxy, Method method, Object[] args) {
            if ("saveAll".equals(method.getName())) {
                @SuppressWarnings("unchecked")
                Iterable<ReservationDailyPrice> rows = (Iterable<ReservationDailyPrice>) args[0];
                rows.forEach(dailyPrices::add);
                return args[0];
            }
            return objectMethodOrFail(proxy, method, args);
        }

        private Object handlePaymentRepository(Object proxy, Method method, Object[] args) {
            if ("save".equals(method.getName())) {
                Payment payment = (Payment) args[0];
                if (payment.getId() == null) {
                    payment.setId(++paymentSequence);
                }
                payments.add(payment);
                return payment;
            }
            return objectMethodOrFail(proxy, method, args);
        }

        private Object handleRoomRepository(Object proxy, Method method, Object[] args) {
            if ("findByStoreIdAndIdForUpdate".equals(method.getName())) {
                return Optional.ofNullable(
                        Objects.equals(room.getStoreId(), args[0])
                                && Objects.equals(room.getId(), args[1])
                                ? room
                                : null
                );
            }
            return objectMethodOrFail(proxy, method, args);
        }

        private Object handleBlockoutRepository(Object proxy, Method method, Object[] args) {
            if ("findByStoreIdAndRoom_IdInAndBlockDateBetween".equals(method.getName())) {
                return List.of();
            }
            return objectMethodOrFail(proxy, method, args);
        }

        private Object handleStoreRepository(Object proxy, Method method, Object[] args) {
            if ("findById".equals(method.getName())) {
                return Optional.ofNullable(Objects.equals(store.getId(), args[0]) ? store : null);
            }
            return objectMethodOrFail(proxy, method, args);
        }

        private Object handleUserRepository(Object proxy, Method method, Object[] args) {
            if ("findById".equals(method.getName())) {
                return Optional.ofNullable(Objects.equals(owner.getId(), args[0]) ? owner : null);
            }
            return objectMethodOrFail(proxy, method, args);
        }

        private Reservation saveReservation(Reservation reservation) {
            if (reservation.getId() == null) {
                reservation.setId(++reservationSequence);
                reservation.setOrderNumber("RSV-" + reservation.getId());
                reservations.add(reservation);
            }
            return reservation;
        }

        private PaymentAttempt saveAttempt(PaymentAttempt attempt) {
            if (attempt.getId() == null) {
                attempt.setId(++attemptSequence);
            }
            attemptsByIdempotency.put(attempt.getIdempotencyKey(), attempt);
            attemptsByPublicReference.put(attempt.getPublicReference(), attempt);
            return attempt;
        }

        private IndependentSiteDtos.HoldRequest holdRequest(String idempotencyKey, String guestName) {
            return new IndependentSiteDtos.HoldRequest(
                    idempotencyKey,
                    room.getRoomType().getId(),
                    LocalDate.of(2026, 8, 1),
                    LocalDate.of(2026, 8, 2),
                    1,
                    1,
                    0,
                    new IndependentSiteDtos.Guest(
                            guestName,
                            "13800000000",
                            "guest@example.com",
                            "Late arrival"
                    )
            );
        }

        private IndependentSiteQuoteService.QuoteComputation quote() {
            PricePlan pricePlan = site.getChannel().getDefaultPricePlan();
            RoomType roomType = room.getRoomType();
            RoomTypePricePlan mapping = new RoomTypePricePlan();
            LocalDate date = LocalDate.of(2026, 8, 1);
            BigDecimal amount = new BigDecimal("110.00");
            OffsetDateTime quotedAt = OffsetDateTime.ofInstant(CLOCK.instant(), ZoneOffset.UTC);
            IndependentSiteDtos.QuoteResponse response = new IndependentSiteDtos.QuoteResponse(
                    site.getSlug(),
                    roomType.getId(),
                    roomType.getName(),
                    date,
                    date.plusDays(1),
                    1,
                    1,
                    0,
                    1,
                    "CNY",
                    new BigDecimal("10.00"),
                    List.of(new IndependentSiteDtos.NightlyRate(
                            date,
                            new BigDecimal("100.00"),
                            amount,
                            BigDecimal.ZERO.setScale(2),
                            amount
                    )),
                    amount,
                    quotedAt,
                    quotedAt.plusMinutes(5)
            );
            return new IndependentSiteQuoteService.QuoteComputation(
                    site,
                    roomType,
                    pricePlan,
                    mapping,
                    List.of(room.getId()),
                    List.of(new IndependentSiteQuoteService.GuestAllocation(1, 0)),
                    List.of(List.of(new IndependentSiteQuoteService.DailyAmount(date, amount))),
                    List.of(amount),
                    response
            );
        }

        private static IndependentSite site(StoreProfile profile) {
            PricePlan pricePlan = new PricePlan();
            pricePlan.setId(profile.pricePlanId());
            pricePlan.setStoreId(profile.storeId());
            pricePlan.setName("Standard");

            Channel channel = new Channel();
            channel.setId(profile.channelId());
            channel.setStoreId(profile.storeId());
            channel.setCode(IndependentSiteManagementService.BOOKING_ENGINE_CHANNEL_CODE);
            channel.setEnabled(true);
            channel.setIsActive(true);
            channel.setPriceAdjustmentType(PriceAdjustmentType.PERCENTAGE);
            channel.setPriceAdjustmentValue(new BigDecimal("10.00"));
            channel.setDefaultPricePlan(pricePlan);

            IndependentSite site = new IndependentSite();
            site.setId(profile.siteId());
            site.setStoreId(profile.storeId());
            site.setSlug(profile.slug());
            site.setEnabled(true);
            site.setChannel(channel);
            site.setPaymentProvider(IndependentSitePaymentProvider.SIMULATED);
            site.setSimulatedPaymentEnabled(true);
            site.setPublishedAt(LocalDateTime.of(2026, 7, 20, 0, 0));
            return site;
        }

        private static Store store(StoreProfile profile) {
            Store store = new Store();
            store.setId(profile.storeId());
            store.setUserId(profile.userId());
            store.setName(profile.storeName());
            store.setTimezone("UTC");
            store.setCurrency("CNY");
            return store;
        }

        private static User owner(StoreProfile profile) {
            User owner = new User();
            owner.setId(profile.userId());
            return owner;
        }

        private static Room room(StoreProfile profile) {
            RoomType roomType = new RoomType();
            roomType.setId(profile.roomTypeId());
            roomType.setStoreId(profile.storeId());
            roomType.setName("King Room");
            roomType.setCode("KING");
            roomType.setMaxGuests(2);

            Room room = new Room();
            room.setId(profile.roomId());
            room.setStoreId(profile.storeId());
            room.setUserId(profile.userId());
            room.setRoomNumber("101");
            room.setRoomType(roomType);
            room.setStatus(RoomStatus.AVAILABLE);
            return room;
        }
    }

    private static final class RecordingQuoteService extends IndependentSiteQuoteService {

        private final IndependentSite site;
        private final QuoteComputation quote;
        private int calculateCalls;

        private RecordingQuoteService(IndependentSite site, QuoteComputation quote) {
            super(null, null, publishedHomePageRepository(site), null, null, null, null, null, null, null,
                    null, null, null, null);
            this.site = site;
            this.quote = quote;
        }

        @Override
        QuoteComputation calculate(IndependentSite requestedSite, IndependentSiteDtos.QuoteRequest request) {
            assertSame(site, requestedSite);
            calculateCalls++;
            return quote;
        }

        @Override
        void assertHoldInventoryAvailable(
                QuoteComputation quote,
                IndependentSiteDtos.QuoteRequest request
        ) {
            // 默认库存充足
        }

        @Override
        IndependentSite resolveEnabledSite(String slug) {
            if (!Objects.equals(site.getSlug(), slug)) {
                throw new AssertionError("Unexpected slug: " + slug);
            }
            return site;
        }
    }

    private static final class RecordingLifecycleService
            extends IndependentSiteReservationLifecycleService {

        private final List<Event> events = new ArrayList<>();

        private RecordingLifecycleService() {
            super(null, null, Optional.empty(), Optional.empty(), null, null);
        }

        @Override
        public void onChanged(List<Reservation> reservations, Event event, Long fallbackUserId) {
            assertFalse(reservations.isEmpty());
            events.add(event);
        }
    }

    private static final class RecordingInventoryLockService
            extends RoomTypeInventoryLockService {

        private RecordingInventoryLockService() {
            super(null);
        }

        @Override
        public Set<Long> lockRoomTypes(Long storeId, Collection<Long> roomTypeIds) {
            assertNotNull(storeId);
            return Set.copyOf(roomTypeIds);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T repository(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private static IndependentSitePageRepository publishedHomePageRepository(
            IndependentSite site
    ) {
        IndependentSitePage home = new IndependentSitePage();
        home.setId(9000L + site.getId());
        home.setStoreId(site.getStoreId());
        home.setSite(site);
        home.setPath("/");
        home.setType(IndependentSitePageType.HOME);
        home.setTitle("Home");
        home.setPublishedSchemaJson("{}");
        home.setPublishedAt(LocalDateTime.of(2026, 7, 20, 0, 0));
        home.setEnabled(true);
        return repository(
                IndependentSitePageRepository.class,
                (proxy, method, args) -> {
                    if ("findBySiteIdAndTypeAndPublishedAtIsNotNullAndEnabledTrue"
                            .equals(method.getName())) {
                        return Optional.of(home);
                    }
                    return objectMethodOrFail(proxy, method, args);
                }
        );
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
