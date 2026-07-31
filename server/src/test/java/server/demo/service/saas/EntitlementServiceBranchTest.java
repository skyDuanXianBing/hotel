package server.demo.service.saas;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import server.demo.entity.saas.SaasFeature;
import server.demo.entity.saas.SaasQuotaAccount;
import server.demo.entity.saas.SaasQuotaLog;
import server.demo.entity.saas.SaasSubscription;
import server.demo.enums.SaasFeatureType;
import server.demo.enums.SaasQuotaAction;
import server.demo.enums.SaasQuotaResetCycle;
import server.demo.enums.SaasSubscriptionStatus;
import server.demo.exception.NeedUpgradeException;
import server.demo.repository.saas.SaasFeatureRepository;
import server.demo.repository.saas.SaasQuotaAccountRepository;
import server.demo.repository.saas.SaasQuotaLogRepository;
import server.demo.repository.saas.SaasSubscriptionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;

/**
 * EntitlementService 剩余分支：
 * 快照 JSON 解析失败、ACTIVE 过期惰性 EXPIRED 后后续拒绝、非正 delta 短路、
 * refundQuota 无账户无操作、CAPACITY 超限消息文案（字典名/featureCode 兜底）、
 * 审查 G1/G6/G2 修复后的返还新口径（不限额度/used<=0 no-op）与 C1 并发建账自愈、
 * 以及“锁定当前行为”语义（adjustQuota 负 delta 处罚）。
 * 既有用例（EntitlementServiceTest / EntitlementServiceAdjustTest）已覆盖的分支不重复。
 */
class EntitlementServiceBranchTest {

    private static final long STORE_ID = 7L;
    private static final String FEATURE = "ai_website_gen";

    private SaasSubscriptionRepository subscriptionRepository;
    private SaasQuotaAccountRepository quotaAccountRepository;
    private SaasQuotaLogRepository quotaLogRepository;
    private SaasFeatureRepository featureRepository;
    private EntitlementService service;

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
        subscription.setStatus(SaasSubscriptionStatus.ACTIVE);
        Mockito.when(subscriptionRepository.findFirstByStoreIdAndStatusOrderByEndTimeDesc(
                        STORE_ID, SaasSubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(subscription));
        return subscription;
    }

    private SaasQuotaAccount account(long id, Long total, long used) {
        SaasQuotaAccount account = new SaasQuotaAccount();
        account.setId(id);
        account.setStoreId(STORE_ID);
        account.setFeatureCode(FEATURE);
        account.setTotalQuota(total);
        account.setUsedQuota(used);
        account.setResetCycle(SaasQuotaResetCycle.MONTHLY);
        account.setPeriodStart(LocalDateTime.now().minusDays(3));
        account.setPeriodEnd(LocalDateTime.now().plusDays(27));
        Mockito.when(quotaAccountRepository.findByStoreIdAndFeatureCode(STORE_ID, FEATURE))
                .thenReturn(Optional.of(account));
        return account;
    }

    // ------------------------------------------------------------------
    // P1/P10 修复：entry==null 分支区分"无订阅"与"套餐不含该权益"
    // ------------------------------------------------------------------

    /**
     * 无有效订阅（snapshot==null）：BOOLEAN/QUOTA/CAPACITY 三个入口统一抛
     * NO_SUBSCRIPTION + “尚未开通套餐，请先购买套餐”，而非笼统的“不包含该功能”。
     */
    @Test
    void entryMissing_noSubscription_throwsNoSubscriptionReasonAndPurchaseHint() {
        Mockito.when(subscriptionRepository.findFirstByStoreIdAndStatusOrderByEndTimeDesc(
                        eq(STORE_ID), any()))
                .thenReturn(Optional.empty());

        NeedUpgradeException booleanEx = assertThrows(NeedUpgradeException.class,
                () -> service.requireBooleanFeature(STORE_ID, "independent_website"));
        assertEquals(NeedUpgradeException.Reason.NO_SUBSCRIPTION, booleanEx.getReason());
        assertEquals("当前门店尚未开通套餐，请先购买套餐", booleanEx.getMessage());

        NeedUpgradeException quotaEx = assertThrows(NeedUpgradeException.class,
                () -> service.deductQuota(STORE_ID, FEATURE, 1, "biz"));
        assertEquals(NeedUpgradeException.Reason.NO_SUBSCRIPTION, quotaEx.getReason());
        assertEquals("当前门店尚未开通套餐，请先购买套餐", quotaEx.getMessage());

        NeedUpgradeException capacityEx = assertThrows(NeedUpgradeException.class,
                () -> service.checkCapacity(STORE_ID, "room_count", 0, 1));
        assertEquals(NeedUpgradeException.Reason.NO_SUBSCRIPTION, capacityEx.getReason());
        assertEquals("当前门店尚未开通套餐，请先购买套餐", capacityEx.getMessage());
    }

