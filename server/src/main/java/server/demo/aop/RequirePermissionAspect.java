package server.demo.aop;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import server.demo.annotation.RequirePermission;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.exception.PermissionDeniedException;
import server.demo.service.PermissionService;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * 基于 {@link RequirePermission} 的细粒度权限校验切面。
 *
 * 默认关闭（permission.enforcement.enabled=false），避免在权限未配置完成前影响现有流程。
 * 配置完成后可开启进行强制校验。
 */
@Aspect
@Component
public class RequirePermissionAspect {

    private final PermissionService permissionService;

    @Value("${permission.enforcement.enabled:false}")
    private boolean enforcementEnabled;

    public RequirePermissionAspect(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Around("@annotation(requirePermission)")
    public Object around(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) throws Throwable {
        if (!enforcementEnabled) {
            return joinPoint.proceed();
        }

        StoreContext context = StoreContextHolder.getContext();
        Long storeId = context != null ? context.getStoreId() : null;
        Long userId = context != null ? context.getUserId() : null;

        // 兜底：部分接口可能尚未接入 StoreScoped，但仍在 /api/v1/** 下，此时从 request attribute 取 userId/storeId
        if (storeId == null || userId == null) {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attrs != null ? attrs.getRequest() : null;
            if (request != null) {
                Object userIdAttr = request.getAttribute("userId");
                Object storeIdAttr = request.getAttribute("storeId");
                if (userId == null && userIdAttr instanceof Long) {
                    userId = (Long) userIdAttr;
                }
                if (storeId == null && storeIdAttr instanceof Long) {
                    storeId = (Long) storeIdAttr;
                }
            }
        }

        // 兜底：StoreController 等接口 storeId 在 PathVariable 中（/stores/{id}/...），但未进入 StoreContext
        if (storeId == null) {
            storeId = inferStoreIdFromPathVariables(joinPoint);
        }

        if (storeId == null || userId == null) {
            throw new PermissionDeniedException("缺少门店上下文，无法进行权限校验");
        }

        boolean ok = permissionService.hasPermission(storeId, userId, requirePermission.module(), requirePermission.action());
        if (!ok) {
            throw new PermissionDeniedException("您没有权限执行此操作");
        }

        return joinPoint.proceed();
    }

    private static Long inferStoreIdFromPathVariables(ProceedingJoinPoint joinPoint) {
        if (!(joinPoint.getSignature() instanceof MethodSignature methodSignature)) {
            return null;
        }

        Method method = methodSignature.getMethod();
        Object[] args = joinPoint.getArgs();
        if (method == null || args == null) {
            return null;
        }

        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        String[] parameterNames = methodSignature.getParameterNames();

        for (int i = 0; i < parameterAnnotations.length && i < args.length; i++) {
            Object arg = args[i];
            if (!(arg instanceof Long) && !(arg instanceof Integer)) {
                continue;
            }

            for (Annotation annotation : parameterAnnotations[i]) {
                if (!(annotation instanceof org.springframework.web.bind.annotation.PathVariable pathVariable)) {
                    continue;
                }

                String name = pathVariable.name();
                if (name == null || name.isBlank()) {
                    name = pathVariable.value();
                }
                if ((name == null || name.isBlank()) && parameterNames != null && i < parameterNames.length) {
                    name = parameterNames[i];
                }

                if (name == null) {
                    continue;
                }

                if ("storeId".equalsIgnoreCase(name) || "id".equalsIgnoreCase(name)) {
                    if (arg instanceof Integer integerArg) {
                        return integerArg.longValue();
                    }
                    return (Long) arg;
                }
            }
        }

        return null;
    }
}
