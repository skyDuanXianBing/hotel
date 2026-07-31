package server.demo.service.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.admin.AdminDtos.PagedResponse;
import server.demo.dto.admin.AdminDtos.SubscriptionGrantRequest;
import server.demo.dto.admin.AdminDtos.SubscriptionView;
import server.demo.entity.Store;
import server.demo.entity.saas.SaasSubscription;
import server.demo.enums.SaasSubscriptionStatus;
import server.demo.repository.StoreRepository;
import server.demo.repository.saas.SaasSubscriptionRepository;
import server.demo.service.saas.SaasBillingReplayService;
import server.demo.service.saas.SaasBillingService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 平台管理端：租户订阅查询 / 人工开通切换 / 人工取消。
 */
@Service
public class AdminSubscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(AdminSubscriptionService.class);

    private static final int MAX_PAGE_SIZE = 100;
    /** 订单 remark 列宽（V065 VARCHAR(500)），操作人前缀 + 业务备注整体截断到该长度。 */
    private static final int MAX_ORDER_REMARK_LENGTH = 500;
    /** permanent 调控的订阅终点。 */
    static final LocalDateTime PERMANENT_END_TIME = LocalDateTime.of(2099, 12, 31, 23, 59, 59);

    private final SaasSubscriptionRepository subscriptionRepository;
    private final StoreRepository storeRepository;
    private final SaasBillingService billingService;
    private final SaasBillingReplayService billingReplayService;

    public AdminSubscriptionService(
            SaasSubscriptionRepository subscriptionRepository,
            StoreRepository storeRepository,
            SaasBillingService billingService,
            SaasBillingReplayService billingReplayService
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.storeRepository = storeRepository;
        this.billingService = billingService;
        this.billingReplayService = billingReplayService;
    }

    /** 订阅分页列表（可按门店筛选），关联门店名称。 */
    @Transactional(readOnly = true)
    public PagedResponse<SubscriptionView> listSubscriptions(Long storeId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<SaasSubscription> result = storeId != null
                ? subscriptionRepository.findByStoreIdOrderByIdDesc(storeId, pageable)
                : subscriptionRepository.findAllByOrderByIdDesc(pageable);

        List<Long> storeIds = result.getContent().stream()
                .map(SaasSubscription::getStoreId)
                .distinct()
                .toList();
        Map<Long, String> storeNames = storeRepository.findAllById(storeIds).stream()
                .collect(Collectors.toMap(Store::getId, Store::getName));

        List<SubscriptionView> content = result.getContent().stream()
                .map(subscription -> toView(subscription, storeNames))
                .toList();
        return new PagedResponse<>(content, result.getTotalElements(), result.getTotalPages(), safePage, safeSize);
    }

    /**
     * 人工为门店开通/切换套餐：复用 SaasBillingService 事务激活流程，
     * 生成 provider=DIRECT、amount=0 的人工订单；operator 记录到配额流水。
     * idempotencyKey 命中已有订单时幂等重放，重复点击/重试不产生重复人工订单。
     *
     * <p>等级调控（P9）：
     * <ul>
     *   <li>durationDays 为 null 且 permanent 非 true → 按套餐周期（同套餐重购走续费顺延）；</li>
     *   <li>durationDays 1..36500 → endTime = now + 该天数；</li>
     *   <li>permanent=true → endTime = 2099-12-31 23:59:59（与 durationDays 互斥）；</li>
     *   <li>remark 必填（非空、≤500），与操作人（「操作人:{operator}」前缀）一起写入订单 remark。
     * </ul>
     */
    @Transactional
    public SubscriptionView grantSubscription(SubscriptionGrantRequest request, String operator) {
        Store store = storeRepository.findById(request.storeId())
                .orElseThrow(() -> new IllegalArgumentException("门店不存在: " + request.storeId()));

        String remark = request.remark() == null ? "" : request.remark().trim();
        if (remark.isEmpty()) {
            throw new IllegalArgumentException("remark 不能为空");
        }
        if (remark.length() > 500) {
            throw new IllegalArgumentException("remark 长度不能超过 500");
        }
        boolean permanent = Boolean.TRUE.equals(request.permanent());
        if (permanent && request.durationDays() != null) {
            throw new IllegalArgumentException("permanent 与 durationDays 不可同时指定");
        }
        LocalDateTime endTimeOverride = null;
        if (permanent) {
            endTimeOverride = PERMANENT_END_TIME;
        } else if (request.durationDays() != null) {
            if (request.durationDays() < 1 || request.durationDays() > 36500) {
                throw new IllegalArgumentException("durationDays 需在 1-36500 之间");
            }
            endTimeOverride = LocalDateTime.now().plusDays(request.durationDays());
        }

        SaasSubscription subscription = billingService.grantByAdmin(
                request.storeId(), request.packageId(), operator, request.idempotencyKey(),
                endTimeOverride, composeOrderRemark(operator, remark));
        logger.info("人工开通/切换订阅: storeId={}, packageId={}, durationDays={}, permanent={}, operator={}",
                request.storeId(), request.packageId(), request.durationDays(), permanent, operator);
        // P10：grant 响应补 storeName（复用入口已加载的门店实体，与列表口径一致）
        return toView(subscription, storeNameMap(store));
    }

    /** 订单备注：「操作人:{operator}；{remark}」，整体截断到列宽 500。 */
    static String composeOrderRemark(String operator, String remark) {
        String composed = "操作人:" + (operator == null || operator.isBlank() ? "unknown" : operator)
                + "；" + remark;
        return composed.length() <= MAX_ORDER_REMARK_LENGTH
                ? composed
                : composed.substring(0, MAX_ORDER_REMARK_LENGTH);
    }

    /**
     * 人工开通的幂等恢复重查（D2）：grantSubscription 因并发同幂等键 uk 冲突回滚后，
     * 由 AdminSubscriptionController 在事务边界之外调用——独立新事务（REQUIRES_NEW）的
     * 全新一致性快照可读到先到者已提交的订单/订阅，将冲突恢复为幂等重放视图。
     * 未命中订单（冲突与幂等键无关）时返回 empty，由调用方原样抛出原始异常。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<SubscriptionView> findReplaySubscriptionView(Long storeId, String idempotencyKey) {
        return billingReplayService.findReplaySubscription(storeId, idempotencyKey)
                .map(subscription -> toView(subscription, storeNameMap(subscription.getStoreId())));
    }

    /** 人工取消订阅（软取消，不清理任何存量业务数据）；操作人记入日志备审计。 */
    @Transactional
    public SubscriptionView cancelSubscription(Long subscriptionId, String operator) {
        SaasSubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("订阅不存在: " + subscriptionId));
        if (subscription.getStatus() != SaasSubscriptionStatus.ACTIVE) {
            throw new IllegalArgumentException("仅进行中的订阅可取消，当前状态: " + subscription.getStatus());
        }
        subscription.setStatus(SaasSubscriptionStatus.CANCELLED);
        subscriptionRepository.save(subscription);
        logger.info("人工取消订阅: subscriptionId={}, storeId={}, packageId={}, operator={}",
                subscription.getId(), subscription.getStoreId(), subscription.getPackageId(), operator);
        return toView(subscription, storeNameMap(subscription.getStoreId()));
    }

    /** 单门店名称查询（与列表口径一致：stores.name，门店缺失时为 null 不阻断）。 */
    private Map<Long, String> storeNameMap(Long storeId) {
        return storeRepository.findById(storeId).map(this::storeNameMap).orElse(Map.of());
    }

    /** 门店实体 → 名称映射；name 为 null 时给空映射（视图 storeName=null，与列表口径一致）。 */
    private Map<Long, String> storeNameMap(Store store) {
        return store.getName() != null ? Map.of(store.getId(), store.getName()) : Map.of();
    }

    private SubscriptionView toView(SaasSubscription subscription, Map<Long, String> storeNames) {
        return new SubscriptionView(
                subscription.getId(),
                subscription.getStoreId(),
                storeNames.getOrDefault(subscription.getStoreId(), null),
                subscription.getPackageId(),
                subscription.getPackageName(),
                subscription.getPricePaid(),
                subscription.getStartTime(),
                subscription.getEndTime(),
                subscription.getStatus(),
                subscription.getCreatedAt()
        );
    }
}
