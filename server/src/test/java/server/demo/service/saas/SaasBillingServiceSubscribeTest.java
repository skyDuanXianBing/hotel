package server.demo.service.saas;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import server.demo.entity.saas.SaasBillingOrder;
import server.demo.entity.saas.SaasFeature;
import server.demo.entity.saas.SaasPackage;
import server.demo.entity.saas.SaasPackageFeature;
import server.demo.entity.saas.SaasQuotaAccount;
import server.demo.entity.saas.SaasQuotaLog;
import server.demo.entity.saas.SaasSubscription;
import server.demo.enums.SaasBillingOrderStatus;
import server.demo.enums.SaasBillingProvider;
import server.demo.enums.SaasFeatureType;
import server.demo.enums.SaasPackagePeriod;
import server.demo.enums.SaasPackageStatus;
import server.demo.enums.SaasQuotaAction;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

/**
 * 订阅购买全流程：直连订单（DIRECT/PAID）→ 旧订阅取消 → 新订阅激活（快照冻结）→ 配额账户初始化。
 */
class SaasBillingServiceSubscribeTest {

    private static final long STORE_ID = 9L;

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

    private SaasPackage standardPackage(SaasPackageStatus status) {
        SaasPackage pkg = new SaasPackage();
        pkg.setId(1L);
        pkg.setName("标准版");
        pkg.setVersion(1);
        pkg.setPrice(new BigDecimal("99.00"));
        pkg.setPeriod(SaasPackagePeriod.MONTH);
        pkg.setStatus(status);
        return pkg;
    }

    private void stubStandardPackageFeatures() {
        SaasPackageFeature roomCount = new SaasPackageFeature();
        roomCount.setPackageId(1L);
        roomCount.setFeatureCode("room_count");
        roomCount.setQuotaLimit(10L);
        SaasPackageFeature aiGen = new SaasPackageFeature();
        aiGen.setPackageId(1L);
        aiGen.setFeatureCode("ai_website_gen");
        aiGen.setQuotaLimit(5L);
        Mockito.when(packageFeatureRepository.findByPackageId(1L))
                .thenReturn(List.of(roomCount, aiGen));

        SaasFeature roomFeature = new SaasFeature();
        roomFeature.setFeatureCode("room_count");
        roomFeature.setType(SaasFeatureType.CAPACITY);
        SaasFeature aiFeature = new SaasFeature();
        aiFeature.setFeatureCode("ai_website_gen");
        aiFeature.setType(SaasFeatureType.QUOTA);
        aiFeature.setDefaultResetCycle(SaasQuotaResetCycle.MONTHLY);
        Mockito.when(featureRepository.findByFeatureCode("room_count"))
                .thenReturn(Optional.of(roomFeature));
        Mockito.when(featureRepository.findByFeatureCode("ai_website_gen"))
                .thenReturn(Optional.of(aiFeature));
    }

