package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.IndependentSiteDtos;
import server.demo.entity.IndependentSiteStripeSettings;
import server.demo.repository.IndependentSiteStripeSettingsRepository;
import server.demo.util.AesGcmCrypto;

import java.util.Optional;

/**
 * 门店级 Stripe 密钥配置：一店一套，该店所有独立站共享。
 * sk/whsec 经 AES-GCM 加密落库（密钥来自 env INDEPENDENT_SITE_STRIPE_ENCRYPTION_KEY），
 * 明文永不出现在任何响应；读取侧解密失败按"未配置"降级并 warn，绝不抛 500 炸掉站点接口。
 * 加密密钥未配置时：读取照常被拒之门外（视为未配置），仅"保存 sk/whsec"报明确配置错误。
 */
@Service
public class IndependentSiteStripeSettingsService {

    private static final Logger logger = LoggerFactory.getLogger(IndependentSiteStripeSettingsService.class);

    private static final String PUBLISHABLE_KEY_PREFIX = "pk_";
    private static final String SECRET_KEY_PREFIX = "sk_";
    private static final String WEBHOOK_SECRET_PREFIX = "whsec_";

    private static final IndependentSiteDtos.StripeSettingsResponse UNCONFIGURED_RESPONSE =
            new IndependentSiteDtos.StripeSettingsResponse(false, null, false, null, false, null);

    /**
     * 支付链路视角的门店密钥（已解密）；仅服务端内部使用，任何字段不得直接序列化出管理端响应
     * （publishableKey 除外，它本就公开）。
     */
    public record ResolvedStripeKeys(
            String publishableKey,
            String secretKey,
            String webhookSecret
    ) {
        public boolean hasSecretKey() {
            return secretKey != null && !secretKey.isBlank();
        }

        public boolean hasWebhookSecret() {
            return webhookSecret != null && !webhookSecret.isBlank();
        }

        /** 门店三密钥齐全 = 站点可选 STRIPE / 公开下单可用 STRIPE 的统一门槛。 */
        public boolean isFullyConfigured() {
            return publishableKey != null && !publishableKey.isBlank()
                    && hasSecretKey()
                    && hasWebhookSecret();
        }
    }

    private final IndependentSiteStripeSettingsRepository repository;
    private final AesGcmCrypto crypto;

    public IndependentSiteStripeSettingsService(
            IndependentSiteStripeSettingsRepository repository,
            @Value("${independent-site.stripe.encryption-key:}") String encryptionKeyBase64
    ) {
        this.repository = repository;
        // 未配置 = 功能关闭（保存 sk/whsec 报明确错误）；配置了但非法 = 启动即暴露，快速失败
        this.crypto = encryptionKeyBase64 == null || encryptionKeyBase64.isBlank()
                ? null
                : AesGcmCrypto.fromBase64Key(encryptionKeyBase64);
    }

    @Transactional(readOnly = true)
    public IndependentSiteDtos.StripeSettingsResponse getSettings(Long storeId) {
        return repository.findByStoreId(storeId)
                .map(this::toResponse)
                .orElse(UNCONFIGURED_RESPONSE);
    }

    /**
     * 写入语义（D5）：字段缺省/空串 = 保持不变，填新值 = 覆盖；不提供单独清除。
     * 前缀校验 pk_ / sk_ / whsec_；sk/whsec 需要加密密钥可用，否则报明确配置错误。
     */
    @Transactional
    public IndependentSiteDtos.StripeSettingsResponse updateSettings(
            Long storeId,
            IndependentSiteDtos.StripeSettingsUpdateRequest request
    ) {
        String publishableKey = normalizeOptional(request != null ? request.publishableKey() : null);
        String secretKey = normalizeOptional(request != null ? request.secretKey() : null);
        String webhookSecret = normalizeOptional(request != null ? request.webhookSecret() : null);
        requirePrefix(publishableKey, PUBLISHABLE_KEY_PREFIX, "publishableKey");
        requirePrefix(secretKey, SECRET_KEY_PREFIX, "secretKey");
        requirePrefix(webhookSecret, WEBHOOK_SECRET_PREFIX, "webhookSecret");
        if ((secretKey != null || webhookSecret != null) && crypto == null) {
            throw new IndependentSiteServiceException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "STRIPE_ENCRYPTION_NOT_CONFIGURED",
                    "服务端未配置独立站 Stripe 密钥加密密钥（INDEPENDENT_SITE_STRIPE_ENCRYPTION_KEY），无法保存密钥"
            );
        }

