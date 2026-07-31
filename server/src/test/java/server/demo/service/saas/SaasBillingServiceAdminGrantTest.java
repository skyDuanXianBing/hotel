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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * 平台管理端人工开通/切换订阅：复用订阅激活事务流程，
 * 生成 provider=DIRECT、amount=0 的人工订单；停售套餐（默认版）亦可人工授予。
 */
class SaasBillingServiceAdminGrantTest {

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

    private SaasPackage defaultPackage() {
        SaasPackage pkg = new SaasPackage();
        pkg.setId(4L);
        pkg.setName("默认版");
        pkg.setVersion(1);
        pkg.setPrice(BigDecimal.ZERO);
        pkg.setPeriod(SaasPackagePeriod.MONTH);
        pkg.setStatus(SaasPackageStatus.OFF_SHELF);
        return pkg;
    }

    private void stubDefaultPackageFeatures() {
        SaasPackageFeature aiGen = new SaasPackageFeature();
        aiGen.setPackageId(4L);
        aiGen.setFeatureCode("ai_website_gen");
        aiGen.setQuotaLimit(null);
        Mockito.when(packageFeatureRepository.findByPackageId(4L)).thenReturn(List.of(aiGen));

        SaasFeature aiFeature = new SaasFeature();
        aiFeature.setFeatureCode("ai_website_gen");
        aiFeature.setType(SaasFeatureType.QUOTA);
        aiFeature.setDefaultResetCycle(SaasQuotaResetCycle.MONTHLY);
        Mockito.when(featureRepository.findByFeatureCode("ai_website_gen"))
                .thenReturn(Optional.of(aiFeature));
    }

    @Test
    void grantByAdmin_shouldCreateZeroAmountDirectOrderAndActivateSubscription() {
        SaasPackage pkg = defaultPackage();
        Mockito.when(packageRepository.findById(4L)).thenReturn(Optional.of(pkg));
        stubDefaultPackageFeatures();
        Mockito.when(subscriptionRepository.findByStoreIdAndStatus(STORE_ID, SaasSubscriptionStatus.ACTIVE))
                .thenReturn(List.of());

        SaasSubscription subscription = billingService.grantByAdmin(STORE_ID, 4L, "admin");

        // 人工订单：DIRECT / PAID / amount=0
        ArgumentCaptor<SaasBillingOrder> orderCaptor = ArgumentCaptor.forClass(SaasBillingOrder.class);
        Mockito.verify(billingOrderRepository).save(orderCaptor.capture());
        SaasBillingOrder order = orderCaptor.getValue();
        assertEquals(0, order.getAmount().compareTo(BigDecimal.ZERO));
        assertEquals(SaasBillingProvider.DIRECT, order.getProvider());
        assertEquals(SaasBillingOrderStatus.PAID, order.getStatus());
        assertEquals(STORE_ID, order.getStoreId());
        assertEquals(4L, order.getPackageId());

        // 订阅激活：快照冻结、实付 0
        assertEquals(SaasSubscriptionStatus.ACTIVE, subscription.getStatus());
        assertEquals(0, subscription.getPricePaid().compareTo(BigDecimal.ZERO));
        assertEquals("默认版", subscription.getPackageName());
        assertTrue(subscription.getEntitlementSnapshotJson().contains("ai_website_gen"));
        assertTrue(subscription.getEndTime().isAfter(LocalDateTime.now()));
    }

    @Test
    void grantByAdmin_shouldAllowOffShelfPackageAndCancelPreviousSubscription() {
        SaasPackage pkg = defaultPackage();
        Mockito.when(packageRepository.findById(4L)).thenReturn(Optional.of(pkg));
        stubDefaultPackageFeatures();

        SaasSubscription previous = new SaasSubscription();
        previous.setId(50L);
        previous.setStoreId(STORE_ID);
        previous.setPackageId(1L);
        previous.setPackageName("标准版");
        previous.setEntitlementSnapshotJson("{\"features\":[]}");
        previous.setPricePaid(new BigDecimal("99.00"));
        previous.setStartTime(LocalDateTime.now().minusDays(10));
        previous.setEndTime(LocalDateTime.now().plusDays(20));
        previous.setStatus(SaasSubscriptionStatus.ACTIVE);
        Mockito.when(subscriptionRepository.findByStoreIdAndStatus(STORE_ID, SaasSubscriptionStatus.ACTIVE))
                .thenReturn(List.of(previous));

        SaasSubscription granted = billingService.grantByAdmin(STORE_ID, 4L, "admin");

        // 停售的默认版可人工授予（区别于门店自助 subscribe 会拒绝 OFF_SHELF）
        assertEquals(SaasSubscriptionStatus.ACTIVE, granted.getStatus());
        // 旧订阅被软取消
        assertEquals(SaasSubscriptionStatus.CANCELLED, previous.getStatus());
        // operator 进入配额 GRANT 流水
        ArgumentCaptor<SaasQuotaLog> logCaptor = ArgumentCaptor.forClass(SaasQuotaLog.class);
        Mockito.verify(quotaLogRepository, Mockito.atLeastOnce()).save(logCaptor.capture());
        assertTrue(logCaptor.getAllValues().stream()
                .anyMatch(log -> log.getAction() == SaasQuotaAction.GRANT
                        && "admin".equals(log.getOperator())));
    }

    @Test
    void grantByAdmin_shouldRejectUnknownPackage() {
        Mockito.when(packageRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> billingService.grantByAdmin(STORE_ID, 999L, "admin"));
    }
}