    @Test
    void subscribe_fullFlow_orderSnapshotAndQuotaAccountsInOneGo() {
        Mockito.when(packageRepository.findById(1L))
                .thenReturn(Optional.of(standardPackage(SaasPackageStatus.ON_SHELF)));
        stubStandardPackageFeatures();

        SaasSubscription oldSubscription = new SaasSubscription();
        oldSubscription.setId(88L);
        oldSubscription.setStoreId(STORE_ID);
        oldSubscription.setStatus(SaasSubscriptionStatus.ACTIVE);
        Mockito.when(subscriptionRepository.findByStoreIdAndStatus(STORE_ID, SaasSubscriptionStatus.ACTIVE))
                .thenReturn(List.of(oldSubscription));
        Mockito.when(quotaAccountRepository.findByStoreIdAndFeatureCode(eq(STORE_ID), any()))
                .thenReturn(Optional.empty());

        SaasSubscription result = billingService.subscribe(STORE_ID, 1L, "user:1");

        // 1. 直连支付订单：DIRECT + PAID + 套餐价
        ArgumentCaptor<SaasBillingOrder> orderCaptor = ArgumentCaptor.forClass(SaasBillingOrder.class);
        Mockito.verify(billingOrderRepository).save(orderCaptor.capture());
        SaasBillingOrder order = orderCaptor.getValue();
        assertEquals(SaasBillingProvider.DIRECT, order.getProvider());
        assertEquals(SaasBillingOrderStatus.PAID, order.getStatus());
        assertEquals(new BigDecimal("99.00"), order.getAmount());
        assertEquals(STORE_ID, order.getStoreId());
        assertEquals(1L, order.getPackageId());

        // 2. 旧订阅被 CANCELLED（软切换，不清理存量数据）
        assertEquals(SaasSubscriptionStatus.CANCELLED, oldSubscription.getStatus());
        Mockito.verify(subscriptionRepository).save(oldSubscription);

        // 3. 新订阅 ACTIVE + 权益快照冻结 + 一个月周期
        assertEquals(SaasSubscriptionStatus.ACTIVE, result.getStatus());
        assertEquals("标准版", result.getPackageName());
        assertEquals(new BigDecimal("99.00"), result.getPricePaid());
        assertEquals(1L, result.getPackageId());
        long days = ChronoUnit.DAYS.between(result.getStartTime(), result.getEndTime());
        assertTrue(days >= 28 && days <= 31, "MONTH 套餐应约 1 个月，实际 " + days + " 天");

        EntitlementSnapshot snapshot = entitlementService.parseSnapshot(result);
        assertNotNull(snapshot.find("room_count"));
        assertEquals(SaasFeatureType.CAPACITY, snapshot.find("room_count").type());
        assertEquals(10L, snapshot.find("room_count").limit());
        assertNotNull(snapshot.find("ai_website_gen"));
        assertEquals(SaasFeatureType.QUOTA, snapshot.find("ai_website_gen").type());
        assertEquals(5L, snapshot.find("ai_website_gen").limit());

        // 4. QUOTA 账户初始化：总额=套餐额度、used=0、MONTHLY 周期、GRANT 流水
        ArgumentCaptor<SaasQuotaAccount> accountCaptor = ArgumentCaptor.forClass(SaasQuotaAccount.class);
        Mockito.verify(quotaAccountRepository).save(accountCaptor.capture());
        SaasQuotaAccount account = accountCaptor.getValue();
        assertEquals("ai_website_gen", account.getFeatureCode());
        assertEquals(5L, account.getTotalQuota());
        assertEquals(0L, account.getUsedQuota());
        assertEquals(SaasQuotaResetCycle.MONTHLY, account.getResetCycle());
        assertTrue(account.getPeriodEnd().isAfter(LocalDateTime.now()));

        ArgumentCaptor<SaasQuotaLog> logCaptor = ArgumentCaptor.forClass(SaasQuotaLog.class);
        Mockito.verify(quotaLogRepository).save(logCaptor.capture());
        assertEquals(SaasQuotaAction.GRANT, logCaptor.getValue().getAction());
        assertEquals("ai_website_gen", logCaptor.getValue().getFeatureCode());
    }

    @Test
    void subscribe_existingQuotaAccount_updatesLimitAndKeepsUsedAndAnchors() {
        Mockito.when(packageRepository.findById(1L))
                .thenReturn(Optional.of(standardPackage(SaasPackageStatus.ON_SHELF)));
        stubStandardPackageFeatures();
        Mockito.when(subscriptionRepository.findByStoreIdAndStatus(STORE_ID, SaasSubscriptionStatus.ACTIVE))
                .thenReturn(List.of());

        LocalDateTime periodStart = LocalDateTime.now().minusDays(12);
        LocalDateTime periodEnd = LocalDateTime.now().plusDays(18);
        SaasQuotaAccount existing = new SaasQuotaAccount();
        existing.setId(60L);
        existing.setStoreId(STORE_ID);
        existing.setFeatureCode("ai_website_gen");
        existing.setTotalQuota(200L);
        existing.setUsedQuota(150L);
        existing.setResetCycle(SaasQuotaResetCycle.MONTHLY);
        existing.setPeriodStart(periodStart);
        existing.setPeriodEnd(periodEnd);
        Mockito.when(quotaAccountRepository.findByStoreIdAndFeatureCode(STORE_ID, "ai_website_gen"))
                .thenReturn(Optional.of(existing));

        billingService.subscribe(STORE_ID, 1L, null);

        // P9 配额保留语义：仅额度上限按新套餐覆盖（200→5），used 与周期锚点全部保留
        assertEquals(5L, existing.getTotalQuota());
        assertEquals(150L, existing.getUsedQuota());
        assertEquals(periodStart, existing.getPeriodStart());
        assertEquals(periodEnd, existing.getPeriodEnd());
        Mockito.verify(quotaAccountRepository).save(existing);

        // 上限实际变化 → LIMIT_CHANGE 流水（delta=0，不动 used 对账）
        ArgumentCaptor<SaasQuotaLog> logCaptor = ArgumentCaptor.forClass(SaasQuotaLog.class);
        Mockito.verify(quotaLogRepository).save(logCaptor.capture());
        assertEquals(SaasQuotaAction.LIMIT_CHANGE, logCaptor.getValue().getAction());
        assertEquals(0L, logCaptor.getValue().getDelta());
        assertEquals("ai_website_gen", logCaptor.getValue().getFeatureCode());
    }

