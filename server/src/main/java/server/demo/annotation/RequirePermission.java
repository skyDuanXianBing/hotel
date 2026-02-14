package server.demo.annotation;

import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记接口需要具备指定权限才可访问。
 *
 * 注意：当前项目鉴权由 JwtInterceptor + StoreContextInterceptor 完成身份/门店校验。
 * 本注解用于在业务层补充细粒度权限控制。
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    PermissionModule module();
    PermissionAction action();
}

