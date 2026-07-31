package server.demo.service.saas;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import server.demo.entity.Store;
import server.demo.entity.saas.SaasBillingOrder;
import server.demo.entity.saas.SaasFeature;
import server.demo.entity.saas.SaasPackage;
import server.demo.entity.saas.SaasPackageFeature;
import server.demo.entity.saas.SaasQuotaAccount;
import server.demo.entity.saas.SaasQuotaLog;
import server.demo.entity.saas.SaasSubscription;
import server.demo.enums.SaasFeatureType;
import server.demo.enums.SaasPackagePeriod;
import server.demo.enums.SaasPackageStatus;
import server.demo.enums.SaasQuotaResetCycle;
import server.demo.enums.SaasSubscriptionStatus;
import server.demo.repository.StoreRepository;
import server.demo.repository.saas.SaasBillingOrderRepository;
import server.demo.repository.saas.SaasFeatureRepository;
import server.demo.repository.saas.SaasPackageFeatureRepository;
import server.demo.repository.saas.SaasPackageRepository;
import server.demo.repository.saas.SaasQuotaAccountRepository;
import server.demo.repository.saas.SaasQuotaLogRepository;
import server.demo.repository.saas.SaasSubscriptionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

/**
 * 购买/人工开通幂等（审查 B1/B2 修复）：
 * 同 idempotencyKey 的重复请求幂等重放——返回原订阅、不新建订单/订阅；
 * 门店行悲观锁串行化 + 锁内复查将并发冲突转化为重放；uk 仅为最终兜底。
 */
class SaasBillingServiceIdempotencyTest {

    private static final long STORE_ID = 9L;
    private static final String KEY = "6f9c2f6e-9b1a-4c7e-9f2d-2f6f9d2a0001";

    private SaasPackageRepository packageRepository;
    private SaasPackageFeatureRepository packageFeatureRepository;
    private SaasFeatureRepository featureRepository;
    private SaasSubscriptionRepository subscriptionRepository;
    private SaasQuotaAccountRepository quotaAccountRepository;
    private SaasBillingOrderRepository billingOrderRepository;
    private SaasQuotaLogRepository quotaLogRepository;
    private StoreRepository storeRepository;
    private EntitlementService entitlementService;
    private SaasBillingService billingService;

