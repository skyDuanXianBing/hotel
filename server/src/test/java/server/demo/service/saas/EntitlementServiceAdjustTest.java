package server.demo.service.saas;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import server.demo.entity.saas.SaasQuotaAccount;
import server.demo.entity.saas.SaasQuotaLog;
import server.demo.entity.saas.SaasSubscription;
import server.demo.enums.SaasFeatureType;
import server.demo.enums.SaasQuotaAction;
import server.demo.enums.SaasQuotaResetCycle;
import server.demo.enums.SaasSubscriptionStatus;
import server.demo.repository.saas.SaasFeatureRepository;
import server.demo.repository.saas.SaasQuotaAccountRepository;
import server.demo.repository.saas.SaasQuotaLogRepository;
import server.demo.repository.saas.SaasSubscriptionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

/**
 * 平台管理端配额人工调整：写 ADJUST 流水（operator=管理员账号），并按 delta 正负增减剩余额度。
 */
class EntitlementServiceAdjustTest {

    private static final long STORE_ID = 7L;
    private static final String FEATURE = "ai_website_gen";

    private SaasSubscriptionRepository subscriptionRepository;
    private SaasQuotaAccountRepository quotaAccountRepository;
    private SaasQuotaLogRepository quotaLogRepository;
    private SaasFeatureRepository featureRepository;
    private EntitlementService service;

    private SaasQuotaAccount account;

