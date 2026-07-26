package server.demo.service;

import org.junit.jupiter.api.Test;
import server.demo.dto.IndependentSiteDtos;
import server.demo.entity.IndependentSiteStripeSettings;
import server.demo.repository.IndependentSiteStripeSettingsRepository;
import server.demo.util.AesGcmCrypto;

import java.lang.reflect.Proxy;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 门店 Stripe 设置服务测试。仓库为 JDK 动态代理假实现（不用 Mockito）；
 * 加密为真实 AES-GCM（测试密钥），覆盖写入/留空不变/前缀校验/尾 4 位/脱敏/解密失败降级。
 */
class IndependentSiteStripeSettingsServiceTest {

    private static final String ENCRYPTION_KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String OTHER_ENCRYPTION_KEY;

    static {
        byte[] other = new byte[32];
        other[0] = 7;
        OTHER_ENCRYPTION_KEY = Base64.getEncoder().encodeToString(other);
    }

    private static final String PUBLISHABLE_KEY = "pk_test_settings_1001";
    private static final String SECRET_KEY = "sk_test_settings_secret_1002";
    private static final String WEBHOOK_SECRET = "whsec_settings_secret_1003";

    @Test
    void updateSettings_shouldEncryptSecretsAndReturnMaskedView() {
        Fixture fixture = new Fixture(ENCRYPTION_KEY);

        IndependentSiteDtos.StripeSettingsResponse response = fixture.service.updateSettings(
                1L,
                new IndependentSiteDtos.StripeSettingsUpdateRequest(
                        PUBLISHABLE_KEY,
                        SECRET_KEY,
                        WEBHOOK_SECRET
                )
        );

        assertTrue(response.configured());
        assertEquals(PUBLISHABLE_KEY, response.publishableKey());
        assertTrue(response.secretKeyConfigured());
        assertEquals("1002", response.secretKeyLast4());
        assertTrue(response.webhookSecretConfigured());
        assertEquals("1003", response.webhookSecretLast4());

        // 落库两列必须是密文，且可用同一密钥解密还原
        IndependentSiteStripeSettings row = fixture.rows.get(1L);
        assertNotEquals(SECRET_KEY, row.getSecretKeyEncrypted());
        assertNotEquals(WEBHOOK_SECRET, row.getWebhookSecretEncrypted());
        assertFalse(row.getSecretKeyEncrypted().contains(SECRET_KEY));
        AesGcmCrypto crypto = AesGcmCrypto.fromBase64Key(ENCRYPTION_KEY);
        assertEquals(SECRET_KEY, crypto.decrypt(row.getSecretKeyEncrypted()));
        assertEquals(WEBHOOK_SECRET, crypto.decrypt(row.getWebhookSecretEncrypted()));

        // GET 同源：configured=true，任何字段都不含 sk/whsec 明文
        IndependentSiteDtos.StripeSettingsResponse read = fixture.service.getSettings(1L);
        assertTrue(read.configured());
        assertEquals(response.publishableKey(), read.publishableKey());
        assertEquals("1002", read.secretKeyLast4());
        assertEquals("1003", read.webhookSecretLast4());
    }

    @Test
    void getSettings_withoutRow_shouldReturnUnconfigured() {
        Fixture fixture = new Fixture(ENCRYPTION_KEY);

        IndependentSiteDtos.StripeSettingsResponse response = fixture.service.getSettings(99L);

        assertFalse(response.configured());
        assertNull(response.publishableKey());
        assertFalse(response.secretKeyConfigured());
        assertNull(response.secretKeyLast4());
        assertFalse(response.webhookSecretConfigured());
        assertNull(response.webhookSecretLast4());
    }