    /**
     * 有有效订阅但快照不含该权益（entry==null）：维持 NOT_INCLUDED + “不包含该功能，请升级套餐”；
     * 权益类型不匹配（如 QUOTA 权益走 BOOLEAN 入口）同样按 NOT_INCLUDED 处理。
     */
    @Test
    void entryMissing_hasSubscription_throwsNotIncludedReasonAndUpgradeHint() {
        activeSubscription(new EntitlementSnapshot.Entry("room_count", SaasFeatureType.CAPACITY, 10L));

        NeedUpgradeException booleanEx = assertThrows(NeedUpgradeException.class,
                () -> service.requireBooleanFeature(STORE_ID, "independent_website"));
        assertEquals(NeedUpgradeException.Reason.NOT_INCLUDED, booleanEx.getReason());
        assertEquals("当前套餐不包含该功能，请升级套餐", booleanEx.getMessage());

        // ai_website_gen 不在快照中
        NeedUpgradeException quotaEx = assertThrows(NeedUpgradeException.class,
                () -> service.deductQuota(STORE_ID, FEATURE, 1, "biz"));
        assertEquals(NeedUpgradeException.Reason.NOT_INCLUDED, quotaEx.getReason());
        assertEquals("当前套餐不包含该功能，请升级套餐", quotaEx.getMessage());

        // 类型不匹配：room_count 是 CAPACITY，走 QUOTA 扣减入口
        NeedUpgradeException typeMismatchEx = assertThrows(NeedUpgradeException.class,
                () -> service.deductQuota(STORE_ID, "room_count", 1, "biz"));
        assertEquals(NeedUpgradeException.Reason.NOT_INCLUDED, typeMismatchEx.getReason());
    }

    @Test
    void deductQuota_exhausted_carriesQuotaExhaustedReason() {
        activeSubscription(new EntitlementSnapshot.Entry(FEATURE, SaasFeatureType.QUOTA, 5L));
        account(50L, 5L, 5L);
        Mockito.when(quotaAccountRepository.deductIfAvailable(STORE_ID, FEATURE, 1L)).thenReturn(0);

        NeedUpgradeException e = assertThrows(NeedUpgradeException.class,
                () -> service.deductQuota(STORE_ID, FEATURE, 1, "biz"));
        assertEquals(NeedUpgradeException.Reason.QUOTA_EXHAUSTED, e.getReason());
        assertEquals(5L, e.getLimit());
        assertEquals(5L, e.getUsed());
    }

    @Test
    void checkCapacity_overLimit_carriesCapacityExceededReason() {
        activeSubscription(new EntitlementSnapshot.Entry("room_count", SaasFeatureType.CAPACITY, 10L));

        NeedUpgradeException e = assertThrows(NeedUpgradeException.class,
                () -> service.checkCapacity(STORE_ID, "room_count", 10, 1));
        assertEquals(NeedUpgradeException.Reason.CAPACITY_EXCEEDED, e.getReason());
    }

    // ------------------------------------------------------------------
    // 快照解析
    // ------------------------------------------------------------------

