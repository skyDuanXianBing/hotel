package server.demo.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import server.demo.annotation.RequireFeature;
import server.demo.entity.saas.SaasFeature;
import server.demo.enums.SaasFeatureType;
import server.demo.repository.saas.SaasFeatureRepository;
import server.demo.service.saas.EntitlementService;
import server.demo.util.StoreContextUtils;

import server.demo.i18n.ApiMessages;
/**
 * 类级 {@link RequireFeature} 模块门禁切面（BOOLEAN）。
 *
 * 与 {@link RequireFeatureAspect}（方法级）分离为独立切面类：同一切面内多个 @Around
 * 的执行顺序未定义，拆分类后通过 @Order 保证“类级模块门禁先于方法级配额扣减”，
 * 避免无模块权益时先扣配额再走返还补偿的浪费。
 *
 * 类级仅允许 BOOLEAN；QUOTA/CAPACITY 标在类上属于配置错误，直接 fail-fast。
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequireFeatureClassAspect {

    private final EntitlementService entitlementService;
    private final SaasFeatureRepository featureRepository;

    public RequireFeatureClassAspect(EntitlementService entitlementService, SaasFeatureRepository featureRepository) {
        this.entitlementService = entitlementService;
        this.featureRepository = featureRepository;
    }

    /**
     * 注意：不用 @within(requireFeature) 参数绑定——CGLIB 代理下 JoinPointMatch 可能不绑定，
     * 运行期抛 IllegalStateException（验收实测）。改为从方法声明类反射读取注解。
     */
    @Around("@within(server.demo.annotation.RequireFeature)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        RequireFeature requireFeature = (RequireFeature) joinPoint.getSignature().getDeclaringType()
                .getAnnotation(RequireFeature.class);
        if (requireFeature == null) {
            return joinPoint.proceed();
        }
        String featureCode = requireFeature.value();
        Long storeId = StoreContextUtils.requireStoreId();

        SaasFeatureType type = featureRepository.findByFeatureCode(featureCode)
                .map(SaasFeature::getType)
                .orElseThrow(() -> new IllegalStateException(ApiMessages.get("api.t.4a8671ebbd4b") + featureCode));

        if (type != SaasFeatureType.BOOLEAN) {
            throw new IllegalStateException(ApiMessages.get("api.t.8aecb8037226") + featureCode);
        }
        entitlementService.requireBooleanFeature(storeId, featureCode);
        return joinPoint.proceed();
    }
}
