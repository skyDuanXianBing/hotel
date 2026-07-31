package server.demo.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记接口需要指定 SaaS 权益：
 * - BOOLEAN：校验当前订阅快照包含该功能，否则抛 NeedUpgradeException（402）；
 * - QUOTA：进入方法前预扣 cost 点，方法抛异常时自动返还；
 * - CAPACITY：不适用本注解（容量校验在新增入口显式调 EntitlementService.checkCapacity）。
 *
 * 支持方法级与类级标注：类级建议用于 BOOLEAN 模块门禁（由 RequireFeatureClassAspect 处理，
 * 先于方法级切面执行）；方法级 BOOLEAN/QUOTA 由 RequireFeatureAspect 处理。
 *
 * 切面无条件生效，不依赖 permission.enforcement.enabled 开关。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireFeature {

    /** 功能字典 feature_code，见 SaasFeatureCodes。 */
    String value();

    /** QUOTA 权益单次扣减点数，默认 1。 */
    long cost() default 1;
}
