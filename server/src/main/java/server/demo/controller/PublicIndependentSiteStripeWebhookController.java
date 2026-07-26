package server.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.demo.dto.ApiResponse;
import server.demo.service.IndependentSiteStripeWebhookService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Stripe webhook 公开端点。验签（Stripe-Signature 头 + webhook secret）是唯一鉴权，
 * 绝不可加 @StoreScoped / @RequirePermission；必须读取原始请求体字节验签。
 */
@RestController
@RequestMapping("/api/public/independent-sites/stripe")
public class PublicIndependentSiteStripeWebhookController {

    private final IndependentSiteStripeWebhookService webhookService;

    public PublicIndependentSiteStripeWebhookController(IndependentSiteStripeWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Object>> handleWebhook(HttpServletRequest request) throws IOException {
        byte[] payload = request.getInputStream().readAllBytes();
        String signature = request.getHeader("Stripe-Signature");
        webhookService.handle(new String(payload, StandardCharsets.UTF_8), signature);
        return ResponseEntity.ok(ApiResponse.success("webhook 已处理", null));
    }
}
