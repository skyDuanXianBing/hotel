package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import server.demo.config.StripeConfig;
import server.demo.entity.PaymentAttempt;
import server.demo.repository.PaymentAttemptRepository;

/**
 * Stripe webhook 分发（单端点多门店）。验签是唯一鉴权，处理顺序（D4）：
 * 未验签解析 payload 提取 metadata.publicReference（仅只读查找 attempt，绝不做状态变更）
 * → storeId → 该门店 webhook secret → Webhook.constructEvent 验签 → 通过才分发。
 * 任一步失败返回 400 且无任何状态副作用；状态变更只使用验签后事件中的引用。
 * 确认/失败落库复用 IndependentSiteBookingService 的 transition 管线，只从 PENDING 出发，
 * 重复投递天然幂等；解析或落库的内部异常向上抛，由异常处理器返回 500 让 Stripe 重试。
 * payment_failed 不等于终态：普通卡被拒后 PaymentIntent 回到可重试状态（requires_payment_method
 * 等），前端允许换卡重试同一 intent，故先以 Stripe 侧当前状态为准，仅 canceled 终态才落 FAILED
 * 释放保留；retrieve 失败同样保守放行，交给 15 分钟过期调度器兜底，不误杀可重试支付。
 */
@Service
public class IndependentSiteStripeWebhookService {

    private static final Logger logger = LoggerFactory.getLogger(IndependentSiteStripeWebhookService.class);

    private final IndependentSiteStripeSettingsService stripeSettingsService;
    private final IndependentSiteBookingService bookingService;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final StripeConfig stripeConfig;
    private final ObjectMapper objectMapper;

    public IndependentSiteStripeWebhookService(
            IndependentSiteStripeSettingsService stripeSettingsService,
            IndependentSiteBookingService bookingService,
            PaymentAttemptRepository paymentAttemptRepository,
            StripeConfig stripeConfig,
            ObjectMapper objectMapper
    ) {
        this.stripeSettingsService = stripeSettingsService;
        this.bookingService = bookingService;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.stripeConfig = stripeConfig;
        this.objectMapper = objectMapper;
    }

