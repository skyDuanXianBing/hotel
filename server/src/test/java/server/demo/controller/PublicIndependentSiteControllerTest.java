package server.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import server.demo.constants.SaasFeatureCodes;
import server.demo.dto.ApiResponse;
import server.demo.dto.IndependentSiteDtos;
import server.demo.service.IndependentSiteBookingService;
import server.demo.service.IndependentSitePublicRateLimiter;
import server.demo.service.IndependentSiteQuoteService;
import server.demo.service.saas.EntitlementService;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

/**
 * 公开独立站停止接单（P9，业主拍板）：
 * 交易型端点（报价/holds/支付确认/支付意图）在门店缺失 independent_website 权益时
 * 返回 403 success=false「该店铺暂停接单」且不进入业务层；权益具备时正常放行。
 */
class PublicIndependentSiteControllerTest {

    private static final String SLUG = "alpha-hotel";
    private static final long STORE_ID = 1L;

    private IndependentSiteQuoteService quoteService;
    private IndependentSiteBookingService bookingService;
    private IndependentSitePublicRateLimiter rateLimiter;
    private EntitlementService entitlementService;
    private PublicIndependentSiteController controller;
    private HttpServletRequest httpRequest;

    @BeforeEach
    void setUp() {
        quoteService = Mockito.mock(IndependentSiteQuoteService.class);
        bookingService = Mockito.mock(IndependentSiteBookingService.class);
        rateLimiter = Mockito.mock(IndependentSitePublicRateLimiter.class);
        entitlementService = Mockito.mock(EntitlementService.class);
        controller = new PublicIndependentSiteController(
                quoteService, bookingService, rateLimiter, entitlementService);
        httpRequest = Mockito.mock(HttpServletRequest.class);
        lenient().when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        lenient().when(quoteService.resolveEnabledStoreId(SLUG)).thenReturn(STORE_ID);
    }

    private void stubEntitlement(boolean hasFeature) {
        Mockito.when(entitlementService.storeHasFeature(STORE_ID, SaasFeatureCodes.INDEPENDENT_WEBSITE))
                .thenReturn(hasFeature);
    }

    private static IndependentSiteDtos.QuoteRequest quoteRequest() {
        return new IndependentSiteDtos.QuoteRequest(
                101L, LocalDate.now().plusDays(7), LocalDate.now().plusDays(9), 1, 2, 0);
    }

    private static IndependentSiteDtos.HoldRequest holdRequest() {
        return new IndependentSiteDtos.HoldRequest(
                "idem-key-0001",
                101L,
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(9),
                1,
                2,
                0,
                new IndependentSiteDtos.Guest("张三", "13800000000", null, null)
        );
    }

    // ------------------------------------------------------------------
    // 权益缺失 → 403 暂停接单
    // ------------------------------------------------------------------

    @Test
    void quote_entitlementMissing_forbiddenWithClosedMessage() {
        stubEntitlement(false);

        ResponseEntity<ApiResponse<IndependentSiteDtos.QuoteResponse>> response =
                controller.quote(SLUG, quoteRequest(), httpRequest);

        assertEquals(403, response.getStatusCode().value());
        assertFalse(response.getBody().isSuccess());
        assertEquals("该店铺暂停接单", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        // 不进入报价业务层，也不消耗限流配额
        Mockito.verify(quoteService, never()).quote(any(), any());
        Mockito.verify(rateLimiter, never()).checkQuote(any(), any());
    }

    @Test
    void createHold_entitlementMissing_forbiddenWithClosedMessage() {
        stubEntitlement(false);

        ResponseEntity<ApiResponse<IndependentSiteDtos.PaymentAttemptResponse>> response =
                controller.createHold(SLUG, holdRequest(), httpRequest);

        assertEquals(403, response.getStatusCode().value());
        assertFalse(response.getBody().isSuccess());
        assertEquals("该店铺暂停接单", response.getBody().getMessage());
        Mockito.verify(bookingService, never()).createPublicHold(any(), any());
        Mockito.verify(rateLimiter, never()).checkHold(any(), any());
    }

    @Test
    void confirmPayment_entitlementMissing_forbiddenWithClosedMessage() {
        stubEntitlement(false);

        ResponseEntity<ApiResponse<IndependentSiteDtos.PaymentAttemptResponse>> response =
                controller.confirmPayment(SLUG, "pay-1");

        assertEquals(403, response.getStatusCode().value());
        assertFalse(response.getBody().isSuccess());
        assertEquals("该店铺暂停接单", response.getBody().getMessage());
        Mockito.verify(bookingService, never()).confirmPublicPayment(any(), any());
    }

    @Test
    void createStripeIntent_entitlementMissing_forbiddenWithClosedMessage() {
        stubEntitlement(false);

        ResponseEntity<ApiResponse<IndependentSiteDtos.StripeIntentResponse>> response =
                controller.createStripeIntent(SLUG, "pay-1", httpRequest);

        assertEquals(403, response.getStatusCode().value());
        assertFalse(response.getBody().isSuccess());
        assertEquals("该店铺暂停接单", response.getBody().getMessage());
        Mockito.verify(bookingService, never()).createStripeIntent(any(), any());
        Mockito.verify(rateLimiter, never()).checkIntent(any(), any());
    }

    // ------------------------------------------------------------------
    // 权益具备 → 正常放行
    // ------------------------------------------------------------------

    @Test
    void quote_entitlementPresent_passesThrough() {
        stubEntitlement(true);
        IndependentSiteDtos.QuoteResponse quoteResponse = Mockito.mock(IndependentSiteDtos.QuoteResponse.class);
        Mockito.when(quoteService.quote(eq(SLUG), any())).thenReturn(quoteResponse);

        ResponseEntity<ApiResponse<IndependentSiteDtos.QuoteResponse>> response =
                controller.quote(SLUG, quoteRequest(), httpRequest);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertTrue(response.getBody().isSuccess());
        Mockito.verify(rateLimiter).checkQuote(SLUG, "127.0.0.1");
        Mockito.verify(quoteService).quote(eq(SLUG), any());
    }

    @Test
    void createHold_entitlementPresent_passesThrough() {
        stubEntitlement(true);

        controller.createHold(SLUG, holdRequest(), httpRequest);

        Mockito.verify(rateLimiter).checkHold(SLUG, "127.0.0.1");
        Mockito.verify(bookingService).createPublicHold(eq(SLUG), any());
    }

    // ------------------------------------------------------------------
    // 站点信息型端点不做 403 拦截（closed 标记由 quoteService 在响应中携带）
    // ------------------------------------------------------------------

    @Test
    void siteInfoEndpoints_notGuardedByController() {
        controller.getSite(SLUG);
        controller.getPaymentStatus(SLUG, "pay-1");

        // 信息型端点不经控制器权益守卫（closed 标记在 quoteService 响应体内增量下发）
        Mockito.verify(entitlementService, never()).storeHasFeature(any(), any());
        Mockito.verify(quoteService).getPublicSite(SLUG);
        Mockito.verify(bookingService).getPaymentStatus(SLUG, "pay-1");
    }
}
