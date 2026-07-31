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
import server.demo.exception.NeedUpgradeException;
import server.demo.repository.saas.SaasFeatureRepository;
import server.demo.repository.saas.SaasQuotaAccountRepository;
import server.demo.repository.saas.SaasQuotaLogRepository;
import server.demo.repository.saas.SaasSubscriptionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

class EntitlementServiceTest {

    private static final long STORE_ID = 7L;
    private static final String FEATURE = "ai_website_gen";

    private SaasSubscriptionRepository subscriptionRepository;
    private SaasQuotaAccountRepository quotaAccountRepository;
    private SaasQuotaLogRepository quotaLogRepository;
    private SaasFeatureRepository featureRepository;
    private EntitlementService service;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
                objectMapper);
    }

    private SaasSubscription activeSubscription(EntitlementSnapshot.Entry... entries) {
        SaasSubscription subscription = new SaasSubscription();
        subscription.setId(100L);
        subscription.setStoreId(STORE_ID);
        subscription.setPackageId(1L);
        subscription.setPackageName("标准版");
        subscription.setEntitlementSnapshotJson(
                service.serializeSnapshot(new EntitlementSnapshot(List.of(entries))));
        subscription.setPricePaid(BigDecimal.ZERO);
        subscription.setStartTime(LocalDateTime.now().minusDays(1));
        subscription.setEndTime(LocalDateTime.now().plusDays(29));
        subscription.setStatus(server.demo.enums.SaasSubscriptionStatus.ACTIVE);
        Mockito.when(subscriptionRepository.findFirstByStoreIdAndStatusOrderByEndTimeDesc(
                        eq(STORE_ID), eq(server.demo.enums.SaasSubscriptionStatus.ACTIVE)))
                .thenReturn(Optional.of(subscription));
        return subscription;
    }

    private SaasQuotaAccount account(long total, long used, boolean expired) {
        SaasQuotaAccount account = new SaasQuotaAccount();
        account.setId(50L);
        account.setStoreId(STORE_ID);
        account.setFeatureCode(FEATURE);
        account.setTotalQuota(total);
        account.setUsedQuota(used);
        account.setResetCycle(SaasQuotaResetCycle.MONTHLY);
        account.setPeriodStart(LocalDateTime.now().minusMonths(1));
        account.setPeriodEnd(expired
                ? LocalDateTime.now().minusDays(1)
                : LocalDateTime.now().plusDays(10));
        Mockito.when(quotaAccountRepository.findByStoreIdAndFeatureCode(STORE_ID, FEATURE))
                .thenReturn(Optional.of(account));
        return account;
    }

    // ------------------------------------------------------------------
    // fail-closed
    // ------------------------------------------------------------------

    @Test
    void noSubscription_booleanQuotaCapacity_allRejected() {
        Mockito.when(subscriptionRepository.findFirstByStoreIdAndStatusOrderByEndTimeDesc(
                        eq(STORE_ID), any()))
                .thenReturn(Optional.empty());

        assertThrows(NeedUpgradeException.class,
                () -> service.requireBooleanFeature(STORE_ID, "independent_website"));
        assertThrows(NeedUpgradeException.class,
                () -> service.deductQuota(STORE_ID, FEATURE, 1, "biz"));
        assertThrows(NeedUpgradeException.class,
                () -> service.checkCapacity(STORE_ID, "room_count", 0, 1));
    }

    @Test
    void expiredSubscription_lazilyMarkedExpired_andRejected() {
        SaasSubscription subscription = activeSubscription(
                new EntitlementSnapshot.Entry("independent_website", SaasFeatureType.BOOLEAN, null));
        subscription.setEndTime(LocalDateTime.now().minusMinutes(1));

        assertThrows(NeedUpgradeException.class,
                () -> service.requireBooleanFeature(STORE_ID, "independent_website"));

        assertEquals(server.demo.enums.SaasSubscriptionStatus.EXPIRED, subscription.getStatus());
        Mockito.verify(subscriptionRepository).save(subscription);
    }

    @Test
    void featureMissingFromSnapshot_rejected() {
        activeSubscription(
                new EntitlementSnapshot.Entry("room_count", SaasFeatureType.CAPACITY, 10L));
        // ai_website_gen 不在快照中
        assertThrows(NeedUpgradeException.class,
                () -> service.deductQuota(STORE_ID, FEATURE, 1, "biz"));
    }

    // ------------------------------------------------------------------
    // BOOLEAN
    // ------------------------------------------------------------------

    @Test
    void booleanFeature_present_passes() {
        activeSubscription(
                new EntitlementSnapshot.Entry("independent_website", SaasFeatureType.BOOLEAN, null));
        service.requireBooleanFeature(STORE_ID, "independent_website");
    }

    // ------------------------------------------------------------------
    // QUOTA 预扣 / 并发 / 滚动 / 返还
    // ------------------------------------------------------------------

    @Test
    void deductQuota_success_writesDeductLog() {
        activeSubscription(new EntitlementSnapshot.Entry(FEATURE, SaasFeatureType.QUOTA, 5L));
        account(5L, 0L, false);
        Mockito.when(quotaAccountRepository.deductIfAvailable(STORE_ID, FEATURE, 1L)).thenReturn(1);

        service.deductQuota(STORE_ID, FEATURE, 1, "biz-1");

        ArgumentCaptor<SaasQuotaLog> captor = ArgumentCaptor.forClass(SaasQuotaLog.class);
        Mockito.verify(quotaLogRepository).save(captor.capture());
        SaasQuotaLog log = captor.getValue();
        assertEquals(SaasQuotaAction.DEDUCT, log.getAction());
        assertEquals(1L, log.getDelta());
        assertEquals("biz-1", log.getBizId());
        assertEquals(STORE_ID, log.getStoreId());
    }

    @Test
    void deductQuota_exhausted_throwsNeedUpgradeWithLimitAndUsed() {
        activeSubscription(new EntitlementSnapshot.Entry(FEATURE, SaasFeatureType.QUOTA, 5L));
        account(5L, 5L, false);
        Mockito.when(quotaAccountRepository.deductIfAvailable(STORE_ID, FEATURE, 1L)).thenReturn(0);

        NeedUpgradeException e = assertThrows(NeedUpgradeException.class,
                () -> service.deductQuota(STORE_ID, FEATURE, 1, "biz"));
        assertEquals(FEATURE, e.getFeatureCode());
        assertEquals(5L, e.getLimit());
        assertEquals(5L, e.getUsed());
    }

    @Test
    void deductQuota_unlimitedLimit_skipsAccounting() {
        activeSubscription(new EntitlementSnapshot.Entry(FEATURE, SaasFeatureType.QUOTA, null));

        service.deductQuota(STORE_ID, FEATURE, 1, "biz");

        Mockito.verifyNoInteractions(quotaLogRepository);
        Mockito.verify(quotaAccountRepository, Mockito.never())
                .deductIfAvailable(anyLong(), anyString(), anyLong());
    }

    @Test
    void deductQuota_concurrent_neverOverDeducts() throws Exception {
        long total = 10;
        int threads = 20;
        activeSubscription(new EntitlementSnapshot.Entry(FEATURE, SaasFeatureType.QUOTA, total));
        account(total, 0L, false);

        // 模拟数据库原子条件 UPDATE 语义：只有 used+delta<=total 才生效
        AtomicLong used = new AtomicLong(0);
        Object lock = new Object();
        Mockito.when(quotaAccountRepository.deductIfAvailable(eq(STORE_ID), eq(FEATURE), anyLong()))
                .thenAnswer(inv -> {
                    long delta = inv.getArgument(2);
                    synchronized (lock) {
                        if (used.get() + delta <= total) {
                            used.addAndGet(delta);
                            return 1;
                        }
                        return 0;
                    }
                });

        AtomicInteger success = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    service.deductQuota(STORE_ID, FEATURE, 1, "concurrent");
                    success.incrementAndGet();
                } catch (NeedUpgradeException e) {
                    rejected.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        assertTrue(ready.await(10, TimeUnit.SECONDS));
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));

        assertEquals(10, success.get());
        assertEquals(10, rejected.get());
        assertEquals(10, used.get());
    }

    @Test
    void deductQuota_expiredPeriod_rollsWindowAndWritesResetLog() {
        activeSubscription(new EntitlementSnapshot.Entry(FEATURE, SaasFeatureType.QUOTA, 5L));
        SaasQuotaAccount account = account(5L, 4L, true);
        Mockito.when(quotaAccountRepository.resetPeriodIfExpired(
                        eq(account.getId()), any(), any(), any()))
                .thenReturn(1);
        Mockito.when(quotaAccountRepository.deductIfAvailable(STORE_ID, FEATURE, 1L)).thenReturn(1);

        service.deductQuota(STORE_ID, FEATURE, 1, "biz");

        // 惰性滚动：条件 UPDATE 重置窗口
        Mockito.verify(quotaAccountRepository).resetPeriodIfExpired(
                eq(account.getId()), any(), any(), any());
        // 流水：RESET + DEDUCT 各一条，RESET 记录重置前 used 的负值
        ArgumentCaptor<SaasQuotaLog> captor = ArgumentCaptor.forClass(SaasQuotaLog.class);
        Mockito.verify(quotaLogRepository, Mockito.times(2)).save(captor.capture());
        List<SaasQuotaLog> logs = captor.getAllValues();
        SaasQuotaLog resetLog = logs.stream()
                .filter(l -> l.getAction() == SaasQuotaAction.RESET)
                .findFirst().orElseThrow();
        assertEquals(-4L, resetLog.getDelta());
        assertEquals(0L, account.getUsedQuota());
        assertTrue(account.getPeriodEnd().isAfter(LocalDateTime.now()));
        assertTrue(logs.stream().anyMatch(l -> l.getAction() == SaasQuotaAction.DEDUCT));
    }

    @Test
    void getQuotaUsage_expiredPeriod_rollsAndReturnsFreshWindow() {
        activeSubscription(new EntitlementSnapshot.Entry(FEATURE, SaasFeatureType.QUOTA, 5L));
        SaasQuotaAccount account = account(5L, 3L, true);
        Mockito.when(quotaAccountRepository.resetPeriodIfExpired(
                        eq(account.getId()), any(), any(), any()))
                .thenReturn(1);

        QuotaUsage usage = service.getQuotaUsage(STORE_ID, FEATURE);

        assertEquals(0L, usage.usedQuota());
        assertEquals(5L, usage.totalQuota());
        assertEquals(5L, usage.remaining());
        assertTrue(usage.periodEnd().isAfter(LocalDateTime.now()));
    }

    @Test
    void refundQuota_afterFailure_decrementsAndWritesRefundLog() {
        account(5L, 1L, false);
        Mockito.when(quotaAccountRepository.refund(STORE_ID, FEATURE, 1L)).thenReturn(1);

        service.refundQuota(STORE_ID, FEATURE, 1, "biz");

        ArgumentCaptor<SaasQuotaLog> captor = ArgumentCaptor.forClass(SaasQuotaLog.class);
        Mockito.verify(quotaLogRepository).save(captor.capture());
        assertEquals(SaasQuotaAction.REFUND, captor.getValue().getAction());
        assertEquals(-1L, captor.getValue().getDelta());
    }

    // ------------------------------------------------------------------
    // CAPACITY
    // ------------------------------------------------------------------

    @Test
    void checkCapacity_overLimit_throwsWithLimitAndUsed() {
        activeSubscription(new EntitlementSnapshot.Entry("room_count", SaasFeatureType.CAPACITY, 10L));

        NeedUpgradeException e = assertThrows(NeedUpgradeException.class,
                () -> service.checkCapacity(STORE_ID, "room_count", 9, 2));
        assertEquals("room_count", e.getFeatureCode());
        assertEquals(10L, e.getLimit());
        assertEquals(9L, e.getUsed());
    }

    @Test
    void checkCapacity_withinLimit_passes() {
        activeSubscription(new EntitlementSnapshot.Entry("room_count", SaasFeatureType.CAPACITY, 10L));
        service.checkCapacity(STORE_ID, "room_count", 8, 2);
    }

    @Test
    void checkCapacity_unlimited_passes() {
        activeSubscription(new EntitlementSnapshot.Entry("room_count", SaasFeatureType.CAPACITY, null));
        service.checkCapacity(STORE_ID, "room_count", 9999, 100);
    }

    @Test
    void checkCapacity_snapshotWithoutFeature_rejectedFailClosed() {
        activeSubscription(new EntitlementSnapshot.Entry(FEATURE, SaasFeatureType.QUOTA, 5L));
        assertThrows(NeedUpgradeException.class,
                () -> service.checkCapacity(STORE_ID, "room_count", 0, 1));
    }

    @Test
    void getQuotaUsage_noAccount_returnsFullRemainingFromSnapshot() {
        activeSubscription(new EntitlementSnapshot.Entry(FEATURE, SaasFeatureType.QUOTA, 5L));
        Mockito.when(quotaAccountRepository.findByStoreIdAndFeatureCode(STORE_ID, FEATURE))
                .thenReturn(Optional.empty());

        QuotaUsage usage = service.getQuotaUsage(STORE_ID, FEATURE);
        assertEquals(5L, usage.totalQuota());
        assertEquals(0L, usage.usedQuota());
        assertEquals(5L, usage.remaining());
        assertNull(usage.periodStart());
    }

    @Test
    void getQuotaUsage_accountExists_limitComesFromSnapshotNotAccount() {
        // P10 回归：回退默认版场景——快照 ai_website_gen 不限（limit=null），
        // 账户是豪华版时期遗留（total=50）；用量上限必须取快照（null=不限），used 取账户
        activeSubscription(new EntitlementSnapshot.Entry(FEATURE, SaasFeatureType.QUOTA, null));
        account(50L, 0L, false);

        QuotaUsage usage = service.getQuotaUsage(STORE_ID, FEATURE);
        assertNull(usage.totalQuota());
        assertEquals(0L, usage.usedQuota());
        assertNull(usage.remaining());
    }

    @Test
    void getQuotaUsage_snapshotLimitDiffersFromAccount_remainingFromSnapshotLimit() {
        // P10 双保险：账户总额滞后（未对齐）时，remaining 也按快照上限计算
        activeSubscription(new EntitlementSnapshot.Entry(FEATURE, SaasFeatureType.QUOTA, 200L));
        account(50L, 30L, false);

        QuotaUsage usage = service.getQuotaUsage(STORE_ID, FEATURE);
        assertEquals(200L, usage.totalQuota());
        assertEquals(30L, usage.usedQuota());
        assertEquals(170L, usage.remaining());
    }
}