    public void handle(String payload, String signatureHeader) {
        // 未验签 payload 仅提取引用做路由：任何状态变更都发生在验签之后
        String routingReference = extractRoutingReference(payload);
        PaymentAttempt attempt = paymentAttemptRepository.findByPublicReference(routingReference)
                .orElseThrow(() -> badRequest(
                        "STRIPE_REFERENCE_UNKNOWN",
                        "Stripe webhook 引用的支付尝试不存在"
                ));
        IndependentSiteStripeSettingsService.ResolvedStripeKeys keys = stripeSettingsService
                .resolveForStore(attempt.getStoreId())
                .filter(IndependentSiteStripeSettingsService.ResolvedStripeKeys::hasWebhookSecret)
                .orElseThrow(() -> badRequest(
                        "STRIPE_WEBHOOK_NOT_CONFIGURED",
                        "该门店未配置 Stripe webhook 密钥"
                ));

        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, keys.webhookSecret());
        } catch (SignatureVerificationException e) {
            throw badRequest("STRIPE_SIGNATURE_INVALID", "Stripe webhook 验签失败");
        }

        switch (event.getType()) {
            case "payment_intent.succeeded" -> {
                PaymentIntent intent = deserializePaymentIntent(event);
                String publicReference = publicReferenceOf(intent);
                if (publicReference != null) {
                    bookingService.confirmStripePayment(publicReference, intent.getId());
                }
            }
            case "payment_intent.payment_failed" -> {
                PaymentIntent intent = deserializePaymentIntent(event);
                String publicReference = publicReferenceOf(intent);
                if (publicReference != null) {
                    handlePaymentFailed(intent, publicReference, keys);
                }
            }
            default -> logger.debug("忽略未订阅处理的 Stripe 事件类型: {}", event.getType());
        }
    }

    /**
     * 卡被拒等失败事件不等于终态：仅 PaymentIntent 当前处于 canceled 才落 FAILED 释放保留；
     * requires_payment_method / requires_confirmation / requires_action / processing 等形态
     * 前端仍可换卡重试同一 intent，保持 attempt PENDING 并正常返回 200。
     */
    private void handlePaymentFailed(
            PaymentIntent intent,
            String publicReference,
            IndependentSiteStripeSettingsService.ResolvedStripeKeys keys
    ) {
        String status = currentIntentStatus(intent, keys);
        if (!"canceled".equals(status)) {
            logger.info(
                    "Stripe PaymentIntent {} 非终态失败（status={}），支付尝试保持 PENDING 等待重试",
                    intent.getId(),
                    status
            );
            return;
        }
        bookingService.failStripePayment(publicReference, failureReasonOf(intent));
    }

    /**
     * 以 Stripe 侧 retrieve 到的当前状态为准（事件内 object 可能只是投递时的快照）；
     * 门店未配置 secret key 时退回事件内状态；retrieve 失败返回 null 按可重试保守处理。
     */
    private String currentIntentStatus(
            PaymentIntent intent,
            IndependentSiteStripeSettingsService.ResolvedStripeKeys keys
    ) {
        if (!keys.hasSecretKey()) {
            return intent.getStatus();
        }
        try {
            StripeClient client = stripeConfig.clientFor(keys.secretKey());
            PaymentIntent current = client.v1().paymentIntents().retrieve(intent.getId());
            return current != null && current.getStatus() != null
                    ? current.getStatus()
                    : intent.getStatus();
        } catch (StripeException e) {
            logger.warn("retrieve PaymentIntent {} 失败，按可重试保守处理: {}", intent.getId(), e.getMessage());
            return null;
        }
    }

    /** 未验签解析：只读提取 data.object.metadata.publicReference，供路由查找门店密钥。 */
    private String extractRoutingReference(String payload) {
        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (Exception e) {
            throw badRequest("STRIPE_PAYLOAD_INVALID", "Stripe webhook 负载无法解析");
        }
        JsonNode reference = root.path("data").path("object").path("metadata").path("publicReference");
        if (!reference.isTextual() || reference.asText().isBlank()) {
            throw badRequest("STRIPE_REFERENCE_MISSING", "Stripe webhook 缺少支付引用");
        }
        return reference.asText();
    }

    private static PaymentIntent deserializePaymentIntent(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        // 账户 API 版本与 SDK 固定版本不一致时，PaymentIntent 字段集通常仍兼容，回退不安全反序列化
        StripeObject stripeObject = deserializer.getObject().orElseGet(() -> {
            try {
                return deserializer.deserializeUnsafe();
            } catch (EventDataObjectDeserializationException e) {
                throw new IllegalStateException("无法解析 Stripe 事件中的 PaymentIntent", e);
            }
        });
        if (!(stripeObject instanceof PaymentIntent paymentIntent)) {
            throw new IllegalStateException("Stripe 事件负载不是 PaymentIntent");
        }
        return paymentIntent;
    }

    private static String publicReferenceOf(PaymentIntent intent) {
        if (intent.getMetadata() == null) {
            return null;
        }
        String value = intent.getMetadata().get("publicReference");
        return value == null || value.isBlank() ? null : value;
    }

    private static String failureReasonOf(PaymentIntent intent) {
        if (intent.getLastPaymentError() != null
                && intent.getLastPaymentError().getMessage() != null
                && !intent.getLastPaymentError().getMessage().isBlank()) {
            return "Stripe 支付失败：" + intent.getLastPaymentError().getMessage();
        }
        return "Stripe 支付失败";
    }

    private static IndependentSiteServiceException badRequest(String code, String message) {
        return new IndependentSiteServiceException(HttpStatus.BAD_REQUEST, code, message);
    }
}
