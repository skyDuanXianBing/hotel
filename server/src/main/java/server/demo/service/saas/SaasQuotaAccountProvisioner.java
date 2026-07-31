package server.demo.service.saas;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.saas.SaasFeature;
import server.demo.entity.saas.SaasQuotaAccount;
import server.demo.enums.SaasQuotaResetCycle;
import server.demo.repository.saas.SaasFeatureRepository;
import server.demo.repository.saas.SaasQuotaAccountRepository;

import java.time.LocalDateTime;

/**
 * 配额账户惰性建账（审查 C1 修复的协作组件）。
 *
 * 建账在独立事务（REQUIRES_NEW）中执行：并发首用时两个事务同时 INSERT 会撞
 * uk_saas_quota_account_store_feature，失败方的 DataIntegrityViolationException 只会回滚这个
 * 独立小事务（独立连接、独立提交点），不会污染调用方（如 deductQuota/adjustQuota）所在的大事务——
 * 调用方捕获后重新 findByStoreIdAndFeatureCode 即可读到并发胜者已提交的账户行透明恢复。
 * 若在调用方大事务内直接 INSERT，唯一键异常会把大事务标记回滚（参与方失败全局回滚），
 * 即使捕获继续执行，提交时仍会 UnexpectedRollbackException 500。
 */
@Component
public class SaasQuotaAccountProvisioner {

    private final SaasQuotaAccountRepository quotaAccountRepository;
    private final SaasFeatureRepository featureRepository;

    public SaasQuotaAccountProvisioner(
            SaasQuotaAccountRepository quotaAccountRepository,
            SaasFeatureRepository featureRepository
    ) {
        this.quotaAccountRepository = quotaAccountRepository;
        this.featureRepository = featureRepository;
    }

    /**
     * 按快照额度即时建账（订阅存在但账户缺失的兜底，如存量门店首用）。
     * 独立事务提交：即便调用方大事务后续回滚，账户行留存也无害（与 activate 建账口径一致，
     * 下次调用直接复用）。并发撞 uk 时本事务回滚并向上抛 DataIntegrityViolationException，
     * 由调用方捕获后重查恢复。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SaasQuotaAccount ensureCreated(Long storeId, String featureCode, Long totalQuota) {
        SaasQuotaResetCycle resetCycle = featureRepository.findByFeatureCode(featureCode)
                .map(SaasFeature::getDefaultResetCycle)
                .orElse(SaasQuotaResetCycle.MONTHLY);
        LocalDateTime now = LocalDateTime.now();
        SaasQuotaAccount account = new SaasQuotaAccount();
        account.setStoreId(storeId);
        account.setFeatureCode(featureCode);
        account.setTotalQuota(totalQuota);
        account.setUsedQuota(0L);
        account.setPeriodStart(now);
        account.setPeriodEnd(EntitlementService.nextPeriodEnd(now, resetCycle));
        account.setResetCycle(resetCycle != null ? resetCycle : SaasQuotaResetCycle.MONTHLY);
        return quotaAccountRepository.save(account);
    }
}
