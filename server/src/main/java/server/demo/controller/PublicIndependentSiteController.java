package server.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.demo.constants.SaasFeatureCodes;
import server.demo.dto.ApiResponse;
import server.demo.dto.IndependentSiteDtos;
import server.demo.service.IndependentSiteBookingService;
import server.demo.service.IndependentSitePublicRateLimiter;
import server.demo.service.IndependentSiteQuoteService;
import server.demo.service.saas.EntitlementService;

import server.demo.i18n.ApiMessages;
@RestController
@RequestMapping("/api/public/independent-sites")
public class PublicIndependentSiteController {

    private final IndependentSiteQuoteService quoteService;
    private final IndependentSiteBookingService bookingService;
    private final IndependentSitePublicRateLimiter rateLimiter;
    private final EntitlementService entitlementService;

    public PublicIndependentSiteController(
            IndependentSiteQuoteService quoteService,
            IndependentSiteBookingService bookingService,
            IndependentSitePublicRateLimiter rateLimiter,
            EntitlementService entitlementService
    ) {
        this.quoteService = quoteService;
        this.bookingService = bookingService;
        this.rateLimiter = rateLimiter;
        this.entitlementService = entitlementService;
    }

    /**
     * 交易型端点守卫（P9，业主拍板：公开独立站停止接单）：按站点解析归属门店，
     * 门店有效订阅缺失 independent_website 权益时返回 403 success=false「该店铺暂停接单」。
     * 站点不可用（未发布/渠道失效）由解析层抛 404；权益具备时返回 null 放行。
     */
    private <T> ResponseEntity<ApiResponse<T>> storeClosedResponse(String slug) {
        Long storeId = quoteService.resolveEnabledStoreId(slug);
        if (entitlementService.storeHasFeature(storeId, SaasFeatureCodes.INDEPENDENT_WEBSITE)) {
            return null;
        }
        return ResponseEntity.status(403).body(ApiResponse.error(ApiMessages.get("api.t.8b117e857598")));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<IndependentSiteDtos.PublicSiteResponse>> getSite(
            @PathVariable String slug
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                ApiMessages.get("api.t.3be446956424"),
                quoteService.getPublicSite(slug)
        ));
    }

    @GetMapping("/{slug}/pages/**")
    public ResponseEntity<ApiResponse<IndependentSiteDtos.PublicPageResponse>> getPage(
            @PathVariable String slug,
            HttpServletRequest httpRequest
    ) {
        String marker = "/" + slug + "/pages/";
        String uri = httpRequest.getRequestURI();
        int markerIndex = uri.indexOf(marker);
        String tail = markerIndex >= 0 ? uri.substring(markerIndex + marker.length()) : "";
        return ResponseEntity.ok(ApiResponse.success(
                ApiMessages.get("api.t.546ca15b4030"),
                quoteService.getPublicPage(slug, "/" + tail)
        ));
    }

    @PostMapping("/{slug}/quotes")
    public ResponseEntity<ApiResponse<IndependentSiteDtos.QuoteResponse>> quote(
            @PathVariable String slug,
            @Valid @RequestBody IndependentSiteDtos.QuoteRequest request,
            HttpServletRequest httpRequest
    ) {
        ResponseEntity<ApiResponse<IndependentSiteDtos.QuoteResponse>> closed = storeClosedResponse(slug);
        if (closed != null) {
            return closed;
        }
        rateLimiter.checkQuote(slug, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(
                ApiMessages.get("api.t.7fa7fc9c34e8"),
                quoteService.quote(slug, request)
        ));
    }

    @PostMapping("/{slug}/holds")
    public ResponseEntity<ApiResponse<IndependentSiteDtos.PaymentAttemptResponse>> createHold(
            @PathVariable String slug,
            @Valid @RequestBody IndependentSiteDtos.HoldRequest request,
            HttpServletRequest httpRequest
    ) {
        ResponseEntity<ApiResponse<IndependentSiteDtos.PaymentAttemptResponse>> closed = storeClosedResponse(slug);
        if (closed != null) {
            return closed;
        }
        rateLimiter.checkHold(slug, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(
                ApiMessages.get("api.t.405c3fbfcae6"),
                bookingService.createPublicHold(slug, request)
        ));
    }

    @PostMapping("/{slug}/payments/{paymentAttemptId}/confirm")
    public ResponseEntity<ApiResponse<IndependentSiteDtos.PaymentAttemptResponse>> confirmPayment(
            @PathVariable String slug,
            @PathVariable String paymentAttemptId
    ) {
        ResponseEntity<ApiResponse<IndependentSiteDtos.PaymentAttemptResponse>> closed = storeClosedResponse(slug);
        if (closed != null) {
            return closed;
        }
        return ResponseEntity.ok(ApiResponse.success(
                ApiMessages.get("api.t.2a8466d47517"),
                bookingService.confirmPublicPayment(slug, paymentAttemptId)
        ));
    }

    @PostMapping("/{slug}/payments/{paymentAttemptId}/intent")
    public ResponseEntity<ApiResponse<IndependentSiteDtos.StripeIntentResponse>> createStripeIntent(
            @PathVariable String slug,
            @PathVariable String paymentAttemptId,
            HttpServletRequest httpRequest
    ) {
        ResponseEntity<ApiResponse<IndependentSiteDtos.StripeIntentResponse>> closed = storeClosedResponse(slug);
        if (closed != null) {
            return closed;
        }
        rateLimiter.checkIntent(slug, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(
                ApiMessages.get("api.t.c3e4943b3793"),
                bookingService.createStripeIntent(slug, paymentAttemptId)
        ));
    }

    @GetMapping("/{slug}/payments/{paymentAttemptId}")
    public ResponseEntity<ApiResponse<IndependentSiteDtos.PaymentAttemptResponse>> getPaymentStatus(
            @PathVariable String slug,
            @PathVariable String paymentAttemptId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                ApiMessages.get("api.t.62ff16741df8"),
                bookingService.getPaymentStatus(slug, paymentAttemptId)
        ));
    }
}
