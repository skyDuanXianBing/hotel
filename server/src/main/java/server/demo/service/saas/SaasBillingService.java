package server.demo.service.saas;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.saas.SaasBillingOrder;
import server.demo.entity.saas.SaasFeature;
import server.demo.entity.saas.SaasPackage;
import server.demo.entity.saas.SaasPackageFeature;
import server.demo.entity.saas.SaasSubscription;
import server.demo.enums.SaasBillingOrderStatus;
import server.demo.enums.SaasBillingProvider;
import server.demo.enums.SaasFeatureType;
import server.demo.enums.SaasPackagePeriod;
import server.demo.enums.SaasPackageStatus;
import server.demo.enums.SaasSubscriptionStatus;
import server.demo.repository.StoreRepository;
import server.demo.repository.saas.SaasBillingOrderRepository;
import server.demo.repository.saas.SaasFeatureRepository;
import server.demo.repository.saas.SaasPackageFeatureRepository;
import server.demo.repository.saas.SaasPackageRepository;
import server.demo.repository.saas.SaasSubscriptionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 套餐售卖与订阅激活。当前为 DIRECT 直连支付（点击购买即成功），provider 预留 STRIPE。
 */
@Service
public class SaasBillingService {

    private final SaasPackageRepository packageRepository;
    private final SaasPackageFeatureRepository packageFeatureRepository;
    private final SaasFeatureRepository featureRepository;
    private final SaasSubscriptionRepository subscriptionRepository;
    private final SaasQuotaAccountAligner quotaAccountAligner;
    private final SaasBillingOrderRepository billingOrderRepository;
    private final EntitlementService entitlementService;
    private final StoreRepository storeRepository;
    private final SaasBillingReplayService replayService;

    public SaasBillingService(
            SaasPackageRepository packageRepository,
            SaasPackageFeatureRepository packageFeatureRepository,
            SaasFeatureRepository featureRepository,
            SaasSubscriptionRepository subscriptionRepository,
            SaasQuotaAccountAligner quotaAccountAligner,
            SaasBillingOrderRepository billingOrderRepository,
            EntitlementService entitlementService,
            StoreRepository storeRepository,
            SaasBillingReplayService replayService
    ) {
        this.packageRepository = packageRepository;
        this.packageFeatureRepository = packageFeatureRepository;
        this.featureRepository = featureRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.quotaAccountAligner = quotaAccountAligner;
        this.billingOrderRepository = billingOrderRepository;
        this.entitlementService = entitlementService;
        this.storeRepository = storeRepository;
        this.replayService = replayService;
    }

    /**
     * 在售套餐（含权益模板行）。
     */
    @Transactional(readOnly = true)
    public List<SaasPackage> listOnShelfPackages() {
        return packageRepository.findByStatusOrderByPriceAsc(SaasPackageStatus.ON_SHELF);
    }

    @Transactional(readOnly = true)
    public List<SaasPackageFeature> listPackageFeatures(Long packageId) {
        return packageFeatureRepository.findByPackageId(packageId);
    }

    @Transactional(readOnly = true)
    public Optional<SaasFeature> findFeature(String featureCode) {
        return featureRepository.findByFeatureCode(featureCode);
    }

    /**
     * 直连购买：生成 PAID 订单 → 取消旧订阅 → 激活新订阅（冻结权益快照）→ 对齐配额账户额度。
     * 同套餐重购按续费处理（剩余时长叠加），换档立即替换；配额账户不再清零（保留用量）。
     * 全流程一个事务；软限制：不清理任何存量业务数据。
     * 未带幂等键的兼容入口（等价于 idempotencyKey=null，无防重保护）。
     */
    @Transactional
    public SaasSubscription subscribe(Long storeId, Long packageId, String operator) {
        return subscribe(storeId, packageId, operator, null);
    }

