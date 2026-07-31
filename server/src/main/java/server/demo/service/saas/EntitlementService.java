package server.demo.service.saas;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SaaS 权益判定与配额扣减核心服务。
 *
 * 关键策略：
 * 1. 全部判定基于订阅成交时的权益快照（entitlement_snapshot_json），不回查套餐模板。
 * 2. 无 ACTIVE 订阅一律拒绝（fail-closed）；ACTIVE 订阅已过 end_time 时惰性标记 EXPIRED，
 *    并经 {@link SaasDefaultPackageFallbackService} 自动回退「默认版」兜底订阅（P9，业主拍板）。
 * 3. QUOTA 并发安全：先惰性周期滚动（条件 UPDATE，过期才重置），再原子条件 UPDATE 预扣
 *    （used+delta &lt;= total 才生效），不靠应用层先读后写，也无长事务行锁。
 * 4. AI 等外部调用失败由调用方（切面）调 {@link #refundQuota} 补偿返还。
 */
@Service
public class EntitlementService {

    private static final Logger logger = LoggerFactory.getLogger(EntitlementService.class);

    private final SaasSubscriptionRepository subscriptionRepository;
    private final SaasQuotaAccountRepository quotaAccountRepository;
    private final SaasQuotaLogRepository quotaLogRepository;
    private final SaasFeatureRepository featureRepository;
    private final SaasQuotaAccountProvisioner quotaAccountProvisioner;
    private final SaasDefaultPackageFallbackService defaultPackageFallbackService;
    private final ObjectMapper objectMapper;

    public EntitlementService(
            SaasSubscriptionRepository subscriptionRepository,
            SaasQuotaAccountRepository quotaAccountRepository,
            SaasQuotaLogRepository quotaLogRepository,
            SaasFeatureRepository featureRepository,
            SaasQuotaAccountProvisioner quotaAccountProvisioner,
            SaasDefaultPackageFallbackService defaultPackageFallbackService,
            ObjectMapper objectMapper
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.quotaAccountRepository = quotaAccountRepository;
        this.quotaLogRepository = quotaLogRepository;
        this.featureRepository = featureRepository;
        this.quotaAccountProvisioner = quotaAccountProvisioner;
        this.defaultPackageFallbackService = defaultPackageFallbackService;
        this.objectMapper = objectMapper;
    }

    // ------------------------------------------------------------------
    // 订阅与快照查询
    // ------------------------------------------------------------------

    /**
     * 当前有效订阅；若 ACTIVE 记录已过 end_time，则惰性标记 EXPIRED，并自动为该门店创建
     * 「默认版」兜底订阅（ACTIVE、不下订单，幂等：已存在兜底则直接复用）后返回兜底订阅。
     * 从未有过订阅的门店不回退（返回 empty，调用方按无订阅 402 处理）；
     * 未配置系统兜底套餐（is_system=1）时同样返回 empty 维持 fail-closed。
     */
    @Transactional
    public Optional<SaasSubscription> findActiveSubscription(Long storeId) {
        Optional<SaasSubscription> found = subscriptionRepository
                .findFirstByStoreIdAndStatusOrderByEndTimeDesc(storeId, SaasSubscriptionStatus.ACTIVE);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        SaasSubscription subscription = found.get();
        if (!subscription.getEndTime().isAfter(LocalDateTime.now())) {
            subscription.setStatus(SaasSubscriptionStatus.EXPIRED);
            subscriptionRepository.save(subscription);
            return defaultPackageFallbackService.ensureFallbackSubscription(storeId);
        }
        return found;
    }

    /**
     * 当前权益快照；无有效订阅返回 null（调用方按 fail-closed 处理）。
     */
    @Transactional
    public EntitlementSnapshot getSnapshot(Long storeId) {
        return findActiveSubscription(storeId)
                .map(this::parseSnapshot)
                .orElse(null);
    }

    public EntitlementSnapshot parseSnapshot(SaasSubscription subscription) {
        try {
            return objectMapper.readValue(subscription.getEntitlementSnapshotJson(), EntitlementSnapshot.class);
        } catch (Exception e) {
            throw new IllegalStateException("订阅权益快照解析失败: subscriptionId=" + subscription.getId(), e);
        }
    }

    public String serializeSnapshot(EntitlementSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            throw new IllegalStateException("订阅权益快照序列化失败", e);
        }
    }

    // ------------------------------------------------------------------
    // BOOLEAN 权益
    // ------------------------------------------------------------------

    @Transactional
    public void requireBooleanFeature(Long storeId, String featureCode) {
        EntitlementSnapshot snapshot = getSnapshot(storeId);
        EntitlementSnapshot.Entry entry = snapshot != null ? snapshot.find(featureCode) : null;
        if (entry == null || entry.type() != SaasFeatureType.BOOLEAN) {
            throw entryMissingException(snapshot, featureCode);
        }
    }

    /**
     * 非抛异常的权益判定：门店当前有效订阅快照中是否包含指定 BOOLEAN 权益。
     * 供公开站等场景做分支判断（关站/暂停接单）；无订阅、权益缺失或类型不符一律 false
     * （fail-closed），与 {@link #requireBooleanFeature} 的判定口径一致。
     */
    @Transactional
    public boolean storeHasFeature(Long storeId, String featureCode) {
        EntitlementSnapshot snapshot = getSnapshot(storeId);
        EntitlementSnapshot.Entry entry = snapshot != null ? snapshot.find(featureCode) : null;
        return entry != null && entry.type() == SaasFeatureType.BOOLEAN;
    }

    // ------------------------------------------------------------------
    // QUOTA 权益（预扣 / 返还 / 用量查询）
    // ------------------------------------------------------------------

    /**
     * 并发安全预扣。额度不足 / 无权益抛 {@link NeedUpgradeException}。
     * 扣减成功后若业务失败，调用方必须调 {@link #refundQuota} 返还。
     */
    @Transactional
    public void deductQuota(Long storeId, String featureCode, long delta, String bizId) {
        if (delta <= 0) {
            return;
        }
        EntitlementSnapshot snapshot = getSnapshot(storeId);
        EntitlementSnapshot.Entry entry = snapshot != null ? snapshot.find(featureCode) : null;
        if (entry == null || entry.type() != SaasFeatureType.QUOTA) {
            throw entryMissingException(snapshot, featureCode);
        }
        if (entry.limit() == null) {
            // 不限额度：直接放行，无需记账
            return;
        }

        SaasQuotaAccount account = ensureAccount(storeId, featureCode, entry.limit());
        rollPeriodIfExpired(account);

        int updated = quotaAccountRepository.deductIfAvailable(storeId, featureCode, delta);
        if (updated == 0) {
            Long used = quotaAccountRepository.findByStoreIdAndFeatureCode(storeId, featureCode)
                    .map(SaasQuotaAccount::getUsedQuota)
                    .orElse(null);
            String cycleText = account.getResetCycle() == SaasQuotaResetCycle.MONTHLY ? "本月" : "";
            throw new NeedUpgradeException(featureCode, entry.limit(), used,
                    cycleText + "额度已用尽，请升级套餐", NeedUpgradeException.Reason.QUOTA_EXHAUSTED);
        }
        writeLog(storeId, featureCode, delta, SaasQuotaAction.DEDUCT, bizId, null);
    }

    /**
     * 失败补偿返还（可重复调用，used 下限为 0）。无账户时为无操作。
     *
     * <p>审查 G1/G6/G2 修复后的口径：
     * <ul>
     *   <li>不限额度（totalQuota IS NULL）：deductQuota 本就不记账，返还直接 no-op，
     *       不再产生无配对 DEDUCT 的 REFUND 噪音流水（G1）；</li>
     *   <li>refund SQL 带 used_quota &gt; 0 守卫：重复返还、used=0 的无效返还不命中行、
     *       不写流水（G6），对账时 REFUND 条数恒等于真实返还次数；</li>
     *   <li>used 为负（人工补偿未消耗）时返还同样不命中：补偿额度不再被 floor-0 抬升吞掉（G2）。
     * </ul>
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refundQuota(Long storeId, String featureCode, long delta, String bizId) {
        if (delta <= 0) {
            return;
        }
        Optional<SaasQuotaAccount> account = quotaAccountRepository.findByStoreIdAndFeatureCode(storeId, featureCode);
        if (account.isEmpty()) {
            return;
        }
        if (account.get().getTotalQuota() == null) {
            // 不限额度：扣减时未记账，返还无对应 DEDUCT，直接 no-op（G1）
            return;
        }
        int updated = quotaAccountRepository.refund(storeId, featureCode, delta);
        if (updated > 0) {
            writeLog(storeId, featureCode, -delta, SaasQuotaAction.REFUND, bizId, null);
        }
    }

    /**
     * 单 feature 用量查询（读取时若窗口过期则惰性滚动并写 RESET 流水）。
     *
     * <p>P10 修复：上限（totalQuota/remaining）一律取<b>当前订阅快照权益</b>（唯一事实源），
     * 账户只提供 used 与周期窗口。修复前账户存在时上限取自账户 totalQuota——回退默认版后
     * 账户仍是豪华版时期的 50，而默认版 ai_website_gen 为不限（快照 limit=null），
     * my-subscription 误显示「已用 0/50」。账户总额由激活/回退路径经
     * {@link SaasQuotaAccountAligner} 对齐（含对齐为 null），此处再按快照出视图，双保险。</p>
     */
    @Transactional
    public QuotaUsage getQuotaUsage(Long storeId, String featureCode) {
        EntitlementSnapshot snapshot = getSnapshot(storeId);
        EntitlementSnapshot.Entry entry = snapshot != null ? snapshot.find(featureCode) : null;
        Long limit = entry != null ? entry.limit() : null;

        Optional<SaasQuotaAccount> found = quotaAccountRepository.findByStoreIdAndFeatureCode(storeId, featureCode);
        if (found.isEmpty()) {
            return new QuotaUsage(featureCode, limit, 0L, limit, null, null);
        }
        SaasQuotaAccount account = rollPeriodIfExpired(found.get());
        Long remaining = limit == null
                ? null
                : Math.max(0L, limit - account.getUsedQuota());
        return new QuotaUsage(
                featureCode,
                limit,
                account.getUsedQuota(),
                remaining,
                account.getPeriodStart(),
                account.getPeriodEnd()
        );
    }

    /**
     * 当前订阅快照中全部 QUOTA 权益的用量。
     */
    @Transactional
    public List<QuotaUsage> listQuotaUsages(Long storeId) {
        EntitlementSnapshot snapshot = getSnapshot(storeId);
        if (snapshot == null || snapshot.features() == null) {
            return List.of();
        }
        List<QuotaUsage> usages = new ArrayList<>();
        for (EntitlementSnapshot.Entry entry : snapshot.features()) {
            if (entry.type() == SaasFeatureType.QUOTA) {
                usages.add(getQuotaUsage(storeId, entry.featureCode()));
            }
        }
        return usages;
    }

    // ------------------------------------------------------------------
    // QUOTA 人工调整（平台管理端）
    // ------------------------------------------------------------------

    /**
     * 平台管理端人工调整配额：delta &gt; 0 增加剩余额度（如客服补偿），delta &lt; 0 扣减剩余额度。
     * 写 ADJUST 流水，operator 为操作的管理员账号。
     * 不限额度（limit == null）不记账，仅留审计流水；无有效订阅或权益缺失抛 IllegalArgumentException。
     *
     * <p>语义说明：正 delta 允许 used 变负（补偿后剩余额度可超过套餐总额），负 delta 允许 used
     * 超过总额（处罚语义，视图层 remaining 下限为 0）。与 {@link #refundQuota} 的交互（审查 G2
     * 修复后）：失败返还的 SQL 仅命中 used &gt; 0 的行，used 为负（补偿未消耗完）时返还是 no-op，
     * 不再把 used 抬回 0——未消耗的补偿额度得以保留，不会被返还吞掉。
     *
     * @return 调整后的用量视图
     */
    @Transactional
    public QuotaUsage adjustQuota(Long storeId, String featureCode, long delta, String remark, String operator) {
        if (delta == 0) {
            throw new IllegalArgumentException("调整量不能为 0");
        }
        EntitlementSnapshot snapshot = getSnapshot(storeId);
        EntitlementSnapshot.Entry entry = snapshot != null ? snapshot.find(featureCode) : null;
        if (entry == null || entry.type() != SaasFeatureType.QUOTA) {
            throw new IllegalArgumentException("该门店当前订阅不包含此配额权益: " + featureCode);
        }
        if (entry.limit() == null) {
            // 不限额度：调整无实际效果，仅写审计流水
            writeLog(storeId, featureCode, delta, SaasQuotaAction.ADJUST, remark, operator);
            return getQuotaUsage(storeId, featureCode);
        }
        SaasQuotaAccount account = ensureAccount(storeId, featureCode, entry.limit());
        rollPeriodIfExpired(account);
        quotaAccountRepository.adjustUsedByRemainingDelta(account.getId(), delta);
        writeLog(storeId, featureCode, delta, SaasQuotaAction.ADJUST, remark, operator);
        return getQuotaUsage(storeId, featureCode);
    }

    // ------------------------------------------------------------------
    // CAPACITY 权益（实时 COUNT 由调用方提供，容量型不经本服务记账）
    // ------------------------------------------------------------------

    /**
     * 容量校验：currentCount + delta 超过上限则抛 {@link NeedUpgradeException}。
     * 软限制语义由调用方保证（只在新增路径调用，存量不受影响）。
     */
    @Transactional
    public void checkCapacity(Long storeId, String featureCode, long currentCount, long delta) {
        EntitlementSnapshot snapshot = getSnapshot(storeId);
        EntitlementSnapshot.Entry entry = snapshot != null ? snapshot.find(featureCode) : null;
        if (entry == null || entry.type() != SaasFeatureType.CAPACITY) {
            throw entryMissingException(snapshot, featureCode);
        }
        if (entry.limit() == null) {
            return;
        }
        if (currentCount + delta > entry.limit()) {
            String featureName = featureRepository.findByFeatureCode(featureCode)
                    .map(SaasFeature::getName)
                    .orElse(featureCode);
            throw new NeedUpgradeException(
                    featureCode,
                    entry.limit(),
                    currentCount,
                    "已达到套餐的" + featureName + "上限（" + entry.limit() + "），请升级套餐后再新增",
                    NeedUpgradeException.Reason.CAPACITY_EXCEEDED
            );
        }
    }

    // ------------------------------------------------------------------
    // 内部：权益缺失拒绝 / 账户初始化 / 惰性滚动 / 流水
    // ------------------------------------------------------------------

    /**
     * 权益缺失的统一拒绝（requireBooleanFeature/deductQuota/checkCapacity 的 entry==null 分支）。
     * 区分两种 402 情形并写入 reason：
     * <ul>
     *   <li>snapshot==null（门店无任何有效订阅）→ NO_SUBSCRIPTION，"请先购买套餐"；</li>
     *   <li>entry==null（有订阅但套餐不含该权益，或权益类型不匹配）→ NOT_INCLUDED，"请升级套餐"。</li>
     * </ul>
     */
    private NeedUpgradeException entryMissingException(EntitlementSnapshot snapshot, String featureCode) {
        if (snapshot == null) {
            return new NeedUpgradeException(featureCode, null, null,
                    "当前门店尚未开通套餐，请先购买套餐", NeedUpgradeException.Reason.NO_SUBSCRIPTION);
        }
        return new NeedUpgradeException(featureCode, null, null,
                "当前套餐不包含该功能，请升级套餐", NeedUpgradeException.Reason.NOT_INCLUDED);
    }

    /**
     * 查找或惰性建账。并发首用竞态（审查 C1）：建账经 {@link SaasQuotaAccountProvisioner}
     * 在 REQUIRES_NEW 独立事务中执行；并发失败方的唯一键异常仅回滚该独立事务，
     * 此处捕获后重新查找——并发胜者的账户行在其提交后对本事务可见，透明恢复不再 500。
     * （不能在本类大事务内直接 save 后捕获：参与方失败会把大事务标记回滚，提交时仍
     *  UnexpectedRollbackException。）
     */
    private SaasQuotaAccount ensureAccount(Long storeId, String featureCode, Long totalQuota) {
        Optional<SaasQuotaAccount> found = quotaAccountRepository.findByStoreIdAndFeatureCode(storeId, featureCode);
        if (found.isPresent()) {
            return found.get();
        }
        // 兜底：订阅存在但账户缺失（如历史数据），按快照额度即时建账
        try {
            return quotaAccountProvisioner.ensureCreated(storeId, featureCode, totalQuota);
        } catch (DataIntegrityViolationException e) {
            // 并发首用建账撞 uk：读取并发胜者已提交的账户行继续
            return quotaAccountRepository.findByStoreIdAndFeatureCode(storeId, featureCode)
                    .orElseThrow(() -> new IllegalStateException(
                            "配额账户并发创建后读取失败: storeId=" + storeId + ", feature=" + featureCode, e));
        }
    }

    /**
     * 惰性周期滚动：窗口过期则条件 UPDATE 重置（并发下仅一个事务生效），并写 RESET 流水。
     */
    private SaasQuotaAccount rollPeriodIfExpired(SaasQuotaAccount account) {
        if (account.getResetCycle() != SaasQuotaResetCycle.MONTHLY) {
            return account;
        }
        LocalDateTime now = LocalDateTime.now();
        if (account.getPeriodEnd() == null || account.getPeriodEnd().isAfter(now)) {
            return account;
        }
        LocalDateTime newStart = account.getPeriodEnd();
        LocalDateTime newEnd = newStart.plusMonths(1);
        while (!newEnd.isAfter(now)) {
            newStart = newEnd;
            newEnd = newEnd.plusMonths(1);
        }
        int updated = quotaAccountRepository.resetPeriodIfExpired(account.getId(), now, newStart, newEnd);
        if (updated > 0) {
            writeLog(account.getStoreId(), account.getFeatureCode(),
                    -account.getUsedQuota(), SaasQuotaAction.RESET, null, "system");
            account.setUsedQuota(0L);
            account.setPeriodStart(newStart);
            account.setPeriodEnd(newEnd);
        } else {
            // 并发滚动：另一个事务已重置，重新读取新窗口
            account = quotaAccountRepository.findByStoreIdAndFeatureCode(
                    account.getStoreId(), account.getFeatureCode()).orElse(account);
        }
        return account;
    }

    static LocalDateTime nextPeriodEnd(LocalDateTime start, SaasQuotaResetCycle cycle) {
        if (cycle == SaasQuotaResetCycle.NONE) {
            // 不自动重置：给一个足够远的窗口终点
            return start.plusYears(100);
        }
        return start.plusMonths(1);
    }

    void writeLog(Long storeId, String featureCode, Long delta, SaasQuotaAction action, String bizId, String operator) {
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