    // ------------------------------------------------------------------
    // 续费语义（P9）：同套餐重购 = 续费顺延；换档 = 立即替换
    // ------------------------------------------------------------------

    @Test
    void subscribe_samePackage_renewalExtendsEndTimeAndKeepsUsed() {
        Mockito.when(packageRepository.findById(1L))
                .thenReturn(Optional.of(standardPackage(SaasPackageStatus.ON_SHELF)));
        stubStandardPackageFeatures();

        // 旧 ACTIVE 订阅：同套餐，还剩 10 天
        LocalDateTime oldEnd = LocalDateTime.now().plusDays(10);
        SaasSubscription oldSubscription = new SaasSubscription();
        oldSubscription.setId(88L);
        oldSubscription.setStoreId(STORE_ID);
        oldSubscription.setPackageId(1L);
        oldSubscription.setStatus(SaasSubscriptionStatus.ACTIVE);
        oldSubscription.setEndTime(oldEnd);
        Mockito.when(subscriptionRepository.findByStoreIdAndStatus(STORE_ID, SaasSubscriptionStatus.ACTIVE))
                .thenReturn(List.of(oldSubscription));

        // 存量配额账户：上限与续费套餐相同（5），used 已消耗 3
        SaasQuotaAccount existing = new SaasQuotaAccount();
        existing.setId(60L);
        existing.setStoreId(STORE_ID);
        existing.setFeatureCode("ai_website_gen");
        existing.setTotalQuota(5L);
        existing.setUsedQuota(3L);
        Mockito.when(quotaAccountRepository.findByStoreIdAndFeatureCode(STORE_ID, "ai_website_gen"))
                .thenReturn(Optional.of(existing));

        SaasSubscription result = billingService.subscribe(STORE_ID, 1L, "user:1");

        // 续费：新终点 = 旧终点 + 套餐周期（剩余 10 天叠加，不从 now 重算）
        assertEquals(oldEnd.plusMonths(1), result.getEndTime());
        // 旧订阅仍被软取消（新订阅行替换）
        assertEquals(SaasSubscriptionStatus.CANCELLED, oldSubscription.getStatus());
        // used 保留；额度未变 → 账户不落库、不写流水
        assertEquals(3L, existing.getUsedQuota());
        assertEquals(5L, existing.getTotalQuota());
        Mockito.verify(quotaAccountRepository, Mockito.never()).save(any());
        Mockito.verifyNoInteractions(quotaLogRepository);
    }

    @Test
    void subscribe_samePackage_renewalAfterLapse_extendsFromNow() {
        Mockito.when(packageRepository.findById(1L))
                .thenReturn(Optional.of(standardPackage(SaasPackageStatus.ON_SHELF)));
        stubStandardPackageFeatures();

        // 旧 ACTIVE 订阅：同套餐但已过期 3 天（惰性过期尚未标记）→ max(now, 旧end) = now
        SaasSubscription oldSubscription = new SaasSubscription();
        oldSubscription.setId(88L);
        oldSubscription.setStoreId(STORE_ID);
        oldSubscription.setPackageId(1L);
        oldSubscription.setStatus(SaasSubscriptionStatus.ACTIVE);
        oldSubscription.setEndTime(LocalDateTime.now().minusDays(3));
        Mockito.when(subscriptionRepository.findByStoreIdAndStatus(STORE_ID, SaasSubscriptionStatus.ACTIVE))
                .thenReturn(List.of(oldSubscription));
        Mockito.when(quotaAccountRepository.findByStoreIdAndFeatureCode(eq(STORE_ID), any()))
                .thenReturn(Optional.empty());

        SaasSubscription result = billingService.subscribe(STORE_ID, 1L, "user:1");

        long days = ChronoUnit.DAYS.between(result.getStartTime(), result.getEndTime());
        assertTrue(days >= 28 && days <= 31,
                "断缴后续费应从 now 起一个周期，实际 " + days + " 天");
        assertEquals(SaasSubscriptionStatus.CANCELLED, oldSubscription.getStatus());
    }

