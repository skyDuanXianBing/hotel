package server.demo.service.saas;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.saas.SaasSubscription;
import server.demo.enums.SaasSubscriptionStatus;
import server.demo.repository.saas.SaasBillingOrderRepository;
import server.demo.repository.saas.SaasSubscriptionRepository;

import java.util.Optional;

/**
 * 幂等重放解析：按 (store_id, idempotency_key) 命中已成交订单时，解析应重放给调用方的订阅。
 *
 * <p>两个传播级别对应两类调用场景（D2 修复）：
 * <ul>
 *   <li>{@link #findReplaySubscription}（REQUIRED）：加入调用方事务，供
 *       {@link SaasBillingService} 的锁外快路径/锁内复查复用（沿用调用方事务快照，语义不变）；</li>
 *   <li>{@link #findReplaySubscriptionInNewTransaction}（REQUIRES_NEW）：在调用方事务已回滚后，
 *       以全新事务快照重查——可读到并发先到者<b>已提交</b>的订单，不受原 REPEATABLE READ
 *       快照的可见性限制，供 uk 冲突后的幂等恢复（SaasBillingIdempotentGateway / AdminSubscriptionService）使用。
 * </ul>
 */
@Service
public class SaasBillingReplayService {

    private final SaasBillingOrderRepository billingOrderRepository;
    private final SaasSubscriptionRepository subscriptionRepository;

    public SaasBillingReplayService(
            SaasBillingOrderRepository billingOrderRepository,
            SaasSubscriptionRepository subscriptionRepository
    ) {
        this.billingOrderRepository = billingOrderRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    /**
     * 幂等重放解析（加入调用方事务）：幂等键命中已成交订单时，返回该门店当前生效订阅
     * （与 my-subscription 口径一致）；若订单对应订阅此后已被切换/取消，返回门店最新一条订阅
     * （视图可如实展示其状态）。键为空（null/空白）时不命中。
     */
    @Transactional(readOnly = true)
    public Optional<SaasSubscription> findReplaySubscription(Long storeId, String idempotencyKey) {
        String key = normalizeKey(idempotencyKey);
        if (key == null) {
            return Optional.empty();
        }
        return billingOrderRepository.findByStoreIdAndIdempotencyKey(storeId, key)
                .map(order -> subscriptionRepository
                        .findFirstByStoreIdAndStatusOrderByEndTimeDesc(storeId, SaasSubscriptionStatus.ACTIVE)
                        .orElseGet(() -> subscriptionRepository.findFirstByStoreIdOrderByIdDesc(storeId)
                                .orElseThrow(() -> new IllegalStateException(
                                        "幂等订单存在但订阅缺失: orderId=" + order.getId()))));
    }

    /**
     * 幂等重放解析（独立新事务）：用于原事务已因 uk 冲突回滚后的恢复重查。
     * 新事务的全新一致性快照可读到并发先到者已提交的订单/订阅，将唯一键冲突恢复为幂等重放。
     *
     * <p>内部直接调用 {@link #findReplaySubscription}：此时当前线程已在 REQUIRES_NEW 新事务内，
     * 被调用方法的 @Transactional 注解虽不生效（自调用），其方法体恰好运行在该新事务中，语义正确。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<SaasSubscription> findReplaySubscriptionInNewTransaction(Long storeId, String idempotencyKey) {
        return findReplaySubscription(storeId, idempotencyKey);
    }

    /**
     * 重放侧键归一化：仅做 trim/空白判空（长度校验已由写入路径在落库前完成；
     * 空键订单不建 uk 冲突，无需重放）。
     */
    private String normalizeKey(String idempotencyKey) {
        if (idempotencyKey == null) {
            return null;
        }
        String key = idempotencyKey.trim();
        return key.isEmpty() ? null : key;
    }
}