    @BeforeEach
    void setUp() {
        subscriptionRepository = Mockito.mock(SaasSubscriptionRepository.class);
        quotaAccountRepository = Mockito.mock(SaasQuotaAccountRepository.class);
        quotaLogRepository = Mockito.mock(SaasQuotaLogRepository.class);
        featureRepository = Mockito.mock(SaasFeatureRepository.class);
        service = new EntitlementService(
                subscriptionRepository, quotaAccountRepository, quotaLogRepository,
                featureRepository,
                new SaasQuotaAccountProvisioner(quotaAccountRepository, featureRepository),
                Mockito.mock(SaasDefaultPackageFallbackService.class),
                new ObjectMapper());

        account = new SaasQuotaAccount();
        account.setId(100L);
        account.setStoreId(STORE_ID);
        account.setFeatureCode(FEATURE);
        account.setTotalQuota(5L);
        account.setUsedQuota(2L);
        account.setResetCycle(SaasQuotaResetCycle.MONTHLY);
        account.setPeriodStart(LocalDateTime.now().minusDays(3));
        account.setPeriodEnd(LocalDateTime.now().plusDays(27));

        lenient().when(quotaAccountRepository.findByStoreIdAndFeatureCode(STORE_ID, FEATURE))
                .thenReturn(Optional.of(account));
        lenient().when(quotaLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void stubActiveSubscription(Long quotaLimit) throws Exception {
        EntitlementSnapshot snapshot = new EntitlementSnapshot(List.of(
                new EntitlementSnapshot.Entry(FEATURE, SaasFeatureType.QUOTA, quotaLimit)));
        SaasSubscription subscription = new SaasSubscription();
        subscription.setId(11L);
        subscription.setStoreId(STORE_ID);
        subscription.setPackageId(1L);
        subscription.setPackageName("标准版");
        subscription.setEntitlementSnapshotJson(new ObjectMapper().writeValueAsString(snapshot));
        subscription.setPricePaid(new BigDecimal("99.00"));
        subscription.setStartTime(LocalDateTime.now().minusDays(1));
        subscription.setEndTime(LocalDateTime.now().plusDays(29));
        subscription.setStatus(SaasSubscriptionStatus.ACTIVE);
        Mockito.when(subscriptionRepository.findFirstByStoreIdAndStatusOrderByEndTimeDesc(
                        STORE_ID, SaasSubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(subscription));
    }

    @Test
    void adjustQuota_shouldIncreaseRemainingAndWriteAdjustLog() throws Exception {
        stubActiveSubscription(5L);

        QuotaUsage usage = service.adjustQuota(STORE_ID, FEATURE, 3L, "客服补偿", "admin");

        Mockito.verify(quotaAccountRepository).adjustUsedByRemainingDelta(100L, 3L);
        ArgumentCaptor<SaasQuotaLog> logCaptor = ArgumentCaptor.forClass(SaasQuotaLog.class);
        Mockito.verify(quotaLogRepository).save(logCaptor.capture());
        SaasQuotaLog log = logCaptor.getValue();
        assertEquals(SaasQuotaAction.ADJUST, log.getAction());
        assertEquals(3L, log.getDelta());
        assertEquals("admin", log.getOperator());
        assertEquals("客服补偿", log.getBizId());
        assertEquals(STORE_ID, log.getStoreId());
        assertEquals(FEATURE, log.getFeatureCode());
        assertEquals(5L, usage.totalQuota());
    }

    @Test
    void adjustQuota_shouldDecreaseRemainingWhenDeltaNegative() throws Exception {
        stubActiveSubscription(5L);

        service.adjustQuota(STORE_ID, FEATURE, -2L, "误操作回收", "ops01");

        Mockito.verify(quotaAccountRepository).adjustUsedByRemainingDelta(100L, -2L);
        ArgumentCaptor<SaasQuotaLog> logCaptor = ArgumentCaptor.forClass(SaasQuotaLog.class);
        Mockito.verify(quotaLogRepository).save(logCaptor.capture());
        assertEquals(-2L, logCaptor.getValue().getDelta());
        assertEquals("ops01", logCaptor.getValue().getOperator());
    }

    /**
     * F1 修复语义：正 delta 补偿允许 used 变负，剩余额度可超过套餐总额
     * （如 total=5/used=2 补偿 +10 → used=-8 → remaining=13）。
     */
    @Test
    void adjustQuota_positiveDelta_allowsRemainingAboveTotal() throws Exception {
        stubActiveSubscription(5L);
        // 模拟 adjustUsedByRemainingDelta 的 SQL 语义：used = used - delta（无下限守卫）
        Mockito.when(quotaAccountRepository.adjustUsedByRemainingDelta(Mockito.eq(100L), anyLong()))
                .thenAnswer(inv -> {
                    long delta = inv.getArgument(1);
                    account.setUsedQuota(account.getUsedQuota() - delta);
                    return 1;
                });

        QuotaUsage usage = service.adjustQuota(STORE_ID, FEATURE, 10L, "客服补偿", "admin");

        Mockito.verify(quotaAccountRepository).adjustUsedByRemainingDelta(100L, 10L);
        assertEquals(-8L, account.getUsedQuota()); // used=2-10=-8，允许变负
        assertEquals(-8L, usage.usedQuota());
        assertEquals(13L, usage.remaining()); // remaining=5-(-8)=13，超过套餐总额 5
        assertEquals(5L, usage.totalQuota());

        ArgumentCaptor<SaasQuotaLog> logCaptor = ArgumentCaptor.forClass(SaasQuotaLog.class);
        Mockito.verify(quotaLogRepository).save(logCaptor.capture());
        assertEquals(SaasQuotaAction.ADJUST, logCaptor.getValue().getAction());
        assertEquals(10L, logCaptor.getValue().getDelta());
    }

    @Test
    void adjustQuota_shouldRejectZeroDelta() throws Exception {
        stubActiveSubscription(5L);

        assertThrows(IllegalArgumentException.class,
                () -> service.adjustQuota(STORE_ID, FEATURE, 0L, "无意义", "admin"));
        Mockito.verify(quotaLogRepository, never()).save(any());
    }

    @Test
    void adjustQuota_shouldRejectFeatureNotInSubscription() throws Exception {
        stubActiveSubscription(5L);

        assertThrows(IllegalArgumentException.class,
                () -> service.adjustQuota(STORE_ID, "independent_website", 1L, null, "admin"));
        Mockito.verify(quotaAccountRepository, never())
                .adjustUsedByRemainingDelta(anyLong(), anyLong());
        Mockito.verify(quotaLogRepository, never()).save(any());
    }

    @Test
    void adjustQuota_shouldRejectWhenNoActiveSubscription() {
        Mockito.when(subscriptionRepository.findFirstByStoreIdAndStatusOrderByEndTimeDesc(
                        STORE_ID, SaasSubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.adjustQuota(STORE_ID, FEATURE, 1L, null, "admin"));
    }

    @Test
    void adjustQuota_shouldOnlyAuditWhenQuotaUnlimited() throws Exception {
        stubActiveSubscription(null);
        // 不限额度的订阅不建配额账户
        Mockito.when(quotaAccountRepository.findByStoreIdAndFeatureCode(STORE_ID, FEATURE))
                .thenReturn(Optional.empty());

        QuotaUsage usage = service.adjustQuota(STORE_ID, FEATURE, 10L, "无限额度门店补偿", "admin");

        Mockito.verify(quotaAccountRepository, never())
                .adjustUsedByRemainingDelta(anyLong(), anyLong());
        ArgumentCaptor<SaasQuotaLog> logCaptor = ArgumentCaptor.forClass(SaasQuotaLog.class);
        Mockito.verify(quotaLogRepository).save(logCaptor.capture());
        assertEquals(SaasQuotaAction.ADJUST, logCaptor.getValue().getAction());
        assertEquals(10L, logCaptor.getValue().getDelta());
        assertEquals(null, usage.totalQuota());
    }
}
