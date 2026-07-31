package server.demo.service.saas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import server.demo.entity.saas.SaasFeature;
import server.demo.entity.saas.SaasQuotaAccount;
import server.demo.entity.saas.SaasQuotaLog;
import server.demo.enums.SaasFeatureType;
import server.demo.enums.SaasQuotaAction;
import server.demo.enums.SaasQuotaResetCycle;
import server.demo.repository.saas.SaasFeatureRepository;
import server.demo.repository.saas.SaasQuotaAccountRepository;
import server.demo.repository.saas.SaasQuotaLogRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

/**
 * 配额账户对齐（P10 抽取的共享组件）：账户已存在仅对齐总额为新快照 quotaLimit
 * （含对齐为 null=不限，保留 used/周期锚点，变化写 LIMIT_CHANGE）；缺失则建账（GRANT）；
 * 非 QUOTA 权益不动账户。
 */
class SaasQuotaAccountAlignerTest {

    private static final long STORE_ID = 9L;
    private static final String FEATURE = "ai_website_gen";

    private SaasQuotaAccountRepository quotaAccountRepository;
    private SaasFeatureRepository featureRepository;
    private SaasQuotaLogRepository quotaLogRepository;
    private SaasQuotaAccountAligner aligner;

    @BeforeEach
    void setUp() {
        quotaAccountRepository = Mockito.mock(SaasQuotaAccountRepository.class);
        featureRepository = Mockito.mock(SaasFeatureRepository.class);
        quotaLogRepository = Mockito.mock(SaasQuotaLogRepository.class);
        aligner = new SaasQuotaAccountAligner(quotaAccountRepository, featureRepository, quotaLogRepository);
        lenient().when(quotaAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private SaasQuotaAccount existingAccount(Long total, long used) {
        SaasQuotaAccount account = new SaasQuotaAccount();
        account.setId(50L);
        account.setStoreId(STORE_ID);
        account.setFeatureCode(FEATURE);
        account.setTotalQuota(total);
        account.setUsedQuota(used);
        account.setResetCycle(SaasQuotaResetCycle.MONTHLY);
        account.setPeriodStart(LocalDateTime.now().minusDays(10));
        account.setPeriodEnd(LocalDateTime.now().plusDays(20));
        Mockito.when(quotaAccountRepository.findByStoreIdAndFeatureCode(STORE_ID, FEATURE))
                .thenReturn(Optional.of(account));
        return account;
    }

    private List<EntitlementSnapshot.Entry> quotaEntry(Long limit) {
        return List.of(new EntitlementSnapshot.Entry(FEATURE, SaasFeatureType.QUOTA, limit));
    }

    @Test
    void existingAccount_sameLimit_noSaveNoLog() {
        existingAccount(50L, 12L);

        aligner.alignQuotaAccounts(STORE_ID, quotaEntry(50L), "subscription:1", "admin");

        Mockito.verify(quotaAccountRepository, never()).save(any());
        Mockito.verify(quotaLogRepository, never()).save(any());
    }

    @Test
    void existingAccount_differentLimit_alignsAndWritesLimitChangeLog() {
        SaasQuotaAccount account = existingAccount(50L, 12L);

        aligner.alignQuotaAccounts(STORE_ID, quotaEntry(200L), "subscription:1", "admin");

        // 总额对齐为新上限；used 与周期锚点保留
        assertEquals(200L, account.getTotalQuota());
        assertEquals(12L, account.getUsedQuota());
        Mockito.verify(quotaAccountRepository).save(account);
        ArgumentCaptor<SaasQuotaLog> logCaptor = ArgumentCaptor.forClass(SaasQuotaLog.class);
        Mockito.verify(quotaLogRepository).save(logCaptor.capture());
        assertEquals(SaasQuotaAction.LIMIT_CHANGE, logCaptor.getValue().getAction());
        assertEquals(0L, logCaptor.getValue().getDelta());
        assertEquals("admin", logCaptor.getValue().getOperator());
    }

    @Test
    void existingAccount_newLimitNull_alignsToNullUnlimited() {
        // 核心回归（P10）：豪华版账户 total=50，回退默认版（quotaLimit=null）必须对齐为 null
        SaasQuotaAccount account = existingAccount(50L, 12L);

        aligner.alignQuotaAccounts(STORE_ID, quotaEntry(null), "subscription:2", "system");

        assertNull(account.getTotalQuota());
        assertEquals(12L, account.getUsedQuota());
        Mockito.verify(quotaAccountRepository).save(account);
        ArgumentCaptor<SaasQuotaLog> logCaptor = ArgumentCaptor.forClass(SaasQuotaLog.class);
        Mockito.verify(quotaLogRepository).save(logCaptor.capture());
        assertEquals(SaasQuotaAction.LIMIT_CHANGE, logCaptor.getValue().getAction());
    }

    @Test
    void existingAccount_nullToLimited_alignsToNewLimit() {
        SaasQuotaAccount account = existingAccount(null, 3L);

        aligner.alignQuotaAccounts(STORE_ID, quotaEntry(50L), "subscription:3", "admin");

        assertEquals(50L, account.getTotalQuota());
        assertEquals(3L, account.getUsedQuota());
        Mockito.verify(quotaAccountRepository).save(account);
    }

    @Test
    void missingAccount_createdWithZeroUsedAndGrantLog() {
        Mockito.when(quotaAccountRepository.findByStoreIdAndFeatureCode(STORE_ID, FEATURE))
                .thenReturn(Optional.empty());
        SaasFeature feature = new SaasFeature();
        feature.setFeatureCode(FEATURE);
        feature.setType(SaasFeatureType.QUOTA);
        feature.setDefaultResetCycle(SaasQuotaResetCycle.MONTHLY);
        Mockito.when(featureRepository.findByFeatureCode(FEATURE)).thenReturn(Optional.of(feature));

        aligner.alignQuotaAccounts(STORE_ID, quotaEntry(null), "subscription:4", "system");

        ArgumentCaptor<SaasQuotaAccount> accountCaptor = ArgumentCaptor.forClass(SaasQuotaAccount.class);
        Mockito.verify(quotaAccountRepository).save(accountCaptor.capture());
        SaasQuotaAccount created = accountCaptor.getValue();
        assertNull(created.getTotalQuota());
        assertEquals(0L, created.getUsedQuota());
        assertEquals(SaasQuotaResetCycle.MONTHLY, created.getResetCycle());
        assertTrue(created.getPeriodEnd().isAfter(LocalDateTime.now()));

        ArgumentCaptor<SaasQuotaLog> logCaptor = ArgumentCaptor.forClass(SaasQuotaLog.class);
        Mockito.verify(quotaLogRepository).save(logCaptor.capture());
        assertEquals(SaasQuotaAction.GRANT, logCaptor.getValue().getAction());
    }

    @Test
    void nonQuotaEntries_areSkipped() {
        List<EntitlementSnapshot.Entry> entries = List.of(
                new EntitlementSnapshot.Entry("independent_website", SaasFeatureType.BOOLEAN, null),
                new EntitlementSnapshot.Entry("room_count", SaasFeatureType.CAPACITY, 10L));

        aligner.alignQuotaAccounts(STORE_ID, entries, "subscription:5", "system");

        Mockito.verifyNoInteractions(quotaAccountRepository);
        Mockito.verifyNoInteractions(quotaLogRepository);
    }
}