    @Test
    void parseSnapshot_malformedJson_throwsIllegalStateException() {
        SaasSubscription subscription = new SaasSubscription();
        subscription.setId(77L);
        subscription.setEntitlementSnapshotJson("{not-a-valid-json");

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> service.parseSnapshot(subscription));
        assertTrue(e.getMessage().contains("订阅权益快照解析失败"));
        assertTrue(e.getMessage().contains("77"));
    }

    // ------------------------------------------------------------------
    // 过期订阅惰性 EXPIRED
    // ------------------------------------------------------------------

    @Test
    void findActiveSubscription_pastEndTime_lazilyExpires_andLaterUsageRejected() {
        SaasSubscription subscription = activeSubscription(
                new EntitlementSnapshot.Entry(FEATURE, SaasFeatureType.QUOTA, 5L));
        subscription.setEndTime(LocalDateTime.now().minusDays(10)); // ACTIVE 但已过 end_time

        // 第一次读取：惰性标记 EXPIRED 并视为无订阅
        assertTrue(service.findActiveSubscription(STORE_ID).isEmpty());
        assertEquals(SaasSubscriptionStatus.EXPIRED, subscription.getStatus());
        Mockito.verify(subscriptionRepository).save(subscription);

        // 后续读取：数据库中已非 ACTIVE → 快照为空 → 业务调用 fail-closed 拒绝
        Mockito.when(subscriptionRepository.findFirstByStoreIdAndStatusOrderByEndTimeDesc(
                        STORE_ID, SaasSubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        assertThrows(NeedUpgradeException.class,
                () -> service.deductQuota(STORE_ID, FEATURE, 1, "biz"));
    }

    // ------------------------------------------------------------------
    // 审查 G1 修复：不限额度的失败返还为 no-op（不再写无配对 REFUND 流水）
    // ------------------------------------------------------------------

    /**
     * 审查 G1 修复（取代原“锁定当前行为（验收 D1）”用例，语义反转）：
     * 不限额度（limit=null）时 deductQuota 直接放行不记账；refundQuota 对 totalQuota IS NULL
     * 的账户同样 no-op——不执行 refund UPDATE、不写 REFUND 流水，消除无配对 DEDUCT 的返还噪音。
     */
    @Test
    void deductUnlimitedQuota_thenFailureRefund_isNoOp_noUnpairedRefundLog() {
        activeSubscription(new EntitlementSnapshot.Entry(FEATURE, SaasFeatureType.QUOTA, null));
        account(50L, null, 0L); // 不限额度账户行（totalQuota=NULL）

        // 1) 不限额度：扣减直接放行，不调用原子 UPDATE、不记账
        service.deductQuota(STORE_ID, FEATURE, 1, "biz-d1");
        Mockito.verify(quotaAccountRepository, never())
                .deductIfAvailable(anyLong(), anyString(), anyLong());

        // 2) 业务失败 → 切面返还 → totalQuota IS NULL → no-op：无 refund UPDATE、无任何流水
        service.refundQuota(STORE_ID, FEATURE, 1, "biz-d1");

        Mockito.verify(quotaAccountRepository, never()).refund(anyLong(), anyString(), anyLong());
        Mockito.verifyNoInteractions(quotaLogRepository);
    }

    /**
     * 审查 G6/G2 修复：refund SQL 带 used &gt; 0 守卫。used=0 的重复返还、used 为负
     * （人工补偿未消耗）的返还不命中行（affected=0）→ 不写流水，补偿额度不被 floor-0 吞掉。
     */
    @Test
    void refundQuota_negativeUsedFromCompensation_noAffectedRows_noLog_compensationPreserved() {
        activeSubscription(new EntitlementSnapshot.Entry(FEATURE, SaasFeatureType.QUOTA, 5L));
        SaasQuotaAccount account = account(50L, 5L, -3L); // used=-3：正 delta 人工补偿未消耗
        // used>0 守卫不命中：数据库返回 0 行
        Mockito.when(quotaAccountRepository.refund(STORE_ID, FEATURE, 1L)).thenReturn(0);

        service.refundQuota(STORE_ID, FEATURE, 1, "biz");

        Mockito.verify(quotaAccountRepository).refund(STORE_ID, FEATURE, 1L);
        assertEquals(-3L, account.getUsedQuota()); // 补偿额度原样保留
        Mockito.verifyNoInteractions(quotaLogRepository);
    }

    // ------------------------------------------------------------------
    // 审查 C1 修复：并发首用建账撞 uk 后捕获重查、透明恢复
    // ------------------------------------------------------------------

    @Test
    void deductQuota_ensureAccountRaceLoser_recoversByReFind() {
        activeSubscription(new EntitlementSnapshot.Entry(FEATURE, SaasFeatureType.QUOTA, 5L));
        // 并发胜者已提交的账户行（ensureAccount 捕获冲突后重查命中）
        SaasQuotaAccount winner = new SaasQuotaAccount();
        winner.setId(60L);
        winner.setStoreId(STORE_ID);
        winner.setFeatureCode(FEATURE);
        winner.setTotalQuota(5L);
        winner.setUsedQuota(0L);
        winner.setResetCycle(SaasQuotaResetCycle.MONTHLY);
        winner.setPeriodStart(LocalDateTime.now().minusDays(1));
        winner.setPeriodEnd(LocalDateTime.now().plusDays(29));
        Mockito.when(quotaAccountRepository.findByStoreIdAndFeatureCode(STORE_ID, FEATURE))
                .thenReturn(Optional.empty())          // ensureAccount 首次查找：无账户
                .thenReturn(Optional.of(winner));      // 捕获唯一键冲突后重查：命中胜者行
        // provisioner 建账撞 uk_saas_quota_account_store_feature
        Mockito.when(quotaAccountRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry '7-ai_website_gen'"));
        Mockito.when(quotaAccountRepository.deductIfAvailable(STORE_ID, FEATURE, 1L)).thenReturn(1);

        // 不抛异常（不再 500）：恢复后扣减继续执行并记账
        service.deductQuota(STORE_ID, FEATURE, 1, "biz-c1");

        Mockito.verify(quotaAccountRepository).deductIfAvailable(STORE_ID, FEATURE, 1L);
        ArgumentCaptor<SaasQuotaLog> captor = ArgumentCaptor.forClass(SaasQuotaLog.class);
        Mockito.verify(quotaLogRepository).save(captor.capture());
        assertEquals(SaasQuotaAction.DEDUCT, captor.getValue().getAction());
        assertEquals("biz-c1", captor.getValue().getBizId());
    }

    @Test
    void deductQuota_ensureAccountRaceLoser_rowStillMissing_throwsIllegalState() {
        activeSubscription(new EntitlementSnapshot.Entry(FEATURE, SaasFeatureType.QUOTA, 5L));
        Mockito.when(quotaAccountRepository.findByStoreIdAndFeatureCode(STORE_ID, FEATURE))
                .thenReturn(Optional.empty()); // 重查仍不可见（胜者未提交/已回滚的极端残留窗口）
        Mockito.when(quotaAccountRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry"));

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> service.deductQuota(STORE_ID, FEATURE, 1, "biz-c1"));
        assertTrue(e.getMessage().contains("配额账户并发创建后读取失败"));
        // 未执行扣减，不产生任何流水
        Mockito.verify(quotaAccountRepository, never())
                .deductIfAvailable(anyLong(), anyString(), anyLong());
        Mockito.verifyNoInteractions(quotaLogRepository);
    }

    // ------------------------------------------------------------------
    // refundQuota / deductQuota 短路分支
    // ------------------------------------------------------------------

    @Test
    void refundQuota_noAccount_isNoOp() {
        Mockito.when(quotaAccountRepository.findByStoreIdAndFeatureCode(STORE_ID, FEATURE))
                .thenReturn(Optional.empty());

        service.refundQuota(STORE_ID, FEATURE, 1, "biz");

        Mockito.verify(quotaAccountRepository, never()).refund(anyLong(), anyString(), anyLong());
        Mockito.verifyNoInteractions(quotaLogRepository);
    }

    @Test
    void nonPositiveDelta_deductAndRefund_areNoOp() {
        service.deductQuota(STORE_ID, FEATURE, 0, "biz");
        service.refundQuota(STORE_ID, FEATURE, 0, "biz");

        // delta<=0 在进入任何仓储调用前短路
        Mockito.verifyNoInteractions(subscriptionRepository, quotaAccountRepository, quotaLogRepository);
    }

    // ------------------------------------------------------------------
    // CAPACITY 超限消息文案
    // ------------------------------------------------------------------

    @Test
    void checkCapacity_overLimit_messageUsesDictionaryFeatureName() {
        activeSubscription(new EntitlementSnapshot.Entry("room_count", SaasFeatureType.CAPACITY, 10L));
        SaasFeature feature = new SaasFeature();
        feature.setFeatureCode("room_count");
        feature.setName("房间数量");
        Mockito.when(featureRepository.findByFeatureCode("room_count"))
                .thenReturn(Optional.of(feature));

        NeedUpgradeException e = assertThrows(NeedUpgradeException.class,
                () -> service.checkCapacity(STORE_ID, "room_count", 10, 1));
        assertTrue(e.getMessage().contains("房间数量"));
        assertTrue(e.getMessage().contains("10"));
        assertEquals(10L, e.getLimit());
        assertEquals(10L, e.getUsed());
    }

    @Test
    void checkCapacity_overLimit_dictionaryMissing_fallsBackToFeatureCodeInMessage() {
        activeSubscription(new EntitlementSnapshot.Entry("room_count", SaasFeatureType.CAPACITY, 10L));
        Mockito.when(featureRepository.findByFeatureCode("room_count"))
                .thenReturn(Optional.empty());

        NeedUpgradeException e = assertThrows(NeedUpgradeException.class,
                () -> service.checkCapacity(STORE_ID, "room_count", 10, 1));
        assertTrue(e.getMessage().contains("room_count"));
    }

    // ------------------------------------------------------------------
    // 锁定当前行为：adjustQuota 负 delta 处罚语义（used 可超 total）
    // ------------------------------------------------------------------

    /**
     * 锁定当前行为：adjustUsedByRemainingDelta 的 SQL 无任何上下限守卫（used = used - delta），
     * 因此大幅负 delta 可令 used 超过 total（处罚语义）；视图层 remaining 由 Math.max(0, total-used) 兜底为 0。
     * 对称地，正 delta 允许 used 变负（补偿后剩余可超总额），该正向语义由
     * EntitlementServiceAdjustTest#adjustQuota_positiveDelta_allowsRemainingAboveTotal 覆盖。
     */
    @Test
    void adjustQuota_largeNegativeDelta_usedMayExceedTotal_lockCurrentBehavior() {
        activeSubscription(new EntitlementSnapshot.Entry(FEATURE, SaasFeatureType.QUOTA, 5L));
        SaasQuotaAccount account = account(60L, 5L, 2L);
        // 模拟 adjustUsedByRemainingDelta 的 SQL 语义：used = used - delta（无守卫）
        Mockito.when(quotaAccountRepository.adjustUsedByRemainingDelta(eq(60L), anyLong()))
                .thenAnswer(inv -> {
                    long delta = inv.getArgument(1);
                    account.setUsedQuota(account.getUsedQuota() - delta);
                    return 1;
                });

        QuotaUsage usage = service.adjustQuota(STORE_ID, FEATURE, -10L, "违规处罚", "admin");

        Mockito.verify(quotaAccountRepository).adjustUsedByRemainingDelta(60L, -10L);
        assertEquals(12L, account.getUsedQuota()); // used=2-(-10)=12，超过 total=5（无上限守卫）
        assertEquals(12L, usage.usedQuota());
        assertEquals(0L, usage.remaining()); // remaining 下限 0

        ArgumentCaptor<SaasQuotaLog> captor = ArgumentCaptor.forClass(SaasQuotaLog.class);
        Mockito.verify(quotaLogRepository).save(captor.capture());
        assertEquals(SaasQuotaAction.ADJUST, captor.getValue().getAction());
        assertEquals(-10L, captor.getValue().getDelta());
        assertEquals("admin", captor.getValue().getOperator());
        assertEquals("违规处罚", captor.getValue().getBizId());
    }

    // ------------------------------------------------------------------
    // 用量查询
    // ------------------------------------------------------------------

    @Test
    void getQuotaUsage_noSubscription_returnsNullLimitView() {
        Mockito.when(subscriptionRepository.findFirstByStoreIdAndStatusOrderByEndTimeDesc(
                        eq(STORE_ID), any()))
                .thenReturn(Optional.empty());
        Mockito.when(quotaAccountRepository.findByStoreIdAndFeatureCode(STORE_ID, FEATURE))
                .thenReturn(Optional.empty());

        QuotaUsage usage = service.getQuotaUsage(STORE_ID, FEATURE);

        assertEquals(FEATURE, usage.featureCode());
        assertNull(usage.totalQuota());
        assertEquals(0L, usage.usedQuota());
        assertNull(usage.remaining());
        assertNull(usage.periodStart());
        assertNull(usage.periodEnd());
    }

    @Test
    void listQuotaUsages_mixedSnapshot_returnsOnlyQuotaEntries() {
        activeSubscription(
                new EntitlementSnapshot.Entry("independent_website", SaasFeatureType.BOOLEAN, null),
                new EntitlementSnapshot.Entry(FEATURE, SaasFeatureType.QUOTA, 5L),
                new EntitlementSnapshot.Entry("room_count", SaasFeatureType.CAPACITY, 10L));
        account(50L, 5L, 2L);

        List<QuotaUsage> usages = service.listQuotaUsages(STORE_ID);

        // 仅 QUOTA 权益出用量视图；BOOLEAN/CAPACITY 不经配额账户
        assertEquals(1, usages.size());
        assertEquals(FEATURE, usages.get(0).featureCode());
        assertEquals(5L, usages.get(0).totalQuota());
        assertEquals(2L, usages.get(0).usedQuota());
        assertEquals(3L, usages.get(0).remaining());
    }
}
