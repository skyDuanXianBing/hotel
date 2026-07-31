package server.demo.service.saas;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import server.demo.entity.saas.SaasFeature;
import server.demo.entity.saas.SaasPackage;
import server.demo.entity.saas.SaasPackageFeature;
import server.demo.entity.saas.SaasQuotaAccount;
import server.demo.entity.saas.SaasQuotaLog;
import server.demo.entity.saas.SaasSubscription;
import server.demo.enums.SaasFeatureType;
import server.demo.enums.SaasPackagePeriod;
import server.demo.enums.SaasPackageStatus;
import server.demo.enums.SaasQuotaAction;
import server.demo.enums.SaasQuotaResetCycle;
import server.demo.enums.SaasSubscriptionStatus;
import server.demo.repository.StoreRepository;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

/**
 * 到期自动回退「默认版」（P9，业主拍板）：
 * 惰性判过期 → 标记 EXPIRED → 创建系统兜底订阅（ACTIVE/2099/0/remark，不下订单）；
 * 幂等（已存在兜底不重复创建）；从未有过订阅的门店不回退；未配置系统套餐时 fail-closed。
 */
class SaasDefaultPackageFallbackServiceTest {

    private static final long STORE_ID = 9L;
    private static final long SYSTEM_PACKAGE_ID = 4L;

    private SaasPackageRepository packageRepository;
    private SaasPackageFeatureRepository packageFeatureRepository;
    private SaasFeatureRepository featureRepository;
    private SaasSubscriptionRepository subscriptionRepository;
    private StoreRepository storeRepository;
    private SaasQuotaAccountRepository quotaAccountRepository;
    private SaasQuotaLogRepository quotaLogRepository;
    private SaasDefaultPackageFallbackService fallbackService;
    private EntitlementService entitlementService;