        IndependentSiteStripeSettings settings = repository.findByStoreId(storeId)
                .orElseGet(() -> {
                    IndependentSiteStripeSettings created = new IndependentSiteStripeSettings();
                    created.setStoreId(storeId);
                    return created;
                });
        if (publishableKey != null) {
            settings.setPublishableKey(publishableKey);
        }
        if (secretKey != null) {
            settings.setSecretKeyEncrypted(crypto.encrypt(secretKey));
        }
        if (webhookSecret != null) {
            settings.setWebhookSecretEncrypted(crypto.encrypt(webhookSecret));
        }
        return toResponse(repository.save(settings));
    }

    /**
     * 支付链路解析：门店设置存在且可解密时返回密钥集；任一密文解密失败按未配置处理
     * （对应字段置空，整体不再"齐全"）并 warn，不抛出。
     */
    @Transactional(readOnly = true)
    public Optional<ResolvedStripeKeys> resolveForStore(Long storeId) {
        return repository.findByStoreId(storeId)
                .map(settings -> new ResolvedStripeKeys(
                        normalizeOptional(settings.getPublishableKey()),
                        decryptOrNull(storeId, "secretKey", settings.getSecretKeyEncrypted()),
                        decryptOrNull(storeId, "webhookSecret", settings.getWebhookSecretEncrypted())
                ));
    }

    /** 站点 STRIPE 门槛 / SiteDetailResponse.stripeAvailable 的唯一事实源。 */
    public boolean isFullyConfigured(Long storeId) {
        return resolveForStore(storeId)
                .map(ResolvedStripeKeys::isFullyConfigured)
                .orElse(false);
    }

    private IndependentSiteDtos.StripeSettingsResponse toResponse(IndependentSiteStripeSettings settings) {
        String publishableKey = normalizeOptional(settings.getPublishableKey());
        String secretKey = decryptOrNull(settings.getStoreId(), "secretKey", settings.getSecretKeyEncrypted());
        String webhookSecret = decryptOrNull(
                settings.getStoreId(),
                "webhookSecret",
                settings.getWebhookSecretEncrypted()
        );
        boolean secretConfigured = secretKey != null;
        boolean webhookConfigured = webhookSecret != null;
        return new IndependentSiteDtos.StripeSettingsResponse(
                publishableKey != null && secretConfigured && webhookConfigured,
                publishableKey,
                secretConfigured,
                last4(secretKey),
                webhookConfigured,
                last4(webhookSecret)
        );
    }

    /** 密文为空返回 null；配置了密文但解密失败（密钥轮换/损坏）warn 并按未配置处理。 */
    private String decryptOrNull(Long storeId, String field, String ciphertext) {
        if (ciphertext == null || ciphertext.isBlank()) {
            return null;
        }
        if (crypto == null) {
            logger.warn(
                    "门店 {} 的 Stripe {} 已存密文，但服务端未配置加密密钥，按未配置处理",
                    storeId,
                    field
            );
            return null;
        }
        try {
            return normalizeOptional(crypto.decrypt(ciphertext));
        } catch (IllegalStateException ex) {
            logger.warn(
                    "门店 {} 的 Stripe {} 密文解密失败，按未配置处理: {}",
                    storeId,
                    field,
                    ex.getMessage()
            );
            return null;
        }
    }

    private static void requirePrefix(String value, String prefix, String field) {
        if (value != null && !value.startsWith(prefix)) {
            throw new IndependentSiteServiceException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_STRIPE_KEY",
                    field + " 必须以 " + prefix + " 开头"
            );
        }
    }

    private static String last4(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        return plaintext.length() <= 4 ? plaintext : plaintext.substring(plaintext.length() - 4);
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
