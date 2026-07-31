package server.demo.service.saas;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.UnexpectedRollbackException;
import server.demo.entity.saas.SaasSubscription;

/**
 * 门店侧购买的幂等安全包装层（D2 修复）：刻意<b>不</b>开事务，运行于
 * {@link SaasBillingService#subscribe} 事务边界之外。
 *
 * <p>背景：REPEATABLE READ 下 activate() 的锁外预查会建立事务快照，同毫秒并发的后到者
 * 在锁内复查中仍可能看不到先到者已提交的订单，直达 uk(store_id, idempotency_key) 冲突
 * （DataIntegrityViolationException；提交期冲突或嵌套事务场景可能表现为
 * UnexpectedRollbackException）。此时原事务已回滚，本层捕获后在<b>新事务</b>中按幂等键
 * 重查先到者已提交的订单，将冲突恢复为幂等重放（返回先到者的订阅视图），而不是裸 500。
 *
 * <p>若重查仍无命中订单（说明冲突来自其他约束，与幂等键无关），原样抛出原始异常，
 * 交由 BillingExceptionHandler 映射为结构化冲突响应。
 */
@Service
public class SaasBillingIdempotentGateway {

    private final SaasBillingService billingService;
    private final SaasBillingReplayService replayService;

    public SaasBillingIdempotentGateway(
            SaasBillingService billingService,
            SaasBillingReplayService replayService
    ) {
        this.billingService = billingService;
        this.replayService = replayService;
    }

    /**
     * 直连购买（带幂等键冲突恢复）：正常路径直接委托 {@link SaasBillingService#subscribe}；
     * 唯一键/事务回滚异常时在独立新事务中按幂等键重放已成交结果。
     */
    public SaasSubscription subscribe(Long storeId, Long packageId, String operator, String idempotencyKey) {
        try {
            return billingService.subscribe(storeId, packageId, operator, idempotencyKey);
        } catch (DataIntegrityViolationException | UnexpectedRollbackException conflict) {
            return replayService.findReplaySubscriptionInNewTransaction(storeId, idempotencyKey)
                    .orElseThrow(() -> conflict);
        }
    }
}
