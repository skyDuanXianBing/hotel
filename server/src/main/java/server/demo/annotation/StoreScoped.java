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
}