    @Test
    void updateSettings_blankFields_shouldKeepExistingValues() {
        Fixture fixture = new Fixture(ENCRYPTION_KEY);
        fixture.service.updateSettings(
                1L,
                new IndependentSiteDtos.StripeSettingsUpdateRequest(
                        PUBLISHABLE_KEY,
                        SECRET_KEY,
                        WEBHOOK_SECRET
                )
        );
        String originalSecretCiphertext = fixture.rows.get(1L).getSecretKeyEncrypted();
        String originalWebhookCiphertext = fixture.rows.get(1L).getWebhookSecretEncrypted();

        // 只更新 publishableKey，其余留空 = 不变
        IndependentSiteDtos.StripeSettingsResponse response = fixture.service.updateSettings(
                1L,
                new IndependentSiteDtos.StripeSettingsUpdateRequest("pk_test_replaced_2001", " ", null)
        );

        assertEquals("pk_test_replaced_2001", response.publishableKey());
        assertEquals("1002", response.secretKeyLast4());
        assertEquals("1003", response.webhookSecretLast4());
        assertEquals(originalSecretCiphertext, fixture.rows.get(1L).getSecretKeyEncrypted());
        assertEquals(originalWebhookCiphertext, fixture.rows.get(1L).getWebhookSecretEncrypted());

        // 只更新 secretKey，whsec/pk 不动
        IndependentSiteDtos.StripeSettingsResponse rotated = fixture.service.updateSettings(
                1L,
                new IndependentSiteDtos.StripeSettingsUpdateRequest(null, "sk_test_rotated_3002", "")
        );
        assertEquals("3002", rotated.secretKeyLast4());
        assertEquals("pk_test_replaced_2001", rotated.publishableKey());
        assertEquals("1003", rotated.webhookSecretLast4());
    }

    @Test
    void updateSettings_shouldValidateKeyPrefixes() {
        Fixture fixture = new Fixture(ENCRYPTION_KEY);

        IndependentSiteServiceException badPk = assertThrows(
                IndependentSiteServiceException.class,
                () -> fixture.service.updateSettings(
                        1L,
                        new IndependentSiteDtos.StripeSettingsUpdateRequest("sk_test_wrong_prefix", null, null)
                )
        );
        assertEquals("INVALID_STRIPE_KEY", badPk.getCode());
        assertEquals(400, badPk.getStatus().value());

        IndependentSiteServiceException badSk = assertThrows(
                IndependentSiteServiceException.class,
                () -> fixture.service.updateSettings(
                        1L,
                        new IndependentSiteDtos.StripeSettingsUpdateRequest(null, "pk_test_wrong_prefix", null)
                )
        );
        assertEquals("INVALID_STRIPE_KEY", badSk.getCode());

        IndependentSiteServiceException badWhsec = assertThrows(
                IndependentSiteServiceException.class,
                () -> fixture.service.updateSettings(
                        1L,
                        new IndependentSiteDtos.StripeSettingsUpdateRequest(null, null, "sk_test_wrong_prefix")
                )
        );
        assertEquals("INVALID_STRIPE_KEY", badWhsec.getCode());
        assertTrue(fixture.rows.isEmpty());
    }

    @Test
    void updateSettings_secretsWithoutEncryptionKey_shouldFailWithExplicitError() {
        Fixture fixture = new Fixture("");

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> fixture.service.updateSettings(
                        1L,
                        new IndependentSiteDtos.StripeSettingsUpdateRequest(null, SECRET_KEY, null)
                )
        );

        assertEquals("STRIPE_ENCRYPTION_NOT_CONFIGURED", exception.getCode());
        assertEquals(500, exception.getStatus().value());
        assertTrue(fixture.rows.isEmpty());

