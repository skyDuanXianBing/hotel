package server.demo.config;

import com.stripe.StripeClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 独立站 Stripe 客户端工厂。门店级密钥存于数据库（IndependentSiteStripeSettingsService 解析），
 * 本类只负责按 secretKey 构建并缓存 StripeClient（stripe-java 实例模式天然支持多租户逐店一密钥）。
 * apiBase 默认指向 Stripe 正式 API，仅测试覆盖为本地 mock server。
 */
@Component
public class StripeConfig {

    private static final String DEFAULT_API_BASE = "https://api.stripe.com";

    private final String apiBase;
    private final ConcurrentHashMap<String, StripeClient> clients = new ConcurrentHashMap<>();

    public StripeConfig(@Value("${stripe.api-base:https://api.stripe.com}") String apiBase) {
        this.apiBase = apiBase == null || apiBase.isBlank() ? DEFAULT_API_BASE : apiBase.trim();
    }

    public String getApiBase() {
        return apiBase;
    }

    /** 按门店 secretKey 建并缓存 StripeClient；secretKey 缺失时抛 IllegalStateException。 */
    public StripeClient clientFor(String secretKey) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("Stripe secret key 缺失，无法构建客户端");
        }
        return clients.computeIfAbsent(secretKey, key -> StripeClient.builder()
                .setApiKey(key)
                .setApiBase(apiBase)
                .build());
    }
}