    @BeforeEach
    void setUp() {
        packageRepository = Mockito.mock(SaasPackageRepository.class);
        packageFeatureRepository = Mockito.mock(SaasPackageFeatureRepository.class);
        featureRepository = Mockito.mock(SaasFeatureRepository.class);
        subscriptionRepository = Mockito.mock(SaasSubscriptionRepository.class);
        quotaAccountRepository = Mockito.mock(SaasQuotaAccountRepository.class);
        billingOrderRepository = Mockito.mock(SaasBillingOrderRepository.class);
        quotaLogRepository = Mockito.mock(SaasQuotaLogRepository.class);
        storeRepository = Mockito.mock(StoreRepository.class);
        entitlementService = new EntitlementService(
                subscriptionRepository, quotaAccountRepository, quotaLogRepository,
                featureRepository,
                new SaasQuotaAccountProvisioner(quotaAccountRepository, featureRepository),
                Mockito.mock(SaasDefaultPackageFallbackService.class),
                new ObjectMapper());
        billingService = new SaasBillingService(
                packageRepository, packageFeatureRepository, featureRepository,
                subscriptionRepository,
                new SaasQuotaAccountAligner(quotaAccountRepository, featureRepository, quotaLogRepository),
                billingOrderRepository, entitlementService, storeRepository,
                new SaasBillingReplayService(billingOrderRepository, subscriptionRepository));

        lenient().when(billingOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(quotaAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(quotaLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private SaasPackage onShelfPackage() {
        SaasPackage pkg = new SaasPackage();
        pkg.setId(1L);
        pkg.setName("标准版");
        pkg.setVersion(1);
        pkg.setPrice(new BigDecimal("99.00"));
        pkg.setPeriod(SaasPackagePeriod.MONTH);
        pkg.setStatus(SaasPackageStatus.ON_SHELF);
        return pkg;
    }

    private SaasSubscription activeSubscription() {
        SaasSubscription subscription = new SaasSubscription();
        subscription.setId(100L);
        subscription.setStoreId(STORE_ID);
        subscription.setPackageId(1L);
        subscription.setPackageName("标准版");
        subscription.setStatus(SaasSubscriptionStatus.ACTIVE);
        subscription.setStartTime(LocalDateTime.now().minusDays(1));
        subscription.setEndTime(LocalDateTime.now().plusDays(29));
        return subscription;
    }

    private SaasBillingOrder existingOrder(String key) {
        SaasBillingOrder order = new SaasBillingOrder();
        order.setStoreId(STORE_ID);
        order.setPackageId(1L);
        order.setAmount(new BigDecimal("99.00"));
        order.setIdempotencyKey(key);
        return order;
    }

    private void stubPackageFeatures() {
        SaasPackageFeature aiGen = new SaasPackageFeature();
        aiGen.setPackageId(1L);
        aiGen.setFeatureCode("ai_website_gen");
        aiGen.setQuotaLimit(5L);
        Mockito.when(packageFeatureRepository.findByPackageId(1L)).thenReturn(List.of(aiGen));
        SaasFeature aiFeature = new SaasFeature();
        aiFeature.setFeatureCode("ai_website_gen");
        aiFeature.setType(SaasFeatureType.QUOTA);
        aiFeature.setDefaultResetCycle(SaasQuotaResetCycle.MONTHLY);
        Mockito.when(featureRepository.findByFeatureCode("ai_website_gen"))
                .thenReturn(Optional.of(aiFeature));
    }

    // ------------------------------------------------------------------
    // 锁外快路径：同 key 二次请求直接重放
    // ------------------------------------------------------------------

    @Test
    void subscribe_sameKeySecondTime_replaysOriginalSubscription_noNewOrderNoNewSubscription() {
        Mockito.when(packageRepository.findById(1L)).thenReturn(Optional.of(onShelfPackage()));
        SaasSubscription existing = activeSubscription();
        Mockito.when(billingOrderRepository.findByStoreIdAndIdempotencyKey(STORE_ID, KEY))
                .thenReturn(Optional.of(existingOrder(KEY)));
        Mockito.when(subscriptionRepository.findFirstByStoreIdAndStatusOrderByEndTimeDesc(
                        STORE_ID, SaasSubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(existing));

        SaasSubscription result = billingService.subscribe(STORE_ID, 1L, "user:1", KEY);

        // 幂等重放：返回已有订单对应的订阅，订单数/订阅数不增
        assertSame(existing, result);
        Mockito.verify(billingOrderRepository, never()).save(any());
        Mockito.verify(subscriptionRepository, never()).save(any());
        Mockito.verifyNoInteractions(quotaAccountRepository, quotaLogRepository);
        // 锁外快路径命中，无需进入门店行锁
        Mockito.verify(storeRepository, never()).findByIdForUpdate(any());
    }

    // ------------------------------------------------------------------
    // 锁内复查：并发先到者提交后，后到者在锁内转化为重放（不触发唯一键冲突）
    // ------------------------------------------------------------------

    @Test
    void subscribe_concurrentWinnerCommitted_recheckAfterLockReplays() {
        Mockito.when(packageRepository.findById(1L)).thenReturn(Optional.of(onShelfPackage()));
        Store store = new Store();
        store.setId(STORE_ID);
        Mockito.when(storeRepository.findByIdForUpdate(STORE_ID)).thenReturn(Optional.of(store));
        SaasSubscription winnerSubscription = activeSubscription();
        // 锁外首次查询：并发订单尚不可见；锁内复查：先到者已提交，命中
        Mockito.when(billingOrderRepository.findByStoreIdAndIdempotencyKey(STORE_ID, KEY))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingOrder(KEY)));
        Mockito.when(subscriptionRepository.findFirstByStoreIdAndStatusOrderByEndTimeDesc(
                        STORE_ID, SaasSubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(winnerSubscription));

        SaasSubscription result = billingService.subscribe(STORE_ID, 1L, "user:1", KEY);

        assertSame(winnerSubscription, result);
        Mockito.verify(storeRepository).findByIdForUpdate(STORE_ID);
        Mockito.verify(billingOrderRepository, Mockito.times(2))
                .findByStoreIdAndIdempotencyKey(STORE_ID, KEY);
        Mockito.verify(billingOrderRepository, never()).save(any());
        Mockito.verify(subscriptionRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // 正常首购：幂等键随订单落库
    // ------------------------------------------------------------------

    @Test
    void subscribe_freshKey_createsOrderCarryingKey() {
        Mockito.when(packageRepository.findById(1L)).thenReturn(Optional.of(onShelfPackage()));
        stubPackageFeatures();
        Mockito.when(billingOrderRepository.findByStoreIdAndIdempotencyKey(STORE_ID, KEY))
                .thenReturn(Optional.empty());
        Mockito.when(subscriptionRepository.findByStoreIdAndStatus(STORE_ID, SaasSubscriptionStatus.ACTIVE))
                .thenReturn(List.of());
        Mockito.when(quotaAccountRepository.findByStoreIdAndFeatureCode(STORE_ID, "ai_website_gen"))
                .thenReturn(Optional.empty());

        SaasSubscription result = billingService.subscribe(STORE_ID, 1L, "user:1", KEY);

        ArgumentCaptor<SaasBillingOrder> orderCaptor = ArgumentCaptor.forClass(SaasBillingOrder.class);
        Mockito.verify(billingOrderRepository).save(orderCaptor.capture());
        assertEquals(KEY, orderCaptor.getValue().getIdempotencyKey());
        assertEquals(SaasSubscriptionStatus.ACTIVE, result.getStatus());
        // 每次激活都会取门店行锁串行化
        Mockito.verify(storeRepository).findByIdForUpdate(STORE_ID);
    }

    @Test
    void subscribe_blankKey_treatedAsAbsent_behavesLikeLegacyCaller() {
        Mockito.when(packageRepository.findById(1L)).thenReturn(Optional.of(onShelfPackage()));
        stubPackageFeatures();
        Mockito.when(subscriptionRepository.findByStoreIdAndStatus(STORE_ID, SaasSubscriptionStatus.ACTIVE))
                .thenReturn(List.of());
        Mockito.when(quotaAccountRepository.findByStoreIdAndFeatureCode(STORE_ID, "ai_website_gen"))
                .thenReturn(Optional.empty());

        billingService.subscribe(STORE_ID, 1L, "user:1", "   ");

        // 空白键视为未带键：不按 key 查询、订单键为 null（兼容旧调用，uk 多个 NULL 不冲突）
        Mockito.verify(billingOrderRepository, never()).findByStoreIdAndIdempotencyKey(any(), any());
        ArgumentCaptor<SaasBillingOrder> orderCaptor = ArgumentCaptor.forClass(SaasBillingOrder.class);
        Mockito.verify(billingOrderRepository).save(orderCaptor.capture());
        assertNull(orderCaptor.getValue().getIdempotencyKey());
    }

    @Test
    void subscribe_tooLongKey_rejectedBeforeAnyWrite() {
        Mockito.when(packageRepository.findById(1L)).thenReturn(Optional.of(onShelfPackage()));
        String tooLong = "x".repeat(65);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> billingService.subscribe(STORE_ID, 1L, "user:1", tooLong));
        assertEquals("idempotencyKey 长度不能超过 64 字符", e.getMessage());
        Mockito.verify(billingOrderRepository, never()).save(any());
        Mockito.verify(subscriptionRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // 管理端人工开通同机制（B2）
    // ------------------------------------------------------------------

    @Test
    void grantByAdmin_sameKeySecondTime_replaysOriginalSubscription_noDuplicateManualOrder() {
        SaasPackage offShelf = onShelfPackage();
        offShelf.setStatus(SaasPackageStatus.OFF_SHELF); // 人工开通允许停售套餐
        Mockito.when(packageRepository.findById(1L)).thenReturn(Optional.of(offShelf));
        SaasSubscription existing = activeSubscription();
        Mockito.when(billingOrderRepository.findByStoreIdAndIdempotencyKey(STORE_ID, KEY))
                .thenReturn(Optional.of(existingOrder(KEY)));
        Mockito.when(subscriptionRepository.findFirstByStoreIdAndStatusOrderByEndTimeDesc(
                        STORE_ID, SaasSubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(existing));

        SaasSubscription result = billingService.grantByAdmin(STORE_ID, 1L, "admin", KEY);

        assertSame(existing, result);
        Mockito.verify(billingOrderRepository, never()).save(any());
        Mockito.verify(subscriptionRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // 重放解析：订单对应订阅此后被切换/取消时，返回门店最新订阅（如实展示状态）
    // ------------------------------------------------------------------

    @Test
    void replay_noActiveSubscription_fallsBackToLatestSubscription() {
        Mockito.when(packageRepository.findById(1L)).thenReturn(Optional.of(onShelfPackage()));
        SaasSubscription cancelled = activeSubscription();
        cancelled.setStatus(SaasSubscriptionStatus.CANCELLED);
        Mockito.when(billingOrderRepository.findByStoreIdAndIdempotencyKey(STORE_ID, KEY))
                .thenReturn(Optional.of(existingOrder(KEY)));
        Mockito.when(subscriptionRepository.findFirstByStoreIdAndStatusOrderByEndTimeDesc(
                        STORE_ID, SaasSubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        Mockito.when(subscriptionRepository.findFirstByStoreIdOrderByIdDesc(STORE_ID))
                .thenReturn(Optional.of(cancelled));

        SaasSubscription result = billingService.subscribe(STORE_ID, 1L, "user:1", KEY);

        assertSame(cancelled, result);
        Mockito.verify(billingOrderRepository, never()).save(any());
    }

    @Test
    void replay_orderWithoutAnySubscription_throwsIllegalState() {
        Mockito.when(packageRepository.findById(1L)).thenReturn(Optional.of(onShelfPackage()));
        Mockito.when(billingOrderRepository.findByStoreIdAndIdempotencyKey(STORE_ID, KEY))
                .thenReturn(Optional.of(existingOrder(KEY)));
        Mockito.when(subscriptionRepository.findFirstByStoreIdAndStatusOrderByEndTimeDesc(
                        STORE_ID, SaasSubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        Mockito.when(subscriptionRepository.findFirstByStoreIdOrderByIdDesc(STORE_ID))
                .thenReturn(Optional.empty());

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> billingService.subscribe(STORE_ID, 1L, "user:1", KEY));
        assertTrue(e.getMessage().contains("幂等订单存在但订阅缺失"));
    }
}
