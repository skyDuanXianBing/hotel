package server.demo.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标识接口或方法需要门店级上下文。
 * JwtInterceptor 已注入 userId，本注解用于后续 StoreContextInterceptor 做 storeId 校验。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface StoreScoped {
    /**
     * 是否必须提供 storeId。若为 false，则允许缺省但仍可写入上下文。
     */
    boolean required() default true;

    /**
     * 是否允许门店上下文拦截器执行渠道价格预热。财务只读接口可显式关闭，
     * 默认保持开启以兼容既有控制器行为。
     */
    boolean warmupChannelPrices() default true;
}
