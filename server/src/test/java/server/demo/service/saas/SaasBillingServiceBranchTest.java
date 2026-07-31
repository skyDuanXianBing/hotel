package server.demo.service.saas;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import server.demo.entity.saas.SaasFeature;
import server.demo.entity.saas.SaasPackage;
import server.demo.entity.saas.SaasPackageFeature;
import server.demo.entity.saas.SaasQuotaAccount;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * SaasBillingService 剩余分支：
 * 拒绝文案契约（OFF_SHELF / 不存在套餐的 message）、YEAR 周期订阅终点、
 * 已存在 QUOTA 账户时仅更新额度上限、used 与周期锚点保留（P9 起不再清零/重启窗口）。
 * 既有用例（SaasBillingServiceSubscribeTest / SaasBillingServiceAdminGrantTest）
 * 已覆盖的异常类型与额度覆盖/LIMIT_CHANGE 流水语义不重复断言。
 */
class SaasBillingServiceBranchTest {

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

    private SaasPackage onShelfPackage(SaasPackagePeriod period) {
        SaasPackage pkg = new SaasPackage();
        pkg.setId(1L);
        pkg.setName("标准版");
        pkg.setVersion(1);
        pkg.setPrice(new BigDecimal("99.00"));
        pkg.setPeriod(period);
        pkg.setStatus(SaasPackageStatus.ON_SHELF);
        return pkg;
    }

    private void stubRoomCountOnlyFeatures() {
        SaasPackageFeature roomCount = new SaasPackageFeature();
        roomCount.setPackageId(1L);
        roomCount.setFeatureCode("room_count");
        roomCount.setQuotaLimit(10L);
        Mockito.when(packageFeatureRepository.findByPackageId(1L))
                .thenReturn(List.of(roomCount));

        SaasFeature roomFeature = new SaasFeature();
        roomFeature.setFeatureCode("room_count");
        roomFeature.setType(SaasFeatureType.CAPACITY);
        Mockito.when(featureRepository.findByFeatureCode("room_count"))
                .thenReturn(Optional.of(roomFeature));
    }

    // ------------------------------------------------------------------
    // 拒绝文案契约
    // ------------------------------------------------------------------

    @Test
    void subscribe_offShelfPackage_rejectedWithOffShelfMessage() {
        SaasPackage pkg = onShelfPackage(SaasPackagePeriod.MONTH);
        pkg.setStatus(SaasPackageStatus.OFF_SHELF);
        Mockito.when(packageRepository.findById(1L)).thenReturn(Optional.of(pkg));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> billingService.subscribe(STORE_ID, 1L, "user:1"));
        assertEquals("套餐已下架，无法购买", e.getMessage());
        Mockito.verifyNoInteractions(billingOrderRepository);
    }

    @Test
    void subscribe_missingPackage_rejectedWithMessage() {
        Mockito.when(packageRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> billingService.subscribe(STORE_ID, 99L, "user:1"));
        assertEquals("套餐不存在", e.getMessage());
        Mockito.verifyNoInteractions(billingOrderRepository);
    }

    // ------------------------------------------------------------------
    // 计费周期
    // ------------------------------------------------------------------

    @Test
    void subscribe_yearPeriodPackage_endTimePlusOneYear() {
        Mockito.when(packageRepository.findById(1L))
                .thenReturn(Optional.of(onShelfPackage(SaasPackagePeriod.YEAR)));
        stubRoomCountOnlyFeatures();
        Mockito.when(subscriptionRepository.findByStoreIdAndStatus(STORE_ID, SaasSubscriptionStatus.ACTIVE))
                .thenReturn(List.of());

        SaasSubscription result = billingService.subscribe(STORE_ID, 1L, "user:1");

        long days = ChronoUnit.DAYS.between(result.getStartTime(), result.getEndTime());
        assertTrue(days >= 365 && days <= 366,
                "YEAR 套餐应约 1 年，实际 " + days + " 天");
        assertEquals(SaasSubscriptionStatus.ACTIVE, result.getStatus());
    }

    // ------------------------------------------------------------------
    // 已存在 QUOTA 账户：总额覆盖 + used 保留 + 周期锚点保留（P9 语义反转）
    // ------------------------------------------------------------------

    @Test
    void subscribe_existingQuotaAccount_keepsUsedAndPeriodAnchors() {
        Mockito.when(packageRepository.findById(1L))
                .thenReturn(Optional.of(onShelfPackage(SaasPackagePeriod.MONTH)));

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

        Mockito.when(subscriptionRepository.findByStoreIdAndStatus(STORE_ID, SaasSubscriptionStatus.ACTIVE))
                .thenReturn(List.of());

        // 存量账户：上一周期的旧窗口（已过期），used 有消耗
        LocalDateTime oldPeriodStart = LocalDateTime.now().minusDays(40);
        LocalDateTime oldPeriodEnd = LocalDateTime.now().minusDays(10);
        SaasQuotaAccount existing = new SaasQuotaAccount();
        existing.setId(60L);
        existing.setStoreId(STORE_ID);
        existing.setFeatureCode("ai_website_gen");
        existing.setTotalQuota(200L);
        existing.setUsedQuota(150L);
        existing.setResetCycle(SaasQuotaResetCycle.MONTHLY);
        existing.setPeriodStart(oldPeriodStart);
        existing.setPeriodEnd(oldPeriodEnd);
        Mockito.when(quotaAccountRepository.findByStoreIdAndFeatureCode(STORE_ID, "ai_website_gen"))
                .thenReturn(Optional.of(existing));

        billingService.subscribe(STORE_ID, 1L, "user:1");

        // 总额按新套餐覆盖；used 保留（不清零）
        assertEquals(5L, existing.getTotalQuota());
        assertEquals(150L, existing.getUsedQuota());

        // 周期锚点保留：窗口不随新订阅重启（惰性滚动仍由读取路径负责）
        assertEquals(oldPeriodStart, existing.getPeriodStart());
        assertEquals(oldPeriodEnd, existing.getPeriodEnd());
        assertEquals(SaasQuotaResetCycle.MONTHLY, existing.getResetCycle());
    }
}
