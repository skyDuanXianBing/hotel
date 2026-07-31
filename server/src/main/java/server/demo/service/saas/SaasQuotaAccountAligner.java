package server.demo.service.saas;

import org.springframework.stereotype.Component;
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
import java.util.Objects;
import java.util.Optional;

/**
 * 配额账户与新订阅快照对齐（P10 抽取的共享组件）。
 *
 * <p>供「购买/人工开通激活」（{@link SaasBillingService}）与「到期回退默认版」
 * （{@link SaasDefaultPackageFallbackService}）两条路径复用同一套对齐口径：
 * <ul>
 *   <li>新快照内每个 QUOTA 权益——账户已存在则仅将 totalQuota 对齐为新 quotaLimit
 *       （{@link Objects#equals} 语义，<b>包括对齐为 null（不限）</b>；保留 used 与周期锚点，
 *       上限实际变化时写 LIMIT_CHANGE 流水，delta=0 不动对账）；</li>
 *   <li>账户不存在则新建（used=0、周期自 now，写 GRANT 流水）；</li>
 *   <li>新套餐没有的 QUOTA 权益不动其账户。</li>
 * </ul>
 *
 * <p>独立 bean、只依赖 repository——SaasDefaultPackageFallbackService 由 EntitlementService
 * 调用，若把对齐留在 SaasBillingService 会形成 EntitlementService → SaasBillingService →
 * EntitlementService 循环依赖。
 */
@Component
public class SaasQuotaAccountAligner {

    private final SaasQuotaAccountRepository quotaAccountRepository;
    private final SaasFeatureRepository featureRepository;
    private final SaasQuotaLogRepository quotaLogRepository;

    public SaasQuotaAccountAligner(
            SaasQuotaAccountRepository quotaAccountRepository,
            SaasFeatureRepository featureRepository,
            SaasQuotaLogRepository quotaLogRepository
    ) {
        this.quotaAccountRepository = quotaAccountRepository;
        this.featureRepository = featureRepository;
        this.quotaLogRepository = quotaLogRepository;
    }

    /**
     * 将门店配额账户总额对齐到订阅快照权益（保留用量）。
     *
     * @param storeId  门店 id
     * @param entries  新订阅的权益快照条目（仅 QUOTA 型参与对齐）
     * @param bizId    流水关联业务 id（如 subscription:{id}）
     * @param operator 操作人（回退等系统路径为 "system"）
     */
    public void alignQuotaAccounts(Long storeId, List<EntitlementSnapshot.Entry> entries,
                                   String bizId, String operator) {
        for (EntitlementSnapshot.Entry entry : entries) {
            if (entry.type() != SaasFeatureType.QUOTA) {
                continue;
            }
            Optional<SaasQuotaAccount> found = quotaAccountRepository
                    .findByStoreIdAndFeatureCode(storeId, entry.featureCode());
            if (found.isPresent()) {
                SaasQuotaAccount account = found.get();
                // null 安全比较：上限从有额度回退为不限（null）也必须落库
                if (!Objects.equals(account.getTotalQuota(), entry.limit())) {
                    account.setTotalQuota(entry.limit());
                    quotaAccountRepository.save(account);
                    writeLog(storeId, entry.featureCode(), 0L,
                            SaasQuotaAction.LIMIT_CHANGE, bizId, operator);
                }
                continue;
            }

            SaasQuotaResetCycle resetCycle = featureRepository.findByFeatureCode(entry.featureCode())
                    .map(SaasFeature::getDefaultResetCycle)
                    .orElse(SaasQuotaResetCycle.MONTHLY);
            LocalDateTime now = LocalDateTime.now();
            SaasQuotaAccount account = new SaasQuotaAccount();
            account.setStoreId(storeId);
            account.setFeatureCode(entry.featureCode());
            account.setTotalQuota(entry.limit());
            account.setUsedQuota(0L);
            account.setPeriodStart(now);
            account.setPeriodEnd(EntitlementService.nextPeriodEnd(now, resetCycle));
            account.setResetCycle(resetCycle != null ? resetCycle : SaasQuotaResetCycle.MONTHLY);
            quotaAccountRepository.save(account);

            writeLog(storeId, entry.featureCode(), 0L, SaasQuotaAction.GRANT, bizId, operator);
        }
    }

    private void writeLog(Long storeId, String featureCode, Long delta,
                          SaasQuotaAction action, String bizId, String operator) {
        SaasQuotaLog log = new SaasQuotaLog();
        log.setStoreId(storeId);
        log.setFeatureCode(featureCode);
        log.setDelta(delta);
        log.setAction(action);
        log.setBizId(bizId);
        log.setOperator(operator);
        quotaLogRepository.save(log);
    }
}
