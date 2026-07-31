package server.demo.service.saas;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.saas.SaasFeature;
import server.demo.entity.saas.SaasPackage;
import server.demo.entity.saas.SaasPackageFeature;
import server.demo.entity.saas.SaasSubscription;
import server.demo.enums.SaasSubscriptionStatus;
import server.demo.repository.StoreRepository;
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
 * 订阅到期后的「默认版」自动兜底（P9，业主拍板：到期回退默认版）。
 *
 * <p>当 {@link EntitlementService#findActiveSubscription} 惰性判定 ACTIVE 订阅已过 end_time 时，
 * 标记 EXPIRED 后经本服务为该门店创建系统兜底套餐（is_system=1，即种子「默认版」）的
 * ACTIVE 订阅：不下订单、实付 0、end_time 2099-12-31 23:59:59、
 * remark='auto-fallback-after-expiry'，门店权益无缝回退到默认版而非硬 402。
 *
 * <p>设计约束：
 * <ul>
 *   <li>独立 bean、只依赖 repository + ObjectMapper + {@link SaasQuotaAccountAligner}（后者亦仅
 *       依赖 repository）——由 EntitlementService 调用，若复用 SaasBillingService 会形成
 *       EntitlementService → SaasBillingService → EntitlementService 循环依赖；</li>
 *   <li>幂等：同门店多次触发只建一条——先对 stores 行加悲观写锁串行化，锁内再查
 *       是否已存在该系统套餐的 ACTIVE 订阅（并发先到者提交后后到者命中复查）；</li>
 *   <li>从未有过订阅的门店不会走到这里（无 ACTIVE 即返回 empty，不回退，维持 402）；</li>
 *   <li>未配置系统兜底套餐（V065 未执行/被移除）时返回 empty，维持 fail-closed；</li>
 *   <li>功能字典缺失的权益行跳过并告警，不阻断兜底创建（读路径不得 500）。</li>
 * </ul>
 */
@Service
public class SaasDefaultPackageFallbackService {

    public static final String FALLBACK_REMARK = "auto-fallback-after-expiry";
    public static final LocalDateTime FALLBACK_END_TIME = LocalDateTime.of(2099, 12, 31, 23, 59, 59);

    private static final Logger logger = LoggerFactory.getLogger(SaasDefaultPackageFallbackService.class);

    private final SaasPackageRepository packageRepository;
    private final SaasPackageFeatureRepository packageFeatureRepository;
    private final SaasFeatureRepository featureRepository;
    private final SaasSubscriptionRepository subscriptionRepository;
    private final StoreRepository storeRepository;
    private final SaasQuotaAccountAligner quotaAccountAligner;
    private final ObjectMapper objectMapper;

    public SaasDefaultPackageFallbackService(
            SaasPackageRepository packageRepository,
            SaasPackageFeatureRepository packageFeatureRepository,
            SaasFeatureRepository featureRepository,
            SaasSubscriptionRepository subscriptionRepository,
            StoreRepository storeRepository,
            SaasQuotaAccountAligner quotaAccountAligner,
            ObjectMapper objectMapper
    ) {
        this.packageRepository = packageRepository;
        this.packageFeatureRepository = packageFeatureRepository;
        this.featureRepository = featureRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.storeRepository = storeRepository;
        this.quotaAccountAligner = quotaAccountAligner;
        this.objectMapper = objectMapper;
    }

    /**
     * 确保该门店存在系统兜底套餐的 ACTIVE 订阅：已存在则幂等返回，否则创建。
     *
     * <p>P10：创建与复用两条路径都会将配额账户总额对齐到兜底套餐快照（保留用量），
     * 修复「豪华版到期回退默认版后，配额账户仍是旧总额（如 50），my-subscription 显示
     * 已用 0/50」——兜底套餐 ai_website_gen 为不限（quotaLimit=null），对齐必须覆盖
     * 更新为 null 的情形；复用路径对齐用于修复本修复上线前已创建的存量兜底订阅账户。
     *
     * @return 已存在或新建的兜底订阅；未配置系统兜底套餐时 empty
     */
    @Transactional
    public Optional<SaasSubscription> ensureFallbackSubscription(Long storeId) {
        Optional<SaasPackage> systemPackage = packageRepository.findFirstByIsSystemTrueOrderByIdAsc();
        if (systemPackage.isEmpty()) {
            logger.warn("未配置系统兜底套餐（is_system=1），门店 {} 到期后维持无订阅 fail-closed", storeId);
            return Optional.empty();
        }
        SaasPackage pkg = systemPackage.get();

        // 冻结兜底套餐的权益快照（字典缺失的权益行跳过并告警，不阻断创建）
        List<SaasPackageFeature> packageFeatures = packageFeatureRepository.findByPackageId(pkg.getId());
        List<EntitlementSnapshot.Entry> entries = new ArrayList<>();
        for (SaasPackageFeature packageFeature : packageFeatures) {
            Optional<SaasFeature> feature = featureRepository.findByFeatureCode(packageFeature.getFeatureCode());
            if (feature.isEmpty()) {
                logger.warn("系统兜底套餐 {} 权益 {} 在功能字典缺失，已跳过",
                        pkg.getId(), packageFeature.getFeatureCode());
                continue;
            }
            entries.add(new EntitlementSnapshot.Entry(
                    packageFeature.getFeatureCode(), feature.get().getType(), packageFeature.getQuotaLimit()));
        }

        // 串行化同门店兜底创建（与计费激活共用 stores 行锁，避免并发双建）
        storeRepository.findByIdForUpdate(storeId);

        // 锁内复查：已存在该系统套餐的 ACTIVE 订阅 → 幂等返回，不重复创建；
        // 复用前仍做一次配额对齐（幂等，仅在实际不一致时落库），修复存量未对齐账户
        Optional<SaasSubscription> existing = subscriptionRepository
                .findByStoreIdAndStatus(storeId, SaasSubscriptionStatus.ACTIVE).stream()
                .filter(subscription -> pkg.getId().equals(subscription.getPackageId()))
                .findFirst();
        if (existing.isPresent()) {
            quotaAccountAligner.alignQuotaAccounts(
                    storeId, entries, "subscription:" + existing.get().getId(), "system");
            return existing;
        }

        LocalDateTime now = LocalDateTime.now();
        SaasSubscription fallback = new SaasSubscription();
        fallback.setStoreId(storeId);
        fallback.setPackageId(pkg.getId());
        fallback.setPackageName(pkg.getName());
        fallback.setEntitlementSnapshotJson(serializeSnapshot(new EntitlementSnapshot(entries)));
        fallback.setPricePaid(BigDecimal.ZERO);
        fallback.setStartTime(now);
        fallback.setEndTime(FALLBACK_END_TIME);
        fallback.setStatus(SaasSubscriptionStatus.ACTIVE);
        fallback.setRemark(FALLBACK_REMARK);
        logger.info("门店 {} 订阅到期，自动回退系统兜底套餐 {}（不下订单）", storeId, pkg.getId());
        SaasSubscription saved = subscriptionRepository.save(fallback);
        // 回退即对齐配额账户：总额按兜底快照（含 null=不限），保留 used 与周期锚点
        quotaAccountAligner.alignQuotaAccounts(
                storeId, entries, "subscription:" + saved.getId(), "system");
        return Optional.of(saved);
    }

    private String serializeSnapshot(EntitlementSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            throw new IllegalStateException("兜底订阅权益快照序列化失败", e);
        }
    }
}