        // 仅保存 publishableKey（无需加密）仍可用
        IndependentSiteDtos.StripeSettingsResponse pkOnly = fixture.service.updateSettings(
                1L,
                new IndependentSiteDtos.StripeSettingsUpdateRequest(PUBLISHABLE_KEY, null, null)
        );
        assertEquals(PUBLISHABLE_KEY, pkOnly.publishableKey());
        assertFalse(pkOnly.configured());
    }

    @Test
    void resolveForStore_shouldReturnDecryptedKeys() {
        Fixture fixture = new Fixture(ENCRYPTION_KEY);
        fixture.service.updateSettings(
                1L,
                new IndependentSiteDtos.StripeSettingsUpdateRequest(
                        PUBLISHABLE_KEY,
                        SECRET_KEY,
                        WEBHOOK_SECRET
                )
        );

        Optional<IndependentSiteStripeSettingsService.ResolvedStripeKeys> resolved =
                fixture.service.resolveForStore(1L);

        assertTrue(resolved.isPresent());
        assertEquals(PUBLISHABLE_KEY, resolved.get().publishableKey());
        assertEquals(SECRET_KEY, resolved.get().secretKey());
        assertEquals(WEBHOOK_SECRET, resolved.get().webhookSecret());
        assertTrue(resolved.get().isFullyConfigured());
        assertTrue(fixture.service.isFullyConfigured(1L));
        assertTrue(fixture.service.resolveForStore(99L).isEmpty());
        assertFalse(fixture.service.isFullyConfigured(99L));
    }

    @Test
    void resolveForStore_partialSettings_shouldNotBeFullyConfigured() {
        Fixture fixture = new Fixture(ENCRYPTION_KEY);
        fixture.service.updateSettings(
                1L,
                new IndependentSiteDtos.StripeSettingsUpdateRequest(PUBLISHABLE_KEY, SECRET_KEY, null)
        );

        IndependentSiteStripeSettingsService.ResolvedStripeKeys resolved =
                fixture.service.resolveForStore(1L).orElseThrow();

        assertTrue(resolved.hasSecretKey());
        assertFalse(resolved.hasWebhookSecret());
        assertFalse(resolved.isFullyConfigured());
        assertFalse(fixture.service.isFullyConfigured(1L));
    }

    @Test
    void readWithWrongEncryptionKey_shouldDegradeToUnconfiguredWithoutThrowing() {
        Fixture writer = new Fixture(ENCRYPTION_KEY);
        writer.service.updateSettings(
                1L,
                new IndependentSiteDtos.StripeSettingsUpdateRequest(
                        PUBLISHABLE_KEY,
                        SECRET_KEY,
                        WEBHOOK_SECRET
                )
        );

        // 另一把密钥构造读取侧：解密失败按未配置处理（warn 降级，不抛出）
        Fixture wrongKey = new Fixture(OTHER_ENCRYPTION_KEY);
        wrongKey.rows.putAll(writer.rows);

        IndependentSiteDtos.StripeSettingsResponse response = wrongKey.service.getSettings(1L);
        assertFalse(response.configured());
        assertFalse(response.secretKeyConfigured());
        assertNull(response.secretKeyLast4());
        assertFalse(response.webhookSecretConfigured());
        assertNull(response.webhookSecretLast4());
        assertFalse(wrongKey.service.isFullyConfigured(1L));
        IndependentSiteStripeSettingsService.ResolvedStripeKeys resolved =
                wrongKey.service.resolveForStore(1L).orElseThrow();
        assertFalse(resolved.hasSecretKey());
        assertFalse(resolved.hasWebhookSecret());
    }

    @Test
    void readWithoutEncryptionKey_shouldDegradeSecretsToUnconfigured() {
        Fixture writer = new Fixture(ENCRYPTION_KEY);
        writer.service.updateSettings(
                1L,
                new IndependentSiteDtos.StripeSettingsUpdateRequest(
                        PUBLISHABLE_KEY,
                        SECRET_KEY,
                        WEBHOOK_SECRET
                )
        );

        Fixture noKey = new Fixture("");
        noKey.rows.putAll(writer.rows);

        IndependentSiteDtos.StripeSettingsResponse response = noKey.service.getSettings(1L);
        assertEquals(PUBLISHABLE_KEY, response.publishableKey());
        assertFalse(response.configured());
        assertFalse(response.secretKeyConfigured());
        assertFalse(noKey.service.isFullyConfigured(1L));
    }

    // ------------------------------------------------------------------
    // 测试基础设施
    // ------------------------------------------------------------------

    private static final class Fixture {
        private final Map<Long, IndependentSiteStripeSettings> rows = new LinkedHashMap<>();
        private long sequence = 5000L;
        private final IndependentSiteStripeSettingsService service;

        private Fixture(String encryptionKey) {
            IndependentSiteStripeSettingsRepository repository = repository();
            this.service = new IndependentSiteStripeSettingsService(repository, encryptionKey);
        }

        private IndependentSiteStripeSettingsRepository repository() {
            return (IndependentSiteStripeSettingsRepository) Proxy.newProxyInstance(
                    IndependentSiteStripeSettingsRepository.class.getClassLoader(),
                    new Class<?>[]{IndependentSiteStripeSettingsRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findByStoreId" -> Optional.ofNullable(rows.get(args[0]));
                        case "save" -> save((IndependentSiteStripeSettings) args[0]);
                        case "toString" -> "IndependentSiteStripeSettingsRepositoryProxy";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new AssertionError("Unexpected repository method: " + method);
                    }
            );
        }

        private IndependentSiteStripeSettings save(IndependentSiteStripeSettings settings) {
            if (settings.getId() == null) {
                settings.setId(++sequence);
            }
            rows.put(settings.getStoreId(), settings);
            return settings;
        }
    }
}
