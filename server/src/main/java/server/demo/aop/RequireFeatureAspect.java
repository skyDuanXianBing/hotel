package server.demo.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import server.demo.annotation.RequireFeature;
import server.demo.entity.saas.SaasFeature;
import server.demo.enums.SaasFeatureType;
import server.demo.repository.saas.SaasFeatureRepository;
import server.demo.service.saas.EntitlementService;
import server.demo.util.StoreContextUtils;

/**
 * {@link RequireFeature} 权益校验切面。模式参考 RequirePermissionAspect，
 * 但无条件生效（不依赖 permission.enforcement.enabled）。
 *
 * QUOTA 扣减在入口层按"生成动作"扣 1 次：方法内部重试（如 AI 修复重试）不重复扣，
 * 方法最终抛异常时补偿返还。
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // 晚于 RequireFeatureClassAspect：先过模块门禁再扣配额
public class RequireFeatureAspect {

    private static final Logger logger = LoggerFactory.getLogger(RequireFeatureAspect.class);

    private final EntitlementService entitlementService;
    private final SaasFeatureRepository featureRepository;

    public RequireFeatureAspect(EntitlementService entitlementService, SaasFeatureRepository featureRepository) {
        this.entitlementService = entitlementService;
        this.featureRepository = featureRepository;
    }

    @Around("@annotation(requireFeature)")
    public Object around(ProceedingJoinPoint joinPoint, RequireFeature requireFeature) throws Throwable {
        String featureCode = requireFeature.value();
        Long storeId = StoreContextUtils.requireStoreId();

        SaasFeatureType type = featureRepository.findByFeatureCode(featureCode)
                .map(SaasFeature::getType)
                .orElseThrow(() -> new IllegalStateException("功能字典缺失: " + featureCode));

        switch (type) {
            case BOOLEAN -> {
                entitlementService.requireBooleanFeature(storeId, featureCode);
                return joinPoint.proceed();
            }
            case QUOTA -> {
                String bizId = joinPoint.getSignature().toShortString();
                entitlementService.deductQuota(storeId, featureCode, requireFeature.cost(), bizId);
                try {
                    return joinPoint.proceed();
                } catch (Throwable t) {
                    try {
                        entitlementService.refundQuota(storeId, featureCode, requireFeature.cost(), bizId);
                    } catch (Exception refundError) {
                        // 返还失败只记日志，不掩盖原始异常；差额可通过 saas_quota_log 对账人工调整
                        logger.error("SaaS 配额返还失败: storeId={}, feature={}, cost={}",
                                storeId, featureCode, requireFeature.cost(), refundError);
                    }
                    throw t;
                }
            }
            default -> throw new IllegalStateException(
                    "@RequireFeature 不适用于 CAPACITY 权益，请在新增入口显式调用 checkCapacity: " + featureCode);
        }
    }
}