    @BeforeEach
    void setUp() {
        packageRepository = Mockito.mock(SaasPackageRepository.class);
        packageFeatureRepository = Mockito.mock(SaasPackageFeatureRepository.class);
        featureRepository = Mockito.mock(SaasFeatureRepository.class);
        subscriptionRepository = Mockito.mock(SaasSubscriptionRepository.class);
        storeRepository = Mockito.mock(StoreRepository.class);
        quotaAccountRepository = Mockito.mock(SaasQuotaAccountRepository.class);
        quotaLogRepository = Mockito.mock(SaasQuotaLogRepository.class);
        fallbackService = new SaasDefaultPackageFallbackService(
                packageRepository, packageFeatureRepository, featureRepository,
                subscriptionRepository, storeRepository,
                new SaasQuotaAccountAligner(quotaAccountRepository, featureRepository, quotaLogRepository),
                new ObjectMapper());
        entitlementService = new EntitlementService(
                subscriptionRepository, quotaAccountRepository, quotaLogRepository,
                featureRepository,
                new SaasQuotaAccountProvisioner(quotaAccountRepository, featureRepository),
                fallbackService,
                new ObjectMapper());

        lenient().when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private SaasPackage systemPackage() {
        SaasPackage pkg = new SaasPackage();
        pkg.setId(SYSTEM_PACKAGE_ID);
        pkg.setName("默认版");
        pkg.setVersion(1);
        pkg.setPrice(BigDecimal.ZERO);
        pkg.setPeriod(SaasPackagePeriod.MONTH);
        pkg.setStatus(SaasPackageStatus.OFF_SHELF);
        pkg.setIsSystem(true);
        return pkg;
    }

    private void stubSystemPackageWithFeatures() {
        Mockito.when(packageRepository.findFirstByIsSystemTrueOrderByIdAsc())
                .thenReturn(Optional.of(systemPackage()));

        SaasPackageFeature website = new SaasPackageFeature();
        website.setPackageId(SYSTEM_PACKAGE_ID);
        website.setFeatureCode("independent_website");
        website.setQuotaLimit(null);
        SaasPackageFeature aiGen = new SaasPackageFeature();
        aiGen.setPackageId(SYSTEM_PACKAGE_ID);
        aiGen.setFeatureCode("ai_website_gen");
        aiGen.setQuotaLimit(null);
        Mockito.when(packageFeatureRepository.findByPackageId(SYSTEM_PACKAGE_ID))
                .thenReturn(List.of(website, aiGen));

        SaasFeature websiteFeature = new SaasFeature();
        websiteFeature.setFeatureCode("independent_website");
        websiteFeature.setType(SaasFeatureType.BOOLEAN);
        SaasFeature aiFeature = new SaasFeature();
        aiFeature.setFeatureCode("ai_website_gen");
        aiFeature.setType(SaasFeatureType.QUOTA);
        Mockito.when(featureRepository.findByFeatureCode("independent_website"))
                .thenReturn(Optional.of(websiteFeature));
        Mockito.when(featureRepository.findByFeatureCode("ai_website_gen"))
                .thenReturn(Optional.of(aiFeature));
    }

    private SaasSubscription expiredActiveSubscription() {
        SaasSubscription subscription = new SaasSubscription();
        subscription.setId(88L);
        subscription.setStoreId(STORE_ID);
        subscription.setPackageId(1L);
        subscription.setPackageName("标准版");
        subscription.setEntitlementSnapshotJson("{\"features\":[]}");
        subscription.setPricePaid(new BigDecimal("99.00"));
        subscription.setStartTime(LocalDateTime.now().minusDays(35));
        subscription.setEndTime(LocalDateTime.now().minusDays(5));
        subscription.setStatus(SaasSubscriptionStatus.ACTIVE);
        return subscription;
    }

    // ------------------------------------------------------------------
    // 过期触发回退
    // ------------------------------------------------------------------

    @Test
    void expiredSubscription_marksExpiredAndCreatesFallbackWithoutOrder() {
        stubSystemPackageWithFeatures();
        SaasSubscription expired = expiredActiveSubscription();
        Mockito.when(subscriptionRepository.findFirstByStoreIdAndStatusOrderByEndTimeDesc(
                        STORE_ID, SaasSubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(expired));
        Mockito.when(subscriptionRepository.findByStoreIdAndStatus(STORE_ID, SaasSubscriptionStatus.ACTIVE))
                .thenReturn(List.of());

        Optional<SaasSubscription> result = entitlementService.findActiveSubscription(STORE_ID);

        // 旧订阅惰性标记 EXPIRED
        assertEquals(SaasSubscriptionStatus.EXPIRED, expired.getStatus());

        // 兜底订阅返回并落库：系统套餐 / ACTIVE / 2099 终点 / 实付 0 / remark 标记 / 快照冻结
        assertTrue(result.isPresent());
        ArgumentCaptor<SaasSubscription> captor = ArgumentCaptor.forClass(SaasSubscription.class);
        Mockito.verify(subscriptionRepository, Mockito.times(2)).save(captor.capture());
        SaasSubscription fallback = captor.getAllValues().stream()
                .filter(s -> SYSTEM_PACKAGE_ID == s.getPackageId())
                .findFirst().orElseThrow();
        assertEquals(STORE_ID, fallback.getStoreId());
        assertEquals("默认版", fallback.getPackageName());
        assertEquals(SaasSubscriptionStatus.ACTIVE, fallback.getStatus());
        assertEquals(SaasDefaultPackageFallbackService.FALLBACK_END_TIME, fallback.getEndTime());
        assertEquals(0, fallback.getPricePaid().compareTo(BigDecimal.ZERO));
        assertEquals(SaasDefaultPackageFallbackService.FALLBACK_REMARK, fallback.getRemark());
        EntitlementSnapshot snapshot = entitlementService.parseSnapshot(fallback);
        assertNotNull(snapshot.find("independent_website"));
        assertNotNull(snapshot.find("ai_website_gen"));

        // 同门店创建经 stores 行锁串行化
        Mockito.verify(storeRepository).findByIdForUpdate(STORE_ID);
    }

    @Test
    void expiredSubscription_existingFallback_idempotentReuse() {
        stubSystemPackageWithFeatures();
        SaasSubscription expired = expiredActiveSubscription();
        Mockito.when(subscriptionRepository.findFirstByStoreIdAndStatusOrderByEndTimeDesc(
                        STORE_ID, SaasSubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(expired));

        // 并发先到者已建好的兜底订阅（同系统套餐 ACTIVE）→ 直接复用，不重复创建
        SaasSubscription existingFallback = new SaasSubscription();
        existingFallback.setId(90L);
        existingFallback.setStoreId(STORE_ID);
        existingFallback.setPackageId(SYSTEM_PACKAGE_ID);
        existingFallback.setStatus(SaasSubscriptionStatus.ACTIVE);
        existingFallback.setEndTime(SaasDefaultPackageFallbackService.FALLBACK_END_TIME);
        Mockito.when(subscriptionRepository.findByStoreIdAndStatus(STORE_ID, SaasSubscriptionStatus.ACTIVE))
                .thenReturn(List.of(existingFallback));

        Optional<SaasSubscription> result = entitlementService.findActiveSubscription(STORE_ID);

        assertTrue(result.isPresent());
        assertEquals(90L, result.get().getId());
        // 仅旧订阅的 EXPIRED 落库，不再新建订阅
        Mockito.verify(subscriptionRepository, Mockito.times(1)).save(expired);
        Mockito.verify(subscriptionRepository, never()).save(existingFallback);
    }

    // ------------------------------------------------------------------
    // 回退时配额账户对齐（P10）
    // ------------------------------------------------------------------

    @Test
    void expiredSubscription_fallbackAlignsLegacyQuotaAccountToNullLimit() {
        // 核心回归：豪华版时期建的配额账户 total=50，回退默认版（ai_website_gen 不限）后
        // 必须对齐为 null（保留 used），my-subscription 不再误显示「已用 0/50」
        stubSystemPackageWithFeatures();
        SaasSubscription expired = expiredActiveSubscription();
        Mockito.when(subscriptionRepository.findFirstByStoreIdAndStatusOrderByEndTimeDesc(
                        STORE_ID, SaasSubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(expired));
        Mockito.when(subscriptionRepository.findByStoreIdAndStatus(STORE_ID, SaasSubscriptionStatus.ACTIVE))
                .thenReturn(List.of());

        SaasQuotaAccount legacyAccount = new SaasQuotaAccount();
        legacyAccount.setId(51L);
        legacyAccount.setStoreId(STORE_ID);
        legacyAccount.setFeatureCode("ai_website_gen");
        legacyAccount.setTotalQuota(50L);
        legacyAccount.setUsedQuota(12L);
        legacyAccount.setResetCycle(SaasQuotaResetCycle.MONTHLY);
        legacyAccount.setPeriodStart(LocalDateTime.now().minusDays(10));
        legacyAccount.setPeriodEnd(LocalDateTime.now().plusDays(20));
        Mockito.when(quotaAccountRepository.findByStoreIdAndFeatureCode(STORE_ID, "ai_website_gen"))
                .thenReturn(Optional.of(legacyAccount));

        Optional<SaasSubscription> result = entitlementService.findActiveSubscription(STORE_ID);

        assertTrue(result.isPresent());
        // 账户总额对齐为 null（不限），used 保留，写 LIMIT_CHANGE 流水
        assertEquals(null, legacyAccount.getTotalQuota());
        assertEquals(12L, legacyAccount.getUsedQuota());
        Mockito.verify(quotaAccountRepository).save(legacyAccount);
        ArgumentCaptor<SaasQuotaLog> logCaptor = ArgumentCaptor.forClass(SaasQuotaLog.class);
        Mockito.verify(quotaLogRepository).save(logCaptor.capture());
        assertEquals(SaasQuotaAction.LIMIT_CHANGE, logCaptor.getValue().getAction());
        assertEquals("system", logCaptor.getValue().getOperator());
    }

    @Test
    void existingFallback_reusePathAlsoHealsStaleQuotaAccount() {
        // 存量兜底订阅（本修复前创建，账户从未对齐）：复用路径也做幂等对齐
        stubSystemPackageWithFeatures();
        SaasSubscription expired = expiredActiveSubscription();
        Mockito.when(subscriptionRepository.findFirstByStoreIdAndStatusOrderByEndTimeDesc(
                        STORE_ID, SaasSubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(expired));

        SaasSubscription existingFallback = new SaasSubscription();
        existingFallback.setId(90L);
        existingFallback.setStoreId(STORE_ID);
        existingFallback.setPackageId(SYSTEM_PACKAGE_ID);
        existingFallback.setStatus(SaasSubscriptionStatus.ACTIVE);
        existingFallback.setEndTime(SaasDefaultPackageFallbackService.FALLBACK_END_TIME);
        Mockito.when(subscriptionRepository.findByStoreIdAndStatus(STORE_ID, SaasSubscriptionStatus.ACTIVE))
                .thenReturn(List.of(existingFallback));

        SaasQuotaAccount staleAccount = new SaasQuotaAccount();
        staleAccount.setId(52L);
        staleAccount.setStoreId(STORE_ID);
        staleAccount.setFeatureCode("ai_website_gen");
        staleAccount.setTotalQuota(50L);
        staleAccount.setUsedQuota(7L);
        staleAccount.setResetCycle(SaasQuotaResetCycle.MONTHLY);
        staleAccount.setPeriodStart(LocalDateTime.now().minusDays(3));
        staleAccount.setPeriodEnd(LocalDateTime.now().plusDays(27));
        Mockito.when(quotaAccountRepository.findByStoreIdAndFeatureCode(STORE_ID, "ai_website_gen"))
                .thenReturn(Optional.of(staleAccount));

        Optional<SaasSubscription> result = entitlementService.findActiveSubscription(STORE_ID);

        assertTrue(result.isPresent());
        assertEquals(90L, result.get().getId());
        assertEquals(null, staleAccount.getTotalQuota());
        assertEquals(7L, staleAccount.getUsedQuota());
        Mockito.verify(quotaAccountRepository).save(staleAccount);
        // 不重复创建订阅（仅旧订阅 EXPIRED 落库一次）
        Mockito.verify(subscriptionRepository, Mockito.times(1)).save(expired);
    }

    // ------------------------------------------------------------------
    // 不回退 / fail-closed 分支
    // ------------------------------------------------------------------

    @Test
    void neverSubscribedStore_noFallbackCreated() {
        Mockito.when(subscriptionRepository.findFirstByStoreIdAndStatusOrderByEndTimeDesc(
                        eq(STORE_ID), any()))
                .thenReturn(Optional.empty());

        Optional<SaasSubscription> result = entitlementService.findActiveSubscription(STORE_ID);

        // 从未有过订阅的门店维持无订阅（402），不触发兜底
        assertTrue(result.isEmpty());
        Mockito.verifyNoInteractions(packageRepository);
        Mockito.verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void missingSystemPackage_failClosed() {
        Mockito.when(packageRepository.findFirstByIsSystemTrueOrderByIdAsc())
                .thenReturn(Optional.empty());
        SaasSubscription expired = expiredActiveSubscription();
        Mockito.when(subscriptionRepository.findFirstByStoreIdAndStatusOrderByEndTimeDesc(
                        STORE_ID, SaasSubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(expired));

        Optional<SaasSubscription> result = entitlementService.findActiveSubscription(STORE_ID);

        assertEquals(SaasSubscriptionStatus.EXPIRED, expired.getStatus());
        assertTrue(result.isEmpty());
        Mockito.verify(subscriptionRepository, never()).save(
                Mockito.argThat(s -> s.getPackageId() != null && s.getPackageId() == SYSTEM_PACKAGE_ID));
    }

    @Test
    void missingFeatureDictEntry_skippedButFallbackStillCreated() {
        stubSystemPackageWithFeatures();
        // 字典缺失 ai_website_gen：兜底订阅仍创建，快照仅含可查到的权益
        Mockito.when(featureRepository.findByFeatureCode("ai_website_gen"))
                .thenReturn(Optional.empty());
        SaasSubscription expired = expiredActiveSubscription();
        Mockito.when(subscriptionRepository.findFirstByStoreIdAndStatusOrderByEndTimeDesc(
                        STORE_ID, SaasSubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(expired));
        Mockito.when(subscriptionRepository.findByStoreIdAndStatus(STORE_ID, SaasSubscriptionStatus.ACTIVE))
                .thenReturn(List.of());

        Optional<SaasSubscription> result = entitlementService.findActiveSubscription(STORE_ID);

        assertTrue(result.isPresent());
        EntitlementSnapshot snapshot = entitlementService.parseSnapshot(result.get());
        assertNotNull(snapshot.find("independent_website"));
        assertEquals(null, snapshot.find("ai_website_gen"));
    }

    // ------------------------------------------------------------------
    // storeHasFeature 非抛异常判定
    // ------------------------------------------------------------------

    @Test
    void storeHasFeature_activeSubscriptionWithBooleanFeature_true() {
        SaasSubscription active = expiredActiveSubscription();
        active.setEndTime(LocalDateTime.now().plusDays(10));
        active.setEntitlementSnapshotJson(
                "{\"features\":[{\"featureCode\":\"independent_website\",\"type\":\"BOOLEAN\",\"limit\":null}]}");
        Mockito.when(subscriptionRepository.findFirstByStoreIdAndStatusOrderByEndTimeDesc(
                        STORE_ID, SaasSubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(active));

        assertTrue(entitlementService.storeHasFeature(STORE_ID, "independent_website"));
    }

    @Test
    void storeHasFeature_noSubscriptionOrFeatureMissing_false() {
        Mockito.when(subscriptionRepository.findFirstByStoreIdAndStatusOrderByEndTimeDesc(
                        eq(STORE_ID), any()))
                .thenReturn(Optional.empty());
        assertTrue(!entitlementService.storeHasFeature(STORE_ID, "independent_website"));

        SaasSubscription active = expiredActiveSubscription();
        active.setEndTime(LocalDateTime.now().plusDays(10));
        Mockito.when(subscriptionRepository.findFirstByStoreIdAndStatusOrderByEndTimeDesc(
                        STORE_ID, SaasSubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(active));
        // 快照为空 features → 权益缺失 false（不抛异常）
        assertTrue(!entitlementService.storeHasFeature(STORE_ID, "independent_website"));
    }
}