    @Test
    void subscribe_differentPackage_switchKeepsUsedAndUpdatesLimit() {
        // 换档：豪华版（packageId=2，AI 额度 50/月）
        SaasPackage luxury = new SaasPackage();
        luxury.setId(2L);
        luxury.setName("豪华版");
        luxury.setVersion(1);
        luxury.setPrice(new BigDecimal("999.00"));
        luxury.setPeriod(SaasPackagePeriod.MONTH);
        luxury.setStatus(SaasPackageStatus.ON_SHELF);
        Mockito.when(packageRepository.findById(2L)).thenReturn(Optional.of(luxury));

        SaasPackageFeature aiGen = new SaasPackageFeature();
        aiGen.setPackageId(2L);
        aiGen.setFeatureCode("ai_website_gen");
        aiGen.setQuotaLimit(50L);
        Mockito.when(packageFeatureRepository.findByPackageId(2L)).thenReturn(List.of(aiGen));
        SaasFeature aiFeature = new SaasFeature();
        aiFeature.setFeatureCode("ai_website_gen");
        aiFeature.setType(SaasFeatureType.QUOTA);
        aiFeature.setDefaultResetCycle(SaasQuotaResetCycle.MONTHLY);
        Mockito.when(featureRepository.findByFeatureCode("ai_website_gen"))
                .thenReturn(Optional.of(aiFeature));

        // 旧 ACTIVE 订阅：标准版（packageId=1），还剩 20 天 → 换档不顺延
        SaasSubscription oldSubscription = new SaasSubscription();
        oldSubscription.setId(88L);
        oldSubscription.setStoreId(STORE_ID);
        oldSubscription.setPackageId(1L);
        oldSubscription.setStatus(SaasSubscriptionStatus.ACTIVE);
        oldSubscription.setEndTime(LocalDateTime.now().plusDays(20));
        Mockito.when(subscriptionRepository.findByStoreIdAndStatus(STORE_ID, SaasSubscriptionStatus.ACTIVE))
                .thenReturn(List.of(oldSubscription));

        LocalDateTime periodStart = LocalDateTime.now().minusDays(12);
        LocalDateTime periodEnd = LocalDateTime.now().plusDays(18);
        SaasQuotaAccount existing = new SaasQuotaAccount();
        existing.setId(60L);
        existing.setStoreId(STORE_ID);
        existing.setFeatureCode("ai_website_gen");
        existing.setTotalQuota(5L);
        existing.setUsedQuota(3L);
        existing.setResetCycle(SaasQuotaResetCycle.MONTHLY);
        existing.setPeriodStart(periodStart);
        existing.setPeriodEnd(periodEnd);
        Mockito.when(quotaAccountRepository.findByStoreIdAndFeatureCode(STORE_ID, "ai_website_gen"))
                .thenReturn(Optional.of(existing));

        SaasSubscription result = billingService.subscribe(STORE_ID, 2L, "user:1");

        // 换档：终点 = now + 周期（立即替换，不顺延旧订阅剩余 20 天）
        long days = ChronoUnit.DAYS.between(result.getStartTime(), result.getEndTime());
        assertTrue(days >= 28 && days <= 31,
                "换档应从 now 起一个周期，实际 " + days + " 天");
        assertEquals(SaasSubscriptionStatus.CANCELLED, oldSubscription.getStatus());

        // used 与周期锚点保留，仅额度上限更新为新套餐 50
        assertEquals(50L, existing.getTotalQuota());
        assertEquals(3L, existing.getUsedQuota());
        assertEquals(periodStart, existing.getPeriodStart());
        assertEquals(periodEnd, existing.getPeriodEnd());

        ArgumentCaptor<SaasQuotaLog> logCaptor = ArgumentCaptor.forClass(SaasQuotaLog.class);
        Mockito.verify(quotaLogRepository).save(logCaptor.capture());
        assertEquals(SaasQuotaAction.LIMIT_CHANGE, logCaptor.getValue().getAction());
        assertEquals(0L, logCaptor.getValue().getDelta());
    }

    @Test
    void subscribe_offShelfPackage_rejected() {
        Mockito.when(packageRepository.findById(1L))
                .thenReturn(Optional.of(standardPackage(SaasPackageStatus.OFF_SHELF)));

        assertThrows(IllegalArgumentException.class,
                () -> billingService.subscribe(STORE_ID, 1L, null));
        Mockito.verifyNoInteractions(billingOrderRepository);
    }

    @Test
    void subscribe_missingPackage_rejected() {
        Mockito.when(packageRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> billingService.subscribe(STORE_ID, 99L, null));
        Mockito.verifyNoInteractions(billingOrderRepository);
    }
}