    /**
     * 直连购买（带幂等键）：idempotencyKey 命中已有订单时幂等重放——返回该门店当前生效订阅，
     * 不再新建订单/订阅。同 key 的重试/双击/成功响应丢失重发均安全。
     */
    @Transactional
    public SaasSubscription subscribe(Long storeId, Long packageId, String operator, String idempotencyKey) {
        SaasPackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new IllegalArgumentException("套餐不存在"));
        if (pkg.getStatus() != SaasPackageStatus.ON_SHELF) {
            throw new IllegalArgumentException("套餐已下架，无法购买");
        }
        return activate(storeId, pkg, pkg.getPrice(), operator, idempotencyKey, null, null);
    }

    /**
     * 平台管理端人工开通/切换套餐（未带幂等键的兼容入口）。
     */
    @Transactional
    public SaasSubscription grantByAdmin(Long storeId, Long packageId, String operator) {
        return grantByAdmin(storeId, packageId, operator, null);
    }

    /**
     * 平台管理端人工开通/切换套餐：复用与 {@link #subscribe} 相同的事务激活流程，
     * 差异仅在于：不校验上架状态（允许回退到停售的“默认版”）、订单与实付金额均为 0。
     * idempotencyKey 命中已有订单时幂等重放（重复点击/重试不产生重复人工订单）。
     */
    @Transactional
    public SaasSubscription grantByAdmin(Long storeId, Long packageId, String operator, String idempotencyKey) {
        return grantByAdmin(storeId, packageId, operator, idempotencyKey, null, null);
    }

    /**
     * 平台管理端人工开通/切换套餐（等级调控版）：endTimeOverride 非空时订阅终点使用该值
     * （durationDays/permanent 调控），不再按套餐周期或续费规则计算；orderRemark 非空时写入订单备注。
     */
    @Transactional
    public SaasSubscription grantByAdmin(Long storeId, Long packageId, String operator, String idempotencyKey,
                                         LocalDateTime endTimeOverride, String orderRemark) {
        SaasPackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new IllegalArgumentException("套餐不存在"));
        return activate(storeId, pkg, BigDecimal.ZERO, operator, idempotencyKey, endTimeOverride, orderRemark);
    }

    /**
     * 按 id 查询套餐（my-subscription 视图回查 systemPackage 标记用）。
     */
    @Transactional(readOnly = true)
    public Optional<SaasPackage> findPackage(Long packageId) {
        return packageRepository.findById(packageId);
    }

    /**
     * 订阅激活核心流程：生成 PAID 订单（amount 由调用方决定）→ 取消旧订阅 → 激活新订阅（冻结快照）→ 对齐配额账户。
     *
     * <p>订阅终点规则（P9 续费语义）：
     * <ul>
     *   <li>endTimeOverride 非空（管理端调控）→ 终点 = endTimeOverride；</li>
     *   <li>同套餐重购（门店有该套餐 ACTIVE 订阅）→ 续费：终点 = max(now, 旧 endTime) + 套餐周期，剩余时长不没收；</li>
     *   <li>换档/首购（不同 packageId）→ 终点 = now + 套餐周期（立即替换）。
     * </ul>
     *
     * <p>配额账户规则（P9 起不再清零）：新快照内每个 QUOTA 权益——账户已存在则仅更新额度上限为
     * 新 quotaLimit（保留 used 与周期锚点，上限变化时写 LIMIT_CHANGE 流水）；不存在则新建
     * （used=0、周期自 now，写 GRANT 流水）；新套餐没有的 QUOTA 权益不动其账户。
     *
     * <p>幂等设计（审查 B1/B2 + D2 修复）：
     * <ol>
     *   <li>先按 (store_id, idempotency_key) 查询——已成交请求的重试直接重放，返回当前生效订阅；</li>
     *   <li>对 stores 行加悲观写锁，串行化同门店的全部激活（同时收敛并发双 ACTIVE 的竞态）；</li>
     *   <li>锁内二次复查——并发先到者提交后，后到者在锁内命中复查而重放，uk(store_id, idempotency_key)
     *       仅作为绕过锁路径的最终兜底。不把捕获唯一键异常作为主流程：异常一旦发生当前事务即被
     *       标记回滚，锁内复查避免了把异常纳入正常控制流。
     *   <li>D2：REPEATABLE READ 下锁外预查已建立事务快照时，锁内复查可能看不到同毫秒先到者的
     *       已提交订单而直达 uk 冲突（DataIntegrityViolationException）。该残存竞态由事务边界外的
     *       包装层（SaasBillingIdempotentGateway / AdminSubscriptionController 边界）捕获，
     *       并经 {@link SaasBillingReplayService#findReplaySubscriptionInNewTransaction} 在新事务
     *       快照中重查已提交订单，恢复为幂等重放，不再裸 500。
     * </ol>
     */
    private SaasSubscription activate(Long storeId, SaasPackage pkg, BigDecimal amount, String operator,
                                      String idempotencyKey, LocalDateTime endTimeOverride, String orderRemark) {
        String key = normalizeIdempotencyKey(idempotencyKey);

        // 1. 锁外快路径：已成功请求的重试直接重放
        if (key != null) {
            Optional<SaasSubscription> replay = replayService.findReplaySubscription(storeId, key);
            if (replay.isPresent()) {
                return replay.get();
            }
        }

        // 2. 串行化同门店开通/购买（门店行必然存在；不存在时与既有行为一致地继续）
        storeRepository.findByIdForUpdate(storeId);

        // 3. 锁内复查：并发先到者已提交的订单在此可见，转化为幂等重放而非唯一键冲突
        if (key != null) {
            Optional<SaasSubscription> replay = replayService.findReplaySubscription(storeId, key);
            if (replay.isPresent()) {
                return replay.get();
            }
        }

        // 4. 订单（直连点击购买=套餐价；管理端人工开通=0；人工调控备注写入 remark）
        SaasBillingOrder order = new SaasBillingOrder();
        order.setStoreId(storeId);
        order.setPackageId(pkg.getId());
        order.setAmount(amount);
        order.setProvider(SaasBillingProvider.DIRECT);
        order.setStatus(SaasBillingOrderStatus.PAID);
        order.setIdempotencyKey(key);
        order.setRemark(orderRemark);
        billingOrderRepository.save(order);

        // 5. 取消当前有效订阅（软切换，不动存量数据）；同套餐重购记录续费基准（旧订阅最远终点）
        List<SaasSubscription> activeSubscriptions = subscriptionRepository.findByStoreIdAndStatus(
                storeId, SaasSubscriptionStatus.ACTIVE);
        LocalDateTime renewBase = null;
        for (SaasSubscription active : activeSubscriptions) {
            if (pkg.getId().equals(active.getPackageId())
                    && (renewBase == null || active.getEndTime().isAfter(renewBase))) {
                renewBase = active.getEndTime();
            }
            active.setStatus(SaasSubscriptionStatus.CANCELLED);
            subscriptionRepository.save(active);
        }

        // 6. 激活新订阅，冻结权益快照
        List<SaasPackageFeature> packageFeatures = packageFeatureRepository.findByPackageId(pkg.getId());
        List<EntitlementSnapshot.Entry> entries = new ArrayList<>();
        for (SaasPackageFeature packageFeature : packageFeatures) {
            SaasFeatureType type = featureRepository.findByFeatureCode(packageFeature.getFeatureCode())
                    .map(SaasFeature::getType)
                    .orElseThrow(() -> new IllegalStateException(
                            "功能字典缺失: " + packageFeature.getFeatureCode()));
            entries.add(new EntitlementSnapshot.Entry(
                    packageFeature.getFeatureCode(), type, packageFeature.getQuotaLimit()));
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime;
        if (endTimeOverride != null) {
            // 管理端调控（durationDays / permanent）：显式终点优先
            endTime = endTimeOverride;
        } else if (renewBase != null) {
            // 同套餐续费：max(now, 旧 endTime) + 套餐周期，剩余时长叠加不没收
            endTime = plusPeriod(renewBase.isAfter(now) ? renewBase : now, pkg.getPeriod());
        } else {
            // 换档/首购：now + 套餐周期（立即替换）
            endTime = plusPeriod(now, pkg.getPeriod());
        }
        SaasSubscription subscription = new SaasSubscription();
        subscription.setStoreId(storeId);
        subscription.setPackageId(pkg.getId());
        subscription.setPackageName(pkg.getName());
        subscription.setEntitlementSnapshotJson(
                entitlementService.serializeSnapshot(new EntitlementSnapshot(entries)));
        subscription.setPricePaid(amount);
        subscription.setStartTime(now);
        subscription.setEndTime(endTime);
        subscription.setStatus(SaasSubscriptionStatus.ACTIVE);
        subscriptionRepository.save(subscription);

        // 7. 对齐 QUOTA 配额账户（P9 起不再清零）：账户已存在仅更新额度上限为新 quotaLimit
        //    （保留 used 与周期锚点，上限实际变化时写 LIMIT_CHANGE 流水）；不存在则新建
        //    （used=0、周期自 now，写 GRANT 流水）；新套餐没有的 QUOTA 权益不动其账户。
        //    P10 起对齐逻辑抽取至 SaasQuotaAccountAligner，与到期回退默认版共用同一口径
        //    （含对齐为 null=不限）。
        quotaAccountAligner.alignQuotaAccounts(
                storeId, entries, "subscription:" + subscription.getId(), operator);

        return subscription;
    }

    private static LocalDateTime plusPeriod(LocalDateTime base, SaasPackagePeriod period) {
        return period == SaasPackagePeriod.YEAR ? base.plusYears(1) : base.plusMonths(1);
    }

    /**
     * 幂等键归一化：trim 后空串视为未带键；长度超过数据库列宽（64）拒绝（400）。
     */
    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null) {
            return null;
        }
        String key = idempotencyKey.trim();
        if (key.isEmpty()) {
            return null;
        }
        if (key.length() > 64) {
            throw new IllegalArgumentException("idempotencyKey 长度不能超过 64 字符");
        }
        return key;
    }
}
